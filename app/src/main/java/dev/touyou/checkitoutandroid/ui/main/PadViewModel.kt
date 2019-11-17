package dev.touyou.checkitoutandroid.ui.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dev.touyou.checkitoutandroid.R

class PadViewModel : ViewModel() {
    val assignedSound by lazy {
        MutableLiveData<List<Int?>>(sounds.toList())
    }

    var sounds = mutableListOf(
        R.raw.touyou, R.raw.chekera_cut, R.raw.minnade_cut, null,
        R.raw.touyou, R.raw.chekera_cut, R.raw.minnade_cut, null,
        R.raw.touyou, R.raw.chekera_cut, R.raw.minnade_cut, null,
        R.raw.touyou, R.raw.chekera_cut, R.raw.minnade_cut, null
    )

    fun changeSound(index: Int, resId: Int) {
        sounds[index] = resId
        assignedSound.value = sounds.toList()
    }
}
