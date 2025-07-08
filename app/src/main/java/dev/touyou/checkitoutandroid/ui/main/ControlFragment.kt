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
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import dev.touyou.checkitoutandroid.R
import dev.touyou.checkitoutandroid.databinding.ControlFragmentBinding
import dev.touyou.checkitoutandroid.entity.PlayMode
import dev.touyou.checkitoutandroid.entity.SoundData
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import io.realm.kotlin.ext.query
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
    private var state: Boolean = false
    private var _binding: ControlFragmentBinding? = null
    private val binding get() = _binding!!

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
    ): View {
        _binding = ControlFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = activity?.let { ViewModelProvider(it)[PadViewModel::class.java] }
            ?: throw Exception("Invalid Activity")

        changeMode(PlayMode.PLAY)
        viewModel.currentMode.observe(viewLifecycleOwner, Observer {
            changeMode(it)
        })

        val config = RealmConfiguration.Builder(schema = setOf(SoundData::class))
            .name("sounddb.realm")
            .schemaVersion(1)
            .build()
        val realm = Realm.open(config)
        val soundList = realm.query<SoundData>().sort("id").find()

        adapter = SoundViewAdapter(soundList.toList(), true)
        viewModel.getSoundData().observe(viewLifecycleOwner, Observer { soundDataList ->
            adapter.updateData(soundDataList)
            viewModel.changeSoundAll(soundDataList.toMutableList())
        })

        adapter.setOnItemClickListener(object : SoundViewAdapter.onItemClickListener {
            override fun onClick(view: View, position: Int) {
                viewModel.selectedPad?.let {
                    if (mode == PlayMode.EDIT) {
                        val soundData = adapter.getItem(position)
                        if (soundData != null) {
                            viewModel.changeSound(it, soundData)
                            viewModel.selectedPad = null
                            adapter.notifyItemChanged(position)
                        }
                    }
                }
            }
        })

        binding.soundRecyclerView.adapter = adapter
        binding.soundRecyclerView.layoutManager = LinearLayoutManager(context)

        setupRecMode()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null

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
            PlayMode.REC -> binding.recBaseView.visibility = View.VISIBLE
            else -> {
                binding.recBaseView.visibility = View.INVISIBLE
                player?.let {
                    it.stop()
                    it.release()
                    player = null
                }
                file?.let {
                    stopRecording()
                    it.delete()
                    file = null
                    binding.displayNameText.setText("")
                }
            }
        }
    }

    private fun setupRecMode() {
        state = false
        binding.recRecButton.setOnClickListener {
            if (file == null) {
                val date = Date()
                val fileName = DateFormat.format("yyyy_MM_dd_kk-mm-ss", date)
                file = File(context?.filesDir, "$fileName.mp3")

                recorder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    MediaRecorder(requireContext())
                } else {
                    @Suppress("DEPRECATION")
                    MediaRecorder()
                }
                recorder?.setAudioSource(MediaRecorder.AudioSource.MIC)
                recorder?.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                recorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                recorder?.setOutputFile(file)
                createNewVisualizerManager(1)
                visualizerManager?.start(binding.visualizer, arrayOf(ColumnarType1Renderer()))
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
        binding.stopRecButton.setOnClickListener {
            stopRecording()
            state = false
        }
        binding.playRecButton.setOnClickListener {
            if (state) return@setOnClickListener
            if (player?.isPlaying == true) return@setOnClickListener
            file?.let {
                player = MediaPlayer()
                player?.setDataSource(it.absolutePath)
                player?.prepare()
                player?.start()
                createNewVisualizerManager(0)
                visualizerManager?.start(binding.visualizer, arrayOf(ColumnarType1Renderer()))
            }
        }
        binding.saveRecButton.setOnClickListener {
            if (state) return@setOnClickListener
            file?.let {
                if (binding.displayNameText.text.isBlank()) {
                    Toast.makeText(context, "名前を入力してください。", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.addSound(binding.displayNameText.text.toString(), it.absolutePath)
                    file = null
                    binding.displayNameText.setText("")
                    Toast.makeText(context, "保存しました。", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun stopRecording() {
        visualizerManager?.pause()
        audioRecord.stop()
        if (state) {
            recorder?.stop()
            recorder?.release()
        }
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
                            if (audioRecordByteBuffer.isEmpty()) return null
                            if (audioLength <= 0) return null
                            if (audioRecordByteBuffer.size < audioLength + buffer.size) {
                                return null
                            }
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
