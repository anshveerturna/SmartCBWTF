package com.smartcbwtf.mobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.smartcbwtf.mobile.repository.AuthRepository
import com.smartcbwtf.mobile.repository.BagEventRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    bagEventRepository: BagEventRepository,
    workManager: WorkManager
) : ViewModel() {

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
