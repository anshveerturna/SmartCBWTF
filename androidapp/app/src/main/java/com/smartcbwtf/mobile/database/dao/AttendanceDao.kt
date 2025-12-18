package com.smartcbwtf.mobile.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.smartcbwtf.mobile.database.entity.AttendanceEventEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface AttendanceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: AttendanceEventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<AttendanceEventEntity>)

    @Update
    suspend fun update(event: AttendanceEventEntity)

    @Query("SELECT * FROM attendance_events WHERE synced = 0 ORDER BY eventTs ASC")
    fun getPending(): Flow<List<AttendanceEventEntity>>

    @Query("SELECT * FROM attendance_events WHERE synced = 0 ORDER BY eventTs ASC")
    suspend fun getPendingList(): List<AttendanceEventEntity>

    @Query("SELECT COUNT(*) FROM attendance_events WHERE synced = 0")
    fun pendingCount(): Flow<Int>

    @Query("SELECT * FROM attendance_events WHERE id = :id")
    suspend fun findById(id: UUID): AttendanceEventEntity?

    @Query("UPDATE attendance_events SET synced = 1, syncedAt = :syncedAt, syncError = NULL WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<UUID>, syncedAt: Long = System.currentTimeMillis())

    @Query("UPDATE attendance_events SET syncError = :error WHERE id = :id")
    suspend fun markSyncError(id: UUID, error: String)

    @Query("DELETE FROM attendance_events WHERE id = :id")
    suspend fun deleteById(id: UUID)

    /**
     * Get the most recent attendance event (for cooldown checking).
     * Returns the latest event regardless of sync status.
     */
    @Query("SELECT * FROM attendance_events ORDER BY eventTs DESC LIMIT 1")
    suspend fun getLatest(): AttendanceEventEntity?

    /**
     * Get attendance history for display.
     */
    @Query("SELECT * FROM attendance_events ORDER BY eventTs DESC LIMIT :limit")
    fun getHistory(limit: Int = 50): Flow<List<AttendanceEventEntity>>

    /**
     * Check if cooldown is active (any attendance within cooldown window).
     */
    @Query("SELECT COUNT(*) > 0 FROM attendance_events WHERE eventTs > :cooldownStartMs")
    suspend fun isCooldownActive(cooldownStartMs: Long): Boolean
}
