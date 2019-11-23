package dev.touyou.checkitoutandroid.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import dev.touyou.checkitoutandroid.R
import dev.touyou.checkitoutandroid.entity.SoundData
import kotlinx.android.synthetic.main.control_fragment.*

class ControlFragment : Fragment() {

    companion object {
        fun newInstance() = ControlFragment()
    }

    private lateinit var viewModel: PadViewModel

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

        var soundList = mutableListOf<SoundData>()
        viewModel.getSoundData().observe(viewLifecycleOwner, Observer {
            soundList = it.toMutableList()
        })

        soundRecyclerView.adapter = SoundViewAdapter(soundList)
        soundRecyclerView.layoutManager = LinearLayoutManager(context)
    }

}
