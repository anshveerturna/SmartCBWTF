package com.smartcbwtf.mobile.viewmodel

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartcbwtf.mobile.network.model.HcfRegistrationRequest
import com.smartcbwtf.mobile.network.model.HcfRegistrationResponse
import com.smartcbwtf.mobile.network.model.TermsResponse
import com.smartcbwtf.mobile.repository.HcfRepository
import com.smartcbwtf.mobile.storage.SessionManager
import com.smartcbwtf.mobile.utils.LocationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HcfRegistrationViewModel @Inject constructor(
    private val hcfRepository: HcfRepository,
    private val locationHelper: LocationHelper,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow<RegistrationState>(RegistrationState.Idle)
    val state: StateFlow<RegistrationState> = _state.asStateFlow()
    
    private val _formState = MutableStateFlow(HcfFormState())
    val formState: StateFlow<HcfFormState> = _formState.asStateFlow()
    
    private val _termsState = MutableStateFlow<TermsState>(TermsState.NotLoaded)
    val termsState: StateFlow<TermsState> = _termsState.asStateFlow()
    
    private val _gpsState = MutableStateFlow<GpsState>(GpsState.NotCaptured)
    val gpsState: StateFlow<GpsState> = _gpsState.asStateFlow()

    init {
        loadTerms()
    }
    
    fun loadTerms(facilityId: String? = null) {
        viewModelScope.launch {
            _termsState.value = TermsState.Loading
            try {
                val terms = hcfRepository.getLatestTerms(facilityId)
                _termsState.value = TermsState.Loaded(terms)
            } catch (e: Exception) {
                _termsState.value = TermsState.Error(e.message ?: "Failed to load terms")
            }
        }
    }
    
    fun captureGpsLocation() {
        viewModelScope.launch {
            _gpsState.value = GpsState.Capturing
            try {
                val location = locationHelper.getCurrentLocation()
                if (location != null) {
                    _gpsState.value = GpsState.Captured(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        accuracy = location.accuracy
                    )
                } else {
                    _gpsState.value = GpsState.Error("Unable to get location. Please try again.")
                }
            } catch (e: Exception) {
                _gpsState.value = GpsState.Error(e.message ?: "Location capture failed")
            }
        }
    }
    
    fun setTermsAccepted(accepted: Boolean) {
        _formState.update { it.copy(termsAccepted = accepted) }
    }
    
    fun setBedded(bedded: Boolean) {
        _formState.update { it.copy(bedded = bedded) }
    }

    fun submit(
        name: String,
        address: String?,
        doctorName: String?,
        phone: String?,
        email: String?,
        panNo: String?,
        gstNo: String?,
        aadharNo: String?,
        pcbNo: String?,
        beds: Int?,
        monthlyCharges: Double?,
        otherNotes: String?
    ) {
        // Validation
        if (name.isBlank()) {
            _state.value = RegistrationState.Error("HCF Name is required")
            return
        }
        
        if (address.isNullOrBlank()) {
            _state.value = RegistrationState.Error("Address is required")
            return
        }
        
        if (doctorName.isNullOrBlank()) {
            _state.value = RegistrationState.Error("Doctor/Owner Name is required")
            return
        }
        
        if (phone.isNullOrBlank()) {
            _state.value = RegistrationState.Error("Contact Phone is required")
            return
        }
        
        val gps = _gpsState.value
        if (gps !is GpsState.Captured) {
            _state.value = RegistrationState.Error("GPS location is required. Please capture your location.")
            return
        }
        
        if (!_formState.value.termsAccepted) {
            _state.value = RegistrationState.Error("Please accept the Terms & Conditions")
            return
        }
        
        val terms = _termsState.value
        if (terms !is TermsState.Loaded) {
            _state.value = RegistrationState.Error("Terms & Conditions must be loaded before registration")
            return
        }
        
        val bedded = _formState.value.bedded
        if (bedded && (beds == null || beds <= 0)) {
            _state.value = RegistrationState.Error("Number of beds is required for bedded facilities")
            return
        }

        viewModelScope.launch {
            _state.value = RegistrationState.Loading
            try {
                val request = HcfRegistrationRequest(
                    name = name.trim(),
                    address = address.trim(),
                    doctorName = doctorName.trim(),
                    phone = phone.trim(),
                    email = email?.trim()?.takeIf { it.isNotBlank() },
                    panNo = panNo?.trim()?.takeIf { it.isNotBlank() }?.uppercase(),
                    gstNo = gstNo?.trim()?.takeIf { it.isNotBlank() }?.uppercase(),
                    aadharNo = aadharNo?.trim()?.takeIf { it.isNotBlank() },
                    pcbAuthorizationNo = pcbNo?.trim()?.takeIf { it.isNotBlank() },
                    bedded = bedded,
                    numberOfBeds = if (bedded) beds else null,
                    monthlyCharges = monthlyCharges,
                    otherNotes = otherNotes?.trim()?.takeIf { it.isNotBlank() },
                    termsAccepted = true,
                    termsVersion = terms.terms.version,
                    gpsLatitude = gps.latitude,
                    gpsLongitude = gps.longitude,
                    gpsAccuracy = gps.accuracy,
                    registeredByUserId = sessionManager.userId,
                    facilityId = sessionManager.facilityId
                )
                
                val response = hcfRepository.register(request)
                _state.value = RegistrationState.Success(response)
            } catch (e: Exception) {
                _state.value = RegistrationState.Error(e.message ?: "Registration failed")
            }
        }
    }

    fun reset() {
        _state.value = RegistrationState.Idle
        _formState.value = HcfFormState()
        _gpsState.value = GpsState.NotCaptured
    }
}

// Form state for UI binding
data class HcfFormState(
    val termsAccepted: Boolean = false,
    val bedded: Boolean = false
)

// GPS capture state
sealed class GpsState {
    object NotCaptured : GpsState()
    object Capturing : GpsState()
    data class Captured(
        val latitude: Double,
        val longitude: Double,
        val accuracy: Float
    ) : GpsState()
    data class Error(val message: String) : GpsState()
}

// Terms loading state
sealed class TermsState {
    object NotLoaded : TermsState()
    object Loading : TermsState()
    data class Loaded(val terms: TermsResponse) : TermsState()
    data class Error(val message: String) : TermsState()
}

// Overall registration state
sealed class RegistrationState {
    object Idle : RegistrationState()
    object Loading : RegistrationState()
    data class Success(val response: HcfRegistrationResponse) : RegistrationState()
    data class Error(val message: String) : RegistrationState()
}
