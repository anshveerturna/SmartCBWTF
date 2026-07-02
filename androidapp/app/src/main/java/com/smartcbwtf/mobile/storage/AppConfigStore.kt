package com.smartcbwtf.mobile.storage

import android.content.SharedPreferences
import com.smartcbwtf.mobile.network.api.MobileConfigResponse
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Store for app configuration values fetched from backend.
 * Values are cached locally and used throughout the app.
 */
@Singleton
class AppConfigStore @Inject constructor(
    private val prefs: SharedPreferences
) {
    private val staticConfigKeys = setOf(
        "geofenceRadiusMeters",
        "locationUpdateIntervalMinutes",
        "attendanceDistanceToleranceMeters",
        "maxVerificationDelayMinutes",
        "weightMismatchTolerancePercent",
        "blueWasteMinPercentage",
        "platformName",
        "supportEmail",
        "supportPhone",
        "androidSyncDisabled",
        "qrVerificationDisabled",
        "gpsEnabled",
        "gpsPingIntervalMinutes",
        "gpsRequireForeground",
        "userRole",
        "subscriptionActive",
        "subscriptionStatus",
        "lastConfigSync"
    )

    // Operational thresholds
    val geofenceRadiusMeters: Int
        get() = prefs.getInt("geofenceRadiusMeters", 100)

    val locationUpdateIntervalMinutes: Int
        get() = prefs.getInt("locationUpdateIntervalMinutes", 5)

    val attendanceDistanceToleranceMeters: Int
        get() = prefs.getInt("attendanceDistanceToleranceMeters", 50)

    val maxVerificationDelayMinutes: Int
        get() = prefs.getInt("maxVerificationDelayMinutes", 30)

    val weightMismatchTolerancePercent: Int
        get() = prefs.getInt("weightMismatchTolerancePercent", 5)

    val blueWasteMinPercentage: Int
        get() = prefs.getInt("blueWasteMinPercentage", 55)

    // Platform info
    val platformName: String
        get() = prefs.getString("platformName", "SmartCBWTF") ?: "SmartCBWTF"

    val supportEmail: String
        get() = prefs.getString("supportEmail", "support@smartcbwtf.com") ?: "support@smartcbwtf.com"

    val supportPhone: String
        get() = prefs.getString("supportPhone", "+91-1800-XXX-XXXX") ?: "+91-1800-XXX-XXXX"

    // Safety controls
    val androidSyncDisabled: Boolean
        get() = prefs.getBoolean("androidSyncDisabled", false)

    val qrVerificationDisabled: Boolean
        get() = prefs.getBoolean("qrVerificationDisabled", false)

    // GPS tracking controls (backend-configurable)
    val gpsEnabled: Boolean
        get() = prefs.getBoolean("gpsEnabled", true)

    val gpsPingIntervalMinutes: Int
        get() = prefs.getInt("gpsPingIntervalMinutes", 5)

    val gpsRequireForeground: Boolean
        get() = prefs.getBoolean("gpsRequireForeground", true)

    // User info (populated from login response / JWT decode)
    val userRole: String
        get() = prefs.getString("userRole", "") ?: ""

    fun setUserRole(role: String) {
        prefs.edit().putString("userRole", role).apply()
    }

    // Subscription status
    val subscriptionActive: Boolean
        get() = prefs.getBoolean("subscriptionActive", true)

    val subscriptionStatus: String
        get() = prefs.getString("subscriptionStatus", "UNKNOWN") ?: "UNKNOWN"

    // Features
    fun isFeatureEnabled(featureKey: String): Boolean {
        return prefs.getBoolean("feature_$featureKey", false)
    }

    /**
     * Update config from backend response.
     */
    fun updateFromResponse(response: MobileConfigResponse) {
        prefs.edit().apply {
            // Subscription
            putBoolean("subscriptionActive", response.active)
            putString("subscriptionStatus", response.subscriptionStatus)

            // Thresholds
            response.thresholds.forEach { (key, value) ->
                when (value) {
                    is Number -> putInt(key, value.toInt())
                    is Boolean -> putBoolean(key, value)
                    is String -> putString(key, value)
                }
            }

            // Features
            response.features.forEach { (key, value) ->
                putBoolean("feature_$key", value)
            }

            putLong("lastConfigSync", System.currentTimeMillis())
            apply()
        }
    }

    /**
     * Check if config needs refresh (older than 1 hour).
     */
    fun needsRefresh(): Boolean {
        val lastSync = prefs.getLong("lastConfigSync", 0)
        val oneHourAgo = System.currentTimeMillis() - (60 * 60 * 1000)
        return lastSync < oneHourAgo
    }

    /**
     * Clear cached config only. Auth/session/location stores share the same
     * encrypted preferences and must remain responsible for their own keys.
     */
    fun clear() {
        prefs.edit().apply {
            staticConfigKeys.forEach(::remove)
            prefs.all.keys
                .filter { it.startsWith("feature_") }
                .forEach(::remove)
            apply()
        }
    }
}
