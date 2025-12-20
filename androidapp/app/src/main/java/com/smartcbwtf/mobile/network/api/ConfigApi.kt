package com.smartcbwtf.mobile.network.api

import retrofit2.Response
import retrofit2.http.GET

/**
 * API for fetching app configuration from backend.
 */
interface ConfigApi {
    @GET("/api/config/mobile")
    suspend fun getMobileConfig(): Response<MobileConfigResponse>
}

/**
 * Response from mobile config endpoint.
 */
data class MobileConfigResponse(
    val subscriptionStatus: String,
    val active: Boolean,
    val features: Map<String, Boolean>,
    val thresholds: Map<String, Any>
)
