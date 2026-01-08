package com.smartcbwtf.mobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartcbwtf.mobile.network.model.MobileRouteResponse
import com.smartcbwtf.mobile.repository.RouteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for My Route screen.
 */
@HiltViewModel
class MyRouteViewModel @Inject constructor(
    private val routeRepository: RouteRepository
) : ViewModel() {

    sealed class UiState {
        object Loading : UiState()
        data class Success(val route: MobileRouteResponse) : UiState()
        object NoRouteAssigned : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        loadRoute()
    }

    fun loadRoute() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            
            routeRepository.getMyRoute()
                .onSuccess { route ->
                    _uiState.value = if (route != null) {
                        UiState.Success(route)
                    } else {
                        UiState.NoRouteAssigned
                    }
                }
                .onFailure { error ->
                    _uiState.value = UiState.Error(error.message ?: "Failed to load route")
                }
        }
    }
}
