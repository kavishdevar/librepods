package me.kavishdevar.librepods.wear.bluetooth

import android.bluetooth.BluetoothDevice
import android.os.ParcelUuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.StateFlow

/**
 * Small bounded reconnect coordinator. It never owns protocol state; it only
 * asks the connection session to establish the transport again.
 */
class AirPodsReconnectManager(
    private val scope: CoroutineScope,
    private val session: AirPodsConnectionSession,
    private val maxAttempts: Int = 5,
) {
    private var job: Job? = null

    fun start(
        device: BluetoothDevice,
        aacpUuid: ParcelUuid,
        aacpPsm: Int,
        attUuid: ParcelUuid,
        attPsm: Int,
    ) {
        job?.cancel()
        job = scope.launch {
            var attempt = 0
            var delayMs = 1_000L

            while (attempt < maxAttempts) {
                attempt++
                try {
                    session.connect(device, aacpUuid, aacpPsm, attUuid, attPsm)
                    return@launch
                } catch (_: Throwable) {
                    if (attempt >= maxAttempts) return@launch
                    delay(delayMs)
                    delayMs = (delayMs * 2).coerceAtMost(16_000L)
                }
            }
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
    }

    fun state(): StateFlow<AirPodsConnectionSession.State> = session.state
}
