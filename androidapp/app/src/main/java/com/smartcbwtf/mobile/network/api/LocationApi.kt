package com.smartcbwtf.mobile.network.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * API for GPS location tracking.
 * Uses the mobile GPS ping endpoint for batch location sync.
 */
interface LocationApi {
    
    /**
     * Send batch of GPS events to backend.
     * Supports offline sync with idempotency via clientEventId.
     */
    @POST("mobile/gps/ping")
    suspend fun pingLocation(@Body request: GpsPingRequest): Response<GpsPingResponse>
}

data class GpsPingRequest(
    val events: List<GpsEventItem>
)

data class GpsEventItem(
    val clientEventId: String,
    val latitude: Double,
    val longitude: Double,
    val speed: Double? = null,
    val heading: Double? = null,
    val accuracyM: Double? = null,
    val recordedAt: String // ISO8601 timestamp
)

data class GpsPingResponse(
    val totalReceived: Int,
    val successCount: Int,
    val duplicateCount: Int,
    val successIds: List<String>
)
