package dev.touyou.checkitoutandroid.ui.main

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import dev.touyou.checkitoutandroid.R
import dev.touyou.checkitoutandroid.entity.SoundData

class SoundViewAdapter(private val soundList: List<SoundData>) :
    RecyclerView.Adapter<SoundViewAdapter.SoundViewHolder>() {
    class SoundViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val soundNameTextView: TextView = view.findViewById(R.id.soundNameTextView)
        val padTextView: TextView = view.findViewById(R.id.padTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SoundViewHolder =
        SoundViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.sound_item, parent, false
            )
        )

    override fun getItemCount(): Int = soundList.size

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: SoundViewHolder, position: Int) {
        holder.soundNameTextView.text = soundList[position].displayName
        holder.padTextView.text = "PAD ${soundList[position].padNum + 1}"
    }
}