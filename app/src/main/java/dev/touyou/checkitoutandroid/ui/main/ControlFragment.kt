package dev.touyou.checkitoutandroid.ui.main

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import dev.touyou.checkitoutandroid.R
import dev.touyou.checkitoutandroid.entity.PlayMode
import dev.touyou.checkitoutandroid.entity.SoundData
import io.realm.Realm
import kotlinx.android.synthetic.main.control_fragment.*
import me.bogerchan.niervisualizer.NierVisualizerManager
import me.bogerchan.niervisualizer.renderer.columnar.ColumnarType1Renderer
import java.io.File
import java.io.IOException
import java.util.*

class ControlFragment : Fragment() {

    companion object {
        const val SAMPLING_RATE = 44100
        const val CHANNEL_MASK = AudioFormat.CHANNEL_IN_MONO
        const val ENCODING = AudioFormat.ENCODING_PCM_8BIT
        fun newInstance() = ControlFragment()
    }

    private lateinit var viewModel: PadViewModel
    private lateinit var adapter: SoundViewAdapter
    private lateinit var mode: PlayMode
    private var player: MediaPlayer? = null
    private var recorder: MediaRecorder? = null
    private var file: File? = null
    private var visualizerManager: NierVisualizerManager? = null

    private val audioBufferSize by lazy {
        AudioRecord.getMinBufferSize(
            SAMPLING_RATE,
            CHANNEL_MASK,
            ENCODING
        )
    }
    private val audioRecord by lazy {
        AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLING_RATE,
            CHANNEL_MASK,
            ENCODING,
            audioBufferSize
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.control_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = activity?.let { ViewModelProviders.of(it).get(PadViewModel::class.java) }
            ?: throw Exception("Invalid Activity")

        changeMode(PlayMode.PLAY)
        viewModel.currentMode.observe(viewLifecycleOwner, Observer {
            changeMode(it)
        })

        val realm = Realm.getDefaultInstance()
        var soundList = realm.where(SoundData::class.java).findAll().toMutableList()
        viewModel.getSoundData().observe(viewLifecycleOwner, Observer {
            soundList = it.toMutableList()
            println("update realm data: $soundList")
            viewModel.changeSoundAll(soundList)
            adapter.notifyDataSetChanged()
        })
        adapter = SoundViewAdapter(soundList)
        adapter.setOnItemClickListener(object : SoundViewAdapter.onItemClickListener {
            override fun onClick(view: View, position: Int) {
                viewModel.selectedPad?.let {
                    if (mode == PlayMode.EDIT) {
                        viewModel.changeSound(it, soundList[position])
                        viewModel.selectedPad = null
                        adapter.notifyItemChanged(position)
                    }
                }
            }
        })

        soundRecyclerView.adapter = adapter
        soundRecyclerView.layoutManager = LinearLayoutManager(context)

        setupRecMode()
    }

    override fun onDestroy() {
        super.onDestroy()

        visualizerManager?.release()
        visualizerManager = null
        player?.release()
        audioRecord.release()
        recorder?.release()
        file?.delete()
    }

    private fun changeMode(mode: PlayMode) {
        this.mode = mode
        when (mode) {
            PlayMode.REC -> recBaseView.visibility = View.VISIBLE
            else -> {
                recBaseView.visibility = View.INVISIBLE
                player?.let {
                    it.stop()
                    it.release()
                    player = null
                }
                file?.let {
                    stopRecording()
                    it.delete()
                    file = null
                }
            }
        }
    }

    private fun setupRecMode() {
        recorder = MediaRecorder()
        recorder?.setAudioSource(MediaRecorder.AudioSource.MIC)
        recorder?.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        recorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)

        var state = false
        recRecButton.setOnClickListener {
            if (file == null) {
                val date = Date()
                val fileName = DateFormat.format("yyyy_MM_dd_kk-mm-ss", date)
                file = File(context?.filesDir, "$fileName.mp3")

                recorder?.setOutputFile(file)
                createNewVisualizerManager(1)
                visualizerManager?.start(visualizer, arrayOf(ColumnarType1Renderer()))
                try {
                    recorder?.prepare()
                    recorder?.start()
                    audioRecord.startRecording()
                    state = true
                } catch (e: IllegalStateException) {
                    e.printStackTrace()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        stopRecButton.setOnClickListener {
            stopRecording()
            state = false
        }
        playRecButton.setOnClickListener {
            if (state) return@setOnClickListener
            if (player?.isPlaying == true) return@setOnClickListener
            player = MediaPlayer()
            player?.setDataSource(file?.absolutePath)
            player?.prepare()
            player?.start()
            createNewVisualizerManager(0)
            visualizerManager?.start(visualizer, arrayOf(ColumnarType1Renderer()))
        }
        saveRecButton.setOnClickListener {
            if (state) return@setOnClickListener
            file?.let {
                if (displayNameText.text.isBlank()) {
                    Toast.makeText(context, "名前を入力してください。", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.addSound(displayNameText.text.toString(), it.absolutePath)
                    file = null
                    displayNameText.setText("")
                    Toast.makeText(context, "保存しました。", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun stopRecording() {
        visualizerManager?.pause()
        audioRecord.stop()
        recorder?.stop()
        recorder?.release()
        visualizerManager?.stop()
        visualizerManager?.release()
    }

    private fun createNewVisualizerManager(mode: Int) {
        visualizerManager?.release()
        visualizerManager = NierVisualizerManager().apply {
            when (mode) {
                0 -> init(player!!.audioSessionId)
                1 -> {
                    init(object : NierVisualizerManager.NVDataSource {
                        private val buffer: ByteArray = ByteArray(512)
                        private val audioRecordByteBuffer by lazy {
                            ByteArray(audioBufferSize / 2)
                        }
                        private val audioLength =
                            (audioRecordByteBuffer.size * 1000f / SAMPLING_RATE).toInt()

                        override fun getDataSamplingInterval() = 0L

                        override fun getDataLength() = buffer.size

                        override fun fetchFftData(): ByteArray? {
                            return null
                        }

                        override fun fetchWaveData(): ByteArray? {
                            if (audioRecord.recordingState != AudioRecord.RECORDSTATE_RECORDING) return null
                            audioRecordByteBuffer.fill(0)
                            audioRecord.read(audioRecordByteBuffer, 0, audioRecordByteBuffer.size)
                            var tempCounter = 0
                            for (idx in audioRecordByteBuffer.indices step (audioRecordByteBuffer.size / (audioLength + buffer.size))) {
                                if (tempCounter >= buffer.size) break
                                buffer[tempCounter++] = audioRecordByteBuffer[idx]
                            }
                            return buffer
                        }
                    })
                }
                else -> return
            }
        }
    }
}
