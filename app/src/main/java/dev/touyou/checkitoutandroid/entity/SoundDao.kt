package dev.touyou.checkitoutandroid.entity

import androidx.lifecycle.LiveData
import dev.touyou.checkitoutandroid.helper.asLiveData
import io.realm.Realm
import io.realm.RealmResults

class SoundDao(val realm: Realm) {

    fun addToSound() {
        realm.executeTransactionAsync {
            val sounds = it.where(SoundData::class.java).findAll().sort("id")
            var sound = SoundData()
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
        return realm.where(SoundData::class.java).findAllAsync().asLiveData()
    }

    fun deleteAll() {
        realm.executeTransactionAsync {
            val result = it.where(SoundData::class.java).findAll()
            result.deleteAllFromRealm()
        }
    }
}

fun Realm.soundDao(): SoundDao = SoundDao(this)