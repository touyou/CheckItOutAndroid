package dev.touyou.checkitoutandroid.ui.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dev.touyou.checkitoutandroid.PlayMode
import dev.touyou.checkitoutandroid.R

class PadViewModel : ViewModel() {
    val assignedSound by lazy {
        MutableLiveData<List<Int?>>(sounds.toList())
    }
    var currentMode = MutableLiveData<PlayMode>(PlayMode.PLAY)
    var selectedPad: Int? = null

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

    fun changeMode(mode: PlayMode) {
        currentMode.value = mode
        if (mode != PlayMode.EDIT) selectedPad = null
    }
}
