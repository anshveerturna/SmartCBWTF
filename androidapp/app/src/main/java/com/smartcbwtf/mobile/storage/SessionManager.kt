package com.smartcbwtf.mobile.storage

import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages user session data including authentication token, user info, and facility info.
 */
@Singleton
class SessionManager @Inject constructor(
    private val prefs: SharedPreferences
) {
    
    private val _isLoggedIn = MutableStateFlow(prefs.getString(KEY_TOKEN, null) != null)
    val isLoggedIn: Flow<Boolean> = _isLoggedIn
    
    val userId: String?
        get() = prefs.getString(KEY_USER_ID, null)
    
    val facilityId: String?
        get() = prefs.getString(KEY_FACILITY_ID, null)
    
    val facilityCode: String?
        get() = prefs.getString(KEY_FACILITY_CODE, null)
    
    val userName: String?
        get() = prefs.getString(KEY_USER_NAME, null)
    
    val userRole: String?
        get() = prefs.getString(KEY_USER_ROLE, null)

    suspend fun saveSession(
        token: String,
        userId: String,
        userName: String? = null,
        userRole: String? = null,
        facilityId: String? = null,
        facilityCode: String? = null
    ) = withContext(Dispatchers.IO) {
        prefs.edit().apply {
            putString(KEY_TOKEN, token)
            putString(KEY_USER_ID, userId)
            userName?.let { putString(KEY_USER_NAME, it) }
            userRole?.let { putString(KEY_USER_ROLE, it) }
            facilityId?.let { putString(KEY_FACILITY_ID, it) }
            facilityCode?.let { putString(KEY_FACILITY_CODE, it) }
            apply()
        }
        _isLoggedIn.emit(true)
    }
    
    suspend fun clearSession() = withContext(Dispatchers.IO) {
        prefs.edit().apply {
            remove(KEY_TOKEN)
            remove(KEY_USER_ID)
            remove(KEY_USER_NAME)
            remove(KEY_USER_ROLE)
            remove(KEY_FACILITY_ID)
            remove(KEY_FACILITY_CODE)
            apply()
        }
        _isLoggedIn.emit(false)
    }
    
    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)
    
    fun isUserLoggedIn(): Boolean = getToken() != null

    companion object {
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_FACILITY_ID = "facility_id"
        private const val KEY_FACILITY_CODE = "facility_code"
    }
}
