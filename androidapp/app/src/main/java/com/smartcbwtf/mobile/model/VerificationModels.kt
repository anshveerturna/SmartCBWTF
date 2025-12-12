package com.smartcbwtf.mobile.model

import java.util.UUID

/**
 * Data class representing a facility (CBWTF plant) with geofence information
 */
data class FacilityInfo(
    val id: String,
    val code: String,
    val name: String,
    val gpsLat: Double?,
    val gpsLon: Double?,
    val geofenceRadiusM: Int?
)

/**
 * Request payload for verifying a bag at CBWTF
 */
data class VerifyBagRequest(
    val bagLabelId: String? = null,
    val qrCode: String,
    val eventType: String = "CBWTF_VERIFICATION",
    val gpsLat: Double,
    val gpsLon: Double,
    val gpsAccuracy: Float,
    val weightKg: Double,
    val deviceId: String,
    val verifiedByUserId: String,
    val notes: String? = null
)

/**
 * Response from bag verification endpoint
 */
data class VerifyBagResponse(
    val status: String,           // "OK" or error status
    val bagLabelId: String?,
    val verifiedAt: String?,      // ISO timestamp from server
    val anomalyState: String?,    // "OK" | "MISMATCH" | "OUT_OF_GEOFENCE"
    val deltaKg: Double?,
    val message: String? = null
)

/**
 * Error response for geofence rejection
 */
data class GeofenceErrorResponse(
    val status: Int,
    val error: String,
    val message: String,
    val distanceMeters: Double?,
    val allowedRadiusMeters: Int?
)
