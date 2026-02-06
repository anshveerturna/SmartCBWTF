package com.smartcbwtf.mobile.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import androidx.appcompat.widget.PopupMenu
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.util.Log
import com.smartcbwtf.mobile.BuildConfig
import com.smartcbwtf.mobile.R
import com.smartcbwtf.mobile.databinding.FragmentHomeBinding
import com.smartcbwtf.mobile.repository.LocationRepository
import com.smartcbwtf.mobile.service.ForegroundLocationService
import com.smartcbwtf.mobile.utils.LocationHelper
import com.smartcbwtf.mobile.viewmodel.AuthState
import com.smartcbwtf.mobile.viewmodel.AuthViewModel
import com.smartcbwtf.mobile.viewmodel.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject
import coil.imageLoader
import coil.request.ImageRequest

@AndroidEntryPoint
class HomeFragment : Fragment(R.layout.fragment_home) {

    @Inject lateinit var locationHelper: LocationHelper
    @Inject lateinit var locationRepository: LocationRepository

    private val viewModel: AuthViewModel by activityViewModels()
    private val homeViewModel: HomeViewModel by viewModels()
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        
        if (fineLocationGranted || coarseLocationGranted) {
            captureLocationAndNavigate()
        } else {
            Toast.makeText(requireContext(), "Location permission is required to mark attendance", Toast.LENGTH_LONG).show()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)

        // Start GPS tracking service if consent was previously given
        ensureLocationServiceRunning()

        setupActions()
        setupProfileMenu()
        bindStatus()
        animateEntry()
    }

    /**
     * Called every time the fragment comes back to the foreground.
     * Ensures services are running even if Android killed them while backgrounded.
     */
    override fun onResume() {
        super.onResume()
        Log.d("HomeFragment", "onResume: ensuring services are running")
        ensureLocationServiceRunning()
    }

    /**
     * Restart ForegroundLocationService if the user has given consent.
     * Safe to call repeatedly — startService is idempotent.
     */
    private fun ensureLocationServiceRunning() {
        if (locationRepository.hasLocationConsent()) {
            Log.d("HomeFragment", "Location consent given, ensuring GPS tracking service is running")
            ForegroundLocationService.startService(requireContext())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupActions() {
        val cards = listOf(binding.cardPickup, binding.cardVerify, binding.cardRegister, binding.cardAttendance, binding.cardMyRoute)
        cards.forEach { card ->
            card.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(100).start()
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                    }
                }
                false
            }
        }

        binding.cardPickup.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_scanWeighFragment)
        }

        binding.cardVerify.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_verifyAtCbtwfFragment)
        }

        binding.cardRegister.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_hcfRegistrationFragment)
        }

        binding.cardAttendance.setOnClickListener {
            checkLocationPermissionAndMarkAttendance()
        }

        binding.cardMyRoute.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_myRouteFragment)
        }

        binding.btnSettings.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_settingsFragment)
        }
    }

    private fun checkLocationPermissionAndMarkAttendance() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                captureLocationAndNavigate()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Location Required")
                    .setMessage("Location access is needed to verify you are at an HCF before marking attendance.")
                    .setPositiveButton("Grant Permission") { _, _ ->
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            else -> {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    private fun captureLocationAndNavigate() {
        viewLifecycleOwner.lifecycleScope.launch {
            binding.cardAttendance.isEnabled = false
            
            try {
                // Get current location before navigating
                val location = locationHelper.getCurrentLocation()
                
                // Navigate to attendance screen with location (or 0,0 if unavailable)
                val lat = location?.latitude?.toFloat() ?: 0f
                val lon = location?.longitude?.toFloat() ?: 0f
                
                val action = HomeFragmentDirections.actionHomeFragmentToAttendanceFragment(lat, lon)
                findNavController().navigate(action)
                
            } catch (e: Exception) {
                // Navigate anyway, let the attendance screen handle location refresh
                val action = HomeFragmentDirections.actionHomeFragmentToAttendanceFragment(0f, 0f)
                findNavController().navigate(action)
            } finally {
                binding.cardAttendance.isEnabled = true
            }
        }
    }

    private fun setupProfileMenu() {
        binding.avatarView.setOnClickListener { anchor ->
            val popup = PopupMenu(requireContext(), anchor, android.view.Gravity.END)
            popup.menuInflater.inflate(R.menu.menu_profile, popup.menu)
            
            // Force icons to show in popup menu
            try {
                val fieldMPopup = PopupMenu::class.java.getDeclaredField("mPopup")
                fieldMPopup.isAccessible = true
                val mPopup = fieldMPopup.get(popup)
                mPopup.javaClass.getDeclaredMethod("setForceShowIcon", Boolean::class.java)
                    .invoke(mPopup, true)
            } catch (e: Exception) {
                // Ignore if reflection fails
            }
            
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_profile -> {
                        findNavController().navigate(R.id.action_homeFragment_to_profileFragment)
                        true
                    }
                    R.id.menu_logout -> {
                        // Just logout - the authState observer will handle navigation
                        viewModel.logout()
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }

    private fun bindStatus() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.authState.collect { state ->
                        // Only navigate if we're still on homeFragment
                        val currentDestId = findNavController().currentDestination?.id
                        if (state is AuthState.Unauthenticated && 
                            isAdded && 
                            currentDestId == R.id.homeFragment) {
                            findNavController().navigate(
                                R.id.action_homeFragment_to_loginFragment,
                                null,
                                navOptions {
                                    popUpTo(R.id.homeFragment) { inclusive = true }
                                }
                            )
                        }
                    }
                }

                launch {
                    homeViewModel.pendingCount.collect { count ->
                        binding.textPendingValue.text = count.toString()
                        if (count > 0) {
                            binding.textPendingValue.setTextColor(resources.getColor(R.color.warningAccent, null))
                        } else {
                            binding.textPendingValue.setTextColor(resources.getColor(R.color.text_title, null))
                        }
                    }
                }

                launch {
                    homeViewModel.lastSyncFailed.collect { failed ->
                        if (failed) {
                            binding.textSyncValue.text = "Sync Issue"
                            binding.textSyncValue.setTextColor(resources.getColor(R.color.errorAccent, null))
                        } else {
                            binding.textSyncValue.text = "Just now"
                            binding.textSyncValue.setTextColor(resources.getColor(R.color.text_title, null))
                        }
                    }
                }

                // Observe greeting name
                launch {
                    homeViewModel.greetingName.collect { name ->
                        binding.textGreetingTitle.text = "Hello, $name"
                    }
                }

                // Observe profile photo for avatar
                launch {
                    homeViewModel.profilePhotoUrl.collect { photoUrl ->
                        Log.d("HomeFragment", "Profile photo URL: $photoUrl")
                        if (!photoUrl.isNullOrBlank()) {
                            // Derive base URL from BuildConfig (strip /api/ suffix)
                            val baseUrl = BuildConfig.BASE_URL.removeSuffix("/").removeSuffix("api").removeSuffix("/")
                            val fullUrl = if (photoUrl.startsWith("http")) photoUrl else "$baseUrl$photoUrl"
                            Log.d("HomeFragment", "Loading avatar from: $fullUrl")
                            
                            // Clear tint and padding before loading
                            binding.avatarView.imageTintList = null
                            binding.avatarView.setPadding(0, 0, 0, 0)
                            binding.avatarView.scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                            
                            requireContext().imageLoader.enqueue(
                                ImageRequest.Builder(requireContext())
                                    .data(fullUrl)
                                    .target(
                                        onSuccess = { result ->
                                            binding.avatarView.setImageDrawable(result)
                                            Log.d("HomeFragment", "Avatar loaded successfully")
                                        },
                                        onError = { _ ->
                                            Log.e("HomeFragment", "Failed to load avatar")
                                            binding.avatarView.setImageResource(R.drawable.ic_avatar_placeholder)
                                        }
                                    )
                                    .build()
                            )
                        } else {
                            Log.d("HomeFragment", "No profile photo URL, using placeholder")
                            binding.avatarView.setImageResource(R.drawable.ic_avatar_placeholder)
                        }
                    }
                }
            }
        }
    }

    private fun animateEntry() {
        val fadeInViews = listOf(binding.cardGreeting, binding.cardPickup, binding.cardVerify, binding.cardRegister, binding.cardAttendance, binding.cardStatus)
        fadeInViews.forEachIndexed { index, view ->
            view.alpha = 0f
            view.translationY = 20f
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay((index * 50).toLong())
                .setDuration(240)
                .setInterpolator(FastOutSlowInInterpolator())
                .start()
        }
    }
}
