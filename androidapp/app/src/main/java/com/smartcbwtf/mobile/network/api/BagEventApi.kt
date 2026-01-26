package com.smartcbwtf.mobile.network.api

import com.smartcbwtf.mobile.network.model.BagEventSyncRequest
import com.smartcbwtf.mobile.network.model.SyncResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface BagEventApi {
    @POST("bags/events/sync")
    suspend fun sync(@Body payload: BagEventSyncRequest): SyncResponse
}
