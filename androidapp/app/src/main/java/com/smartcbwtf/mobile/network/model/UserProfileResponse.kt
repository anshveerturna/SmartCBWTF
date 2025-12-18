package com.smartcbwtf.mobile.network.model

import com.google.gson.annotations.SerializedName

/**
 * User profile response from backend.
 * 
 * This is a READ-ONLY data class. Profile data cannot be
 * modified through the mobile application.
 */
data class UserProfileResponse(
    val id: String,
    val username: String,
    val fullName: String?,
    val email: String?,
    val phone: String?,
    val gender: String?,
    val dob: String?, // ISO date format: "YYYY-MM-DD"
    val role: String,
    @SerializedName("facilityId")
    val facilityId: String?,
    @SerializedName("facilityName")
    val facilityName: String?,
    @SerializedName("profilePhotoUrl")
    val profilePhotoUrl: String?
)
