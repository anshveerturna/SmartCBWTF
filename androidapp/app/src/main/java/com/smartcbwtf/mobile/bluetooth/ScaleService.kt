package com.smartcbwtf.mobile.bluetooth

import kotlinx.coroutines.flow.StateFlow

interface ScaleService {
    val weight: StateFlow<Double?>
    val connectionState: StateFlow<ConnectionState>
    
    suspend fun connect()
    suspend fun disconnect()
    suspend fun startScan()
    suspend fun stopScan()

    suspend fun simulateWeight(value: Double? = null) { /* optional for mocks */ }
}

enum class ConnectionState {
    DISCONNECTED,
    SCANNING,
    CONNECTING,
    CONNECTED,
    ERROR
}
