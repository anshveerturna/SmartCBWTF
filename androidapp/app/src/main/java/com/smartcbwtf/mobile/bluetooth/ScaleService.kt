package com.smartcbwtf.mobile.bluetooth

import android.bluetooth.BluetoothDevice
import kotlinx.coroutines.flow.StateFlow

interface ScaleService {
    val weight: StateFlow<Double?>
    val connectionState: StateFlow<ConnectionState>
    val discoveredDevices: StateFlow<List<BluetoothDevice>>
    
    suspend fun connect()
    suspend fun disconnect()
    suspend fun startScan()
    suspend fun stopScan()
    suspend fun connectToDevice(device: BluetoothDevice)

    suspend fun simulateWeight(value: Double? = null) { /* optional for mocks */ }
}

enum class ConnectionState {
    DISCONNECTED,
    SCANNING,
    CONNECTING,
    CONNECTED,
    ERROR
}
