package com.smartcbwtf.mobile.repository

import com.smartcbwtf.mobile.database.dao.BagEventDao
import com.smartcbwtf.mobile.database.entity.BagEventEntity
import com.smartcbwtf.mobile.model.BagEvent
import com.smartcbwtf.mobile.network.api.BagEventApi
import com.smartcbwtf.mobile.network.model.BagEventPayload
import com.smartcbwtf.mobile.network.model.BagEventSyncRequest
import com.smartcbwtf.mobile.network.model.SyncResponse
import com.smartcbwtf.mobile.utils.Logger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "BagEventSync"
private const val MAX_SYNC_BATCH_SIZE = 500

@Singleton
class DefaultBagEventRepository @Inject constructor(
    private val dao: BagEventDao,
    private val api: BagEventApi,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : BagEventRepository {

    override suspend fun record(event: BagEvent): Unit = withContext(ioDispatcher) {
        Logger.d(TAG, "Recording bag event")
        dao.upsert(event.toEntity())
        Logger.d(TAG, "Bag event saved to local DB")
        
        // Trigger immediate sync attempt
        Logger.d(TAG, "Triggering immediate sync after save")
        try {
            syncPending()
        } catch (e: Exception) {
            Logger.w(TAG, "Immediate sync failed; will retry via WorkManager", e)
        }
    }

    override suspend fun recordBatch(events: List<BagEvent>): Unit = withContext(ioDispatcher) {
        Logger.d(TAG, "Recording bag event batch")
        events.forEach { event -> dao.upsert(event.toEntity()) }
        Logger.d(TAG, "Bag event batch saved to local DB")
        
        // Trigger immediate sync attempt
        Logger.d(TAG, "Triggering immediate sync after batch save")
        try {
            syncPending()
        } catch (e: Exception) {
            Logger.w(TAG, "Immediate sync failed; will retry via WorkManager", e)
        }
    }

    override fun getPending(): Flow<List<BagEvent>> =
        dao.getPending().map { list -> list.map { it.toDomain() } }

    override fun pendingCount(): Flow<Int> = dao.pendingCount()

    override suspend fun markSynced(ids: List<UUID>) = withContext(ioDispatcher) {
        dao.markSynced(ids)
    }

    override suspend fun syncPending() = withContext(ioDispatcher) {
        Logger.d(TAG, "syncPending() called")
        val pendingEntities = dao.getPending().firstOrNull() ?: emptyList()
        Logger.d(TAG, "Found pending bag events to sync")
        if (pendingEntities.isEmpty()) {
            Logger.d(TAG, "No pending events, returning")
            return@withContext
        }
        val syncEntities = pendingEntities.take(MAX_SYNC_BATCH_SIZE)
        val pendingPayloads = syncEntities.map(BagEventEntity::toPayload)

        // Wrap in BagEventSyncRequest to match backend format
        val request = BagEventSyncRequest(events = pendingPayloads)
        Logger.d(TAG, "Calling bag sync API")
        
        try {
            val response = api.sync(request)
            val successIds = resolveSuccessfulEventIds(syncEntities, response.acks)
            if (successIds.isNotEmpty()) {
                dao.markSynced(successIds)
                Logger.d(TAG, "Marked bag events as synced")
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Bag event sync failed", e)
            throw e
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
    gpsAccuracyM = gpsAccuracyM,
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
    gpsAccuracyM = gpsAccuracyM,
    weightKg = weightKg,
    hcfId = hcfId,
    facilityId = facilityId,
    synced = synced,
    deviceId = deviceId,
    driverId = driverId,
)

internal fun BagEventEntity.toPayload(): BagEventPayload = BagEventPayload(
    qrCode = qrCode,
    eventType = eventType,
    eventTs = Instant.ofEpochMilli(eventTs).toString(),  // Convert to ISO-8601 string
    gpsLat = gpsLat,
    gpsLon = gpsLon,
    weightKg = weightKg,
    facilityId = facilityId,
    gpsAccuracyM = gpsAccuracyM,
    appDeviceId = deviceId,
    notes = null,
)

internal fun resolveSuccessfulEventIds(
    pendingEntities: List<BagEventEntity>,
    acks: List<SyncResponse.Ack>
): List<UUID> {
    if (pendingEntities.isEmpty() || acks.isEmpty()) {
        return emptyList()
    }

    val resolved = mutableListOf<UUID>()
    val count = minOf(pendingEntities.size, acks.size)
    for (index in 0 until count) {
        val pending = pendingEntities[index]
        val ack = acks[index]
        if (ack.status == "SUCCESS" && ack.qrCode == pending.qrCode) {
            resolved.add(pending.id)
        }
    }
    return resolved
}
