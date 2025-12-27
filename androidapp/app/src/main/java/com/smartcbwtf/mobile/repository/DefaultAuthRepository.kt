package com.smartcbwtf.mobile.repository

import com.smartcbwtf.mobile.network.api.AuthApi
import com.smartcbwtf.mobile.network.model.AuthRequest
import com.smartcbwtf.mobile.network.model.AuthResponse
import com.smartcbwtf.mobile.storage.AppConfigStore
import com.smartcbwtf.mobile.storage.AuthTokenStore
import com.smartcbwtf.mobile.utils.NetworkMonitor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultAuthRepository @Inject constructor(
    private val api: AuthApi,
    private val tokenStore: AuthTokenStore,
    private val appConfigStore: AppConfigStore,
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
        
        response
    }

    override suspend fun logout() = withContext(ioDispatcher) {
        tokenStore.setToken(null)
        tokenStore.setMustChangePassword(false)
        appConfigStore.setUserRole("") // Clear role on logout
    }

    override suspend fun currentToken(): String? = withContext(ioDispatcher) {
        tokenStore.getToken()
    }

    override fun getAuthStateFlow(): Flow<String?> = tokenStore.getTokenFlow()
    
    override fun mustChangePassword(): Boolean = tokenStore.getMustChangePassword()
}

