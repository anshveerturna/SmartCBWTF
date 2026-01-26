package com.smartcbwtf.mobile.repository

import android.util.Log
import com.smartcbwtf.mobile.database.dao.BagEventDao
import com.smartcbwtf.mobile.database.entity.BagEventEntity
import com.smartcbwtf.mobile.model.BagEvent
import com.smartcbwtf.mobile.network.api.BagEventApi
import com.smartcbwtf.mobile.network.model.BagEventPayload
import com.smartcbwtf.mobile.network.model.BagEventSyncRequest
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

@Singleton
class DefaultBagEventRepository @Inject constructor(
    private val dao: BagEventDao,
    private val api: BagEventApi,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : BagEventRepository {

    override suspend fun record(event: BagEvent): Unit = withContext(ioDispatcher) {
        Log.d(TAG, "Recording BagEvent: qr=${event.qrCode}, type=${event.eventType}, driverId=${event.driverId}, facilityId=${event.facilityId}")
        dao.upsert(event.toEntity())
        Log.d(TAG, "BagEvent saved to local DB")
        
        // Trigger immediate sync attempt
        Log.d(TAG, "Triggering immediate sync after save")
        try {
            syncPending()
        } catch (e: Exception) {
            Log.w(TAG, "Immediate sync failed, will retry via WorkManager: ${e.message}")
        }
    }

    override suspend fun recordBatch(events: List<BagEvent>): Unit = withContext(ioDispatcher) {
        Log.d(TAG, "Recording batch of ${events.size} BagEvents")
        events.forEach { event ->
            Log.d(TAG, "Batch item: qr=${event.qrCode}, driverId=${event.driverId}, facilityId=${event.facilityId}")
            dao.upsert(event.toEntity())
        }
        Log.d(TAG, "Batch saved to local DB")
        
        // Trigger immediate sync attempt
        Log.d(TAG, "Triggering immediate sync after batch save")
        try {
            syncPending()
        } catch (e: Exception) {
            Log.w(TAG, "Immediate sync failed, will retry via WorkManager: ${e.message}")
        }
    }

    override fun getPending(): Flow<List<BagEvent>> =
        dao.getPending().map { list -> list.map { it.toDomain() } }

    override fun pendingCount(): Flow<Int> = dao.pendingCount()

    override suspend fun markSynced(ids: List<UUID>) = withContext(ioDispatcher) {
        dao.markSynced(ids)
    }

    override suspend fun syncPending() = withContext(ioDispatcher) {
        Log.d(TAG, "syncPending() called")
        val pending = dao.getPending().firstOrNull()?.map(BagEventEntity::toPayload) ?: emptyList()
        Log.d(TAG, "Found ${pending.size} pending events to sync")
        if (pending.isEmpty()) {
            Log.d(TAG, "No pending events, returning")
            return@withContext
        }

        // Log payload details
        pending.forEach { p ->
            Log.d(TAG, "Payload: qr=${p.qrCode}, collectedByUserId=${p.collectedByUserId}, facilityId=${p.facilityId}")
        }

        // Wrap in BagEventSyncRequest to match backend format
        val request = BagEventSyncRequest(events = pending)
        Log.d(TAG, "Calling api.sync() with ${pending.size} events")
        
        try {
            val response = api.sync(request)
            Log.d(TAG, "Sync response: successQrCodes=${response.successQrCodes}")
            
            // Mark successfully synced events by QR code
            val successQrCodes = response.successQrCodes
            if (successQrCodes.isNotEmpty()) {
                dao.markSyncedByQrCodes(successQrCodes)
                Log.d(TAG, "Marked ${successQrCodes.size} events as synced")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed: ${e.message}", e)
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
    qrCode = qrCode,
    eventType = eventType,
    eventTs = Instant.ofEpochMilli(eventTs).toString(),  // Convert to ISO-8601 string
    gpsLat = gpsLat,
    gpsLon = gpsLon,
    weightKg = weightKg,
    collectedByUserId = driverId ?: id.toString(),  // Use driverId as collectedByUserId
    facilityId = facilityId,
    gpsAccuracyM = null,  // TODO: Add GPS accuracy tracking
    appDeviceId = deviceId,
    notes = null,
)
