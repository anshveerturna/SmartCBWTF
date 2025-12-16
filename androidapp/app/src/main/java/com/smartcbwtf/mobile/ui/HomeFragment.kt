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
import androidx.appcompat.app.AppCompatActivity
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
import com.smartcbwtf.mobile.R
import com.smartcbwtf.mobile.databinding.FragmentHomeBinding
import com.smartcbwtf.mobile.utils.LocationHelper
import com.smartcbwtf.mobile.viewmodel.AuthState
import com.smartcbwtf.mobile.viewmodel.AuthViewModel
import com.smartcbwtf.mobile.viewmodel.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment : Fragment(R.layout.fragment_home) {

    @Inject lateinit var locationHelper: LocationHelper

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

        (activity as? AppCompatActivity)?.supportActionBar?.hide()

        setupActions()
        setupProfileMenu()
        bindStatus()
        animateEntry()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (activity as? AppCompatActivity)?.supportActionBar?.show()
        _binding = null
    }

    private fun setupActions() {
        val cards = listOf(binding.cardPickup, binding.cardVerify, binding.cardRegister, binding.cardAttendance)
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
                        viewModel.logout()
                        val options = navOptions {
                            popUpTo(R.id.homeFragment) { inclusive = true }
                        }
                        findNavController().navigate(R.id.action_homeFragment_to_loginFragment, null, options)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }

        binding.textGreetingTitle.text = "Hello, Operator"
    }

    private fun bindStatus() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.authState.collect { state ->
                        if (state is AuthState.Unauthenticated && isAdded) {
                            val options = navOptions {
                                popUpTo(R.id.homeFragment) { inclusive = true }
                            }
                            findNavController().navigate(R.id.action_homeFragment_to_loginFragment, null, options)
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
