package com.aimbuddy

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import java.nio.charset.StandardCharsets

class BleSender {

    private var bluetoothGatt: BluetoothGatt? = null
    private var txCharacteristic: BluetoothGattCharacteristic? = null

    fun setConnection(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic
    ) {
        bluetoothGatt = gatt
        txCharacteristic = characteristic
    }

    fun sendPose(result: PoseResult) {

        val builder = StringBuilder()

        builder.append("POSE")
        builder.append(',')
        builder.append(result.centerX)
        builder.append(',')
        builder.append(result.centerY)

        for (point in result.keypoints) {
            builder.append(',')
            builder.append(point.x)
            builder.append(',')
            builder.append(point.y)
            builder.append(',')
            builder.append(point.score)
        }

        val bytes =
            builder.toString()
                .toByteArray(StandardCharsets.UTF_8)

        val characteristic =
            txCharacteristic ?: return

        @Suppress("DEPRECATION")
        characteristic.value = bytes

        @Suppress("DEPRECATION")
        bluetoothGatt?.writeCharacteristic(
            characteristic
        )
    }

    fun close() {
        bluetoothGatt?.close()
        bluetoothGatt = null
        txCharacteristic = null
    }
}
