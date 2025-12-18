package com.smartcbwtf.mobile.network.api

import com.smartcbwtf.mobile.network.model.UserProfileResponse
import retrofit2.http.GET

/**
 * Profile API for fetching user profile data.
 * 
 * IMPORTANT: This API is READ-ONLY by design.
 * Profile data is centrally managed at the backend level.
 * There are intentionally NO mutation endpoints (POST/PUT/PATCH).
 */
interface ProfileApi {
    
    /**
     * Get the current authenticated user's profile.
     * Used for identity confirmation only - not for editing.
     */
    @GET("users/me")
    suspend fun getCurrentUser(): UserProfileResponse
}
