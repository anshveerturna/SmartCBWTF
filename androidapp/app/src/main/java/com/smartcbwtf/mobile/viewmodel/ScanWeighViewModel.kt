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

    private val _submissionState = MutableStateFlow<SubmissionState>(SubmissionState.Idle)
    val submissionState: StateFlow<SubmissionState> = _submissionState.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

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
        _scannedQr.value = qr
    }

    fun submit(hcfId: String, eventType: String) {
        val qr = _scannedQr.value
        val w = weight.value

        if (qr == null || w == null) {
            _submissionState.value = SubmissionState.Error("Missing QR or Weight")
            return
        }

        viewModelScope.launch {
            _submissionState.value = SubmissionState.Loading
            try {
                val location = locationHelper.getCurrentLocation()
                val event = BagEvent(
                    id = UUID.randomUUID(),
                    qrCode = qr,
                    eventType = eventType,
                    eventTs = System.currentTimeMillis(),
                    gpsLat = location?.latitude ?: 0.0,
                    gpsLon = location?.longitude ?: 0.0,
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
    }
}

sealed class SubmissionState {
    object Idle : SubmissionState()
    object Loading : SubmissionState()
    object Success : SubmissionState()
    data class Error(val message: String) : SubmissionState()
}
