package com.smartcbwtf.mobile.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "bag_events")
data class BagEventEntity(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val qrCode: String,
    val eventType: String,
    val eventTs: Long,
    val gpsLat: Double,
    val gpsLon: Double,
    val weightKg: Double,
    val hcfId: String,
    val facilityId: String?,
    val synced: Boolean = false,
    val deviceId: String? = null,
    val driverId: String? = null,
)
