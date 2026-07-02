package com.smartcbwtf.mobile.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.smartcbwtf.mobile.MainActivity
import com.smartcbwtf.mobile.R
import com.smartcbwtf.mobile.repository.LocationRepository
import com.smartcbwtf.mobile.storage.AppConfigStore
import com.smartcbwtf.mobile.storage.AuthTokenStore
import com.smartcbwtf.mobile.utils.LocationLogSanitizer
import com.smartcbwtf.mobile.utils.Logger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Foreground Service for continuous GPS location tracking during active duty.
 * 
 * SECURITY INVARIANTS:
 * - NEVER starts without explicit user consent
 * - Only runs for DRIVER / PLANT_OPERATOR roles
 * - Respects backend GPS config (can be disabled remotely)
 * - Automatically stops on logout
 */
@AndroidEntryPoint
class ForegroundLocationService : Service() {

    companion object {
        private const val TAG = "ForegroundLocationSvc"
        private const val NOTIFICATION_ID = 2001
        private const val CHANNEL_ID = "location_tracking_channel"
        
        // Allowed roles for GPS tracking
        private val ALLOWED_ROLES = setOf("DRIVER", "PLANT_OPERATOR")
        
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"

        fun startService(context: Context) {
            val intent = Intent(context, ForegroundLocationService::class.java).apply {
                action = ACTION_START
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stopService(context: Context) {
            val intent = Intent(context, ForegroundLocationService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    @Inject lateinit var locationRepository: LocationRepository
    @Inject lateinit var authTokenStore: AuthTokenStore
    @Inject lateinit var appConfigStore: AppConfigStore

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
        
        // Watch for logout - stop service if token cleared
        serviceScope.launch {
            authTokenStore.getTokenFlow().collectLatest { token ->
                if (token == null) {
                    Log.i(TAG, "Token cleared, stopping location service")
                    stopSelf()
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                // CRITICAL: Check all conditions before starting
                if (!canStartTracking()) {
                    Log.w(TAG, "Cannot start tracking - guard check failed")
                    stopSelf()
                    return START_NOT_STICKY
                }
                startLocationTracking()
            }
            ACTION_STOP -> {
                stopLocationUpdates()
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
        serviceScope.cancel()
        Log.i(TAG, "Location service destroyed")
    }

    /**
     * Centralized guard: ALL conditions must be met before tracking starts.
     * 
     * Required conditions:
     * 1. User is logged in (token present)
     * 2. User has granted location consent
     * 3. GPS is enabled in backend config
     * 4. User role is DRIVER or PLANT_OPERATOR
     */
    private fun canStartTracking(): Boolean {
        // Check 1: User must be logged in
        val token = runBlocking { authTokenStore.getToken() }
        if (token == null) {
            Log.w(TAG, "Guard failed: Not logged in")
            return false
        }

        // Check 2: CRITICAL - User must have given consent
        if (!locationRepository.hasLocationConsent()) {
            Log.w(TAG, "Guard failed: No location consent")
            return false
        }

        // Check 3: GPS must be enabled in backend config
        if (!appConfigStore.gpsEnabled) {
            Log.w(TAG, "Guard failed: GPS disabled via config")
            return false
        }

        // Check 4: Role check - only DRIVER and PLANT_OPERATOR
        val userRole = appConfigStore.userRole
        if (userRole !in ALLOWED_ROLES) {
            Log.w(TAG, "Guard failed: Role '$userRole' not allowed for GPS tracking")
            return false
        }

        Log.d(TAG, "All tracking guards passed")
        return true
    }

    private fun startLocationTracking() {
        // Start as foreground service
        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // Check location permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
            != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Location permission not granted")
            stopSelf()
            return
        }

        // Configure location updates
        val intervalMinutes = appConfigStore.gpsPingIntervalMinutes.coerceIn(1, 30)
        val intervalMs = TimeUnit.MINUTES.toMillis(intervalMinutes.toLong())

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
            .setMinUpdateIntervalMillis(intervalMs / 2)
            .setWaitForAccurateLocation(false)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                // Re-check consent on each location update (defense in depth)
                if (!locationRepository.hasLocationConsent()) {
                    Log.w(TAG, "Consent revoked during tracking, stopping")
                    stopSelf()
                    return
                }

                result.lastLocation?.let { location ->
                    Logger.d(TAG, LocationLogSanitizer.locationReceivedMessage(location.accuracy.toDouble()))
                    serviceScope.launch {
                        locationRepository.syncLocation(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            accuracy = location.accuracy.toDouble()
                        )
                    }
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
            Log.i(TAG, "Location tracking started with interval: $intervalMinutes min")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException requesting location updates", e)
            stopSelf()
        }
    }

    private fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
            locationCallback = null
        }
        Log.d(TAG, "Location updates stopped")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Location Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Tracking location for waste pickup operations"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Smart CBWTF Active")
            .setContentText("Tracking location for waste pickup operations")
            .setSmallIcon(R.drawable.ic_location_on)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
