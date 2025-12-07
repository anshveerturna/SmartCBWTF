package com.smartcbwtf.mobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartcbwtf.mobile.database.entity.HcfEntity
import com.smartcbwtf.mobile.repository.HcfRepository
import com.smartcbwtf.mobile.utils.LocationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import javax.inject.Inject

@HiltViewModel
class StartPickupViewModel @Inject constructor(
    private val hcfRepository: HcfRepository,
    private val locationHelper: LocationHelper
) : ViewModel() {

    private val _hcfs = MutableStateFlow<List<HcfEntity>>(emptyList())
    val hcfs: StateFlow<List<HcfEntity>> = _hcfs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadHcfs()
    }

    fun loadHcfs() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                hcfRepository.refresh()
                val location = locationHelper.getCurrentLocation()
                hcfRepository.getAll().collect { list ->
                    _hcfs.value = location?.let { loc ->
                        list.sortedBy { distanceKm(loc.latitude, loc.longitude, it) }.take(3)
                    } ?: list.take(3)
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Unable to load HCFs"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun distanceKm(lat: Double, lon: Double, hcf: HcfEntity): Double {
        if (hcf.city == null && hcf.state == null) return Double.MAX_VALUE
        // If HCF lacks coordinates, return max; otherwise use haversine over provided lat/lon if available
        // Currently HcfEntity has no lat/lon fields; using 0 as fallback
        val lat2 = 0.0
        val lon2 = 0.0
        val dLat = Math.toRadians(lat2 - lat)
        val dLon = Math.toRadians(lon2 - lon)
        val a = sin(dLat / 2) * sin(dLat / 2) + cos(Math.toRadians(lat)) * cos(Math.toRadians(lat2)) * sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return 6371 * c
    }
}
