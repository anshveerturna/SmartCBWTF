package com.smartcbwtf.mobile.bluetooth

import kotlinx.coroutines.flow.Flow

interface ScaleService {
    val weight: Flow<Double?>
    val connectionState: Flow<ConnectionState>
    
    suspend fun connect()
    suspend fun disconnect()
    suspend fun startScan()
    suspend fun stopScan()
}

enum class ConnectionState {
    DISCONNECTED,
    SCANNING,
    CONNECTING,
    CONNECTED,
    ERROR
}
