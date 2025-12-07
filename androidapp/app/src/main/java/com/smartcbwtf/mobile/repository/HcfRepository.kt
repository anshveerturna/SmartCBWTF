package com.smartcbwtf.mobile.repository

import com.smartcbwtf.mobile.database.entity.HcfEntity
import kotlinx.coroutines.flow.Flow

interface HcfRepository {
    fun getAll(): Flow<List<HcfEntity>>
    suspend fun refresh()
    suspend fun register(
        name: String,
        address: String?,
        city: String?,
        state: String?,
        postalCode: String?,
        phone: String?,
        email: String?,
        beds: Int?,
        latitude: Double?,
        longitude: Double?
    )
}
