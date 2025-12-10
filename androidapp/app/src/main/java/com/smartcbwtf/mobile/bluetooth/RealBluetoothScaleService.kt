package com.smartcbwtf.mobile.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.util.Log
import com.smartcbwtf.mobile.utils.PermissionHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RealBluetoothScaleService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val permissionHelper: PermissionHelper
) : ScaleService {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter = bluetoothManager.adapter

    private val _weight = MutableStateFlow<Double?>(null)
    override val weight: StateFlow<Double?> = _weight.asStateFlow()

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    override val discoveredDevices: StateFlow<List<BluetoothDevice>> = _discoveredDevices.asStateFlow()

    private var bluetoothGatt: BluetoothGatt? = null
    private var scanCallback: ScanCallback? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private var connectionJob: Job? = null
    private val recentWeights = ArrayDeque<Double>()
    private val discoveredDeviceSet = mutableSetOf<String>() // Track by address to avoid duplicates

    // Known UUIDs for common BLE scales - replace with actual device UUIDs
    private val SERVICE_UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
    private val CHARACTERISTIC_UUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")

    @SuppressLint("MissingPermission")
    override suspend fun startScan() {
        if (!permissionHelper.hasBluetoothPermissions() || bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            _connectionState.value = ConnectionState.ERROR
            return
        }

        // Clear previous discovered devices
        discoveredDeviceSet.clear()
        _discoveredDevices.value = emptyList()
        _connectionState.value = ConnectionState.SCANNING
        
        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device
                val deviceAddress = device.address
                val deviceName = device.name
                
                // Add device if it has a name and hasn't been discovered yet
                if (deviceName != null && deviceAddress !in discoveredDeviceSet) {
                    discoveredDeviceSet.add(deviceAddress)
                    val currentList = _discoveredDevices.value.toMutableList()
                    currentList.add(device)
                    _discoveredDevices.value = currentList
                    Log.d("RealBluetoothScaleService", "Discovered device: $deviceName ($deviceAddress)")
                }
            }

            override fun onScanFailed(errorCode: Int) {
                Log.e("RealBluetoothScaleService", "Scan failed with error code: $errorCode")
                _connectionState.value = ConnectionState.ERROR
            }
        }

        bluetoothAdapter.bluetoothLeScanner.startScan(scanCallback)
        
        // Timeout scan after 15 seconds
        scope.launch {
            delay(15000)
            if (_connectionState.value == ConnectionState.SCANNING) {
                stopScan()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun stopScanInternal() {
        scanCallback?.let {
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(it)
        }
        scanCallback = null
    }

    override suspend fun stopScan() {
        stopScanInternal()
        if (_connectionState.value == ConnectionState.SCANNING) {
            _connectionState.value = ConnectionState.DISCONNECTED
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun connectToDevice(device: BluetoothDevice) {
        stopScanInternal()
        _connectionState.value = ConnectionState.CONNECTING
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
    }

    override suspend fun connect() {
        // Start scanning - UI will handle device selection
        startScan()
    }

    @SuppressLint("MissingPermission")
    override suspend fun disconnect() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        _connectionState.value = ConnectionState.DISCONNECTED
        _weight.value = null
    }

    override suspend fun simulateWeight(value: Double?) {
        // No-op for real hardware
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                _connectionState.value = ConnectionState.CONNECTED
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                _connectionState.value = ConnectionState.DISCONNECTED
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt.getService(SERVICE_UUID)
                val characteristic = service?.getCharacteristic(CHARACTERISTIC_UUID)
                if (characteristic != null) {
                    gatt.setCharacteristicNotification(characteristic, true)
                }
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            val data = characteristic.value
            val stringData = String(data)
            parseWeight(stringData)
        }
    }

    private fun parseWeight(data: String) {
        // Example format: "ST,GS,+   5.0kg"
        try {
            // Simple regex to find a decimal number followed by kg
            val regex = Regex("([0-9]+\\.[0-9]+)")
            val match = regex.find(data)
            match?.let {
                val weightStr = it.groupValues[1]
                val value = weightStr.toDouble()
                recentWeights.addLast(value)
                if (recentWeights.size > 5) recentWeights.removeFirst()

                // consider stable if last 3 readings within 0.05 kg
                if (recentWeights.size >= 3) {
                    val tail = recentWeights.takeLast(3)
                    val max = tail.maxOrNull() ?: value
                    val min = tail.minOrNull() ?: value
                    if (max - min <= 0.05) {
                        _weight.value = tail.last()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("RealBluetoothScaleService", "Error parsing weight", e)
        }
    }
}
