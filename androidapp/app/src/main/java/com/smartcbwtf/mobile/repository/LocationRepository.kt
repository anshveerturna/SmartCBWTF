package com.smartcbwtf.mobile.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.smartcbwtf.mobile.network.api.GpsEventItem
import com.smartcbwtf.mobile.network.api.GpsPingRequest
import com.smartcbwtf.mobile.network.api.LocationApi
import com.smartcbwtf.mobile.storage.AuthTokenStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for GPS location tracking.
 * Handles syncing to backend and local storage of last known location.
 */
interface LocationRepository {
    suspend fun syncLocation(latitude: Double, longitude: Double, accuracy: Double?): Boolean
    fun getLastKnownLocation(): Pair<Double, Double>?
    fun saveLastKnownLocation(latitude: Double, longitude: Double)
    fun hasLocationConsent(): Boolean
    fun setLocationConsent(granted: Boolean)
    fun clearLocationData()
}

@Singleton
class DefaultLocationRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val locationApi: LocationApi,
    private val authTokenStore: AuthTokenStore
) : LocationRepository {

    companion object {
        private const val TAG = "LocationRepository"
        private const val PREFS_NAME = "location_prefs"
        private const val KEY_LAST_LAT = "last_lat"
        private const val KEY_LAST_LON = "last_lon"
        private const val KEY_LOCATION_CONSENT = "location_consent"
    }

    private val prefs: SharedPreferences = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override suspend fun syncLocation(latitude: Double, longitude: Double, accuracy: Double?): Boolean {
        // Check if user is logged in
        if (authTokenStore.getToken() == null) {
            Log.d(TAG, "Not logged in, skipping location sync")
            return false
        }

        // Save locally first
        saveLastKnownLocation(latitude, longitude)

        return try {
            val event = GpsEventItem(
                clientEventId = UUID.randomUUID().toString(),
                latitude = latitude,
                longitude = longitude,
                speed = null,
                heading = null,
                accuracyM = accuracy,
                recordedAt = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
            )

            val request = GpsPingRequest(events = listOf(event))
            val response = locationApi.pingLocation(request)

            if (response.isSuccessful) {
                Log.d(TAG, "Location synced successfully: ${response.body()?.successCount} events")
                true
            } else {
                Log.w(TAG, "Location sync failed: ${response.code()}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Location sync error: ${e.message}", e)
            false
        }
    }

    override fun getLastKnownLocation(): Pair<Double, Double>? {
        val lat = prefs.getFloat(KEY_LAST_LAT, Float.MIN_VALUE)
        val lon = prefs.getFloat(KEY_LAST_LON, Float.MIN_VALUE)
        
        return if (lat != Float.MIN_VALUE && lon != Float.MIN_VALUE) {
            Pair(lat.toDouble(), lon.toDouble())
        } else {
            null
        }
    }

    override fun saveLastKnownLocation(latitude: Double, longitude: Double) {
        prefs.edit()
            .putFloat(KEY_LAST_LAT, latitude.toFloat())
            .putFloat(KEY_LAST_LON, longitude.toFloat())
            .apply()
    }

    override fun hasLocationConsent(): Boolean {
        return prefs.getBoolean(KEY_LOCATION_CONSENT, false)
    }

    override fun setLocationConsent(granted: Boolean) {
        prefs.edit().putBoolean(KEY_LOCATION_CONSENT, granted).apply()
    }

    override fun clearLocationData() {
        prefs.edit()
            .remove(KEY_LAST_LAT)
            .remove(KEY_LAST_LON)
            .remove(KEY_LOCATION_CONSENT)
            .apply()
    }
}
