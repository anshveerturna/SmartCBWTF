package com.smartcbwtf.mobile.viewmodel

import android.content.Context
import android.location.Location
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.smartcbwtf.mobile.database.entity.AttendanceEventEntity
import com.smartcbwtf.mobile.database.entity.HcfEntity
import com.smartcbwtf.mobile.repository.AttendanceRepository
import com.smartcbwtf.mobile.repository.HcfRepository
import com.smartcbwtf.mobile.utils.GeoUtils
import com.smartcbwtf.mobile.utils.LocationHelper
import com.smartcbwtf.mobile.work.SyncAttendanceWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class NearbyHcf(
    val hcf: HcfEntity,
    val distanceMeters: Double
)

sealed class AttendanceLocationState {
    object Loading : AttendanceLocationState()
    data class Success(val location: Location) : AttendanceLocationState()
    data class Error(val message: String) : AttendanceLocationState()
}

sealed class HcfSearchState {
    object Idle : HcfSearchState()
    object Loading : HcfSearchState()
    data class NoHcfNearby(val message: String) : HcfSearchState()
    data class SingleHcf(val hcf: NearbyHcf) : HcfSearchState()
    data class MultipleHcfs(val hcfs: List<NearbyHcf>) : HcfSearchState()
}

sealed class AttendanceResult {
    object Idle : AttendanceResult()
    object Loading : AttendanceResult()
    data class Success(val hcfName: String) : AttendanceResult()
    data class Error(val message: String) : AttendanceResult()
}

@HiltViewModel
class AttendanceViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val locationHelper: LocationHelper,
    private val hcfRepository: HcfRepository,
    private val attendanceRepository: AttendanceRepository
) : ViewModel() {

    companion object {
        const val GEOFENCE_RADIUS_METERS = 50.0
        const val COOLDOWN_DURATION_MS = 5 * 60 * 1000L // 5 minutes
    }

    private val _locationState = MutableStateFlow<AttendanceLocationState>(AttendanceLocationState.Loading)
    val locationState: StateFlow<AttendanceLocationState> = _locationState.asStateFlow()

    private val _hcfSearchState = MutableStateFlow<HcfSearchState>(HcfSearchState.Idle)
    val hcfSearchState: StateFlow<HcfSearchState> = _hcfSearchState.asStateFlow()

    private val _selectedHcf = MutableStateFlow<NearbyHcf?>(null)
    val selectedHcf: StateFlow<NearbyHcf?> = _selectedHcf.asStateFlow()

    private val _attendanceResult = MutableStateFlow<AttendanceResult>(AttendanceResult.Idle)
    val attendanceResult: StateFlow<AttendanceResult> = _attendanceResult.asStateFlow()

    private val _cooldownRemainingMs = MutableStateFlow(0L)
    val cooldownRemainingMs: StateFlow<Long> = _cooldownRemainingMs.asStateFlow()

    private val _lastMarkedHcfId = MutableStateFlow<String?>(null)
    
    private var cooldownJob: Job? = null
    private var currentLocation: Location? = null

    init {
        // Check for existing cooldown on init (persisted via Room)
        viewModelScope.launch {
            val remainingMs = attendanceRepository.getCooldownRemainingMs(COOLDOWN_DURATION_MS)
            if (remainingMs > 0) {
                val latest = attendanceRepository.getLatest()
                _lastMarkedHcfId.value = latest?.hcfId
                startCooldownTimer(remainingMs)
            }
        }
    }

    // Called when fragment is created with location passed from HomeFragment
    fun initWithLocation(latitude: Double, longitude: Double) {
        val location = Location("").apply {
            this.latitude = latitude
            this.longitude = longitude
        }
        currentLocation = location
        _locationState.value = AttendanceLocationState.Success(location)
        searchNearbyHcfs(location)
    }

    fun refreshLocation() {
        viewModelScope.launch {
            _locationState.value = AttendanceLocationState.Loading
            _hcfSearchState.value = HcfSearchState.Loading
            _selectedHcf.value = null
            
            try {
                val location = locationHelper.getCurrentLocation()
                if (location != null) {
                    currentLocation = location
                    _locationState.value = AttendanceLocationState.Success(location)
                    searchNearbyHcfs(location)
                } else {
                    _locationState.value = AttendanceLocationState.Error("Unable to get location. Please ensure GPS is enabled.")
                    _hcfSearchState.value = HcfSearchState.Idle
                }
            } catch (e: Exception) {
                _locationState.value = AttendanceLocationState.Error("Location error: ${e.message}")
                _hcfSearchState.value = HcfSearchState.Idle
            }
        }
    }

    private fun searchNearbyHcfs(location: Location) {
        viewModelScope.launch {
            _hcfSearchState.value = HcfSearchState.Loading
            
            try {
                // Refresh HCF data from server to get latest GPS coordinates
                try {
                    hcfRepository.refresh()
                } catch (e: Exception) {
                    // Continue with cached data if refresh fails
                    android.util.Log.w("AttendanceVM", "HCF refresh failed, using cached data: ${e.message}")
                }
                
                val allHcfs = hcfRepository.getAll().firstOrNull() ?: emptyList()
                
                val nearbyHcfs = allHcfs.mapNotNull { hcf ->
                    val hcfLat = hcf.latitude
                    val hcfLon = hcf.longitude
                    
                    if (hcfLat != null && hcfLon != null) {
                        val distance = GeoUtils.haversineMeters(
                            location.latitude, location.longitude,
                            hcfLat, hcfLon
                        )
                        if (distance <= GEOFENCE_RADIUS_METERS) {
                            NearbyHcf(hcf, distance)
                        } else null
                    } else null
                }.sortedBy { it.distanceMeters }
                
                when {
                    nearbyHcfs.isEmpty() -> {
                        _hcfSearchState.value = HcfSearchState.NoHcfNearby(
                            "You are not within ${GEOFENCE_RADIUS_METERS.toInt()}m of any registered Healthcare Facility."
                        )
                        _selectedHcf.value = null
                    }
                    nearbyHcfs.size == 1 -> {
                        val nearbyHcf = nearbyHcfs.first()
                        _hcfSearchState.value = HcfSearchState.SingleHcf(nearbyHcf)
                        _selectedHcf.value = nearbyHcf
                    }
                    else -> {
                        _hcfSearchState.value = HcfSearchState.MultipleHcfs(nearbyHcfs)
                        _selectedHcf.value = null
                    }
                }
            } catch (e: Exception) {
                _hcfSearchState.value = HcfSearchState.NoHcfNearby("Error searching for HCFs: ${e.message}")
                _selectedHcf.value = null
            }
        }
    }

    fun selectHcf(nearbyHcf: NearbyHcf) {
        _selectedHcf.value = nearbyHcf
    }

    fun canMarkAttendance(): Boolean {
        val selected = _selectedHcf.value ?: return false
        val lastMarkedId = _lastMarkedHcfId.value
        val cooldownRemaining = _cooldownRemainingMs.value
        
        // If cooldown is active and trying to select a different HCF
        if (cooldownRemaining > 0 && lastMarkedId != null && lastMarkedId != selected.hcf.id) {
            return false
        }
        
        return true
    }

    fun markAttendance() {
        if (_attendanceResult.value is AttendanceResult.Loading) {
            return
        }
        val selected = _selectedHcf.value ?: return
        val location = currentLocation ?: return
        
        viewModelScope.launch {
            _attendanceResult.value = AttendanceResult.Loading
            
            try {
                // Create attendance event entity (offline-first)
                val event = AttendanceEventEntity(
                    id = UUID.randomUUID(),
                    hcfId = selected.hcf.id,
                    hcfName = selected.hcf.name,
                    eventTs = System.currentTimeMillis(),
                    gpsLat = location.latitude,
                    gpsLon = location.longitude,
                    gpsAccuracyM = location.accuracy,
                    distanceFromHcfM = selected.distanceMeters,
                    deviceId = getDeviceId(),
                    synced = false
                )
                
                // Save to local database
                attendanceRepository.record(event)
                
                // Schedule sync via WorkManager
                scheduleSyncWork()
                
                // Start cooldown timer
                _lastMarkedHcfId.value = selected.hcf.id
                startCooldownTimer(COOLDOWN_DURATION_MS)
                
                _attendanceResult.value = AttendanceResult.Success(selected.hcf.name)
            } catch (e: Exception) {
                _attendanceResult.value = AttendanceResult.Error("Failed to mark attendance: ${e.message}")
            }
        }
    }

    private fun scheduleSyncWork() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncAttendanceWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(appContext)
            .enqueueUniqueWork(
                SyncAttendanceWorker.WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                syncRequest
            )
    }

    private fun startCooldownTimer(durationMs: Long = COOLDOWN_DURATION_MS) {
        cooldownJob?.cancel()
        cooldownJob = viewModelScope.launch {
            _cooldownRemainingMs.value = durationMs
            while (_cooldownRemainingMs.value > 0) {
                delay(1000)
                _cooldownRemainingMs.value = (_cooldownRemainingMs.value - 1000).coerceAtLeast(0)
            }
            _lastMarkedHcfId.value = null
        }
    }

    fun resetAttendanceResult() {
        _attendanceResult.value = AttendanceResult.Idle
    }

    fun formatCooldownTime(ms: Long): String {
        val minutes = (ms / 1000) / 60
        val seconds = (ms / 1000) % 60
        return String.format("%d:%02d", minutes, seconds)
    }

    @Suppress("HardwareIds")
    private fun getDeviceId(): String {
        return Settings.Secure.getString(appContext.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
    }
}
