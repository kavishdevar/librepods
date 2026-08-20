package me.kavishdevar.librepods

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper
import me.kavishdevar.librepods.utils.XposedServiceHolder
import me.kavishdevar.librepods.utils.XposedState

/**
 * Application entry point kept deliberately small during the Wear OS port.
 *
 * The original Android application initialized billing and several phone-only
 * services here. Those responsibilities do not belong in the autonomous Wear
 * OS application and are intentionally removed from this port.
 */
class LibrePodsApplication : Application(), XposedServiceHelper.OnServiceListener, DefaultLifecycleObserver {

    override fun onCreate() {
        super.onCreate()
        XposedServiceHelper.registerListener(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onResume(owner: LifecycleOwner) {
        XposedState.isAvailable = XposedServiceHolder.service != null
        XposedState.bluetoothScopeEnabled = XposedServiceHolder.service?.scope?.any {
            it == "com.google.android.bluetooth" || it == "com.android.bluetooth"
        } == true
    }

    override fun onServiceBind(service: XposedService) {
        XposedServiceHolder.service = service
        XposedState.isAvailable = true
        XposedState.bluetoothScopeEnabled = service.scope.any {
            it == "com.google.android.bluetooth" || it == "com.android.bluetooth"
        }
    }

    override fun onServiceDied(service: XposedService) {
        XposedServiceHolder.service = null
        XposedState.isAvailable = false
        XposedState.bluetoothScopeEnabled = false
    }
}
