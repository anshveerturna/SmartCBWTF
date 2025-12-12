package com.smartcbwtf.mobile.viewmodel

import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartcbwtf.mobile.bluetooth.ConnectionState
import com.smartcbwtf.mobile.bluetooth.ScaleService
import com.smartcbwtf.mobile.model.BagEvent
import com.smartcbwtf.mobile.model.FacilityInfo
import com.smartcbwtf.mobile.model.OperationMode
import com.smartcbwtf.mobile.model.VerifyBagRequest
import com.smartcbwtf.mobile.model.VerifyBagResponse
import com.smartcbwtf.mobile.network.api.VerificationApi
import com.smartcbwtf.mobile.repository.BagEventRepository
import com.smartcbwtf.mobile.storage.SessionManager
import com.smartcbwtf.mobile.utils.GeoUtils
import com.smartcbwtf.mobile.utils.LocationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
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
    private val locationHelper: LocationHelper,
    private val verificationApi: VerificationApi,
    private val sessionManager: SessionManager
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

    // Current operation mode
    private val _operationMode = MutableStateFlow(OperationMode.COLLECTION)
    val operationMode: StateFlow<OperationMode> = _operationMode.asStateFlow()

    // Geofence state for verification mode
    private val _geofenceState = MutableStateFlow<GeofenceState>(GeofenceState.NotChecked)
    val geofenceState: StateFlow<GeofenceState> = _geofenceState.asStateFlow()

    // Facility info for geofence checking
    private val _facilityInfo = MutableStateFlow<FacilityInfo?>(null)
    val facilityInfo: StateFlow<FacilityInfo?> = _facilityInfo.asStateFlow()

    // Verification result
    private val _verificationResult = MutableStateFlow<VerificationResult?>(null)
    val verificationResult: StateFlow<VerificationResult?> = _verificationResult.asStateFlow()

    // GPS configuration
    companion object {
        const val GPS_TIMEOUT_MS = 8000L
        const val MIN_GPS_ACCURACY_M = 50f
        const val DEFAULT_GEOFENCE_RADIUS_M = 200
    }

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

    // Can verify: GPS ready + inside geofence + BLE connected + stable weight + QR scanned
    val canVerify: StateFlow<Boolean> = combine(
        combine(scannedQr, weight, geofenceState) { qr, w, geoState ->
            Triple(qr, w, geoState)
        },
        combine(connectionState, qrError, submissionState) { connState, qrErr, subState ->
            Triple(connState, qrErr, subState)
        }
    ) { (qr, w, geoState), (connState, qrErr, subState) ->
        !qr.isNullOrBlank() && 
        qrErr == null && 
        w != null && w > 0.0 && 
        geoState is GeofenceState.InsideGeofence &&
        connState == ConnectionState.CONNECTED &&
        subState !is SubmissionState.Loading
    }.stateIn(viewModelScope, SharingStarted.Lazily, false)

    // Scanner enabled only when inside geofence (for verify mode)
    val scannerEnabled = combine(
        operationMode,
        geofenceState
    ) { mode, geoState ->
        when (mode) {
            OperationMode.COLLECTION -> true
            OperationMode.VERIFY -> geoState is GeofenceState.InsideGeofence
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

    /**
     * Set the operation mode (COLLECTION or VERIFY)
     */
    fun setOperationMode(mode: OperationMode) {
        _operationMode.value = mode
        if (mode == OperationMode.VERIFY) {
            // Reset geofence state when entering verify mode
            _geofenceState.value = GeofenceState.NotChecked
        }
    }

    /**
     * Initialize verification mode - fetch facility info and check geofence
     */
    fun initializeVerificationMode() {
        viewModelScope.launch {
            _geofenceState.value = GeofenceState.AcquiringGPS
            
            // Try to load facility info first
            loadFacilityInfo()
            
            // Acquire GPS with timeout
            acquireGpsForVerification()
        }
    }

    private suspend fun loadFacilityInfo() {
        try {
            val response = verificationApi.getActiveFacility()
            if (response.isSuccessful) {
                _facilityInfo.value = response.body()
            } else {
                // Use default facility info if API fails - verification will be server-enforced
                _facilityInfo.value = null
            }
        } catch (e: Exception) {
            _facilityInfo.value = null
        }
    }

    private suspend fun acquireGpsForVerification() {
        _geofenceState.value = GeofenceState.AcquiringGPS
        
        val startTime = System.currentTimeMillis()
        var bestLocation: LocationState.Ready? = null
        
        // Try to get a good GPS fix within timeout
        val result = withTimeoutOrNull(GPS_TIMEOUT_MS) {
            while (true) {
                try {
                    val loc = locationHelper.getCurrentLocation()
                    if (loc != null) {
                        val accuracy = loc.accuracy
                        
                        // Update location state
                        _location.value = LocationState.Ready(
                            lat = loc.latitude,
                            lon = loc.longitude,
                            accuracy = accuracy
                        )
                        
                        // Check if accuracy is good enough
                        if (accuracy <= MIN_GPS_ACCURACY_M) {
                            bestLocation = LocationState.Ready(loc.latitude, loc.longitude, accuracy)
                            return@withTimeoutOrNull bestLocation
                        } else if (bestLocation == null || (bestLocation?.accuracy ?: Float.MAX_VALUE) > accuracy) {
                            bestLocation = LocationState.Ready(loc.latitude, loc.longitude, accuracy)
                        }
                    }
                } catch (e: Exception) {
                    // Continue trying
                }
                delay(1000) // Poll every second
            }
            @Suppress("UNREACHABLE_CODE")
            null
        }
        
        // Use best available location even if not perfect
        val locationToUse = result ?: bestLocation
        
        if (locationToUse != null) {
            _location.value = locationToUse
            checkGeofence(locationToUse)
        } else {
            _location.value = LocationState.Error("Unable to acquire GPS")
            _geofenceState.value = GeofenceState.GpsError("Unable to acquire reliable location. Verification is allowed only within the CBWTF site. Please move the device or enable GPS.")
        }
    }

    private fun checkGeofence(location: LocationState.Ready) {
        val facility = _facilityInfo.value
        
        if (facility == null || facility.gpsLat == null || facility.gpsLon == null) {
            // No facility info - allow but server will enforce
            _geofenceState.value = GeofenceState.InsideGeofence(location.lat, location.lon, location.accuracy)
            return
        }
        
        val radiusM = facility.geofenceRadiusM ?: DEFAULT_GEOFENCE_RADIUS_M
        val distance = GeoUtils.haversineMeters(
            location.lat, location.lon,
            facility.gpsLat, facility.gpsLon
        )
        
        if (distance <= radiusM) {
            _geofenceState.value = GeofenceState.InsideGeofence(location.lat, location.lon, location.accuracy)
        } else {
            _geofenceState.value = GeofenceState.OutsideGeofence(
                distance = distance,
                allowedRadius = radiusM,
                message = "You appear to be outside the CBWTF facility area. Verification not allowed."
            )
        }
    }

    /**
     * Retry GPS acquisition for verification
     */
    fun retryGpsForVerification() {
        viewModelScope.launch {
            acquireGpsForVerification()
        }
    }

    /**
     * Verify a single bag at CBWTF
     */
    fun verifyBag(deviceId: String) {
        val qr = _scannedQr.value
        val w = weight.value
        val geoState = _geofenceState.value as? GeofenceState.InsideGeofence

        if (qr == null) {
            _submissionState.value = SubmissionState.Error("No QR code scanned")
            return
        }
        if (w == null || w <= 0.0) {
            _submissionState.value = SubmissionState.Error("Invalid weight reading")
            return
        }
        if (geoState == null) {
            _submissionState.value = SubmissionState.Error("GPS not ready or outside geofence")
            return
        }

        viewModelScope.launch {
            _submissionState.value = SubmissionState.Loading
            try {
                val userId = sessionManager.userId ?: "unknown"
                
                val request = VerifyBagRequest(
                    qrCode = qr,
                    eventType = "CBWTF_VERIFICATION",
                    gpsLat = geoState.lat,
                    gpsLon = geoState.lon,
                    gpsAccuracy = geoState.accuracy ?: MIN_GPS_ACCURACY_M,
                    weightKg = w,
                    deviceId = deviceId,
                    verifiedByUserId = userId
                )

                val response = verificationApi.verifyBag(request)
                
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        _verificationResult.value = VerificationResult(
                            success = true,
                            bagLabelId = body.bagLabelId,
                            verifiedAt = body.verifiedAt,
                            anomalyState = body.anomalyState ?: "OK",
                            deltaKg = body.deltaKg
                        )
                        _submissionState.value = SubmissionState.VerifySuccess(
                            anomalyState = body.anomalyState ?: "OK",
                            deltaKg = body.deltaKg
                        )
                        // Clear for next scan
                        _scannedQr.value = null
                        _qrError.value = null
                    } else {
                        _submissionState.value = SubmissionState.Error("Empty response from server")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMsg = when (response.code()) {
                        403 -> "Verification must be performed within CBWTF facility geofence"
                        409 -> "Bag already verified"
                        400 -> "Invalid request: ${parseErrorMessage(errorBody)}"
                        else -> "Verification failed: ${response.message()}"
                    }
                    _submissionState.value = SubmissionState.Error(errorMsg)
                }
            } catch (e: Exception) {
                _submissionState.value = SubmissionState.Error(e.message ?: "Verification failed")
            }
        }
    }

    private fun parseErrorMessage(errorBody: String?): String {
        return errorBody?.let {
            try {
                // Try to extract message from JSON
                val regex = """"message"\s*:\s*"([^"]+)"""".toRegex()
                regex.find(it)?.groupValues?.get(1) ?: it
            } catch (e: Exception) {
                it
            }
        } ?: "Unknown error"
    }

    fun clearVerificationResult() {
        _verificationResult.value = null
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

sealed class GeofenceState {
    object NotChecked : GeofenceState()
    object AcquiringGPS : GeofenceState()
    data class InsideGeofence(val lat: Double, val lon: Double, val accuracy: Float?) : GeofenceState()
    data class OutsideGeofence(val distance: Double, val allowedRadius: Int, val message: String) : GeofenceState()
    data class GpsError(val message: String) : GeofenceState()
}

data class VerificationResult(
    val success: Boolean,
    val bagLabelId: String?,
    val verifiedAt: String?,
    val anomalyState: String,
    val deltaKg: Double?
)

sealed class SubmissionState {
    object Idle : SubmissionState()
    object Loading : SubmissionState()
    object Success : SubmissionState()
    data class BatchSuccess(val count: Int) : SubmissionState()
    data class VerifySuccess(val anomalyState: String, val deltaKg: Double?) : SubmissionState()
    data class Error(val message: String) : SubmissionState()
}
