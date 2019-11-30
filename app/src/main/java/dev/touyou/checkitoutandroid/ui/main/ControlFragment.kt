package dev.touyou.checkitoutandroid.ui.main

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
        fun newInstance() = ControlFragment()
    }

    private lateinit var viewModel: PadViewModel
    private lateinit var adapter: SoundViewAdapter
    private lateinit var mode: PlayMode
    private var player: MediaPlayer? = null
    private var recorder: MediaRecorder? = null
    private var file: File? = null
    private var visualizerManager: NierVisualizerManager? = null

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

        recorder?.release()
        visualizerManager?.release()
        player?.release()
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

//        visualizerManager = NierVisualizerManager()
//        if (visualizerManager?.init(0) != NierVisualizerManager.SUCCESS) {
//            visualizerManager = null
//        }

        var state = false
        recRecButton.setOnClickListener {
            if (file == null) {
                val date = Date()
                val fileName = DateFormat.format("yyyy_MM_dd_kk-mm-ss", date)
                file = File(context?.filesDir, "$fileName.mp3")

                recorder?.setOutputFile(file)
                try {
                    recorder?.prepare()
                    recorder?.start()
                    state = true

                    visualizerManager?.start(visualizer, arrayOf(ColumnarType1Renderer()))
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
        recorder?.stop()
        recorder?.release()
        visualizerManager?.stop()
        visualizerManager?.release()
    }
}
