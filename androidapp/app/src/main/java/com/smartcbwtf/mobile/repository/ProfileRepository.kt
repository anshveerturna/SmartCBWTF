package com.smartcbwtf.mobile.repository

import com.smartcbwtf.mobile.database.dao.UserProfileDao
import com.smartcbwtf.mobile.database.entity.UserProfileEntity
import com.smartcbwtf.mobile.network.api.ProfileApi
import com.smartcbwtf.mobile.network.model.UserProfileResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for user profile data.
 * 
 * DESIGN PRINCIPLES:
 * 1. Backend is the SINGLE SOURCE OF TRUTH
 * 2. Room cache is for OFFLINE READING ONLY
 * 3. NO local modifications to profile data
 * 
 * This repository intentionally provides NO methods to update profile data.
 * All profile changes must happen at the backend database level.
 */
@Singleton
class ProfileRepository @Inject constructor(
    private val profileApi: ProfileApi,
    private val userProfileDao: UserProfileDao
) {
    
    /**
     * Get the current user's profile.
     * 
     * Strategy:
     * - Online: Fetch from API, cache to Room, return fresh data
     * - Offline: Return cached data if available
     * 
     * @return UserProfileResponse or null if unavailable
     * @throws Exception on network error with no cache
     */
    suspend fun getCurrentUserProfile(): UserProfileResponse {
        return try {
            // Try to fetch fresh data from backend
            val response = profileApi.getCurrentUser()
            
            // Cache for offline use
            userProfileDao.insertProfile(UserProfileEntity.fromResponse(response))
            
            response
        } catch (e: Exception) {
            // Network failed - try to return cached data
            val cached = userProfileDao.getProfile()
            cached?.toResponse() ?: throw e
        }
    }
    
    /**
     * Get cached profile as a Flow for reactive updates.
     * Useful for observing offline data.
     */
    fun getCachedProfileFlow(): Flow<UserProfileResponse?> {
        return userProfileDao.getProfileFlow().map { it?.toResponse() }
    }
    
    /**
     * Clear cached profile on logout.
     */
    suspend fun clearCache() {
        userProfileDao.clearProfile()
    }
}
