package com.smartcbwtf.mobile.repository

import android.util.Base64
import android.util.Log
import com.smartcbwtf.mobile.network.api.AuthApi
import com.smartcbwtf.mobile.network.model.AuthRequest
import com.smartcbwtf.mobile.network.model.AuthResponse
import com.smartcbwtf.mobile.storage.AppConfigStore
import com.smartcbwtf.mobile.storage.AuthTokenStore
import com.smartcbwtf.mobile.storage.SessionManager
import com.smartcbwtf.mobile.utils.NetworkMonitor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "AuthRepository"

@Singleton
class DefaultAuthRepository @Inject constructor(
    private val api: AuthApi,
    private val tokenStore: AuthTokenStore,
    private val appConfigStore: AppConfigStore,
    private val sessionManager: SessionManager,
    private val networkMonitor: NetworkMonitor,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : AuthRepository {

    override suspend fun login(username: String, password: String): AuthResponse = withContext(ioDispatcher) {
        if (!networkMonitor.isOnline()) {
            throw Exception("No internet connection")
        }
        val response = api.login(AuthRequest(username, password))
        tokenStore.setToken(response.accessToken)
        
        // Store mustChangePassword flag for security enforcement
        if (response.mustChangePassword) {
            tokenStore.setMustChangePassword(true)
        }

        // Store user role for GPS tracking guard
        response.role?.let { role ->
            appConfigStore.setUserRole(role)
        }
        
        // Extract userId from JWT token and save session data
        val userId = extractUserIdFromJwt(response.accessToken)
        val facilityId = response.tenantId
        
        Log.d(TAG, "Login response: userId=$userId, facilityId=$facilityId, role=${response.role}")
        
        // Save to SessionManager for use in bag events
        sessionManager.saveSession(
            token = response.accessToken,
            userId = userId ?: "",
            userName = response.fullName,
            userRole = response.role,
            facilityId = facilityId
        )
        
        response
    }

    /**
     * Extract user ID (user_id claim) from JWT token
     */
    private fun extractUserIdFromJwt(token: String): String? {
        return try {
            val parts = token.split(".")
            if (parts.size != 3) return null
            
            val payload = parts[1]
            // Add padding if needed
            val paddedPayload = when (payload.length % 4) {
                2 -> "$payload=="
                3 -> "$payload="
                else -> payload
            }
            val decoded = Base64.decode(paddedPayload, Base64.URL_SAFE or Base64.NO_WRAP)
            val json = JSONObject(String(decoded, Charsets.UTF_8))
            // Use user_id claim (UUID), not sub (username)
            json.optString("user_id", null)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract userId from JWT", e)
            null
        }
    }

    override suspend fun logout() = withContext(ioDispatcher) {
        tokenStore.setToken(null)
        tokenStore.setMustChangePassword(false)
        appConfigStore.setUserRole("") // Clear role on logout
        sessionManager.clearSession() // Clear session data
    }

    override suspend fun currentToken(): String? = withContext(ioDispatcher) {
        tokenStore.getToken()
    }

    override fun getAuthStateFlow(): Flow<String?> = tokenStore.getTokenFlow()
    
    override fun mustChangePassword(): Boolean = tokenStore.getMustChangePassword()
}

