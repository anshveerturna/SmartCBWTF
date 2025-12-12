package com.smartcbwtf.mobile.repository

import com.smartcbwtf.mobile.database.entity.HcfEntity
import com.smartcbwtf.mobile.network.model.HcfRegistrationRequest
import com.smartcbwtf.mobile.network.model.HcfRegistrationResponse
import com.smartcbwtf.mobile.network.model.TermsResponse
import kotlinx.coroutines.flow.Flow

interface HcfRepository {
    fun getAll(): Flow<List<HcfEntity>>
    suspend fun refresh()
    
    /**
     * Register a new HCF with full agreement details.
     */
    suspend fun register(request: HcfRegistrationRequest): HcfRegistrationResponse
    
    /**
     * Get the latest active terms and conditions for the facility.
     */
    suspend fun getLatestTerms(facilityId: String? = null): TermsResponse
}
