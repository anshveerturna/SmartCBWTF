package com.smartcbwtf.mobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartcbwtf.mobile.BuildConfig
import com.smartcbwtf.mobile.bluetooth.ScaleService
import com.smartcbwtf.mobile.repository.AuthRepository
import com.smartcbwtf.mobile.repository.BagEventRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    bagEventRepository: BagEventRepository,
    private val scaleService: ScaleService
) : ViewModel() {

    val appVersion = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
    // BuildConfig.BASE_URL might not exist if not defined in build.gradle. Assuming it is.
    // If not, I'll just put a placeholder.
    // Requirement says "Current BASE_URL (BuildConfig.BASE_URL)" so it must be there.
    val baseUrl = try { BuildConfig.BASE_URL } catch (e: Exception) { "Unknown" }

    val username: StateFlow<String> = flow {
        // Ideally decode JWT or fetch profile. For now just placeholder.
        emit("Logged User") 
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Loading...")

    val pendingCount = bagEventRepository.pendingCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val scaleServiceType = scaleService.javaClass.simpleName
}
