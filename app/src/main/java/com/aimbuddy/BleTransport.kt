package com.example.poseresearch

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class BleTransport(
    private val context: Context
) {

    companion object {

        val SERVICE_UUID: UUID =
            UUID.fromString(
                "6E400001-B5A3-F393-E0A9-E50E24DCCA9E"
            )

        val CHARACTERISTIC_UUID: UUID =
            UUID.fromString(
                "6E400002-B5A3-F393-E0A9-E50E24DCCA9E"
            )
    }

    private val adapter: BluetoothAdapter? =
        (
            context.getSystemService(
                BluetoothManager::class.java
            )?.adapter
        )

    private var gatt: BluetoothGatt? =
        null

    private var characteristic:
        BluetoothGattCharacteristic? =
        null

    fun connect(device: BluetoothDevice) {

        if (
            BuildPermission.hasConnect(
                context
            )
        ) {
            gatt =
                device.connectGatt(
                    context,
                    false,
                    callback
                )
        }
    }

    fun send(
        result: PoseResult
    ) {

        val g =
            gatt
                ?: return

        val c =
            characteristic
                ?: return

        if (
            !BuildPermission.hasConnect(
                context
            )
        ) {
            return
        }

        val packet =
            JSONObject().apply {

                put(
                    "t",
                    result.timestampMs
                )

                put(
                    "w",
                    result.frameWidth
                )

                put(
                    "h",
                    result.frameHeight
                )

                put(
                    "cx",
                    result.centerX
                )

                put(
                    "cy",
                    result.centerY
                )

                put(
                    "left",
                    result.left
                )

                put(
                    "top",
                    result.top
                )

                put(
                    "right",
                    result.right
                )

                put(
                    "bottom",
                    result.bottom
                )

                put(
                    "latency",
                    result.latencyMs
                )

                val points =
                    JSONArray()

                result.keypoints.forEach {

                    points.put(
                        JSONObject().apply {

                            put(
                                "name",
                                it.name
                            )

                            put(
                                "x",
                                it.x
                            )

                            put(
                                "y",
                                it.y
                            )

                            put(
                                "confidence",
                                it.confidence
                            )
                        }
                    )
                }

                put(
                    "keypoints",
                    points
                )
            }

        val bytes =
            packet
                .toString()
                .toByteArray(
                    Charsets.UTF_8
                )

        val max =
            180

        val chunks =
            bytes
                .asList()
                .chunked(max)

        for (chunk in chunks) {

            c.value =
                chunk.toByteArray()

            try {

                g.writeCharacteristic(
                    c
                )

            } catch (_: Throwable) {
            }
        }
    }

    private val callback =
        object : BluetoothGattCallback() {

            override fun onConnectionStateChange(
                g: BluetoothGatt,
                status: Int,
                newState: Int
            ) {

                if (
                    newState ==
                    android.bluetooth.BluetoothProfile.STATE_CONNECTED
                ) {

                    if (
                        BuildPermission.hasConnect(
                            context
                        )
                    ) {
                        g.discoverServices()
                    }
                }
            }

            override fun onServicesDiscovered(
                g: BluetoothGatt,
                status: Int
            ) {

                characteristic =
                    g.getService(
                        SERVICE_UUID
                    )?.getCharacteristic(
                        CHARACTERISTIC_UUID
                    )
            }
        }

    fun close() {

        if (
            BuildPermission.hasConnect(
                context
            )
        ) {

            try {
                gatt?.disconnect()
                gatt?.close()
            } catch (_: Throwable) {
            }
        }

        gatt = null
        characteristic = null
    }
}

object BuildPermission {

    fun hasConnect(
        context: Context
    ): Boolean {

        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT
        ) ==
            PackageManager.PERMISSION_GRANTED
    }
}
