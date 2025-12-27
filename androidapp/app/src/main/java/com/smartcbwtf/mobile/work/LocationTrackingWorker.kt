package com.smartcbwtf.mobile.work

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.smartcbwtf.mobile.repository.LocationRepository
import com.smartcbwtf.mobile.service.ForegroundLocationService
import com.smartcbwtf.mobile.storage.AppConfigStore
import com.smartcbwtf.mobile.storage.AuthTokenStore
import com.smartcbwtf.mobile.utils.LocationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * WorkManager safety net for location tracking.
 * 
 * SECURITY INVARIANTS (same as ForegroundLocationService):
 * - NEVER runs without user consent
 * - Only runs for DRIVER / PLANT_OPERATOR roles
 * - Respects backend GPS config
 * 
 * Runs every 15 minutes to:
 * - Restart ForegroundLocationService if killed
 * - Sync backup location
 */
@HiltWorker
class LocationTrackingWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted params: WorkerParameters,
    private val locationRepository: LocationRepository,
    private val locationHelper: LocationHelper,
    private val authTokenStore: AuthTokenStore,
    private val appConfigStore: AppConfigStore
) : CoroutineWorker(appContext, params) {

    companion object {
        const val TAG = "LocationTrackingWorker"
        const val WORK_NAME = "location_tracking"
        
        // Allowed roles for GPS tracking
        private val ALLOWED_ROLES = setOf("DRIVER", "PLANT_OPERATOR")
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Location tracking worker started")
        
        // CRITICAL: Check all conditions before any tracking work
        if (!canStartTracking()) {
            Log.d(TAG, "Tracking conditions not met, skipping work")
            return Result.success()
        }

        // Restart foreground service if needed
        try {
            ForegroundLocationService.startService(appContext)
            Log.d(TAG, "Ensured ForegroundLocationService is running")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start ForegroundLocationService: ${e.message}")
        }

        // Sync current location as backup
        return try {
            val location = locationHelper.getCurrentLocation()
            if (location != null) {
                locationRepository.syncLocation(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    accuracy = location.accuracy.toDouble()
                )
                Log.d(TAG, "Backup location sync completed")
            } else {
                Log.w(TAG, "Could not get current location for backup sync")
            }
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Location tracking work failed: ${e.message}", e)
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    /**
     * Centralized guard: ALL conditions must be met before tracking.
     * 
     * Required conditions:
     * 1. User is logged in (token present)
     * 2. User has granted location consent
     * 3. GPS is enabled in backend config
     * 4. User role is DRIVER or PLANT_OPERATOR
     */
    private suspend fun canStartTracking(): Boolean {
        // Check 1: User must be logged in
        val token = authTokenStore.getToken()
        if (token == null) {
            Log.d(TAG, "Guard: Not logged in")
            return false
        }

        // Check 2: CRITICAL - User must have given consent
        if (!locationRepository.hasLocationConsent()) {
            Log.d(TAG, "Guard: No location consent")
            return false
        }

        // Check 3: GPS must be enabled in backend config
        if (!appConfigStore.gpsEnabled) {
            Log.d(TAG, "Guard: GPS disabled via config")
            return false
        }

        // Check 4: Role check - only DRIVER and PLANT_OPERATOR
        val userRole = appConfigStore.userRole
        if (userRole !in ALLOWED_ROLES) {
            Log.d(TAG, "Guard: Role '$userRole' not allowed")
            return false
        }

        Log.d(TAG, "All tracking guards passed")
        return true
    }
}

