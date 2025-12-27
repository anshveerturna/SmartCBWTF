package com.smartcbwtf.mobile.ui

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.smartcbwtf.mobile.R
import com.smartcbwtf.mobile.databinding.FragmentProfileBinding
import com.smartcbwtf.mobile.databinding.ItemProfileFieldBinding
import com.smartcbwtf.mobile.network.model.UserProfileResponse
import com.smartcbwtf.mobile.viewmodel.ProfileState
import com.smartcbwtf.mobile.viewmodel.ProfileViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Read-only Profile screen.
 * 
 * DESIGN PRINCIPLES:
 * 1. This screen ONLY displays profile data - NO editing capabilities
 * 2. Backend is the single source of truth for all profile data
 * 3. Profile changes are managed centrally, not through this app
 * 4. Offline support via Room cache - read-only
 * 
 * This screen exists for identity confirmation, not management.
 */
@AndroidEntryPoint
class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private val viewModel: ProfileViewModel by viewModels()
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    // Field row bindings
    private lateinit var usernameRow: ItemProfileFieldBinding
    private lateinit var phoneRow: ItemProfileFieldBinding
    private lateinit var emailRow: ItemProfileFieldBinding
    private lateinit var genderRow: ItemProfileFieldBinding
    private lateinit var dobRow: ItemProfileFieldBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProfileBinding.bind(view)

        // Bind included layouts
        usernameRow = ItemProfileFieldBinding.bind(binding.rowUsername.root)
        phoneRow = ItemProfileFieldBinding.bind(binding.rowPhone.root)
        emailRow = ItemProfileFieldBinding.bind(binding.rowEmail.root)
        genderRow = ItemProfileFieldBinding.bind(binding.rowGender.root)
        dobRow = ItemProfileFieldBinding.bind(binding.rowDob.root)

        setupLabels()
        setupRetryButton()
        observeState()
    }

    private fun setupLabels() {
        usernameRow.textLabel.text = "Username"
        phoneRow.textLabel.text = "Phone"
        emailRow.textLabel.text = "Email"
        genderRow.textLabel.text = "Gender"
        dobRow.textLabel.text = "Date of Birth"
    }

    private fun setupRetryButton() {
        binding.btnRetry.setOnClickListener {
            viewModel.loadProfile()
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    updateUI(state)
                }
            }
        }
    }

    private fun updateUI(state: ProfileState) {
        // Hide all states first
        binding.progressBar.isVisible = false
        binding.layoutError.isVisible = false
        binding.layoutContent.isVisible = false

        when (state) {
            is ProfileState.Loading -> {
                binding.progressBar.isVisible = true
            }
            is ProfileState.Error -> {
                binding.layoutError.isVisible = true
                binding.textError.text = if (state.isOfflineError) {
                    "Profile unavailable offline.\nPlease connect to the internet."
                } else {
                    state.message
                }
            }
            is ProfileState.Success -> {
                binding.layoutContent.isVisible = true
                displayProfile(state.profile)
            }
        }
    }

    private fun displayProfile(profile: UserProfileResponse) {
        // Header section
        binding.textFullName.text = profile.fullName ?: "Not provided"
        binding.textRole.text = formatRole(profile.role)
        binding.textFacility.text = profile.facilityName ?: "No facility assigned"

        // Detail rows - display "Not provided" for missing fields
        usernameRow.textValue.text = profile.username
        phoneRow.textValue.text = profile.phone ?: "Not provided"
        emailRow.textValue.text = profile.email ?: "Not provided"
        genderRow.textValue.text = formatGender(profile.gender)
        dobRow.textValue.text = formatDob(profile.dob)

        // Load profile photo with Coil
        val photoUrl = profile.profilePhotoUrl
        if (!photoUrl.isNullOrBlank()) {
            // Build full URL (assuming backend is at the same host)
            val baseUrl = "http://10.0.2.2:8080" // For emulator; update for production
            val fullUrl = if (photoUrl.startsWith("http")) photoUrl else "$baseUrl$photoUrl"
            
            coil.ImageLoader(requireContext()).enqueue(
                coil.request.ImageRequest.Builder(requireContext())
                    .data(fullUrl)
                    .target(binding.imgProfilePhoto)
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .build()
            )
            // Remove tint and padding when showing actual photo
            binding.imgProfilePhoto.imageTintList = null
            binding.imgProfilePhoto.setPadding(0, 0, 0, 0)
        } else {
            // Use default placeholder
            binding.imgProfilePhoto.setImageResource(R.drawable.ic_person)
        }
    }

    /**
     * Format role for display (e.g., "CBWTF_ADMIN" -> "CBWTF Admin")
     */
    private fun formatRole(role: String): String {
        return role.replace("_", " ")
            .lowercase()
            .replaceFirstChar { it.uppercase() }
    }

    /**
     * Format gender for display (e.g., "MALE" -> "Male")
     */
    private fun formatGender(gender: String?): String {
        return when (gender?.uppercase()) {
            "MALE" -> "Male"
            "FEMALE" -> "Female"
            "OTHER" -> "Other"
            else -> "Not provided"
        }
    }

    /**
     * Format date of birth for display (e.g., "1990-05-15" -> "May 15, 1990")
     */
    private fun formatDob(dob: String?): String {
        if (dob.isNullOrBlank()) return "Not provided"
        return try {
            val date = LocalDate.parse(dob)
            date.format(DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.US))
        } catch (e: Exception) {
            dob // Return as-is if parsing fails
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
