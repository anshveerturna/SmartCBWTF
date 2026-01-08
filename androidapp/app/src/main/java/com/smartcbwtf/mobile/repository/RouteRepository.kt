package com.smartcbwtf.mobile.repository

import com.smartcbwtf.mobile.network.api.RouteApi
import com.smartcbwtf.mobile.network.model.MobileRouteResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for route-related operations.
 */
@Singleton
class RouteRepository @Inject constructor(
    private val routeApi: RouteApi
) {

    /**
     * Fetch the current user's assigned route.
     * Returns null if no route is assigned or on error.
     */
    suspend fun getMyRoute(): Result<MobileRouteResponse?> = withContext(Dispatchers.IO) {
        try {
            val response = routeApi.getMyRoute()
            if (response.isSuccessful) {
                Result.success(response.body())
            } else if (response.code() == 404) {
                // No route assigned - this is a valid state
                Result.success(null)
            } else {
                Result.failure(Exception("Failed to fetch route: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
