package com.smartcbwtf.mobile.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.smartcbwtf.mobile.repository.AuthRepository
import com.smartcbwtf.mobile.repository.BagEventRepository
import com.smartcbwtf.mobile.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    bagEventRepository: BagEventRepository,
    private val profileRepository: ProfileRepository,
    workManager: WorkManager
) : ViewModel() {

    // Profile data for home screen avatar and greeting (explicitly updated)
    private val _profilePhotoUrl = MutableStateFlow<String?>(null)
    val profilePhotoUrl: StateFlow<String?> = _profilePhotoUrl.asStateFlow()

    private val _greetingName = MutableStateFlow("Operator")
    val greetingName: StateFlow<String> = _greetingName.asStateFlow()

    init {
        // Fetch and cache profile data when home screen loads
        viewModelScope.launch {
            try {
                val profile = profileRepository.getCurrentUserProfile()
                Log.d("HomeViewModel", "Profile fetched: ${profile.fullName}, photo: ${profile.profilePhotoUrl}")
                
                // Explicitly update the state flows
                _profilePhotoUrl.value = profile.profilePhotoUrl
                _greetingName.value = profile.fullName?.split(" ")?.firstOrNull() ?: "Operator"
                
                Log.d("HomeViewModel", "Updated stateflows: photo=${_profilePhotoUrl.value}, name=${_greetingName.value}")
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Failed to fetch profile: ${e.message}")
            }
        }
    }

    val pendingCount: StateFlow<Int> = bagEventRepository.pendingCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val lastSyncFailed: StateFlow<Boolean> = workManager.getWorkInfosForUniqueWorkFlow("bag_sync")
        .map { workInfos ->
            workInfos.any { it.state == WorkInfo.State.FAILED }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun logout() {
        viewModelScope.launch { authRepository.logout() }
    }
}

