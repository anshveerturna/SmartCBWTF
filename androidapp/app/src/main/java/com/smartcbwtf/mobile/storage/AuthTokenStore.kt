package com.smartcbwtf.mobile.storage

interface AuthTokenStore {
    suspend fun getToken(): String?
    suspend fun setToken(token: String?)
}
