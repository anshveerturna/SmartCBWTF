package com.smartcbwtf.mobile.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.smartcbwtf.mobile.network.model.UserProfileResponse

/**
 * Room entity for caching user profile data offline.
 * 
 * DESIGN NOTE: This entity stores a SINGLE user profile (the currently logged-in user).
 * It is used ONLY for READ operations to display the profile when offline.
 * Profile data is NEVER modified on the client side.
 * 
 * The backend is the single source of truth for all profile data.
 */
@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey
    val id: String,
    val username: String,
    val fullName: String?,
    val email: String?,
    val phone: String?,
    val gender: String?,
    val dob: String?,
    val role: String,
    val facilityId: String?,
    val facilityName: String?,
    val profilePhotoUrl: String?,
    val cachedAt: Long = System.currentTimeMillis()
) {
    companion object {
        /**
         * Convert API response to Room entity for caching.
         */
        fun fromResponse(response: UserProfileResponse): UserProfileEntity {
            return UserProfileEntity(
                id = response.id,
                username = response.username,
                fullName = response.fullName,
                email = response.email,
                phone = response.phone,
                gender = response.gender,
                dob = response.dob,
                role = response.role,
                facilityId = response.facilityId,
                facilityName = response.facilityName,
                profilePhotoUrl = response.profilePhotoUrl
            )
        }
    }
    
    /**
     * Convert cached entity back to response format for UI.
     */
    fun toResponse(): UserProfileResponse {
        return UserProfileResponse(
            id = id,
            username = username,
            fullName = fullName,
            email = email,
            phone = phone,
            gender = gender,
            dob = dob,
            role = role,
            facilityId = facilityId,
            facilityName = facilityName,
            profilePhotoUrl = profilePhotoUrl
        )
    }
}
