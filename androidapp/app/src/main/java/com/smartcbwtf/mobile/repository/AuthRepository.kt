package com.smartcbwtf.mobile.repository

import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun login(username: String, password: String): Boolean
    suspend fun logout()
    suspend fun currentToken(): String?
    fun getAuthStateFlow(): Flow<String?>
}
