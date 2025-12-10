package com.smartcbwtf.mobile.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.smartcbwtf.mobile.R
import com.smartcbwtf.mobile.BuildConfig
import com.smartcbwtf.mobile.bluetooth.ConnectionState
import com.smartcbwtf.mobile.databinding.FragmentScanWeighBinding
import com.smartcbwtf.mobile.utils.PermissionHelper
import com.smartcbwtf.mobile.viewmodel.ScanWeighViewModel
import com.smartcbwtf.mobile.viewmodel.SubmissionState
import com.smartcbwtf.mobile.viewmodel.LocationState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ScanWeighFragment : Fragment(R.layout.fragment_scan_weigh) {

    private val viewModel: ScanWeighViewModel by viewModels()
    private val args: ScanWeighFragmentArgs by navArgs()
    private var _binding: FragmentScanWeighBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var permissionHelper: PermissionHelper

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val denied = permissions.filterValues { granted -> !granted }
        if (denied.isNotEmpty()) {
            Toast.makeText(requireContext(), "Permissions needed for scan/weight", Toast.LENGTH_LONG).show()
        } else {
            viewModel.refreshLocation()
        }
    }

    private val qrScannerLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            viewModel.onQrScanned(result.contents)
        } else {
            Toast.makeText(requireContext(), "QR scan cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentScanWeighBinding.bind(view)

        val isVerification = args.eventType.equals("CBWTF_VERIFICATION", ignoreCase = true)
        binding.tvModeTitle.text = if (isVerification) "Verify at CBWTF" else "Scan & Weigh at HCF"
        binding.tvModeSubtitle.text = if (isVerification) "Confirm inbound bags at the CBWTF" else "Collect bags at the HCF"
        binding.btnSubmit.text = if (isVerification) "Save Verification" else "Save Collection"

        if (permissionHelper.hasLocationPermission()) {
            viewModel.refreshLocation()
        }

        // Request permissions on start
        requestPermissionLauncher.launch(permissionHelper.getRequiredPermissions().toTypedArray())

        // Enable simulate button only in debug builds to avoid production exposure
        binding.btnSimulateWeight.isVisible = BuildConfig.DEBUG

        binding.btnConnectScale.setOnClickListener {
            if (permissionHelper.hasBluetoothPermissions()) {
                viewModel.connectScale()
            } else {
                Toast.makeText(requireContext(), "Bluetooth permission required", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnScanQr.setOnClickListener {
            if (permissionHelper.hasCameraPermission()) {
                val options = ScanOptions().apply {
                    setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                    setPrompt("Scan Bag QR Code")
                    setBeepEnabled(true)
                    setCameraId(0)
                    setOrientationLocked(false)
                }
                qrScannerLauncher.launch(options)
            } else {
                Toast.makeText(requireContext(), "Camera permission required", Toast.LENGTH_SHORT).show()
                requestPermissionLauncher.launch(permissionHelper.getRequiredPermissions().toTypedArray())
            }
        }

        binding.btnSubmit.setOnClickListener {
            viewModel.submit(args.hcfId, args.eventType)
        }

        binding.btnSimulateWeight.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.simulateWeight()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.connectionState.collect { state ->
                binding.tvStatus.text = "Status: $state"
                binding.btnConnectScale.text = if (state == ConnectionState.CONNECTED) "Disconnect" else "Connect Scale"
                binding.btnConnectScale.setOnClickListener {
                    if (state == ConnectionState.CONNECTED) viewModel.disconnectScale() else viewModel.connectScale()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.weight.collect { weight ->
                binding.tvWeight.text = if (weight != null) "$weight kg" else "-- kg"
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.scannedQr.collect { qr ->
                binding.tvQrCode.text = qr ?: "No QR Scanned"
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.qrError.collect { message ->
                binding.tvQrCodeError.isVisible = !message.isNullOrBlank()
                binding.tvQrCodeError.text = message ?: ""
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.location.collect { state ->
                when (state) {
                    is LocationState.Waiting -> {
                        binding.tvLocationStatus.text = "Location: Waiting for GPS"
                        binding.tvLocationCoords.text = "Lat: --, Lon: --"
                    }
                    is LocationState.Ready -> {
                        binding.tvLocationStatus.text = "Location: OK"
                        binding.tvLocationCoords.text = "Lat: ${state.lat}, Lon: ${state.lon}"
                    }
                    is LocationState.Error -> {
                        binding.tvLocationStatus.text = "Location: Error"
                        binding.tvLocationCoords.text = state.message
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isSubmitEnabled.collect { enabled ->
                binding.btnSubmit.isEnabled = enabled
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.submissionState.collect { state ->
                if (state is SubmissionState.Success) {
                    Toast.makeText(requireContext(), "Saved!", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                } else if (state is SubmissionState.Error) {
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.error.collect { err ->
                if (!err.isNullOrBlank()) {
                    Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
