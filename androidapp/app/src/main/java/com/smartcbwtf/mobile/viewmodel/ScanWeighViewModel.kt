package com.smartcbwtf.mobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartcbwtf.mobile.bluetooth.ConnectionState
import com.smartcbwtf.mobile.bluetooth.ScaleService
import com.smartcbwtf.mobile.model.BagEvent
import com.smartcbwtf.mobile.repository.BagEventRepository
import com.smartcbwtf.mobile.utils.LocationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * Data class representing a single bag entry in the pickup session
 */
data class BagEntry(
    val qrCode: String,
    val weightKg: Double,
    val gpsLat: Double,
    val gpsLon: Double,
    val timestamp: Long
)

/**
 * State representing the current pickup session
 */
data class PickupSessionState(
    val bags: List<BagEntry> = emptyList(),
    val totalBags: Int = 0,
    val totalWeight: Double = 0.0
) {
    val hasUnsavedBags: Boolean get() = bags.isNotEmpty()
}

@HiltViewModel
class ScanWeighViewModel @Inject constructor(
    private val scaleService: ScaleService,
    private val bagEventRepository: BagEventRepository,
    private val locationHelper: LocationHelper
) : ViewModel() {

    val weight = scaleService.weight
    val connectionState = scaleService.connectionState
    val discoveredDevices = scaleService.discoveredDevices

    private val _scannedQr = MutableStateFlow<String?>(null)
    val scannedQr: StateFlow<String?> = _scannedQr.asStateFlow()

    private val _qrError = MutableStateFlow<String?>(null)
    val qrError: StateFlow<String?> = _qrError.asStateFlow()

    private val _submissionState = MutableStateFlow<SubmissionState>(SubmissionState.Idle)
    val submissionState: StateFlow<SubmissionState> = _submissionState.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _location = MutableStateFlow<LocationState>(LocationState.Waiting)
    val location: StateFlow<LocationState> = _location.asStateFlow()

    // Session state for multi-bag workflow
    private val _sessionState = MutableStateFlow(PickupSessionState())
    val sessionState: StateFlow<PickupSessionState> = _sessionState.asStateFlow()

    // Can add bag: valid QR + weight > 0 + GPS ready + no QR error
    val canAddBag = combine(
        scannedQr,
        weight,
        location,
        qrError
    ) { qr, w, loc, qrErr ->
        !qr.isNullOrBlank() && qrErr == null && w != null && w > 0.0 && loc is LocationState.Ready
    }

    // Can submit all: session has bags
    val canSubmitAll = _sessionState.asStateFlow()
        .let { flow ->
            combine(flow, _submissionState) { session, subState ->
                session.bags.isNotEmpty() && subState !is SubmissionState.Loading
            }
        }

    // Legacy single submit enabled (kept for backward compatibility)
    val isSubmitEnabled = combine(
        scannedQr,
        weight,
        location,
        qrError,
        submissionState
    ) { qr, w, loc, qrErr, subState ->
        !qr.isNullOrBlank() && qrErr == null && w != null && w > 0.0 && loc is LocationState.Ready && subState !is SubmissionState.Loading
    }

    fun connectScale() {
        viewModelScope.launch {
            _error.value = null
            try {
                scaleService.connect()
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun connectToDevice(device: android.bluetooth.BluetoothDevice) {
        viewModelScope.launch {
            _error.value = null
            try {
                scaleService.connectToDevice(device)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun stopScanning() {
        viewModelScope.launch {
            scaleService.stopScan()
        }
    }

    fun disconnectScale() {
        viewModelScope.launch {
            scaleService.disconnect()
        }
    }

    fun onQrScanned(qr: String) {
        if (isValidQr(qr)) {
            _qrError.value = null
            _scannedQr.value = qr
        } else {
            _qrError.value = "Invalid QR format"
            _scannedQr.value = null
        }
    }

    /**
     * Add current bag to the session list
     */
    fun addBag(): Boolean {
        val qr = _scannedQr.value
        val w = weight.value
        val loc = _location.value as? LocationState.Ready

        if (qr == null || w == null || w <= 0.0 || loc == null) {
            _error.value = "Cannot add bag: missing QR, weight, or GPS"
            return false
        }

        val entry = BagEntry(
            qrCode = qr,
            weightKg = w,
            gpsLat = loc.lat,
            gpsLon = loc.lon,
            timestamp = System.currentTimeMillis()
        )

        val currentState = _sessionState.value
        val newBags = currentState.bags + entry
        _sessionState.value = PickupSessionState(
            bags = newBags,
            totalBags = newBags.size,
            totalWeight = newBags.sumOf { it.weightKg }
        )

        // Clear for next scan
        _scannedQr.value = null
        _qrError.value = null

        return true
    }

    /**
     * Submit all bags in the session to the repository
     */
    fun submitAll(eventType: String = "HCF_COLLECTION") {
        val bags = _sessionState.value.bags
        if (bags.isEmpty()) {
            _error.value = "No bags to submit"
            return
        }

        viewModelScope.launch {
            _submissionState.value = SubmissionState.Loading
            try {
                val events = bags.map { bag ->
                    BagEvent(
                        id = UUID.randomUUID(),
                        qrCode = bag.qrCode,
                        eventType = eventType,
                        eventTs = bag.timestamp,
                        gpsLat = bag.gpsLat,
                        gpsLon = bag.gpsLon,
                        weightKg = bag.weightKg,
                        hcfId = "", // Backend resolves from QR
                        facilityId = null,
                        synced = false
                    )
                }
                bagEventRepository.recordBatch(events)
                _submissionState.value = SubmissionState.BatchSuccess(bags.size)
                
                // Clear session after successful submit
                clearSession()
            } catch (e: Exception) {
                _submissionState.value = SubmissionState.Error(e.message ?: "Batch submission failed")
            }
        }
    }

    /**
     * Clear the current session
     */
    fun clearSession() {
        _sessionState.value = PickupSessionState()
        _scannedQr.value = null
        _qrError.value = null
    }

    fun submit(hcfId: String, eventType: String) {
        val qr = _scannedQr.value
        val w = weight.value
        val loc = _location.value as? LocationState.Ready

        if (qr == null || w == null) {
            _submissionState.value = SubmissionState.Error("Missing QR or Weight")
            return
        }

        if (loc == null) {
            _submissionState.value = SubmissionState.Error("Waiting for GPS")
            return
        }

        viewModelScope.launch {
            _submissionState.value = SubmissionState.Loading
            try {
                val event = BagEvent(
                    id = UUID.randomUUID(),
                    qrCode = qr,
                    eventType = eventType,
                    eventTs = System.currentTimeMillis(),
                    gpsLat = loc.lat,
                    gpsLon = loc.lon,
                    weightKg = w,
                    hcfId = hcfId,
                    facilityId = null, // Set if needed
                    synced = false
                )
                bagEventRepository.record(event)
                _submissionState.value = SubmissionState.Success
            } catch (e: Exception) {
                _submissionState.value = SubmissionState.Error(e.message ?: "Submission failed")
            }
        }
    }

    fun resetSubmissionState() {
        _submissionState.value = SubmissionState.Idle
        _scannedQr.value = null
        _qrError.value = null
    }

    fun resetForNextScan() {
        _submissionState.value = SubmissionState.Idle
        _scannedQr.value = null
        _qrError.value = null
        // Keep location, keep scale connected
    }

    fun refreshLocation() {
        viewModelScope.launch {
            _location.value = LocationState.Waiting
            try {
                val loc = locationHelper.getCurrentLocation()
                if (loc != null) {
                    _location.value = LocationState.Ready(
                        lat = loc.latitude, 
                        lon = loc.longitude,
                        accuracy = loc.accuracy
                    )
                } else {
                    _location.value = LocationState.Error("Location unavailable")
                }
            } catch (e: Exception) {
                _location.value = LocationState.Error(e.message ?: "Location error")
            }
        }
    }

    suspend fun simulateWeight() {
        scaleService.simulateWeight()
    }

    private fun isValidQr(qr: String): Boolean {
        val parts = qr.split("|")
        return parts.size >= 4 && parts.all { it.isNotBlank() }
    }
}

sealed class LocationState {
    object Waiting : LocationState()
    data class Ready(val lat: Double, val lon: Double, val accuracy: Float? = null) : LocationState()
    data class Error(val message: String) : LocationState()
}

sealed class SubmissionState {
    object Idle : SubmissionState()
    object Loading : SubmissionState()
    object Success : SubmissionState()
    data class BatchSuccess(val count: Int) : SubmissionState()
    data class Error(val message: String) : SubmissionState()
}
