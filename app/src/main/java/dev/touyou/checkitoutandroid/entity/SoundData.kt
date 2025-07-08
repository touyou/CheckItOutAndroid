package dev.touyou.checkitoutandroid.entity

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey

class SoundData : RealmObject {
    @PrimaryKey
    var id: Long = 0
    var isRaw: Boolean = false
    var urlStr: String? = null
    var rawId: Int? = null
    var displayName: String = ""
    var padNum: Int = -1

    fun toAssignedSound() = AssignedSound(this.isRaw, this.urlStr, this.rawId)
}

data class AssignedSound(
    var isRaw: Boolean = false,
    var urlStr: String? = null,
    var rawId: Int? = null
)
