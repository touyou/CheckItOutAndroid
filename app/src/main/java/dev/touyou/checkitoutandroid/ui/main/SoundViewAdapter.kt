package dev.touyou.checkitoutandroid.ui.main

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import dev.touyou.checkitoutandroid.R
import dev.touyou.checkitoutandroid.entity.SoundData

class SoundViewAdapter(private val soundList: List<SoundData>) :
    RecyclerView.Adapter<SoundViewAdapter.SoundViewHolder>() {
    private var listener: onItemClickListener? = null

    class SoundViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val soundNameTextView: TextView = view.findViewById(R.id.soundNameTextView)
        val padTextView: TextView = view.findViewById(R.id.padTextView)
        val baseLayout: LinearLayout = view.findViewById(R.id.recyclerLinearLayout)
    }

    interface onItemClickListener {
        fun onClick(view: View, position: Int)
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
        holder.padTextView.text =
            if (soundList[position].padNum == -1) "NONE" else "PAD ${soundList[position].padNum + 1}"
        holder.baseLayout.setOnClickListener {
            listener?.onClick(it, position)
        }
    }

    fun setOnItemClickListener(listener: onItemClickListener) {
        this.listener = listener
    }
}