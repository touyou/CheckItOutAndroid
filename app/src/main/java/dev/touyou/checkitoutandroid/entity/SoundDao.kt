package dev.touyou.checkitoutandroid.entity

import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import io.realm.kotlin.Realm
import io.realm.kotlin.ext.query
import io.realm.kotlin.query.RealmResults
import kotlinx.coroutines.flow.map

class SoundDao(private val realm: Realm) {

    suspend fun addToSound(
        displayName: String,
        padNum: Int = -1,
        urlStr: String? = null,
        rawId: Int? = null,
        id: Long? = null
    ) {
        realm.write {
            val sounds = query<SoundData>().find().sortedBy { it.id }
            val lastId = if (sounds.isEmpty()) -1 else sounds.last().id
            val sound = SoundData().apply {
                this.id = id ?: lastId + 1
                this.displayName = displayName
                this.padNum = padNum
                this.isRaw = rawId != null
                this.urlStr = urlStr
                this.rawId = rawId
            }
            copyToRealm(sound)
        }
    }

    suspend fun assignPad(id: Long, padId: Int) {
        realm.write {
            val sounds = query<SoundData>().find()
            sounds.find { it.padNum == padId }?.padNum = -1
            sounds.find { it.id == id }?.padNum = padId
        }
    }

    fun getSound(): LiveData<List<SoundData>> {
        return realm.query<SoundData>().sort("id").asFlow().map { it.list }.asLiveData()
    }

    suspend fun delete(data: SoundData) {
        realm.write {
            val soundToDelete = query<SoundData>("id == $0", data.id).first().find()
            soundToDelete?.let { delete(it) }
        }
    }
}

fun Realm.soundDao(): SoundDao = SoundDao(this)
