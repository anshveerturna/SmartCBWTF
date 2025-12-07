package com.smartcbwtf.mobile.repository

interface AuthRepository {
    suspend fun login(username: String, password: String): Boolean
    suspend fun logout()
    suspend fun currentToken(): String?
}
