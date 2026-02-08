package com.smartcbwtf.mobile.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.smartcbwtf.mobile.database.entity.BagEventEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface BagEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<BagEventEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: BagEventEntity)

    @Query("SELECT * FROM bag_events WHERE synced = 0 ORDER BY eventTs ASC")
    fun getPending(): Flow<List<BagEventEntity>>

    @Query("SELECT COUNT(*) FROM bag_events WHERE synced = 0")
    fun pendingCount(): Flow<Int>

    @Query("SELECT * FROM bag_events WHERE id = :id")
    suspend fun findById(id: UUID): BagEventEntity?

    @Query("UPDATE bag_events SET synced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<UUID>)

    @Query("DELETE FROM bag_events WHERE id = :id")
    suspend fun deleteById(id: UUID)

    @Update
    suspend fun update(entity: BagEventEntity)
}
