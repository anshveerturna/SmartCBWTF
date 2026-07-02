package com.smartcbwtf.mobile.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real Bluetooth Scale Service that supports Classic Bluetooth (SPP) devices.
 * Most industrial weighing scales use Classic Bluetooth with Serial Port Profile.
 * 
 * Target device info:
 * - Address: 00:24:09:00:B4:55
 * - Services: ACL (Classic Bluetooth)
 */
@Singleton
class RealBluetoothScaleService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val permissionHelper: PermissionHelper
) : ScaleService {

    companion object {
        private const val TAG = "RealBTScaleService"
        // Standard Serial Port Profile (SPP) UUID for Classic Bluetooth
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

    private val _weight = MutableStateFlow<Double?>(null)
    override val weight: StateFlow<Double?> = _weight.asStateFlow()

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    override val discoveredDevices: StateFlow<List<BluetoothDevice>> = _discoveredDevices.asStateFlow()

    private var bluetoothSocket: BluetoothSocket? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private var connectionJob: Job? = null
    private var readJob: Job? = null
    private val recentWeights = ArrayDeque<Double>()
    private val discoveredDeviceSet = mutableSetOf<String>()
    private var isReceiverRegistered = false

    // BroadcastReceiver for Classic Bluetooth device discovery
    private val discoveryReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    
                    device?.let {
                        val deviceAddress = it.address ?: return
                        val deviceName = try {
                            it.name?.takeIf { name -> name.isNotBlank() } ?: "Bluetooth Device"
                        } catch (e: SecurityException) {
                            "Bluetooth Device"
                        }
                        val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE)
                        
                        if (deviceAddress !in discoveredDeviceSet) {
                            discoveredDeviceSet.add(deviceAddress)
                            val currentList = _discoveredDevices.value.toMutableList()
                            currentList.add(it)
                            _discoveredDevices.value = currentList
                            Log.d(TAG, "Discovered Classic BT device: $deviceName ($deviceAddress), RSSI: $rssi")
                        }
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    Log.d(TAG, "Bluetooth discovery started")
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    Log.d(TAG, "Bluetooth discovery finished. Found ${_discoveredDevices.value.size} devices")
                    // Also add bonded (paired) devices that might not show up in discovery
                    addBondedDevices()
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun addBondedDevices() {
        try {
            val bondedDevices = bluetoothAdapter?.bondedDevices ?: return
            for (device in bondedDevices) {
                val deviceAddress = device.address ?: continue
                if (deviceAddress !in discoveredDeviceSet) {
                    discoveredDeviceSet.add(deviceAddress)
                    val currentList = _discoveredDevices.value.toMutableList()
                    // Add bonded devices at the top of the list
                    currentList.add(0, device)
                    _discoveredDevices.value = currentList
                    val deviceName = try { device.name?.takeIf { it.isNotBlank() } ?: "Paired Device" } catch (e: SecurityException) { "Paired Device" }
                    Log.d(TAG, "Added bonded device: $deviceName ($deviceAddress)")
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException accessing bonded devices", e)
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun startScan() {
        Log.d(TAG, "startScan() called - Using Classic Bluetooth discovery")
        
        if (!permissionHelper.hasBluetoothPermissions()) {
            Log.e(TAG, "Missing Bluetooth permissions")
            _connectionState.value = ConnectionState.ERROR
            return
        }
        if (bluetoothAdapter == null) {
            Log.e(TAG, "BluetoothAdapter is null - device may not support Bluetooth")
            _connectionState.value = ConnectionState.ERROR
            return
        }
        if (!bluetoothAdapter.isEnabled) {
            Log.e(TAG, "Bluetooth is not enabled")
            _connectionState.value = ConnectionState.ERROR
            return
        }

        // Clear previous discovered devices
        discoveredDeviceSet.clear()
        _discoveredDevices.value = emptyList()
        _connectionState.value = ConnectionState.SCANNING

        // Register receiver for device discovery
        if (!isReceiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_FOUND)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(discoveryReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(discoveryReceiver, filter)
            }
            isReceiverRegistered = true
        }

        // First add already bonded/paired devices (they appear instantly)
        addBondedDevices()

        // Start discovery for new devices
        try {
            if (bluetoothAdapter.isDiscovering) {
                bluetoothAdapter.cancelDiscovery()
            }
            val started = bluetoothAdapter.startDiscovery()
            Log.d(TAG, "Classic Bluetooth discovery started: $started")
            
            if (!started) {
                Log.e(TAG, "Failed to start Bluetooth discovery")
                // Even if discovery fails, we still have bonded devices
                if (_discoveredDevices.value.isEmpty()) {
                    _connectionState.value = ConnectionState.ERROR
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception starting discovery", e)
            _connectionState.value = ConnectionState.ERROR
        }

        // Auto-stop discovery after 15 seconds
        scope.launch {
            delay(15000)
            if (_connectionState.value == ConnectionState.SCANNING) {
                try {
                    bluetoothAdapter.cancelDiscovery()
                } catch (e: Exception) {
                    Log.e(TAG, "Error canceling discovery", e)
                }
                if (_discoveredDevices.value.isEmpty()) {
                    _connectionState.value = ConnectionState.DISCONNECTED
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun stopScan() {
        Log.d(TAG, "stopScan() called")
        try {
            bluetoothAdapter?.cancelDiscovery()
        } catch (e: Exception) {
            Log.e(TAG, "Error canceling discovery", e)
        }
        
        if (_connectionState.value == ConnectionState.SCANNING) {
            _connectionState.value = ConnectionState.DISCONNECTED
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun connectToDevice(device: BluetoothDevice) {
        Log.d(TAG, "connectToDevice() called for ${device.address}")
        
        // Cancel discovery as it slows down connection
        try {
            bluetoothAdapter?.cancelDiscovery()
        } catch (e: Exception) {
            Log.e(TAG, "Error canceling discovery before connect", e)
        }

        _connectionState.value = ConnectionState.CONNECTING
        
        connectionJob?.cancel()
        connectionJob = scope.launch {
            try {
                // Close any existing connection
                bluetoothSocket?.close()
                bluetoothSocket = null

                val deviceName = try { device.name ?: "Unknown" } catch (e: SecurityException) { "Unknown" }
                Log.d(TAG, "Attempting SPP connection to $deviceName (${device.address})")

                // Create socket using SPP UUID
                val socket = try {
                    device.createRfcommSocketToServiceRecord(SPP_UUID)
                } catch (e: Exception) {
                    Log.w(TAG, "createRfcommSocketToServiceRecord failed, trying fallback method", e)
                    // Fallback: use reflection to create socket on channel 1
                    try {
                        val method = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                        method.invoke(device, 1) as BluetoothSocket
                    } catch (e2: Exception) {
                        Log.e(TAG, "Fallback socket creation also failed", e2)
                        throw e
                    }
                }

                Log.d(TAG, "Connecting socket...")
                socket.connect()
                
                bluetoothSocket = socket
                _connectionState.value = ConnectionState.CONNECTED
                Log.d(TAG, "Connected successfully to $deviceName")

                // Start reading data from the scale
                startReadingData(socket)

            } catch (e: IOException) {
                Log.e(TAG, "Connection failed: ${e.message}", e)
                _connectionState.value = ConnectionState.ERROR
                bluetoothSocket?.close()
                bluetoothSocket = null
            } catch (e: SecurityException) {
                Log.e(TAG, "Security exception during connection", e)
                _connectionState.value = ConnectionState.ERROR
            }
        }
    }

    private fun startReadingData(socket: BluetoothSocket) {
        readJob?.cancel()
        readJob = scope.launch {
            val inputStream: InputStream = socket.inputStream
            val outputStream = socket.outputStream
            val buffer = ByteArray(1024)
            val dataBuilder = StringBuilder()

            Log.d(TAG, "Starting to read data from scale...")
            
            // Some scales need a command to start sending data
            // Common commands: "P" (print), "W" (weight), "R" (read), "\r\n", "?"
            // Try sending a weight request command
            try {
                Log.d(TAG, "Sending weight request commands to scale...")
                // Try common commands - the scale will ignore unknown ones
                outputStream.write("W\r\n".toByteArray())
                outputStream.flush()
                delay(100)
                outputStream.write("P\r\n".toByteArray())
                outputStream.flush()
                delay(100)
                outputStream.write("\r\n".toByteArray())
                outputStream.flush()
            } catch (e: Exception) {
                Log.w(TAG, "Could not send initial command to scale (this may be normal): ${e.message}")
            }

            while (isActive && _connectionState.value == ConnectionState.CONNECTED) {
                try {
                    // Check if data is available
                    val available = inputStream.available()
                    if (available > 0) {
                        Log.d(TAG, "Data available: $available bytes")
                    }
                    
                    val bytesRead = inputStream.read(buffer)
                    if (bytesRead > 0) {
                        val hexData = buffer.take(bytesRead).joinToString(" ") { String.format("%02X", it) }
                        val data = String(buffer, 0, bytesRead)
                        Log.d(TAG, "📥 Raw data received ($bytesRead bytes): $hexData")
                        Log.d(TAG, "📥 String data: '$data'")
                        
                        dataBuilder.append(data)
                        
                        // Process complete lines (most scales send data line by line with \r\n or \n)
                        while (dataBuilder.contains("\n") || dataBuilder.contains("\r")) {
                            val newlineIdx = dataBuilder.indexOfFirst { it == '\n' || it == '\r' }
                            if (newlineIdx < 0) break
                            
                            val line = dataBuilder.substring(0, newlineIdx).trim()
                            // Skip past any consecutive newline characters
                            var endIdx = newlineIdx + 1
                            while (endIdx < dataBuilder.length && (dataBuilder[endIdx] == '\n' || dataBuilder[endIdx] == '\r')) {
                                endIdx++
                            }
                            dataBuilder.delete(0, endIdx)
                            
                            if (line.isNotEmpty()) {
                                Log.d(TAG, "Processing line: '$line'")
                                parseWeight(line)
                            }
                        }
                        
                        // Also try to parse if buffer gets too large (some scales don't send newlines)
                        if (dataBuilder.length > 20) {
                            Log.d(TAG, "Buffer has data, attempting to parse: '${dataBuilder}'")
                            parseWeight(dataBuilder.toString())
                            dataBuilder.clear()
                        }
                        
                        // Also try parsing directly without waiting for newlines
                        // This helps with scales that send continuous data
                        if (data.length >= 3) {
                            parseWeight(data)
                        }
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "Error reading from socket: ${e.message}")
                    withContext(Dispatchers.Main) {
                        if (_connectionState.value == ConnectionState.CONNECTED) {
                            _connectionState.value = ConnectionState.DISCONNECTED
                        }
                    }
                    break
                }
            }
            Log.d(TAG, "Stopped reading data from scale")
        }
    }

    override suspend fun connect() {
        // Start scanning - UI will handle device selection
        startScan()
    }

    @SuppressLint("MissingPermission")
    override suspend fun disconnect() {
        Log.d(TAG, "disconnect() called")
        
        readJob?.cancel()
        readJob = null
        connectionJob?.cancel()
        connectionJob = null
        
        try {
            bluetoothSocket?.close()
        } catch (e: IOException) {
            Log.e(TAG, "Error closing socket", e)
        }
        bluetoothSocket = null
        
        _connectionState.value = ConnectionState.DISCONNECTED
        _weight.value = null
        recentWeights.clear()
    }

    private fun parseWeight(data: String) {
        // Supported formats from various scales:
        // - "ST,GS,+   5.0kg" (common industrial scales)
        // - "ST,NT,+  12.34 kg"
        // - "5.00 kg"
        // - "5.00kg"
        // - "  +5.00 kg"
        // - "5.00"
        // - "wt:5.00kg"
        // - "W=5.00kg"
        // - "US,GS,+   0.00kg" (unstable reading)
        // - "OL" (overload)
        // - Raw numbers like "  123" or "45.6"
        Log.d(TAG, "Parsing weight from: '$data'")
        
        // Check for special states
        if (data.contains("OL", ignoreCase = true)) {
            Log.w(TAG, "Scale reports OVERLOAD")
            return
        }

        try {
            // Try multiple regex patterns - most specific to most general
            val patterns = listOf(
                Regex("[SsUu][TtSsNn],.*?([+-]?\\s*[0-9]+\\.?[0-9]*)\\s*(?:kg|KG|g|G|lb|LB)?"),  // ST,GS or US,GS or ST,NT format
                Regex("[Ww][Tt=:]\\s*([0-9]+\\.?[0-9]*)"),  // wt: or W= format
                Regex("([+-]?\\s*[0-9]+\\.[0-9]+)\\s*(?:kg|KG|g|G|lb|LB)"),  // Decimal with unit
                Regex("([0-9]+\\.[0-9]+)"),  // Simple decimal like 5.00
                Regex("([0-9]+)"),  // Integer only like 123
            )

            for (pattern in patterns) {
                val match = pattern.find(data)
                if (match != null) {
                    val weightStr = match.groupValues[1].replace("\\s".toRegex(), "").replace("+", "").replace("-", "")
                    val value = weightStr.toDoubleOrNull()
                    if (value != null && value >= 0 && value < 1000) {  // Sanity check: 0-1000kg range
                        Log.d(TAG, "✓ Parsed weight: $value kg (pattern: ${pattern.pattern})")
                        updateWeight(value)
                        return
                    }
                }
            }
            
            // Last resort: try to find ANY number in the string
            val anyNumber = Regex("(\\d+\\.?\\d*)").find(data)
            if (anyNumber != null) {
                val value = anyNumber.groupValues[1].toDoubleOrNull()
                if (value != null && value >= 0 && value < 1000) {
                    Log.d(TAG, "✓ Parsed weight (fallback): $value kg")
                    updateWeight(value)
                    return
                }
            }
            
            Log.w(TAG, "✗ Could not parse weight from data: '$data'")
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing weight", e)
        }
    }

    private fun updateWeight(value: Double) {
        // ALWAYS update weight immediately for instant UI feedback
        _weight.value = value
        Log.d(TAG, "⚖️ Weight updated to: $value kg")
        
        // Track for stability detection (for future use if needed)
        recentWeights.addLast(value)
        if (recentWeights.size > 5) recentWeights.removeFirst()

        // Log stability status
        if (recentWeights.size >= 3) {
            val tail = recentWeights.takeLast(3)
            val max = tail.maxOrNull() ?: value
            val min = tail.minOrNull() ?: value
            if (max - min <= 0.05) {
                Log.d(TAG, "Weight is STABLE (variance: ${max - min})")
            } else {
                Log.d(TAG, "Weight is fluctuating (variance: ${max - min})")
            }
        }
    }

    // Clean up when service is destroyed
    fun cleanup() {
        scope.launch {
            disconnect()
        }
        
        if (isReceiverRegistered) {
            try {
                context.unregisterReceiver(discoveryReceiver)
                isReceiverRegistered = false
            } catch (e: Exception) {
                Log.e(TAG, "Error unregistering receiver", e)
            }
        }
    }
}
