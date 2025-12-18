package com.smartcbwtf.mobile.repository

import android.util.Log
import com.smartcbwtf.mobile.database.dao.AttendanceDao
import com.smartcbwtf.mobile.database.entity.AttendanceEventEntity
import com.smartcbwtf.mobile.network.api.AttendanceApi
import com.smartcbwtf.mobile.network.model.AttendanceSyncItem
import com.smartcbwtf.mobile.network.model.AttendanceSyncRequest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultAttendanceRepository @Inject constructor(
    private val dao: AttendanceDao,
    private val api: AttendanceApi,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : AttendanceRepository {

    companion object {
        private const val TAG = "AttendanceRepo"
    }

    override suspend fun record(event: AttendanceEventEntity) {
        withContext(ioDispatcher) {
            dao.insert(event)
            Log.d(TAG, "Recorded attendance event: ${event.id} at HCF ${event.hcfName}")
        }
    }

    override fun getPending(): Flow<List<AttendanceEventEntity>> = dao.getPending()

    override fun pendingCount(): Flow<Int> = dao.pendingCount()

    override suspend fun syncPending() = withContext(ioDispatcher) {
        val pending = dao.getPendingList()
        if (pending.isEmpty()) {
            Log.d(TAG, "No pending attendance events to sync")
            return@withContext
        }

        Log.d(TAG, "Syncing ${pending.size} attendance events")

        val syncItems = pending.map { it.toSyncItem() }
        val request = AttendanceSyncRequest(events = syncItems)

        try {
            val response = api.sync(request)
            Log.d(TAG, "Sync response: ${response.successCount} success, ${response.failureCount} failed")

            // Mark successful items as synced
            val successIds = response.successIds.mapNotNull { id ->
                runCatching { UUID.fromString(id) }.getOrNull()
            }
            if (successIds.isNotEmpty()) {
                dao.markSynced(successIds)
                Log.d(TAG, "Marked ${successIds.size} events as synced")
            }

            // Update failed items with error messages
            response.results
                .filter { !it.success }
                .forEach { result ->
                    val id = runCatching { UUID.fromString(result.clientEventId) }.getOrNull()
                    if (id != null) {
                        val errorMsg = result.errorMessage ?: result.errorCode ?: "Unknown error"
                        dao.markSyncError(id, errorMsg)
                        Log.w(TAG, "Event $id failed: $errorMsg")
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed: ${e.message}", e)
            throw e
        }
    }

    override suspend fun getLatest(): AttendanceEventEntity? = withContext(ioDispatcher) {
        dao.getLatest()
    }

    override suspend fun isCooldownActive(cooldownDurationMs: Long): Boolean = withContext(ioDispatcher) {
        val cooldownStartMs = System.currentTimeMillis() - cooldownDurationMs
        dao.isCooldownActive(cooldownStartMs)
    }

    override suspend fun getCooldownRemainingMs(cooldownDurationMs: Long): Long = withContext(ioDispatcher) {
        val latest = dao.getLatest() ?: return@withContext 0L
        val cooldownEndMs = latest.eventTs + cooldownDurationMs
        val now = System.currentTimeMillis()
        (cooldownEndMs - now).coerceAtLeast(0L)
    }

    override fun getHistory(limit: Int): Flow<List<AttendanceEventEntity>> = dao.getHistory(limit)
}

private fun AttendanceEventEntity.toSyncItem(): AttendanceSyncItem = AttendanceSyncItem(
    clientEventId = id.toString(),
    hcfId = hcfId,
    eventTs = eventTs,
    gpsLat = gpsLat,
    gpsLon = gpsLon,
    gpsAccuracyM = gpsAccuracyM,
    appDeviceId = deviceId
)
