package me.kavishdevar.librepods.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.sp

@Composable
fun Int.nonScaledSp() = (this / LocalDensity.current.fontScale).sp

fun String.redactMac(): String {
    val parts = this.split(":")
    if (parts.size != 6) return this
    return "${parts[0]}:${parts[1]}:XX:XX:${parts[4]}:${parts[5]}"
}
