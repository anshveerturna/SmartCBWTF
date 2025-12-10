package com.smartcbwtf.mobile.repository

import com.smartcbwtf.mobile.network.api.AuthApi
import com.smartcbwtf.mobile.network.model.AuthRequest
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
    private val networkMonitor: NetworkMonitor,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : AuthRepository {

    override suspend fun login(username: String, password: String): Boolean = withContext(ioDispatcher) {
        if (!networkMonitor.isOnline()) {
            throw Exception("No internet connection")
        }
        val response = api.login(AuthRequest(username, password))
        tokenStore.setToken(response.token)
        true
    }

    override suspend fun logout() = withContext(ioDispatcher) {
        tokenStore.setToken(null)
    }

    override suspend fun currentToken(): String? = withContext(ioDispatcher) {
        tokenStore.getToken()
    }

    override fun getAuthStateFlow(): Flow<String?> = tokenStore.getTokenFlow()
}
