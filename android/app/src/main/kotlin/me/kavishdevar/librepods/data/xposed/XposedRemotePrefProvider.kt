package me.kavishdevar.librepods.data.xposed

object XposedRemotePrefProvider {
    fun create(): XposedRemotePref = XposedRemotePrefImpl()
}
