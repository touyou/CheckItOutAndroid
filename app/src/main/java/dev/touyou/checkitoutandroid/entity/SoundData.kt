package dev.touyou.checkitoutandroid.entity

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

open class SoundData(
    @PrimaryKey var id: Long = 0,
    var isRaw: Boolean = false,
    var urlStr: String? = null,
    var rawId: Int? = null,
    var displayName: String = "",
    var padNum: Int = -1
) : RealmObject()