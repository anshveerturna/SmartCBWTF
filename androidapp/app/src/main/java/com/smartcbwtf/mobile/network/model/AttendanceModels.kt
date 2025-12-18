package com.smartcbwtf.mobile.network.model

import java.util.UUID

/**
 * Single attendance event item for sync request.
 */
data class AttendanceSyncItem(
    val clientEventId: String,
    val hcfId: String,
    val eventTs: Long,        // Unix millis - will be converted to ISO-8601 by Gson adapter if needed
    val gpsLat: Double,
    val gpsLon: Double,
    val gpsAccuracyM: Float? = null,
    val appDeviceId: String? = null
)

/**
 * Batch sync request payload.
 */
data class AttendanceSyncRequest(
    val events: List<AttendanceSyncItem>
)

/**
 * Result for a single attendance sync item.
 */
data class AttendanceSyncItemResult(
    val clientEventId: String,
    val success: Boolean,
    val errorCode: String? = null,
    val errorMessage: String? = null,
    val cooldownRemainingMs: Long? = null
)

/**
 * Response from attendance sync endpoint.
 */
data class AttendanceSyncResponse(
    val totalReceived: Int,
    val successCount: Int,
    val failureCount: Int,
    val successIds: List<String> = emptyList(),
    val results: List<AttendanceSyncItemResult> = emptyList()
)
