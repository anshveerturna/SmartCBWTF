package com.smartcbwtf.mobile.network.model

import java.util.UUID

data class BagEventPayload(
    val id: UUID,
    val qrCode: String,
    val eventType: String,
    val eventTs: Long,
    val gpsLat: Double,
    val gpsLon: Double,
    val weightKg: Double,
    val hcfId: String,
    val facilityId: String?,
    val deviceId: String?,
    val driverId: String?,
)
