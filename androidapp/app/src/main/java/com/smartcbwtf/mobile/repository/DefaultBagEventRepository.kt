package com.smartcbwtf.mobile.repository

import com.smartcbwtf.mobile.database.dao.BagEventDao
import com.smartcbwtf.mobile.database.entity.BagEventEntity
import com.smartcbwtf.mobile.model.BagEvent
import com.smartcbwtf.mobile.network.api.BagEventApi
import com.smartcbwtf.mobile.network.model.BagEventPayload
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultBagEventRepository @Inject constructor(
    private val dao: BagEventDao,
    private val api: BagEventApi,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : BagEventRepository {

    override suspend fun record(event: BagEvent) = withContext(ioDispatcher) {
        dao.upsert(event.toEntity())
    }

    override suspend fun recordBatch(events: List<BagEvent>) = withContext(ioDispatcher) {
        events.forEach { event ->
            dao.upsert(event.toEntity())
        }
    }

    override fun getPending(): Flow<List<BagEvent>> =
        dao.getPending().map { list -> list.map { it.toDomain() } }

    override fun pendingCount(): Flow<Int> = dao.pendingCount()

    override suspend fun markSynced(ids: List<UUID>) = withContext(ioDispatcher) {
        dao.markSynced(ids)
    }

    override suspend fun syncPending() = withContext(ioDispatcher) {
        val pending = dao.getPending().firstOrNull()?.map(BagEventEntity::toPayload) ?: emptyList()
        if (pending.isEmpty()) return@withContext

        val response = api.sync(pending)
        val successIds = response.successIds.mapNotNull { id -> runCatching { UUID.fromString(id) }.getOrNull() }
        if (successIds.isNotEmpty()) {
            dao.markSynced(successIds)
        }
    }
}

private fun BagEvent.toEntity(): BagEventEntity = BagEventEntity(
    id = id,
    qrCode = qrCode,
    eventType = eventType,
    eventTs = eventTs,
    gpsLat = gpsLat,
    gpsLon = gpsLon,
    weightKg = weightKg,
    hcfId = hcfId,
    facilityId = facilityId,
    synced = synced,
    deviceId = deviceId,
    driverId = driverId,
)

private fun BagEventEntity.toDomain(): BagEvent = BagEvent(
    id = id,
    qrCode = qrCode,
    eventType = eventType,
    eventTs = eventTs,
    gpsLat = gpsLat,
    gpsLon = gpsLon,
    weightKg = weightKg,
    hcfId = hcfId,
    facilityId = facilityId,
    synced = synced,
    deviceId = deviceId,
    driverId = driverId,
)

private fun BagEventEntity.toPayload(): BagEventPayload = BagEventPayload(
    id = id,
    qrCode = qrCode,
    eventType = eventType,
    eventTs = eventTs,
    gpsLat = gpsLat,
    gpsLon = gpsLon,
    weightKg = weightKg,
    hcfId = hcfId,
    facilityId = facilityId,
    deviceId = deviceId,
    driverId = driverId,
)
