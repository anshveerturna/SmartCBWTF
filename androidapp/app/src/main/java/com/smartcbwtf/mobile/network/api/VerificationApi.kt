package com.smartcbwtf.mobile.network.api

import com.smartcbwtf.mobile.model.FacilityInfo
import com.smartcbwtf.mobile.model.VerifyBagRequest
import com.smartcbwtf.mobile.model.VerifyBagResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface VerificationApi {
    
    /**
     * Verify a bag at the CBWTF facility
     * Server performs authoritative geofence check and records verification event
     */
    @POST("bags/verify")
    suspend fun verifyBag(@Body request: VerifyBagRequest): Response<VerifyBagResponse>
    
    /**
     * Get facility information including geofence coordinates
     * Used for client-side pre-check before verification
     */
    @GET("facilities/{facilityId}")
    suspend fun getFacility(@Path("facilityId") facilityId: String): Response<FacilityInfo>
    
    /**
     * Get the default/active facility for the current user
     * Returns the CBWTF facility assigned to the driver
     */
    @GET("facilities/active")
    suspend fun getActiveFacility(): Response<FacilityInfo>
}
