package com.smartcbwtf.mobile

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.smartcbwtf.mobile.databinding.ActivityMainBinding
import com.smartcbwtf.mobile.repository.AuthRepository
import com.smartcbwtf.mobile.repository.LocationRepository
import com.smartcbwtf.mobile.security.SessionLaunchGuard
import com.smartcbwtf.mobile.service.ForegroundLocationService
import com.smartcbwtf.mobile.storage.AppConfigStore
import com.smartcbwtf.mobile.storage.SessionManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Main Activity with navigation lock for security enforcement.
 * 
 * SECURITY: When mustChangePassword is true, user is locked to password change screen.
 * Cannot bypass via: back button, deep links, saved state restore.
 * 
 * SESSION MANAGEMENT: Re-initializes critical services when app resumes from background
 * to ensure proper functionality after process death or app restart.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var navController: NavController
    private lateinit var appUpdateManager: AppUpdateManager
    private var updateReadySnackbar: Snackbar? = null

    private val updateResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) {
            Log.w(TAG, "In-app update flow ended with result ${result.resultCode}")
        }
    }

    private val installStateListener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            showUpdateReadyPrompt()
        }
    }

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var locationRepository: LocationRepository

    @Inject
    lateinit var appConfigStore: AppConfigStore

    @Inject
    lateinit var sessionManager: SessionManager

    // Track if we've already initialized services this session
    private var servicesInitialized = false

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        appUpdateManager = AppUpdateManagerFactory.create(this)
        appUpdateManager.registerListener(installStateListener)
        checkForAppUpdate()

        setSupportActionBar(binding.toolbar)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        
        // Define top-level destinations (no back button for these)
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.splashFragment,
                R.id.loginFragment,
                R.id.homeFragment,
                R.id.changePasswordFragment // Also top-level when locked
            )
        )
        
        setupActionBarWithNavController(navController, appBarConfiguration)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (authRepository.mustChangePassword()) {
                    Toast.makeText(
                        this@MainActivity,
                        "You must change your password or logout to continue",
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }

                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
                isEnabled = true
            }
        })
        
        // Navigation lock: Intercept ALL navigation when mustChangePassword is true
        navController.addOnDestinationChangedListener { _, destination, _ ->
            Log.d(TAG, "Navigation to: ${destination.label}")
            
            // Check if user must change password
            if (authRepository.mustChangePassword() && isProtectedDestination(destination.id)) {
                Log.w(TAG, "BLOCKED navigation to ${destination.label} - mustChangePassword is true")
                
                // Force redirect to change password screen
                navController.navigate(
                    R.id.changePasswordFragment,
                    null,
                    NavOptions.Builder()
                        .setPopUpTo(R.id.nav_graph, true) // Clear entire back stack
                        .build()
                )
                
                Toast.makeText(
                    this,
                    "You must change your password before continuing",
                    Toast.LENGTH_LONG
                ).show()
                return@addOnDestinationChangedListener
            }
            
            // Hide action bar for full-screen fragments
            when (destination.id) {
                R.id.splashFragment, R.id.loginFragment, R.id.homeFragment -> {
                    supportActionBar?.hide()
                }
                else -> {
                    supportActionBar?.show()
                }
            }
        }

        // Global auth state observer (lifecycle-aware)
        // If token is cleared (e.g. 401 Unauthorized), force navigation to login
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                authRepository.getAuthStateFlow().collect { token ->
                    if (token == null) {
                        // Token cleared — stop services and redirect to login
                        servicesInitialized = false
                        ForegroundLocationService.stopService(this@MainActivity)

                        val currentDest = navController.currentDestination?.id
                        if (currentDest != R.id.loginFragment && currentDest != R.id.splashFragment) {
                            Log.i(TAG, "Token cleared (remote logout), redirecting to Login")
                            navController.navigate(
                                R.id.loginFragment,
                                null,
                                NavOptions.Builder()
                                    .setPopUpTo(R.id.nav_graph, true)
                                    .build()
                            )
                            Toast.makeText(this@MainActivity, "Session expired. Please login again.", Toast.LENGTH_LONG).show()
                        }
                    } else if (!servicesInitialized && SessionLaunchGuard.canInitializeOperationalServices(
                            token,
                            authRepository.mustChangePassword()
                        )) {
                        // Token present but services not started — reinitialize
                        reinitializeServices()
                    } else if (authRepository.mustChangePassword()) {
                        servicesInitialized = false
                        ForegroundLocationService.stopService(this@MainActivity)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (BuildConfig.DEBUG || !::appUpdateManager.isInitialized) return

        appUpdateManager.appUpdateInfo.addOnSuccessListener { updateInfo ->
            if (updateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                showUpdateReadyPrompt()
            }
        }
    }

    override fun onDestroy() {
        if (::appUpdateManager.isInitialized) {
            appUpdateManager.unregisterListener(installStateListener)
        }
        updateReadySnackbar?.dismiss()
        super.onDestroy()
    }

    private fun checkForAppUpdate() {
        if (BuildConfig.DEBUG) return

        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { updateInfo ->
                val flexibleUpdateAvailable =
                    updateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                        updateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
                if (flexibleUpdateAvailable) {
                    val options = AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()
                    appUpdateManager.startUpdateFlowForResult(
                        updateInfo,
                        updateResultLauncher,
                        options
                    )
                }
            }
            .addOnFailureListener { error ->
                Log.w(TAG, "Unable to check for an app update", error)
            }
    }

    private fun showUpdateReadyPrompt() {
        if (updateReadySnackbar?.isShown == true) return
        updateReadySnackbar = Snackbar.make(
            binding.root,
            "Update downloaded",
            Snackbar.LENGTH_INDEFINITE
        ).setAction("Restart") {
            appUpdateManager.completeUpdate()
        }
        updateReadySnackbar?.show()
    }

    /**
     * Called every time the activity comes to foreground.
     * Re-initializes critical services that may have been killed by Android.
     */
    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart: checking if services need re-initialization")
        lifecycleScope.launch {
            val token = authRepository.currentToken()
            if (SessionLaunchGuard.canInitializeOperationalServices(token, authRepository.mustChangePassword())) {
                reinitializeServices()
            } else {
                Log.d(TAG, "onStart: session not eligible for service init")
                servicesInitialized = false
                if (authRepository.mustChangePassword()) {
                    ForegroundLocationService.stopService(this@MainActivity)
                }
            }
        }
    }

    /**
     * Re-initialize all services that depend on an active login session.
     * Safe to call multiple times — idempotent.
     */
    private fun reinitializeServices() {
        if (authRepository.mustChangePassword()) {
            Log.i(TAG, "Skipping service initialization while password change is required")
            servicesInitialized = false
            ForegroundLocationService.stopService(this)
            return
        }

        Log.i(TAG, "Reinitializing services after app resume")

        // Restart ForegroundLocationService if consent was given
        if (locationRepository.hasLocationConsent() && appConfigStore.gpsEnabled) {
            val userRole = appConfigStore.userRole
            if (userRole == "DRIVER" || userRole == "PLANT_OPERATOR") {
                Log.d(TAG, "Restarting ForegroundLocationService")
                ForegroundLocationService.startService(this)
            }
        }

        servicesInitialized = true
        Log.d(TAG, "Services re-initialization complete")
    }

    /**
     * Check if destination requires password to be changed first.
     * Only login, splash, and changePassword screens are allowed when locked.
     */
    private fun isProtectedDestination(destinationId: Int): Boolean {
        return destinationId != R.id.changePasswordFragment &&
               destinationId != R.id.loginFragment &&
               destinationId != R.id.splashFragment
    }

    override fun onSupportNavigateUp(): Boolean {
        // Block navigation up when password change is required
        if (authRepository.mustChangePassword()) {
            return false
        }
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    /**
     * Called when password change is complete - clears lock and navigates home.
     */
    fun onPasswordChangeComplete() {
        Log.i(TAG, "Password change complete - unlocking navigation")
        
        // Navigate to home with cleared back stack
        navController.navigate(
            R.id.homeFragment,
            null,
            NavOptions.Builder()
                .setPopUpTo(R.id.nav_graph, true)
                .build()
        )
    }
}
