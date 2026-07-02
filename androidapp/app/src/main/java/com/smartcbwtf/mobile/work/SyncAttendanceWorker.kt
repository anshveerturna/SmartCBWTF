package com.smartcbwtf.mobile.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.smartcbwtf.mobile.repository.AttendanceRepository
import com.smartcbwtf.mobile.utils.Logger
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * WorkManager worker to sync pending attendance events to backend.
 * Scheduled after each attendance marking and periodically.
 */
@HiltWorker
class SyncAttendanceWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val repository: AttendanceRepository,
) : CoroutineWorker(appContext, params) {

    companion object {
        const val TAG = "SyncAttendanceWorker"
        const val WORK_NAME = "sync_attendance"
    }

    override suspend fun doWork(): Result {
        Logger.d(TAG, "Starting attendance sync work")
        return try {
            repository.syncPending()
            Logger.d(TAG, "Attendance sync completed successfully")
            Result.success()
        } catch (e: Exception) {
            Logger.e(TAG, "Attendance sync failed", e)
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}
