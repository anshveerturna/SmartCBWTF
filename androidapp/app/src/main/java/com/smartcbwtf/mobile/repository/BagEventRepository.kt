package com.smartcbwtf.mobile.repository

import com.smartcbwtf.mobile.model.BagEvent
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface BagEventRepository {
    suspend fun record(event: BagEvent)
    fun getPending(): Flow<List<BagEvent>>
    fun pendingCount(): Flow<Int>
    suspend fun markSynced(ids: List<UUID>)
    suspend fun syncPending()
}
