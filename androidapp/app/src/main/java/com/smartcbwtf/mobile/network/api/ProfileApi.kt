package com.smartcbwtf.mobile.network.api

import com.smartcbwtf.mobile.network.model.UserProfileResponse
import com.smartcbwtf.mobile.ui.ChangePasswordRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Profile API for fetching user profile data.
 * 
 * IMPORTANT: This API is READ-ONLY by design EXCEPT for password change.
 * Profile data is centrally managed at the backend level.
 */
interface ProfileApi {
    
    /**
     * Get the current authenticated user's profile.
     * Used for identity confirmation only - not for editing.
     */
    @GET("users/me")
    suspend fun getCurrentUser(): UserProfileResponse

    /**
     * Change the current user's password.
     * Required when mustChangePassword flag is set.
     */
    @POST("users/me/change-password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): Response<Unit>
}
