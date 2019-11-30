package dev.touyou.checkitoutandroid.ui.main

import android.annotation.SuppressLint
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import dev.touyou.checkitoutandroid.R
import dev.touyou.checkitoutandroid.entity.AssignedSound
import dev.touyou.checkitoutandroid.entity.PlayMode
import kotlinx.android.synthetic.main.main_fragment.*

class PadFragment : Fragment() {

    companion object {
        fun newInstance() = PadFragment()
    }

    private lateinit var viewModel: PadViewModel
    private val pads by lazy {
        listOf(
            pad1, pad2, pad3, pad4,
            pad5, pad6, pad7, pad8,
            pad9, pad10, pad11, pad12,
            pad13, pad14, pad15, pad16
        )
    }
    private var mediaPlayers: MutableList<MediaPlayer?> = mutableListOf(
        null, null, null, null,
        null, null, null, null,
        null, null, null, null,
        null, null, null, null
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.main_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = activity?.let { ViewModelProviders.of(it).get(PadViewModel::class.java) }
            ?: throw Exception("Invalid Activity")

        setSounds(viewModel.sounds.toList())
        viewModel.assignedSound.observe(viewLifecycleOwner, Observer {
            setSounds(it)
        })

        changePadTouchListener(PlayMode.PLAY)
        viewModel.currentMode.observe(viewLifecycleOwner, Observer {
            pads.mapIndexed { index, pad -> pad.setImageResource(padImage(index)) }
            changePadTouchListener(it)
        })

        viewModel.initSounds()
    }

    override fun onDestroy() {
        super.onDestroy()

        for (mediaPlayer in mediaPlayers) {
            mediaPlayer?.release()
        }
    }

    private fun setSounds(list: List<AssignedSound?>) {
        pads.mapIndexed { idx, imageView ->
            imageView.setImageResource(padImage(idx))
        }
        for ((index, sound) in list.withIndex()) {
            if (sound != null) {
                if (sound.isRaw) mediaPlayers[index] =
                    sound.rawId?.let { MediaPlayer.create(activity, it) }
                else mediaPlayers[index] =
                    sound.urlStr?.let { MediaPlayer.create(activity, Uri.parse(it)) }
            } else {
                mediaPlayers[index] = null
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun changePadTouchListener(mode: PlayMode) {
        println(viewModel.currentMode.value)
        for ((index, pad) in pads.withIndex()) {
            pad.setOnTouchListener(null)
            pad.setOnTouchListener { _, motionEvent ->
                when (motionEvent.action) {
                    MotionEvent.ACTION_DOWN -> {
                        pad.setImageResource(selectedPadImage(index))
                        if (mode == PlayMode.PLAY) {
                            if (mediaPlayers[index]?.isPlaying == true) {
                                mediaPlayers[index]?.stop()
                                mediaPlayers[index]?.prepare()
                            }
                            mediaPlayers[index]?.start()
                        } else if (mode == PlayMode.EDIT) {
                            viewModel.selectedPad = index
                            pads.mapIndexed { idx, imageView ->
                                if (idx != index) imageView.setImageResource(padImage(idx))
                            }
                        }
                    }
                    MotionEvent.ACTION_UP -> if (mode != PlayMode.EDIT) pad.setImageResource(
                        padImage(index)
                    )
                    else -> {
                    }
                }
                true
            }
        }
    }

    private fun selectedPadImage(index: Int): Int {
        return when (index) {
            in 0..3 -> R.drawable.selected_red_pad
            in 4..7 -> R.drawable.selected_yellow_pad
            in 8..11 -> R.drawable.selected_green_pad
            in 12..15 -> R.drawable.selected_blue_pad
            else -> R.drawable.selected_red_pad
        }
    }

    private fun padImage(index: Int): Int {
        return when (index) {
            in 0..3 -> R.drawable.red_pad
            in 4..7 -> R.drawable.yellow_pad
            in 8..11 -> R.drawable.green_pad
            in 12..15 -> R.drawable.blue_pad
            else -> R.drawable.red_pad
        }
    }
}
