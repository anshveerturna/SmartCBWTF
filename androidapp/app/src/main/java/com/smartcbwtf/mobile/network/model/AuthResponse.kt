package com.smartcbwtf.mobile.network.model

data class AuthResponse(
    val accessToken: String,
    val tokenType: String = "Bearer"
)

