package dev.touyou.checkitoutandroid.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dev.touyou.checkitoutandroid.entity.AssignedSound
import dev.touyou.checkitoutandroid.entity.PlayMode
import dev.touyou.checkitoutandroid.entity.SoundData
import dev.touyou.checkitoutandroid.entity.soundDao
import io.realm.Realm
import io.realm.RealmResults

class PadViewModel : ViewModel() {
    private val realm: Realm by lazy {
        Realm.getDefaultInstance()
    }

    val assignedSound by lazy {
        MutableLiveData<List<AssignedSound?>>(sounds.toList())
    }
    var currentMode = MutableLiveData<PlayMode>(PlayMode.PLAY)
    var selectedPad: Int? = null

    var sounds = mutableListOf<AssignedSound?>(
        null, null, null, null,
        null, null, null, null,
        null, null, null, null,
        null, null, null, null
    )

    override fun onCleared() {
        realm.close()
        super.onCleared()
    }

    fun changeSound(index: Int, sound: AssignedSound) {
        sounds[index] = sound
        assignedSound.value = sounds.toList()
    }

    fun changeSoundAll(list: MutableList<SoundData>) {
        for (data in list) {
            if (data.padNum != -1) sounds[data.padNum] = data.toAssignedSound()
        }
        assignedSound.value = sounds.toList()
    }

    fun changeMode(mode: PlayMode) {
        currentMode.value = mode
        if (mode != PlayMode.EDIT) selectedPad = null
    }

    fun getSoundData(): LiveData<RealmResults<SoundData>> {
        return realm.soundDao().getSound()
    }
}
