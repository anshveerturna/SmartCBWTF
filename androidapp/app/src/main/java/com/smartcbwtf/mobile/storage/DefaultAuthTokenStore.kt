package com.smartcbwtf.mobile.storage

import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Secure storage for authentication token and security flags.
 * Uses EncryptedSharedPreferences (injected via Hilt) for secure persistence.
 * 
 * SECURITY: mustChangePassword flag is persisted to ensure enforcement
 * survives app restart - users cannot bypass by force-killing the app.
 */
@Singleton
class DefaultAuthTokenStore @Inject constructor(
    private val prefs: SharedPreferences
) : AuthTokenStore {

    private val _tokenFlow = MutableStateFlow(prefs.getString(KEY_TOKEN, null))
    
    // In-memory cache backed by persistent storage
    @Volatile
    private var _mustChangePassword: Boolean = prefs.getBoolean(KEY_MUST_CHANGE_PASSWORD, false)

    override suspend fun getToken(): String? = withContext(Dispatchers.IO) {
        val token = prefs.getString(KEY_TOKEN, null)
        Log.d(TAG, "getToken: ${if (token != null) "present (${token.length} chars)" else "null"}")
        token
    }

    override suspend fun setToken(token: String?) {
        withContext(Dispatchers.IO) {
            Log.d(TAG, "setToken: ${if (token != null) "storing (${token.length} chars)" else "clearing"}")
            prefs.edit().putString(KEY_TOKEN, token).apply()
            _tokenFlow.emit(token)
            
            // Clear mustChangePassword when token is cleared (logout)
            if (token == null) {
                setMustChangePassword(false)
            }
        }
    }

    override fun getTokenFlow(): Flow<String?> = _tokenFlow

    /**
     * Get the mustChangePassword flag.
     * This is read from persistent storage on first access, then cached.
     * 
     * SECURITY: Returns true if user must change password before accessing app.
     */
    override fun getMustChangePassword(): Boolean {
        // Refresh from storage to ensure we have latest value
        _mustChangePassword = prefs.getBoolean(KEY_MUST_CHANGE_PASSWORD, false)
        Log.d(TAG, "getMustChangePassword: $_mustChangePassword")
        return _mustChangePassword
    }

    /**
     * Set the mustChangePassword flag.
     * Persisted immediately to survive app restart/kill.
     * 
     * SECURITY: This MUST be persisted, not just in-memory, to prevent bypass.
     */
    override fun setMustChangePassword(required: Boolean) {
        Log.d(TAG, "setMustChangePassword: $required")
        _mustChangePassword = required
        prefs.edit().putBoolean(KEY_MUST_CHANGE_PASSWORD, required).commit() // commit() for immediate persistence
    }

    /**
     * Clear all auth state (for complete logout).
     */
    fun clearAll() {
        Log.d(TAG, "clearAll: clearing all auth state")
        prefs.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_MUST_CHANGE_PASSWORD)
            .commit()
        _mustChangePassword = false
    }

    companion object {
        private const val TAG = "AuthTokenStore"
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_MUST_CHANGE_PASSWORD = "must_change_password"
    }
}
