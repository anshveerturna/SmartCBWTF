package com.smartcbwtf.mobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartcbwtf.mobile.repository.HcfRepository
import com.smartcbwtf.mobile.utils.LocationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HcfRegistrationViewModel @Inject constructor(
    private val hcfRepository: HcfRepository,
    private val locationHelper: LocationHelper
) : ViewModel() {

    private val _state = MutableStateFlow<RegistrationState>(RegistrationState.Idle)
    val state: StateFlow<RegistrationState> = _state.asStateFlow()

    fun submit(
        name: String,
        address: String?,
        city: String?,
        state: String?,
        postalCode: String?,
        phone: String?,
        email: String?,
        beds: Int?
    ) {
        if (name.isBlank()) {
            _state.value = RegistrationState.Error("Name is required")
            return
        }

        viewModelScope.launch {
            _state.value = RegistrationState.Loading
            try {
                val location = locationHelper.getCurrentLocation()
                hcfRepository.register(
                    name = name,
                    address = address,
                    city = city,
                    state = state,
                    postalCode = postalCode,
                    phone = phone,
                    email = email,
                    beds = beds,
                    latitude = location?.latitude,
                    longitude = location?.longitude,
                )
                _state.value = RegistrationState.Success
            } catch (e: Exception) {
                _state.value = RegistrationState.Error(e.message ?: "Registration failed")
            }
        }
    }

    fun reset() {
        _state.value = RegistrationState.Idle
    }
}

sealed class RegistrationState {
    object Idle : RegistrationState()
    object Loading : RegistrationState()
    object Success : RegistrationState()
    data class Error(val message: String) : RegistrationState()
}
