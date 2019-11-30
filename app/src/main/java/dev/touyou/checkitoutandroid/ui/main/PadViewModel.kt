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

    fun initSounds() {
        val soundData = realm.where(SoundData::class.java).findAll()
        for (sound in soundData) {
            if (sound.padNum != -1) {
                sounds[sound.padNum] = sound.toAssignedSound()
            }
        }
        assignedSound.value = sounds.toList()
    }

    fun addSound(name: String, padNum: Int = -1, rawId: Int, id: Long) {
        realm.soundDao().addToSound(name, padNum, rawId = rawId, id = id)
        if (padNum != -1) {
            sounds[padNum] = AssignedSound(isRaw = true, rawId = rawId)
            assignedSound.value = sounds.toList()
        }
    }

    fun addSound(name: String, filePath: String) {
        realm.soundDao().addToSound(displayName = name, urlStr = filePath)
    }

    fun changeSound(index: Int, sound: SoundData) {
        realm.soundDao().assignPad(sound.id, index)
        sounds[index] = sound.toAssignedSound()
        assignedSound.value = sounds.toList()
    }

    fun changeSoundAll(list: MutableList<SoundData>) {
        sounds.fill(null)
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
