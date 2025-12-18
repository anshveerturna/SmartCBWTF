package com.smartcbwtf.mobile.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.smartcbwtf.mobile.database.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for user profile cache operations.
 * 
 * DESIGN NOTE: This DAO is intentionally limited to:
 * - Insert/replace (to cache fresh data from backend)
 * - Query (to read cached data for offline use)
 * - Delete (to clear cache on logout)
 * 
 * There are NO update operations because profile data
 * is never modified on the client side.
 */
@Dao
interface UserProfileDao {
    
    /**
     * Insert or replace the cached profile.
     * Called after fetching fresh data from backend.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: UserProfileEntity)
    
    /**
     * Get the cached profile as a Flow for reactive updates.
     */
    @Query("SELECT * FROM user_profile LIMIT 1")
    fun getProfileFlow(): Flow<UserProfileEntity?>
    
    /**
     * Get the cached profile (one-shot).
     */
    @Query("SELECT * FROM user_profile LIMIT 1")
    suspend fun getProfile(): UserProfileEntity?
    
    /**
     * Clear cached profile on logout.
     */
    @Query("DELETE FROM user_profile")
    suspend fun clearProfile()
}
