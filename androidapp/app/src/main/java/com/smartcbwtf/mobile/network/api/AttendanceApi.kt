package com.smartcbwtf.mobile.network.api

import com.smartcbwtf.mobile.network.model.AttendanceSyncRequest
import com.smartcbwtf.mobile.network.model.AttendanceSyncResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface AttendanceApi {
    @POST("attendance/sync")
    suspend fun sync(@Body request: AttendanceSyncRequest): AttendanceSyncResponse
}
