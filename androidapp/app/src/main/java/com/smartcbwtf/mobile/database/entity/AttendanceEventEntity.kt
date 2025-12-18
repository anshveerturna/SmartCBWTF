package com.smartcbwtf.mobile.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Room entity for attendance events, supporting offline queue.
 * Synced to backend via WorkManager.
 */
@Entity(tableName = "attendance_events")
data class AttendanceEventEntity(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val hcfId: String,
    val hcfName: String,
    val eventTs: Long,           // Unix millis
    val gpsLat: Double,
    val gpsLon: Double,
    val gpsAccuracyM: Float? = null,
    val distanceFromHcfM: Double,
    val deviceId: String? = null,
    val synced: Boolean = false,
    val syncedAt: Long? = null,
    val syncError: String? = null,   // Last sync error if any
    val createdAt: Long = System.currentTimeMillis()
)
