package dev.touyou.checkitoutandroid.ui.main

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import dev.touyou.checkitoutandroid.R
import kotlinx.android.synthetic.main.main_fragment.*

class PadFragment : Fragment() {

    companion object {
        fun newInstance() = PadFragment()
    }

    private lateinit var viewModel: PadViewModel
    private val pads by lazy {
        listOf(pad1, pad2, pad3, pad4,
            pad5, pad6, pad7, pad8,
            pad9, pad10, pad11, pad12,
            pad13, pad14, pad15, pad16)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.main_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(PadViewModel::class.java)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        for ((index, pad) in pads.withIndex()) {
            pad.setOnTouchListener { _, motionEvent ->
                when(motionEvent.action) {
                    MotionEvent.ACTION_DOWN -> pad.setImageResource(selectedPadImage(index))
                    MotionEvent.ACTION_UP -> pad.setImageResource(padImage(index))
                    else -> {}
                }
                true
            }
        }
    }


    private fun selectedPadImage(index: Int): Int {
        return when(index) {
            in 0..3 -> R.drawable.selected_red_pad
            in 4..7 -> R.drawable.selected_yellow_pad
            in 8..11 -> R.drawable.selected_green_pad
            in 12..15 -> R.drawable.selected_blue_pad
            else -> R.drawable.selected_red_pad
        }
    }

    private fun padImage(index: Int): Int {
        return when(index) {
            in 0..3 -> R.drawable.red_pad
            in 4..7 -> R.drawable.yellow_pad
            in 8..11 -> R.drawable.green_pad
            in 12..15 -> R.drawable.blue_pad
            else -> R.drawable.red_pad
        }
    }
}
