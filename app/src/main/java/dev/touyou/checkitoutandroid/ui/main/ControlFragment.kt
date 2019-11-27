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
import dev.touyou.checkitoutandroid.entity.PlayMode
import dev.touyou.checkitoutandroid.entity.SoundData
import io.realm.Realm
import kotlinx.android.synthetic.main.control_fragment.*

class ControlFragment : Fragment() {

    companion object {
        fun newInstance() = ControlFragment()
    }

    private lateinit var viewModel: PadViewModel
    private lateinit var adapter: SoundViewAdapter
    private lateinit var mode: PlayMode

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
    }

    private fun changeMode(mode: PlayMode) {
        this.mode = mode
        when (mode) {
            PlayMode.REC -> recBaseView.visibility = View.VISIBLE
            else -> recBaseView.visibility = View.INVISIBLE
        }
    }
}
