package com.smartcbwtf.mobile.repository

import com.smartcbwtf.mobile.database.entity.AttendanceEventEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * Repository for attendance events with offline-first queue.
 */
interface AttendanceRepository {

    /**
     * Record an attendance event locally.
     * Event will be synced to backend via WorkManager.
     */
    suspend fun record(event: AttendanceEventEntity)

    /**
     * Get all pending (unsynced) attendance events.
     */
    fun getPending(): Flow<List<AttendanceEventEntity>>

    /**
     * Get count of pending events.
     */
    fun pendingCount(): Flow<Int>

    /**
     * Sync all pending events to backend.
     * Called by WorkManager.
     */
    suspend fun syncPending()

    /**
     * Get the most recent attendance event.
     */
    suspend fun getLatest(): AttendanceEventEntity?

    /**
     * Check if cooldown is active.
     * @param cooldownDurationMs Duration of cooldown in milliseconds
     */
    suspend fun isCooldownActive(cooldownDurationMs: Long): Boolean

    /**
     * Get cooldown remaining time in milliseconds.
     * Returns 0 if no cooldown is active.
     */
    suspend fun getCooldownRemainingMs(cooldownDurationMs: Long): Long

    /**
     * Get attendance history.
     */
    fun getHistory(limit: Int = 50): Flow<List<AttendanceEventEntity>>
}
