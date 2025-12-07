package com.smartcbwtf.mobile.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.smartcbwtf.mobile.database.entity.HcfEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HcfDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<HcfEntity>)

    @Query("SELECT * FROM hcfs ORDER BY name ASC")
    fun getAll(): Flow<List<HcfEntity>>
}
