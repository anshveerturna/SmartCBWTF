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
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import com.smartcbwtf.mobile.utils.PermissionHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets
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
    private val permissionHelper: PermissionHelper,
    private val preferences: SharedPreferences
) : ScaleService {

    companion object {
        private const val TAG = "RealBTScaleService"
        private const val PREF_SCALE_ADDRESS = "bluetooth_scale_address"
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

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val socketLock = Any()
    @Volatile private var bluetoothSocket: BluetoothSocket? = null
    @Volatile private var pendingSocket: BluetoothSocket? = null
    @Volatile private var manualDisconnect = true
    private var connectionJob: Job? = null
    private var readJob: Job? = null
    private var reconnectJob: Job? = null
    private var scanTimeoutJob: Job? = null
    private val recentWeights = ArrayDeque<Double>()
    private val discoveredDeviceSet = mutableSetOf<String>()
    private var isReceiverRegistered = false

    private val bluetoothReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device = intent.bluetoothDeviceExtra()
                    
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
                    addBondedDevices()
                    if (_connectionState.value == ConnectionState.SCANNING) {
                        _connectionState.value = ConnectionState.DISCONNECTED
                    }
                }
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                        BluetoothAdapter.STATE_OFF -> handleAdapterTurnedOff()
                        BluetoothAdapter.STATE_ON -> {
                            if (!manualDisconnect && rememberedDevice() != null) {
                                scheduleReconnect(immediate = true)
                            }
                        }
                    }
                }
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    val device = intent.bluetoothDeviceExtra()
                    val savedAddress = preferences.getString(PREF_SCALE_ADDRESS, null)
                    if (device?.address == savedAddress && _connectionState.value == ConnectionState.CONNECTED) {
                        handleConnectionLost(bluetoothSocket, "ACL disconnected")
                    }
                }
            }
        }
    }

    init {
        ensureReceiverRegistered()
    }

    private fun Intent.bluetoothDeviceExtra(): BluetoothDevice? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        }

    private fun ensureReceiverRegistered() {
        if (isReceiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(bluetoothReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(bluetoothReceiver, filter)
            }
            isReceiverRegistered = true
        } catch (e: Exception) {
            Log.e(TAG, "Unable to register Bluetooth receiver", e)
        }
    }

    @SuppressLint("MissingPermission")
    private fun addBondedDevices() {
        try {
            for (device in bondedDevices()) {
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
    override fun bondedDevices(): List<BluetoothDevice> {
        if (!permissionHelper.hasBluetoothPermissions()) return emptyList()
        return try {
            bluetoothAdapter?.bondedDevices
                ?.sortedWith(
                    compareByDescending<BluetoothDevice> { device ->
                        runCatching { device.name?.contains("HC-05", ignoreCase = true) == true }
                            .getOrDefault(false)
                    }.thenBy { device -> runCatching { device.name }.getOrNull().orEmpty() }
                )
                .orEmpty()
        } catch (e: SecurityException) {
            Log.e(TAG, "Unable to read bonded Bluetooth devices", e)
            emptyList()
        }
    }

    @SuppressLint("MissingPermission")
    override fun rememberedDevice(): BluetoothDevice? {
        if (!permissionHelper.hasBluetoothPermissions()) return null
        val savedAddress = preferences.getString(PREF_SCALE_ADDRESS, null) ?: return null
        val isStillBonded = bondedDevices().any { it.address.equals(savedAddress, ignoreCase = true) }
        if (!isStillBonded) {
            Log.w(TAG, "Remembered scale is no longer bonded; clearing it")
            forgetRememberedDevice()
            return null
        }
        return try {
            bluetoothAdapter?.getRemoteDevice(savedAddress)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Remembered scale address is invalid; clearing it", e)
            forgetRememberedDevice()
            null
        }
    }

    override fun forgetRememberedDevice() {
        preferences.edit().remove(PREF_SCALE_ADDRESS).apply()
    }

    @SuppressLint("MissingPermission")
    private fun rememberDeviceIfBonded(device: BluetoothDevice) {
        if (runCatching { device.bondState == BluetoothDevice.BOND_BONDED }.getOrDefault(false)) {
            preferences.edit().putString(PREF_SCALE_ADDRESS, device.address).apply()
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

        ensureReceiverRegistered()

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
        scanTimeoutJob?.cancel()
        scanTimeoutJob = scope.launch {
            delay(15000)
            if (_connectionState.value == ConnectionState.SCANNING) {
                try {
                    bluetoothAdapter.cancelDiscovery()
                } catch (e: Exception) {
                    Log.e(TAG, "Error canceling discovery", e)
                }
                _connectionState.value = ConnectionState.DISCONNECTED
            }
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun stopScan() {
        Log.d(TAG, "stopScan() called")
        scanTimeoutJob?.cancel()
        scanTimeoutJob = null
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
        manualDisconnect = false
        reconnectJob?.cancel()
        reconnectJob = null
        rememberDeviceIfBonded(device)
        launchConnection(device, scheduleRetryOnFailure = true)
    }

    @SuppressLint("MissingPermission")
    private fun launchConnection(device: BluetoothDevice, scheduleRetryOnFailure: Boolean): Job {
        ensureReceiverRegistered()
        scanTimeoutJob?.cancel()
        runCatching { bluetoothAdapter?.cancelDiscovery() }

        connectionJob?.cancel()
        closePendingSocket()
        closeConnectedSocket()
        readJob?.cancel()
        readJob = null
        _weight.value = null
        recentWeights.clear()
        _connectionState.value = ConnectionState.CONNECTING

        return scope.launch {
            var socket: BluetoothSocket? = null
            try {
                val deviceName = runCatching { device.name ?: "HC-05" }.getOrDefault("HC-05")
                Log.d(TAG, "Attempting SPP connection to $deviceName (${device.address})")

                val newSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                socket = newSocket
                synchronized(socketLock) { pendingSocket = newSocket }
                newSocket.connect()

                if (!isActive || manualDisconnect) {
                    newSocket.close()
                    return@launch
                }

                synchronized(socketLock) {
                    pendingSocket = null
                    bluetoothSocket = newSocket
                }
                rememberDeviceIfBonded(device)
                _connectionState.value = ConnectionState.CONNECTED
                Log.d(TAG, "Connected successfully to $deviceName")
                startReadingData(newSocket)
            } catch (e: CancellationException) {
                closeQuietly(socket)
                throw e
            } catch (e: IOException) {
                Log.w(TAG, "SPP connection failed: ${e.message}")
                closeQuietly(socket)
                clearSocketReference(socket)
                if (!manualDisconnect) {
                    _connectionState.value = ConnectionState.ERROR
                    if (scheduleRetryOnFailure) scheduleReconnect()
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "Bluetooth permission was revoked during connection", e)
                closeQuietly(socket)
                clearSocketReference(socket)
                _connectionState.value = ConnectionState.ERROR
            }
        }.also { connectionJob = it }
    }

    private fun startReadingData(socket: BluetoothSocket) {
        readJob?.cancel()
        readJob = scope.launch {
            try {
                val inputStream: InputStream = socket.inputStream
                val outputStream = socket.outputStream
                val buffer = ByteArray(1024)
                val dataBuilder = StringBuilder()

                Log.d(TAG, "Starting to read data from scale...")

                try {
                    outputStream.write("W\r\n".toByteArray(StandardCharsets.US_ASCII))
                    outputStream.flush()
                    delay(100)
                    outputStream.write("P\r\n".toByteArray(StandardCharsets.US_ASCII))
                    outputStream.flush()
                    delay(100)
                    outputStream.write("\r\n".toByteArray(StandardCharsets.US_ASCII))
                    outputStream.flush()
                } catch (e: Exception) {
                    Log.w(TAG, "Could not send initial command to scale: ${e.message}")
                }

                while (isActive && _connectionState.value == ConnectionState.CONNECTED) {
                    val bytesRead = inputStream.read(buffer)
                    if (bytesRead < 0) throw EOFException("HC-05 closed the RFCOMM stream")
                    if (bytesRead > 0) {
                        val hexData = buffer.take(bytesRead).joinToString(" ") { String.format("%02X", it) }
                        val data = String(buffer, 0, bytesRead, StandardCharsets.US_ASCII)
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
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                Log.w(TAG, "Scale connection lost: ${e.message}")
                handleConnectionLost(socket, e.message ?: "read failed")
            } catch (e: SecurityException) {
                Log.e(TAG, "Bluetooth permission was revoked while reading", e)
                handleConnectionLost(socket, "Bluetooth permission revoked")
            }
            Log.d(TAG, "Stopped reading data from scale")
        }
    }

    override suspend fun connect() {
        manualDisconnect = false
        val device = rememberedDevice()
        if (device == null) {
            _connectionState.value = ConnectionState.DISCONNECTED
            return
        }
        launchConnection(device, scheduleRetryOnFailure = true)
    }

    @SuppressLint("MissingPermission")
    override suspend fun disconnect() {
        Log.d(TAG, "disconnect() called")
        manualDisconnect = true
        reconnectJob?.cancel()
        reconnectJob = null
        scanTimeoutJob?.cancel()
        scanTimeoutJob = null
        readJob?.cancel()
        readJob = null
        connectionJob?.cancel()
        connectionJob = null
        closePendingSocket()
        closeConnectedSocket()
        _connectionState.value = ConnectionState.DISCONNECTED
        _weight.value = null
        recentWeights.clear()
    }

    private fun handleAdapterTurnedOff() {
        closePendingSocket()
        closeConnectedSocket()
        readJob?.cancel()
        connectionJob?.cancel()
        _weight.value = null
        recentWeights.clear()
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    private fun handleConnectionLost(socket: BluetoothSocket?, reason: String) {
        if (socket != null && bluetoothSocket !== socket) return
        Log.w(TAG, "Bluetooth scale disconnected: $reason")
        closeQuietly(socket)
        clearSocketReference(socket)
        _weight.value = null
        recentWeights.clear()
        _connectionState.value = ConnectionState.DISCONNECTED
        if (!manualDisconnect) scheduleReconnect()
    }

    private fun scheduleReconnect(immediate: Boolean = false) {
        if (manualDisconnect || reconnectJob?.isActive == true) return
        reconnectJob = scope.launch {
            var attempt = 0
            if (!immediate) delay(ReconnectPolicy.delayForAttempt(attempt++))
            while (isActive && !manualDisconnect) {
                val adapter = bluetoothAdapter
                val device = rememberedDevice()
                if (adapter?.isEnabled == true && device != null) {
                    Log.d(TAG, "Reconnecting to remembered HC-05 (attempt ${attempt + 1})")
                    val job = launchConnection(device, scheduleRetryOnFailure = false)
                    job.join()
                    if (_connectionState.value == ConnectionState.CONNECTED || manualDisconnect) {
                        return@launch
                    }
                }
                delay(ReconnectPolicy.delayForAttempt(attempt++))
            }
        }
    }

    private fun closePendingSocket() {
        val socket = synchronized(socketLock) {
            pendingSocket.also { pendingSocket = null }
        }
        closeQuietly(socket)
    }

    private fun closeConnectedSocket() {
        val socket = synchronized(socketLock) {
            bluetoothSocket.also { bluetoothSocket = null }
        }
        closeQuietly(socket)
    }

    private fun clearSocketReference(socket: BluetoothSocket?) {
        synchronized(socketLock) {
            if (pendingSocket === socket) pendingSocket = null
            if (bluetoothSocket === socket) bluetoothSocket = null
        }
    }

    private fun closeQuietly(socket: BluetoothSocket?) {
        try {
            socket?.close()
        } catch (e: IOException) {
            Log.w(TAG, "Unable to close Bluetooth socket", e)
        }
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
        manualDisconnect = true
        reconnectJob?.cancel()
        connectionJob?.cancel()
        readJob?.cancel()
        scanTimeoutJob?.cancel()
        closePendingSocket()
        closeConnectedSocket()

        if (isReceiverRegistered) {
            try {
                context.unregisterReceiver(bluetoothReceiver)
                isReceiverRegistered = false
            } catch (e: Exception) {
                Log.e(TAG, "Error unregistering receiver", e)
            }
        }
        scope.cancel()
    }
}
