package com.smartcbwtf.mobile.repository

import com.smartcbwtf.mobile.network.model.AuthResponse
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun login(username: String, password: String): AuthResponse
    suspend fun logout()
    suspend fun currentToken(): String?
    fun getAuthStateFlow(): Flow<String?>
    fun mustChangePassword(): Boolean
}
