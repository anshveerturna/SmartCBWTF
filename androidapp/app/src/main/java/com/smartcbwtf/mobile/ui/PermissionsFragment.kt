package com.smartcbwtf.mobile.ui

import android.Manifest
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.smartcbwtf.mobile.R
import com.smartcbwtf.mobile.databinding.FragmentPermissionsBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PermissionsFragment : Fragment(R.layout.fragment_permissions) {

    private var _binding: FragmentPermissionsBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    // Location permission launcher
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        Log.d("PermissionsFragment", "Location permissions result: $permissions")
        updatePermissionStatus()
        checkAllPermissionsGranted()
    }

    // Camera permission launcher
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        Log.d("PermissionsFragment", "Camera permission result: $granted")
        updatePermissionStatus()
        checkAllPermissionsGranted()
    }

    // Bluetooth permission launcher
    private val bluetoothPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        Log.d("PermissionsFragment", "Bluetooth permissions result: $permissions")
        updatePermissionStatus()
        checkAllPermissionsGranted()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentPermissionsBinding.bind(view)

        Log.d("PermissionsFragment", "PermissionsFragment created")

        setupListeners()
        updatePermissionStatus()
        checkAllPermissionsGranted()
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
        checkAllPermissionsGranted()
    }

    private fun setupListeners() {
        binding.btnGrantPermissions.setOnClickListener {
            Log.d("PermissionsFragment", "Grant All button clicked")
            requestNextPermission()
        }

        binding.btnContinue.setOnClickListener {
            markOnboardingComplete()
            navigateToHome()
        }

        binding.tvSkip.setOnClickListener {
            markOnboardingComplete()
            navigateToHome()
        }

        // Tap on location row to request location permission
        binding.permissionLocation.setOnClickListener {
            Log.d("PermissionsFragment", "Location row clicked")
            requestLocationPermission()
        }

        // Tap on camera row to request camera permission
        binding.permissionCamera.setOnClickListener {
            Log.d("PermissionsFragment", "Camera row clicked")
            requestCameraPermission()
        }

        // Tap on bluetooth row to request bluetooth permission
        binding.permissionBluetooth.setOnClickListener {
            Log.d("PermissionsFragment", "Bluetooth row clicked")
            requestBluetoothPermission()
        }
    }

    private fun requestNextPermission() {
        // Request permissions one by one, starting with location
        when {
            !hasLocationPermission() -> {
                Log.d("PermissionsFragment", "Requesting location permission")
                requestLocationPermission()
            }
            !hasCameraPermission() -> {
                Log.d("PermissionsFragment", "Requesting camera permission")
                requestCameraPermission()
            }
            !hasBluetoothPermissions() -> {
                Log.d("PermissionsFragment", "Requesting bluetooth permission")
                requestBluetoothPermission()
            }
            else -> {
                Log.d("PermissionsFragment", "All permissions already granted")
                checkAllPermissionsGranted()
            }
        }
    }

    private fun requestLocationPermission() {
        if (!hasLocationPermission()) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun requestCameraPermission() {
        if (!hasCameraPermission()) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun requestBluetoothPermission() {
        if (!hasBluetoothPermissions() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            bluetoothPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            )
        }
    }

    // Permission check helpers
    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Older devices don't need runtime Bluetooth permissions
        }
    }

    private fun updatePermissionStatus() {
        Log.d("PermissionsFragment", "Updating permission status - Location: ${hasLocationPermission()}, Camera: ${hasCameraPermission()}, Bluetooth: ${hasBluetoothPermissions()}")
        
        updateStatusIcon(binding.iconLocationStatus, hasLocationPermission())
        updateStatusIcon(binding.iconCameraStatus, hasCameraPermission())
        updateStatusIcon(binding.iconBluetoothStatus, hasBluetoothPermissions())
    }

    private fun updateStatusIcon(icon: ImageView, granted: Boolean) {
        if (granted) {
            icon.setImageResource(R.drawable.ic_check_circle)
            icon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.successAccent))
        } else {
            icon.setImageResource(R.drawable.ic_check_circle)
            icon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.divider_color))
        }
    }

    private fun checkAllPermissionsGranted() {
        val locationGranted = hasLocationPermission()
        val cameraGranted = hasCameraPermission()
        val bluetoothGranted = hasBluetoothPermissions()
        val allGranted = locationGranted && cameraGranted && bluetoothGranted

        Log.d("PermissionsFragment", "All permissions granted: $allGranted")

        binding.btnGrantPermissions.isVisible = !allGranted
        binding.btnContinue.isVisible = allGranted
        binding.tvSkip.isVisible = !allGranted

        if (allGranted) {
            binding.tvSubtitle.text = "All permissions granted! You're ready to use SmartCBWTF."
        }
    }

    private fun markOnboardingComplete() {
        sharedPreferences.edit()
            .putBoolean(PREF_ONBOARDING_COMPLETE, true)
            .apply()
    }

    private fun navigateToHome() {
        findNavController().navigate(R.id.action_permissionsFragment_to_homeFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val PREF_ONBOARDING_COMPLETE = "onboarding_complete"
    }
}
