package com.smartcbwtf.mobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartcbwtf.mobile.network.model.UserProfileResponse
import com.smartcbwtf.mobile.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Profile screen.
 * 
 * DESIGN NOTE: This ViewModel intentionally provides NO methods
 * to modify profile data. The profile screen is READ-ONLY.
 * Profile data is centrally managed at the backend level.
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _state = MutableStateFlow<ProfileState>(ProfileState.Loading)
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    init {
        loadProfile()
    }

    /**
     * Load the user's profile from backend (or cache if offline).
     */
    fun loadProfile() {
        viewModelScope.launch {
            _state.value = ProfileState.Loading
            try {
                val profile = profileRepository.getCurrentUserProfile()
                _state.value = ProfileState.Success(profile)
            } catch (e: Exception) {
                _state.value = ProfileState.Error(
                    message = e.message ?: "Failed to load profile",
                    isOfflineError = e is java.net.UnknownHostException || 
                                     e is java.net.ConnectException
                )
            }
        }
    }
}

/**
 * UI state for the Profile screen.
 */
sealed class ProfileState {
    object Loading : ProfileState()
    
    data class Success(
        val profile: UserProfileResponse
    ) : ProfileState()
    
    data class Error(
        val message: String,
        val isOfflineError: Boolean = false
    ) : ProfileState()
}
