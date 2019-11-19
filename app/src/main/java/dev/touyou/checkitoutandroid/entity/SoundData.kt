package dev.touyou.checkitoutandroid.entity

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

data class SoundData(
    @PrimaryKey var id: Long = 0,
    var isBundle: Boolean = false,
    var urlStr: String = "",
    var displayName: String = "",
    var padNum: Int = -1
) : RealmObject()