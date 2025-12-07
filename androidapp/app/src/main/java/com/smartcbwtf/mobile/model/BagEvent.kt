package com.smartcbwtf.mobile.model

import java.util.UUID

data class BagEvent(
    val id: UUID = UUID.randomUUID(),
    val qrCode: String,
    val eventType: String,
    val eventTs: Long,
    val gpsLat: Double,
    val gpsLon: Double,
    val weightKg: Double,
    val hcfId: String,
    val facilityId: String?,
    val deviceId: String? = null,
    val driverId: String? = null,
    val synced: Boolean = false,
)
