package me.kavishdevar.librepods.presentation.components

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.utils.XposedState

@Composable
fun DeviceInfoCard() {
    StyledList(title = stringResource(R.string.device_info)) {
        StyledListItem(
            contentText =  stringResource(R.string.manufacturer),
            supportingText =  Build.MANUFACTURER,
            enabled = false
        )

        StyledListItem(
            contentText =  stringResource(R.string.model_number),
            supportingText =  Build.MODEL,
            enabled = false
        )

        StyledListItem(
            contentText =  stringResource(R.string.build_id),
            supportingText =  Build.DISPLAY,
            enabled = false
        )

        StyledListItem(
            contentText =  stringResource(R.string.version),
            supportingText =  "${Build.ID} (${Build.VERSION.SDK_INT_FULL})",
            enabled = false
        )

        StyledListItem(
            contentText =  stringResource(R.string.xposed_available),
            supportingText =  if (XposedState.isAvailable) {
                stringResource(R.string.yes)
            } else {
                stringResource(R.string.no)
            },
            enabled = false
        )

        StyledListItem(
            contentText =  stringResource(R.string.app_enabled_in_xposed),
            supportingText =  if (XposedState.bluetoothScopeEnabled) {
                stringResource(R.string.yes)
            } else {
                stringResource(R.string.no)
            },
            enabled = false
        )
    }
}
