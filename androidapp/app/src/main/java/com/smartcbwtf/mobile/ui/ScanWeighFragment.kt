package com.smartcbwtf.mobile.ui

import android.Manifest
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
import com.google.zxing.integration.android.IntentIntegrator
import com.smartcbwtf.mobile.R
import com.smartcbwtf.mobile.bluetooth.ConnectionState
import com.smartcbwtf.mobile.databinding.FragmentScanWeighBinding
import com.smartcbwtf.mobile.utils.PermissionHelper
import com.smartcbwtf.mobile.viewmodel.ScanWeighViewModel
import com.smartcbwtf.mobile.viewmodel.SubmissionState
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
        }
    }

    private val qrScanLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val intentResult = IntentIntegrator.parseActivityResult(
            IntentIntegrator.REQUEST_CODE,
            result.resultCode,
            result.data
        )
        if (intentResult != null && intentResult.contents != null) {
            viewModel.onQrScanned(intentResult.contents)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentScanWeighBinding.bind(view)

        // Request permissions on start
        requestPermissionLauncher.launch(permissionHelper.getRequiredPermissions().toTypedArray())

        binding.btnConnectScale.setOnClickListener {
            if (permissionHelper.hasBluetoothPermissions()) {
                viewModel.connectScale()
            } else {
                Toast.makeText(requireContext(), "Bluetooth permission required", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnScanQr.setOnClickListener {
            if (permissionHelper.hasCameraPermission()) {
                val integrator = IntentIntegrator.forSupportFragment(this)
                integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
                integrator.setPrompt("Scan Bag QR Code")
                integrator.setCameraId(0)
                integrator.setBeepEnabled(true)
                integrator.setBarcodeImageEnabled(false)
                // We use custom launcher instead of integrator.initiateScan() to handle result cleanly
                // But IntentIntegrator internally uses startActivityForResult, so we need to override onActivityResult
                // OR use the modern way. The library is old.
                // Let's try the standard way:
                integrator.initiateScan() 
            } else {
                Toast.makeText(requireContext(), "Camera permission required", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnSubmit.setOnClickListener {
            viewModel.submit(args.hcfId, args.eventType)
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
                updateSubmitButton()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.scannedQr.collect { qr ->
                binding.tvQrCode.text = qr ?: "No QR Scanned"
                updateSubmitButton()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.submissionState.collect { state ->
                binding.btnSubmit.isEnabled = state !is SubmissionState.Loading
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

    // Legacy onActivityResult for ZXing
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents != null) {
                viewModel.onQrScanned(result.contents)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun updateSubmitButton() {
        val hasWeight = viewModel.weight.value != null
        val hasQr = viewModel.scannedQr.value != null
        binding.btnSubmit.isEnabled = hasWeight && hasQr
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
