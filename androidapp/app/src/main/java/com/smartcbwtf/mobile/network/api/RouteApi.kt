package com.smartcbwtf.mobile.network.api

import com.smartcbwtf.mobile.network.model.MobileRouteResponse
import retrofit2.Response
import retrofit2.http.GET

/**
 * API for route-related mobile endpoints.
 */
interface RouteApi {
    /**
     * Fetch the current user's assigned route.
     * Returns 404 if no route is assigned.
     */
    @GET("mobile/my-route")
    suspend fun getMyRoute(): Response<MobileRouteResponse>
}
