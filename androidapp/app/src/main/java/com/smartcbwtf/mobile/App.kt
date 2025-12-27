package com.smartcbwtf.mobile

import android.app.Application
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import androidx.hilt.work.HiltWorkerFactory
import com.smartcbwtf.mobile.work.LocationTrackingWorker
import com.smartcbwtf.mobile.work.SyncBagEventsWorker
import java.util.concurrent.TimeUnit

@HiltAndroidApp
class App : Application(), Configuration.Provider {

	@Inject
	lateinit var workerFactory: HiltWorkerFactory

	override val workManagerConfiguration: Configuration
		get() = Configuration.Builder()
			.setWorkerFactory(workerFactory)
			.build()

	override fun onCreate() {
		super.onCreate()
		WorkManager.initialize(this, workManagerConfiguration)
		scheduleSync()
		scheduleLocationTracking()
	}

	private fun scheduleSync() {
		val request = PeriodicWorkRequestBuilder<SyncBagEventsWorker>(15, TimeUnit.MINUTES).build()
		WorkManager.getInstance(this).enqueueUniquePeriodicWork(
			"bag_sync",
			ExistingPeriodicWorkPolicy.KEEP,
			request
		)
	}

	/**
	 * Schedule location tracking worker as safety net.
	 * Runs every 15 minutes to ensure ForegroundService stays alive and syncs backup location.
	 */
	private fun scheduleLocationTracking() {
		val constraints = Constraints.Builder()
			.setRequiredNetworkType(NetworkType.CONNECTED)
			.build()

		val request = PeriodicWorkRequestBuilder<LocationTrackingWorker>(15, TimeUnit.MINUTES)
			.setConstraints(constraints)
			.build()

		WorkManager.getInstance(this).enqueueUniquePeriodicWork(
			LocationTrackingWorker.WORK_NAME,
			ExistingPeriodicWorkPolicy.KEEP,
			request
		)
	}
}

