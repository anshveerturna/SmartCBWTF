package com.smartcbwtf.mobile

import android.app.Application
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import androidx.hilt.work.HiltWorkerFactory
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
		scheduleSync()
	}

	private fun scheduleSync() {
		val request = PeriodicWorkRequestBuilder<SyncBagEventsWorker>(15, TimeUnit.MINUTES).build()
		WorkManager.getInstance(this).enqueueUniquePeriodicWork(
			"bag_sync",
			ExistingPeriodicWorkPolicy.KEEP,
			request
		)
	}
}
