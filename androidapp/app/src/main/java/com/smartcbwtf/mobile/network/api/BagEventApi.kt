package com.smartcbwtf.mobile.network.api

import com.smartcbwtf.mobile.network.model.BagEventPayload
import com.smartcbwtf.mobile.network.model.SyncResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface BagEventApi {
    @POST("bags/sync")
    suspend fun sync(@Body payload: List<BagEventPayload>): SyncResponse
}
