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
import io.realm.OrderedRealmCollection
import io.realm.RealmRecyclerViewAdapter

class SoundViewAdapter(
    private val soundList: OrderedRealmCollection<SoundData>?,
    private val autoUpdate: Boolean
) :
    RealmRecyclerViewAdapter<SoundData, SoundViewAdapter.SoundViewHolder>(soundList, autoUpdate) {
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

    override fun getItemCount(): Int = soundList?.size ?: 0

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: SoundViewHolder, position: Int) {
        val soundData = soundList?.get(position) ?: return
        holder.soundNameTextView.text = soundData.displayName
        holder.padTextView.text =
            if (soundData.padNum == -1) "NONE" else "PAD ${soundData.padNum + 1}"
        holder.baseLayout.setOnClickListener {
            listener?.onClick(it, position)
        }
    }

    fun setOnItemClickListener(listener: onItemClickListener) {
        this.listener = listener
    }
}