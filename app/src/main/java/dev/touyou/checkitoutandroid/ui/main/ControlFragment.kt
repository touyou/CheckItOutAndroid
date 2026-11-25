package dev.touyou.checkitoutandroid.ui.main

import android.media.AudioRecord
import android.os.Bundle
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

class ControlFragment : Fragment() {

    companion object {
        fun newInstance() = ControlFragment()
    }

    private lateinit var viewModel: PadViewModel
    private lateinit var recorder: RecorderViewModel
    private lateinit var adapter: SoundViewAdapter
    private var mode: PlayMode = PlayMode.PLAY
    private var visualizerManager: NierVisualizerManager? = null
    private var _binding: ControlFragmentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ControlFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val provider = ViewModelProvider(requireActivity())
        viewModel = provider[PadViewModel::class.java]
        recorder = provider[RecorderViewModel::class.java]

        // 作り直したときは今のモードのまま表示する(PLAY に戻すと録音中の音を捨ててしまう)
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
        resumeVisualizer()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null

        // 録音と試聴は RecorderViewModel が持ち続ける。ここでは画面に結び付いた波形だけ捨てる
        visualizerManager?.release()
        visualizerManager = null
    }

    private fun changeMode(mode: PlayMode) {
        this.mode = mode
        when (mode) {
            PlayMode.REC -> binding.recBaseView.visibility = View.VISIBLE
            else -> {
                binding.recBaseView.visibility = View.INVISIBLE
                if (recorder.file != null || recorder.player != null) {
                    stopVisualizer()
                    recorder.discard()
                    binding.displayNameText.setText("")
                }
            }
        }
    }

    private fun setupRecMode() {
        binding.recRecButton.setOnClickListener {
            if (recorder.file != null) return@setOnClickListener
            if (!recorder.startRecording()) {
                Toast.makeText(context, "録音を始められませんでした。", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            createNewVisualizerManager(1)
            visualizerManager?.start(binding.visualizer, arrayOf(ColumnarType1Renderer()))
        }
        binding.stopRecButton.setOnClickListener {
            stopVisualizer()
            recorder.stopRecording()
        }
        binding.playRecButton.setOnClickListener {
            recorder.play() ?: return@setOnClickListener
            createNewVisualizerManager(0)
            visualizerManager?.start(binding.visualizer, arrayOf(ColumnarType1Renderer()))
        }
        binding.saveRecButton.setOnClickListener {
            if (recorder.isRecording || recorder.file == null) return@setOnClickListener
            if (binding.displayNameText.text.isBlank()) {
                Toast.makeText(context, "名前を入力してください。", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val file = recorder.takeFile() ?: return@setOnClickListener
            viewModel.addSound(binding.displayNameText.text.toString(), file.absolutePath)
            binding.displayNameText.setText("")
            Toast.makeText(context, "保存しました。", Toast.LENGTH_SHORT).show()
        }
    }

    // 作り直した画面に、続いている録音・試聴の波形をつなぎ直す
    private fun resumeVisualizer() {
        when {
            recorder.isRecording -> createNewVisualizerManager(1)
            recorder.player?.isPlaying == true -> createNewVisualizerManager(0)
            else -> return
        }
        visualizerManager?.start(binding.visualizer, arrayOf(ColumnarType1Renderer()))
    }

    private fun stopVisualizer() {
        visualizerManager?.pause()
        visualizerManager?.stop()
        visualizerManager?.release()
        visualizerManager = null
    }

    private fun createNewVisualizerManager(mode: Int) {
        visualizerManager?.release()
        visualizerManager = NierVisualizerManager().apply {
            when (mode) {
                0 -> init(recorder.player?.audioSessionId ?: return@apply)
                1 -> {
                    val audioRecord = recorder.audioRecord
                    val audioBufferSize = recorder.audioBufferSize
                    init(object : NierVisualizerManager.NVDataSource {
                        private val buffer: ByteArray = ByteArray(512)
                        private val audioRecordByteBuffer by lazy {
                            ByteArray(audioBufferSize / 2)
                        }
                        private val audioLength =
                            (audioRecordByteBuffer.size * 1000f / RecorderViewModel.SAMPLING_RATE).toInt()

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
