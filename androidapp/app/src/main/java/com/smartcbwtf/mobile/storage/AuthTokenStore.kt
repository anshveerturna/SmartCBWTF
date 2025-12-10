package com.smartcbwtf.mobile.storage

import kotlinx.coroutines.flow.Flow

interface AuthTokenStore {
    suspend fun getToken(): String?
    suspend fun setToken(token: String?)
    fun getTokenFlow(): Flow<String?>
}
