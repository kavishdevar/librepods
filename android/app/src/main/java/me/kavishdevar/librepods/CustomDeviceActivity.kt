/*
 * LibrePods - AirPods liberated from Apple’s ecosystem
 * 
 * Copyright (C) 2025 LibrePods contributors
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package me.kavishdevar.librepods

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothDevice.TRANSPORT_LE
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.ui.theme.LibrePodsTheme
import org.lsposed.hiddenapibypass.HiddenApiBypass
import java.util.UUID

class CustomDevice : ComponentActivity() {
    @SuppressLint("MissingPermission", "CoroutineCreationDuringComposition")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LibrePodsTheme {
                val connect = remember { mutableStateOf(false) }
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Custom Device", style = MaterialTheme.typography.titleLarge)
                        }
                    }
                ) { innerPadding ->
                    HiddenApiBypass.addHiddenApiExemptions("Landroid/bluetooth/BluetoothSocket;")
                    val manager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
//                    val device: BluetoothDevice = manager.adapter.getRemoteDevice("EC:D6:F4:3D:89:B8")
                    val device: BluetoothDevice = manager.adapter.getRemoteDevice("E7:48:92:3B:7D:A5")
//                    val socket = device.createInsecureL2capChannel(31)

//                    val batteryLevel = remember { mutableStateOf("") }
//                    socket.outputStream.write(byteArrayOf(0x12,0x3B,0x00,0x02, 0x00))
//                    socket.outputStream.write(byteArrayOf(0x12, 0x3A, 0x00, 0x01, 0x00, 0x08,0x01))

                    val gatt = device.connectGatt(this, true, object: BluetoothGattCallback() {
                        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                            if (status == BluetoothGatt.GATT_SUCCESS) {
                                // Step 2: Iterate through the services and characteristics
                                gatt.services.forEach { service ->
                                    Log.d("GATT", "Service UUID: ${service.uuid}")
                                    service.characteristics.forEach { characteristic ->
                                        characteristic.descriptors.forEach { descriptor ->
                                            Log.d("GATT", "         Descriptor UUID: ${descriptor.uuid}: ${gatt.readDescriptor(descriptor)}")
                                        }
                                    }
                                }

                            }
                        }

                        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                            if (newState == BluetoothGatt.STATE_CONNECTED) {
                                Log.d("GATT", "Connected to GATT server")
                                gatt.discoverServices() // Discover services after connection
                            }
                        }

                        override fun onCharacteristicWrite(
                            gatt: BluetoothGatt,
                            characteristic: BluetoothGattCharacteristic,
                            status: Int
                        ) {
                            if (status == BluetoothGatt.GATT_SUCCESS) {
                                Log.d("BLE", "Write successful for UUID: ${characteristic.uuid}")
                            } else {
                                Log.e("BLE", "Write failed for UUID: ${characteristic.uuid}, status: $status")
                            }
                        }
                    }, TRANSPORT_LE, 1)

                    if (connect.value) {
                        try {
                            gatt.connect()
                        }
                        catch (e: Exception) {
                            e.printStackTrace()
                        }
                        connect.value = false
                    }

                    Column (
                        modifier = Modifier.padding(innerPadding),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    )
                    {
                        Button(
                            onClick = { connect.value = true }
                        )
                        {
                            Text("Connect")
                        }

                        Button(onClick = {
                            val characteristicUuid = "94110001-6D9B-4225-A4F1-6A4A7F01B0DE"
                            val value = byteArrayOf(0x01, 0x00, 0x00, 0x00, 0x00 ,0x00 ,0x01)
                            sendWriteRequest(gatt, characteristicUuid, value)
                        
                        }) {
                            Text("batteryLevel.value")
                        }
                    }
                }
            }
        }
    }
}

@RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
fun sendWriteRequest(
    gatt: BluetoothGatt,
    characteristicUuid: String,
    value: ByteArray
) {
    // Retrieve the service containing the characteristic
    val service = gatt.services.find { service ->
        service.characteristics.any { it.uuid.toString() == characteristicUuid }
    }

    if (service == null) {
        Log.e("GATT", "Service containing characteristic UUID $characteristicUuid not found.")
        return
    }

    // Retrieve the characteristic
    val characteristic = service.getCharacteristic(UUID.fromString(characteristicUuid))
    if (characteristic == null) {
        Log.e("GATT", "Characteristic with UUID $characteristicUuid not found.")
        return
    }


    // Send the write request
    val success = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        gatt.writeCharacteristic(characteristic, value, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
    } else {
        gatt.writeCharacteristic(characteristic)
    }
    Log.d("GATT", "Write request sent $success to UUID: $characteristicUuid")
}