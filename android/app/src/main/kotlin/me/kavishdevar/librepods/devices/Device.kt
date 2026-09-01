package me.kavishdevar.librepods.devices

import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.RECEIVER_EXPORTED
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.ParcelUuid
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import me.kavishdevar.librepods.bluetooth.MacAddress
import me.kavishdevar.librepods.bluetooth.createBluetoothSocket
import kotlin.time.Duration.Companion.milliseconds

sealed interface Device<S: DeviceState, T: DeviceSettings, M: DeviceMetadata> {
    private val TAG: String
        get() = "LibrePodsDevice<${macAddress.toRedactedString()}>"

    // todo: somehow remove context from here. maybe get Profile from service?
    val context: Context

    val macAddress: MacAddress

    val bluetoothAdapter: BluetoothAdapter
    val bluetoothDevice: BluetoothDevice

    val connectionState: StateFlow<ConnectionState>

    val state: StateFlow<S>
    val settings: StateFlow<T>
    val metadata: StateFlow<M>

    val connectionNumber: StateFlow<Int>

    fun connect(): Boolean

    fun disconnect()

    fun createSocket(uuid: ParcelUuid, psm: Int) = createBluetoothSocket(
        adapter = bluetoothAdapter,
        device = bluetoothDevice,
        uuid = uuid,
        psm = psm
    )

    fun disableAudio() {
        disableA2dp()
        disableHeadset()
    }

    fun disableA2dp() {
        val bluetoothAdapter = context.getSystemService(BluetoothManager::class.java).adapter

        if (context.checkSelfPermission("android.permission.BLUETOOTH_PRIVILEGED") == PackageManager.PERMISSION_GRANTED) {
            bluetoothAdapter?.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                    if (profile == BluetoothProfile.A2DP) {
                        try {
                            if (proxy.getConnectionState(bluetoothDevice) == BluetoothProfile.STATE_DISCONNECTED) {
                                Log.d(TAG, "Already disconnected from A2DP")
                                return
                            }
                            val method = proxy.javaClass.getMethod("setConnectionPolicy", BluetoothDevice::class.java, Int::class.java)
                            Log.d(TAG, "calling A2DP.setConnectionPolicy(0)")
                            method.invoke(proxy, bluetoothDevice, 0)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            bluetoothAdapter.closeProfileProxy(BluetoothProfile.A2DP, proxy)
                        }
                    }
                }

                override fun onServiceDisconnected(profile: Int) {}
            }, BluetoothProfile.A2DP)
        } else {
            Log.d(TAG, "not disconnecting A2DP, no BLUETOOTH_PRIVILEGED permission")
        }
    }

    fun disableHeadset() {
        val bluetoothAdapter = context.getSystemService(BluetoothManager::class.java).adapter

        if (context.checkSelfPermission("android.permission.MODIFY_PHONE_STATE") == PackageManager.PERMISSION_GRANTED) {
            bluetoothAdapter?.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                    if (profile == BluetoothProfile.HEADSET) {
                        try {
                            val method = proxy.javaClass.getMethod("setConnectionPolicy", BluetoothDevice::class.java, Int::class.java)
                            Log.d(TAG, "calling HEADSET.setConnectionPolicy(0)")
                            method.invoke(proxy, bluetoothDevice, 0)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            bluetoothAdapter.closeProfileProxy(BluetoothProfile.HEADSET, proxy)
                        }
                    }
                }
                override fun onServiceDisconnected(profile: Int) {}
            }, BluetoothProfile.HEADSET)
        } else {
            Log.d(TAG, "not disconnecting HEADSET, no MODIFIY_PHONE_STATE permission")
        }
    }

    fun enableAudio() {
        enableA2dp()
        enableHeadset()
    }

    fun enableA2dp() {
        val bluetoothAdapter = context.getSystemService(BluetoothManager::class.java).adapter

        bluetoothAdapter?.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                if (profile == BluetoothProfile.A2DP) {
                    if (context.checkSelfPermission("android.permission.BLUETOOTH_PRIVILEGED") == PackageManager.PERMISSION_GRANTED) {
                        try {
                            val policyMethod = proxy.javaClass.getMethod("setConnectionPolicy", BluetoothDevice::class.java, Int::class.java)
                            Log.d(TAG, "calling A2DP.setConnectionPolicy(100)")
                            policyMethod.invoke(proxy, bluetoothDevice, 100)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            bluetoothAdapter.closeProfileProxy(BluetoothProfile.A2DP, proxy)
                        }
                    }
                    else {
                        Log.i(TAG, "not setting connection policy for A2DP, no BLUETOOTH_PRIVILEGED permission. just called connect")
                    }
                }
            }

            override fun onServiceDisconnected(profile: Int) {}
        }, BluetoothProfile.A2DP)
    }

    fun enableHeadset() {
        val bluetoothAdapter = context.getSystemService(BluetoothManager::class.java).adapter

        bluetoothAdapter?.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                if (profile == BluetoothProfile.HEADSET) {
                    if (context.checkSelfPermission("android.permission.MODIFY_PHONE_STATE") == PackageManager.PERMISSION_GRANTED) {
                        try {
                            val policyMethod = proxy.javaClass.getMethod(
                                "setConnectionPolicy",
                                BluetoothDevice::class.java,
                                Int::class.java
                            )
                            Log.d(TAG, "calling HEADSET.setConnectionPolicy(100)")
                            policyMethod.invoke(proxy, bluetoothDevice, 100)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            bluetoothAdapter.closeProfileProxy(BluetoothProfile.HEADSET, proxy)
                        }
                    } else {
                        Log.d(TAG, "not setting connection policy for HEADSET, no MODIFIY_PHONE_STATE permission")
                    }
                }
            }

            override fun onServiceDisconnected(profile: Int) {}
        }, BluetoothProfile.HEADSET)
    }

    fun connectAudio() {
        connectA2dp()
        connectHeadset()
    }

    fun connectA2dp() {
        val bluetoothAdapter = context.getSystemService(BluetoothManager::class.java).adapter

        bluetoothAdapter?.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                if (profile == BluetoothProfile.A2DP) {
                    try {
                        val connectMethod = proxy.javaClass.getMethod("connect", BluetoothDevice::class.java)
                        connectMethod.invoke(proxy, bluetoothDevice)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        bluetoothAdapter.closeProfileProxy(BluetoothProfile.A2DP, proxy)
                    }
                }
            }

            override fun onServiceDisconnected(profile: Int) {}
        }, BluetoothProfile.A2DP)
    }

    fun connectHeadset() {
        val bluetoothAdapter = context.getSystemService(BluetoothManager::class.java).adapter

        bluetoothAdapter?.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                if (profile == BluetoothProfile.HEADSET) {
                    try {
                        val connectMethod = proxy.javaClass.getMethod("connect", BluetoothDevice::class.java)
                        connectMethod.invoke(proxy, bluetoothDevice)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        bluetoothAdapter.closeProfileProxy(BluetoothProfile.HEADSET, proxy)
                    }
                }
            }

            override fun onServiceDisconnected(profile: Int) {}
        }, BluetoothProfile.HEADSET)
    }

    fun disconnectAudio() {
        disconnectA2dp()
        disconnectHeadset()
    }

    fun disconnectA2dp() {
        val bluetoothAdapter = context.getSystemService(BluetoothManager::class.java).adapter

        bluetoothAdapter?.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                if (profile == BluetoothProfile.A2DP) {
                    try {
                        val disconnectMethod = proxy.javaClass.getMethod("disconnect", BluetoothDevice::class.java)
                        disconnectMethod.invoke(proxy, bluetoothDevice)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        bluetoothAdapter.closeProfileProxy(BluetoothProfile.A2DP, proxy)
                    }
                }
            }

            override fun onServiceDisconnected(profile: Int) {}
        }, BluetoothProfile.A2DP)
    }

    fun disconnectHeadset() {
        val bluetoothAdapter = context.getSystemService(BluetoothManager::class.java).adapter

        bluetoothAdapter?.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                if (profile == BluetoothProfile.HEADSET) {
                    try {
                        val disconnectMethod = proxy.javaClass.getMethod("disconnect", BluetoothDevice::class.java)
                        disconnectMethod.invoke(proxy, bluetoothDevice)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        bluetoothAdapter.closeProfileProxy(BluetoothProfile.HEADSET, proxy)
                    }
                }
            }

            override fun onServiceDisconnected(profile: Int) {}
        }, BluetoothProfile.HEADSET)
    }

    fun waitForA2dpConnection(
        context: Context,
        onConnected: () -> Unit
    ): BroadcastReceiver {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val state = intent.getIntExtra(
                    BluetoothProfile.EXTRA_STATE,
                    BluetoothProfile.STATE_DISCONNECTED
                )
                val previousState = intent.getIntExtra(
                    BluetoothProfile.EXTRA_PREVIOUS_STATE,
                    BluetoothProfile.STATE_DISCONNECTED
                )
                val device = intent.getParcelableExtra(
                    BluetoothDevice.EXTRA_DEVICE,
                    BluetoothDevice::class.java
                )

                if (
                    state == BluetoothProfile.STATE_CONNECTED &&
                    previousState != BluetoothProfile.STATE_CONNECTED &&
                    device?.address == macAddress.value
                ) {
                    context.unregisterReceiver(this)
                    onConnected()
                }
            }
        }

        context.registerReceiver(
            receiver,
            IntentFilter(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED),
            RECEIVER_EXPORTED
        )

        return receiver
    }

    fun reconnectA2dp() {
        bluetoothAdapter.getProfileProxy(
            context,
            object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(
                    profile: Int,
                    proxy: BluetoothProfile
                ) {
                    if (profile != BluetoothProfile.A2DP) return

                    val a2dp = proxy as BluetoothA2dp

                    try {
                        val connectMethod = proxy.javaClass.getMethod("connect", BluetoothDevice::class.java)
                        val disconnectMethod = proxy.javaClass.getMethod("disconnect", BluetoothDevice::class.java)

                        disconnectMethod.invoke(proxy, bluetoothDevice)

                        CoroutineScope(Dispatchers.IO).launch {
                            delay(500.milliseconds)

                            connectMethod.invoke(proxy, bluetoothDevice)

                            bluetoothAdapter.closeProfileProxy(
                                BluetoothProfile.A2DP,
                                a2dp
                            )
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to reconnect A2DP", e)
                    }
                }

                override fun onServiceDisconnected(profile: Int) {
                }
            },
            BluetoothProfile.A2DP
        )
    }
}

interface DeviceState
interface DeviceSettings
interface DeviceMetadata {
    val name: String
    val iconName: String
}
