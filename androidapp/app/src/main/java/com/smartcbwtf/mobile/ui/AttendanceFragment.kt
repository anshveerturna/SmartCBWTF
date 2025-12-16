package com.smartcbwtf.mobile.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.smartcbwtf.mobile.R
import com.smartcbwtf.mobile.databinding.FragmentAttendanceBinding
import com.smartcbwtf.mobile.ui.adapter.HcfSelectionAdapter
import com.smartcbwtf.mobile.viewmodel.AttendanceLocationState
import com.smartcbwtf.mobile.viewmodel.AttendanceResult
import com.smartcbwtf.mobile.viewmodel.AttendanceViewModel
import com.smartcbwtf.mobile.viewmodel.HcfSearchState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@AndroidEntryPoint
class AttendanceFragment : Fragment(R.layout.fragment_attendance) {

    private val viewModel: AttendanceViewModel by viewModels()
    private val args: AttendanceFragmentArgs by navArgs()
    
    private var _binding: FragmentAttendanceBinding? = null
    private val binding get() = _binding!!

    private lateinit var hcfAdapter: HcfSelectionAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAttendanceBinding.bind(view)

        setupViews()
        setupRecyclerView()
        observeViewModel()

        // Initialize with location passed from HomeFragment
        if (args.latitude != 0f && args.longitude != 0f) {
            viewModel.initWithLocation(args.latitude.toDouble(), args.longitude.toDouble())
        } else {
            // Fallback: refresh location if not passed
            viewModel.refreshLocation()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupViews() {
        binding.btnRetryLocation.setOnClickListener {
            viewModel.refreshLocation()
        }

        binding.btnMarkAttendance.setOnClickListener {
            if (viewModel.canMarkAttendance()) {
                showConfirmationDialog()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Please wait for the cooldown to complete before marking attendance at another HCF",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun setupRecyclerView() {
        hcfAdapter = HcfSelectionAdapter { nearbyHcf ->
            viewModel.selectHcf(nearbyHcf)
        }
        
        binding.rvHcfList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = hcfAdapter
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe location state
                launch {
                    viewModel.locationState.collect { state ->
                        updateLocationUI(state)
                    }
                }

                // Observe HCF search state
                launch {
                    viewModel.hcfSearchState.collect { state ->
                        updateHcfSearchUI(state)
                    }
                }

                // Observe selected HCF
                launch {
                    viewModel.selectedHcf.collect { selected ->
                        hcfAdapter.setSelectedHcf(selected?.hcf?.id)
                        updateMarkButtonState()
                    }
                }

                // Observe attendance result
                launch {
                    viewModel.attendanceResult.collect { result ->
                        handleAttendanceResult(result)
                    }
                }

                // Observe cooldown
                launch {
                    viewModel.cooldownRemainingMs.collect { remainingMs ->
                        updateCooldownUI(remainingMs)
                    }
                }
            }
        }
    }

    private fun updateLocationUI(state: AttendanceLocationState) {
        when (state) {
            is AttendanceLocationState.Loading -> {
                binding.progressLocation.isVisible = true
                binding.iconLocationCheck.isVisible = false
                binding.iconLocationError.isVisible = false
                binding.tvLocationStatusSubtitle.text = "Acquiring GPS..."
                binding.btnRetryLocation.isVisible = false
            }
            is AttendanceLocationState.Success -> {
                binding.progressLocation.isVisible = false
                binding.iconLocationCheck.isVisible = true
                binding.iconLocationError.isVisible = false
                binding.tvLocationStatusSubtitle.text = "Location acquired"
                binding.btnRetryLocation.isVisible = true
            }
            is AttendanceLocationState.Error -> {
                binding.progressLocation.isVisible = false
                binding.iconLocationCheck.isVisible = false
                binding.iconLocationError.isVisible = true
                binding.tvLocationStatusSubtitle.text = state.message
                binding.btnRetryLocation.isVisible = true
            }
        }
    }

    private fun updateHcfSearchUI(state: HcfSearchState) {
        binding.layoutHcfLoading.isVisible = state is HcfSearchState.Loading
        binding.layoutNotAtHcf.isVisible = state is HcfSearchState.NoHcfNearby
        binding.layoutSingleHcf.isVisible = state is HcfSearchState.SingleHcf
        binding.layoutMultipleHcf.isVisible = state is HcfSearchState.MultipleHcfs

        when (state) {
            is HcfSearchState.SingleHcf -> {
                val hcf = state.hcf.hcf
                binding.tvHcfName.text = hcf.name
                binding.tvHcfAddress.text = buildString {
                    append(hcf.address ?: "")
                    if (!hcf.city.isNullOrEmpty()) {
                        if (isNotEmpty()) append(", ")
                        append(hcf.city)
                    }
                }.ifEmpty { "No address available" }
            }
            is HcfSearchState.MultipleHcfs -> {
                hcfAdapter.submitList(state.hcfs)
            }
            else -> { /* No action needed */ }
        }

        updateMarkButtonState()
    }

    private fun updateMarkButtonState() {
        val canMark = viewModel.selectedHcf.value != null && viewModel.canMarkAttendance()
        binding.btnMarkAttendance.isEnabled = canMark
    }

    private fun updateCooldownUI(remainingMs: Long) {
        val isCooldownActive = remainingMs > 0
        binding.cardCooldown.isVisible = isCooldownActive
        
        if (isCooldownActive) {
            binding.tvCooldownTime.text = "Please wait ${viewModel.formatCooldownTime(remainingMs)} before marking attendance at another HCF"
        }
        
        updateMarkButtonState()
    }

    private fun handleAttendanceResult(result: AttendanceResult) {
        when (result) {
            is AttendanceResult.Loading -> {
                binding.loadingOverlay.isVisible = true
                binding.tvLoadingMessage.text = "Marking attendance..."
            }
            is AttendanceResult.Success -> {
                binding.loadingOverlay.isVisible = false
                showSuccessDialog(result.hcfName)
                viewModel.resetAttendanceResult()
            }
            is AttendanceResult.Error -> {
                binding.loadingOverlay.isVisible = false
                Toast.makeText(requireContext(), result.message, Toast.LENGTH_LONG).show()
                viewModel.resetAttendanceResult()
            }
            is AttendanceResult.Idle -> {
                binding.loadingOverlay.isVisible = false
            }
        }
    }

    private fun showConfirmationDialog() {
        val selected = viewModel.selectedHcf.value ?: return
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Confirm Attendance")
            .setMessage("Mark your attendance at:\n\n${selected.hcf.name}\n${selected.hcf.address ?: ""}")
            .setPositiveButton("Mark Attendance") { _, _ ->
                viewModel.markAttendance()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showSuccessDialog(hcfName: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Attendance Marked")
            .setMessage("Your attendance has been successfully recorded at $hcfName.")
            .setPositiveButton("Done") { _, _ ->
                findNavController().navigateUp()
            }
            .setCancelable(false)
            .show()
    }
}
