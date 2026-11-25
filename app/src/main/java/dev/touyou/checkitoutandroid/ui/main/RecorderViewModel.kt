package dev.touyou.checkitoutandroid.ui.main

import android.app.Application
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.text.format.DateFormat
import androidx.lifecycle.AndroidViewModel
import java.io.File
import java.io.IOException
import java.util.Date

/**
 * 録音・試聴の状態を画面の外に持つ。表示密度の変化などで Activity が作り直されても、
 * 録音中の音と録り終えたファイルが残るようにするため。
 */
class RecorderViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        const val SAMPLING_RATE = 44100
        const val CHANNEL_MASK = AudioFormat.CHANNEL_IN_MONO
        const val ENCODING = AudioFormat.ENCODING_PCM_8BIT
    }

    private var recorder: MediaRecorder? = null

    /** 録り終えて、まだ保存も破棄もしていない音 */
    var file: File? = null
        private set

    var isRecording: Boolean = false
        private set

    var player: MediaPlayer? = null
        private set

    val audioBufferSize: Int by lazy {
        AudioRecord.getMinBufferSize(SAMPLING_RATE, CHANNEL_MASK, ENCODING)
    }

    // 波形の表示に使う。録音そのものは MediaRecorder がファイルに書く
    private val audioRecordLazy = lazy {
        AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLING_RATE, CHANNEL_MASK, ENCODING, audioBufferSize)
    }
    val audioRecord: AudioRecord by audioRecordLazy

    /** 録音を始める。始められたら true */
    fun startRecording(): Boolean {
        if (file != null) return false
        val fileName = DateFormat.format("yyyy_MM_dd_kk-mm-ss", Date())
        val newFile = File(getApplication<Application>().filesDir, "$fileName.mp3")
        val newRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(getApplication())
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        file = newFile
        recorder = newRecorder
        return try {
            newRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            newRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            newRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            newRecorder.setOutputFile(newFile)
            newRecorder.prepare()
            newRecorder.start()
            audioRecord.startRecording()
            isRecording = true
            true
        } catch (e: IllegalStateException) {
            e.printStackTrace()
            false
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    fun stopRecording() {
        if (audioRecordLazy.isInitialized() && audioRecord.state == AudioRecord.STATE_INITIALIZED) {
            audioRecord.stop()
        }
        if (isRecording) {
            try {
                recorder?.stop()
            } catch (e: RuntimeException) {
                // 開始直後に止めると音が無く stop が失敗する。ファイルは使えないので捨てる
                e.printStackTrace()
                file?.delete()
                file = null
            }
            isRecording = false
        }
        recorder?.release()
        recorder = null
    }

    /** 録り終えた音を頭から鳴らす。鳴らし始めたら、そのプレーヤーを返す */
    fun play(): MediaPlayer? {
        if (isRecording || player?.isPlaying == true) return null
        val target = file ?: return null
        player?.release()
        return MediaPlayer().apply {
            setDataSource(target.absolutePath)
            prepare()
            start()
        }.also { player = it }
    }

    /** 保存に回すファイルを受け取る。以後このファイルは消さない */
    fun takeFile(): File? {
        if (isRecording) return null
        return file.also { file = null }
    }

    /** 録音中なら止め、録った音とプレーヤーを捨てる */
    fun discard() {
        stopRecording()
        releasePlayer()
        file?.delete()
        file = null
    }

    private fun releasePlayer() {
        player?.let {
            if (it.isPlaying) it.stop()
            it.release()
        }
        player = null
    }

    override fun onCleared() {
        discard()
        if (audioRecordLazy.isInitialized()) audioRecord.release()
        super.onCleared()
    }
}
