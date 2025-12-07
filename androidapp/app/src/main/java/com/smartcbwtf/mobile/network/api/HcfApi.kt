package com.smartcbwtf.mobile.network.api

import com.smartcbwtf.mobile.network.model.HcfDto
import com.smartcbwtf.mobile.network.model.HcfRegistrationRequest
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Body

interface HcfApi {
    @GET("hcfs")
    suspend fun getAll(): List<HcfDto>

    @POST("hcfs")
    suspend fun register(@Body request: HcfRegistrationRequest)
}
