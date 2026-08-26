package me.kavishdevar.librepods.wear.bluetooth

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.StateFlow

/**
 * Small bounded reconnect coordinator. It owns retry timing only; transport
 * and protocol state remain inside the connection session.
 */
class AirPodsReconnectManager(
    private val scope: CoroutineScope,
    private val session: AirPodsConnectionSession,
    private val maxAttempts: Int = 5,
) {
    private var job: Job? = null

    fun start(target: AirPodsConnectionTarget) {
        job?.cancel()
        job = scope.launch {
            var attempt = 0
            var delayMs = 1_000L

            while (attempt < maxAttempts) {
                attempt++
                try {
                    session.connect(
                        device = target.device,
                        aacpUuid = target.aacpUuid,
                        aacpPsm = target.aacpPsm,
                        attUuid = target.attUuid,
                        attPsm = target.attPsm,
                    )
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
