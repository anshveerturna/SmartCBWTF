package com.smartcbwtf.mobile.utils

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    companion object {
        private const val TAG = "LocationHelper"
        private const val LOCATION_TIMEOUT_MS = 15000L // 15 seconds timeout
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location? {
        Log.d(TAG, "Requesting fresh GPS location...")
        
        return withTimeoutOrNull(LOCATION_TIMEOUT_MS) {
            try {
                val cancellationTokenSource = CancellationTokenSource()
                val location = fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cancellationTokenSource.token
                ).await()
                
                if (location != null) {
                    Log.d(TAG, "Got location: ${location.latitude}, ${location.longitude}, accuracy: ${location.accuracy}m")
                } else {
                    Log.w(TAG, "getCurrentLocation returned null")
                }
                location
            } catch (e: Exception) {
                Log.e(TAG, "Error getting location: ${e.message}", e)
                null
            }
        } ?: run {
            Log.e(TAG, "Location request timed out after ${LOCATION_TIMEOUT_MS/1000}s")
            null
        }
    }
}
