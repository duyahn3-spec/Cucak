package com.example.poseresearch

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.util.Log
import java.util.UUID

class BleManager(private val context: Context) {

    companion object {
        private const val TAG = "BleManager"
        private const val ESP32_MAC = "30:ED:A0:5A:36:A6" // Sửa MAC thực tế
        private val SERVICE_UUID = UUID.fromString("0000FFF0-0000-1000-8000-00805F9B34FB")
        private val CHAR_UUID = UUID.fromString("0000FFF1-0000-1000-8000-00805F9B34FB")
    }

    private var bluetoothGatt: BluetoothGatt? = null
    private var characteristic: BluetoothGattCharacteristic? = null
    private var isConnected = false

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    isConnected = true
                    Log.d(TAG, "BLE connected")
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    isConnected = false
                    Log.d(TAG, "BLE disconnected")
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt.getService(SERVICE_UUID)
                characteristic = service?.getCharacteristic(CHAR_UUID)
                Log.d(TAG, "Service discovered")
            }
        }
    }

    fun connect() {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = bluetoothManager.adapter
        if (adapter == null || !adapter.isEnabled) {
            Log.e(TAG, "Bluetooth not enabled")
            return
        }
        val device = adapter.getRemoteDevice(ESP32_MAC)
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
    }

    fun sendDelta(dx: Int, dy: Int) {
        if (!isConnected || characteristic == null) return
        val packet = byteArrayOf(
            (dx shr 8).toByte(), dx.toByte(),
            (dy shr 8).toByte(), dy.toByte()
        )
        characteristic?.value = packet
        bluetoothGatt?.writeCharacteristic(characteristic)
    }

    fun close() {
        bluetoothGatt?.close()
        bluetoothGatt = null
        characteristic = null
        isConnected = false
    }
}
