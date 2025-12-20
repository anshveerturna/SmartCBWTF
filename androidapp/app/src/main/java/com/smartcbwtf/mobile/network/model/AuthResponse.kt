package com.smartcbwtf.mobile.network.model

/**
 * Response from login endpoint.
 * Backend now returns additional fields for security enforcement.
 */
data class AuthResponse(
    val accessToken: String,
    val tokenType: String = "Bearer",
    val role: String? = null,
    val mustChangePassword: Boolean = false,
    val fullName: String? = null,
    val tenantId: String? = null,
    val hcfId: String? = null
)
