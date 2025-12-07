package com.smartcbwtf.mobile.network.api

import com.smartcbwtf.mobile.network.model.AuthRequest
import com.smartcbwtf.mobile.network.model.AuthResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("auth/login")
    suspend fun login(@Body request: AuthRequest): AuthResponse
}
