package com.smartcbwtf.mobile.bluetooth

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockScaleService @Inject constructor() : ScaleService {

    private val _weight = MutableStateFlow<Double?>(null)
    override val weight: StateFlow<Double?> = _weight.asStateFlow()

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    override suspend fun connect() {
        _connectionState.value = ConnectionState.CONNECTING
        delay(1000)
        _connectionState.value = ConnectionState.CONNECTED
        simulateWeight()
    }

    override suspend fun disconnect() {
        _connectionState.value = ConnectionState.DISCONNECTED
        _weight.value = null
    }

    override suspend fun startScan() {
        _connectionState.value = ConnectionState.SCANNING
        delay(2000)
        // Auto connect in mock
        connect()
    }

    override suspend fun stopScan() {
        if (_connectionState.value == ConnectionState.SCANNING) {
            _connectionState.value = ConnectionState.DISCONNECTED
        }
    }

    private suspend fun simulateWeight() {
        while (_connectionState.value == ConnectionState.CONNECTED) {
            delay(2000)
            // Simulate random weight between 5.0 and 50.0
            val randomWeight = 5.0 + (Math.random() * 45.0)
            _weight.value = String.format("%.2f", randomWeight).toDouble()
        }
    }
}
