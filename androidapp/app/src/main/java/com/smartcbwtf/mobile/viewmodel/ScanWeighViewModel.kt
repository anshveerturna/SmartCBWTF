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
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ScanWeighViewModel @Inject constructor(
    private val scaleService: ScaleService,
    private val bagEventRepository: BagEventRepository,
    private val locationHelper: LocationHelper
) : ViewModel() {

    val weight = scaleService.weight
    val connectionState = scaleService.connectionState

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

    fun refreshLocation() {
        viewModelScope.launch {
            _location.value = LocationState.Waiting
            try {
                val loc = locationHelper.getCurrentLocation()
                if (loc != null) {
                    _location.value = LocationState.Ready(loc.latitude, loc.longitude)
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
    data class Ready(val lat: Double, val lon: Double) : LocationState()
    data class Error(val message: String) : LocationState()
}

sealed class SubmissionState {
    object Idle : SubmissionState()
    object Loading : SubmissionState()
    object Success : SubmissionState()
    data class Error(val message: String) : SubmissionState()
}
