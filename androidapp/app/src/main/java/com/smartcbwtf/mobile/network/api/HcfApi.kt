package com.smartcbwtf.mobile.network.api

import com.smartcbwtf.mobile.network.model.HcfDto
import com.smartcbwtf.mobile.network.model.HcfRegistrationRequest
import com.smartcbwtf.mobile.network.model.HcfRegistrationResponse
import com.smartcbwtf.mobile.network.model.TermsResponse
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Body
import retrofit2.http.Query

interface HcfApi {
    @GET("hcfs")
    suspend fun getAll(): List<HcfDto>

    @POST("hcfs/register")
    suspend fun register(@Body request: HcfRegistrationRequest): HcfRegistrationResponse
    
    @GET("terms/latest")
    suspend fun getLatestTerms(@Query("facilityId") facilityId: String? = null): TermsResponse
}
