package me.kavishdevar.librepods.data.xposed

interface XposedRemotePref {
    fun isAvailable(): Boolean

    fun getBoolean(key: String, def: Boolean): Boolean
    fun putBoolean(key: String, value: Boolean)
}
