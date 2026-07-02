package com.smartcbwtf.mobile.network.model

import com.google.gson.annotations.SerializedName

/**
 * Wrapper request to match backend's BagEventSyncRequest
 */
data class BagEventSyncRequest(
    val events: List<BagEventPayload>
)

/**
 * Matches backend's BagEventSyncItem structure
 */
data class BagEventPayload(
    val qrCode: String,
    val eventType: String,
    @SerializedName("eventTs")
    val eventTs: String,  // ISO-8601 instant string
    val gpsLat: Double,
    val gpsLon: Double,
    val weightKg: Double,
    val facilityId: String?,
    val gpsAccuracyM: Double?,
    val appDeviceId: String?,
    val notes: String?,
)
