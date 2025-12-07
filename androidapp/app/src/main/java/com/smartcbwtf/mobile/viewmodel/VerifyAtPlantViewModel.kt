package com.smartcbwtf.mobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
class VerifyAtPlantViewModel @Inject constructor(
    private val scaleService: ScaleService,
    private val bagEventRepository: BagEventRepository,
    private val locationHelper: LocationHelper
) : ViewModel() {

    val weight = scaleService.weight
    val connectionState = scaleService.connectionState

    private val _qr = MutableStateFlow<String?>(null)
    val qr: StateFlow<String?> = _qr.asStateFlow()

    private val _state = MutableStateFlow<SubmissionState>(SubmissionState.Idle)
    val state: StateFlow<SubmissionState> = _state.asStateFlow()

    fun connect() = viewModelScope.launch { scaleService.connect() }
    fun disconnect() = viewModelScope.launch { scaleService.disconnect() }

    fun onQrScanned(value: String) { _qr.value = value }

    fun submit(hcfId: String) {
        val qrValue = _qr.value
        val w = weight.value
        if (qrValue == null || w == null) {
            _state.value = SubmissionState.Error("Missing QR or Weight")
            return
        }
        viewModelScope.launch {
            _state.value = SubmissionState.Loading
            try {
                val loc = locationHelper.getCurrentLocation()
                bagEventRepository.record(
                    BagEvent(
                        id = UUID.randomUUID(),
                        qrCode = qrValue,
                        eventType = "CBWTF_VERIFICATION",
                        eventTs = System.currentTimeMillis(),
                        gpsLat = loc?.latitude ?: 0.0,
                        gpsLon = loc?.longitude ?: 0.0,
                        weightKg = w,
                        hcfId = hcfId,
                        facilityId = null,
                        synced = false
                    )
                )
                _state.value = SubmissionState.Success
            } catch (e: Exception) {
                _state.value = SubmissionState.Error(e.message ?: "Failed to save")
            }
        }
    }
}
