package dev.touyou.checkitoutandroid.entity

import androidx.lifecycle.LiveData
import dev.touyou.checkitoutandroid.helper.asLiveData
import io.realm.Realm
import io.realm.RealmResults

class SoundDao(val realm: Realm) {

    fun addToSound(
        displayName: String,
        padNum: Int = -1,
        urlStr: String? = null,
        rawId: Int? = null,
        id: Long? = null
    ) {
        realm.executeTransactionAsync {
            val sounds = it.where(SoundData::class.java).findAll().sort("id")
            val lastId = if (sounds.isEmpty()) -1 else sounds.last()!!.id
            val sound = SoundData()
            sound.id = id ?: lastId + 1
            sound.displayName = displayName
            sound.padNum = padNum
            sound.isRaw = rawId != null
            sound.urlStr = urlStr
            sound.rawId = rawId
            it.insert(sound)
        }
    }

    fun assignPad(id: Long, padId: Int) {
        realm.executeTransactionAsync {
            val sounds = it.where(SoundData::class.java).findAll()
            sounds.find { it.padNum == padId }?.padNum = -1
            sounds.find { it.id == id }?.padNum = padId
        }
    }

    fun getSound(): LiveData<RealmResults<SoundData>> {
        return realm.where(SoundData::class.java).findAllAsync().sort("id").asLiveData()
    }

    fun delete(data: SoundData) {
        realm.executeTransactionAsync {
            data.deleteFromRealm()
        }
    }
}

fun Realm.soundDao(): SoundDao = SoundDao(this)