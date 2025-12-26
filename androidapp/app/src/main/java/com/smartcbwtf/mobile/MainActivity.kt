package com.smartcbwtf.mobile

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.smartcbwtf.mobile.databinding.ActivityMainBinding
import com.smartcbwtf.mobile.repository.AuthRepository
import dagger.hilt.android.AndroidEntryPoint
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Main Activity with navigation lock for security enforcement.
 * 
 * SECURITY: When mustChangePassword is true, user is locked to password change screen.
 * Cannot bypass via: back button, deep links, saved state restore.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var navController: NavController

    @Inject
    lateinit var authRepository: AuthRepository

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

        // Global auth state observer
        // If token is cleared (e.g. 401 Unauthorized), force navigation to login
        lifecycleScope.launch {
            authRepository.getAuthStateFlow().collect { token ->
                if (token == null) {
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
                }
            }
        }
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

    /**
     * Override back press to prevent escape from password change screen.
     */
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (authRepository.mustChangePassword()) {
            // If must change password, only allow logout via the UI button
            Toast.makeText(
                this,
                "You must change your password or logout to continue",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        super.onBackPressed()
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
