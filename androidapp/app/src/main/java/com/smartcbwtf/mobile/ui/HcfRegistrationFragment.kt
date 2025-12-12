package com.smartcbwtf.mobile.ui

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.smartcbwtf.mobile.R
import com.smartcbwtf.mobile.databinding.FragmentHcfRegistrationBinding
import com.smartcbwtf.mobile.viewmodel.GpsState
import com.smartcbwtf.mobile.viewmodel.HcfRegistrationViewModel
import com.smartcbwtf.mobile.viewmodel.RegistrationState
import com.smartcbwtf.mobile.viewmodel.TermsState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale

@AndroidEntryPoint
class HcfRegistrationFragment : Fragment(R.layout.fragment_hcf_registration) {

    private val viewModel: HcfRegistrationViewModel by viewModels()
    private var _binding: FragmentHcfRegistrationBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHcfRegistrationBinding.bind(view)

        setupFormFields()
        setupGpsCapture()
        setupTermsCard()
        setupRegisterButton()
        observeStates()
    }
    
    private fun setupFormFields() {
        // Bedded toggle visibility
        binding.switchBedded.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setBedded(isChecked)
            binding.tilBeds.isVisible = isChecked
            if (!isChecked) {
                binding.etBeds.text?.clear()
            }
        }
        
        // Form validation for enabling register button
        val textWatcher = { updateRegisterButtonState() }
        binding.etName.doAfterTextChanged { textWatcher() }
        binding.etAddress.doAfterTextChanged { textWatcher() }
        binding.etDoctorName.doAfterTextChanged { textWatcher() }
        binding.etPhone.doAfterTextChanged { textWatcher() }
    }
    
    private fun setupGpsCapture() {
        binding.btnCaptureGps.setOnClickListener {
            viewModel.captureGpsLocation()
        }
    }
    
    private fun setupTermsCard() {
        binding.cardTerms.setOnClickListener {
            showTermsDialog()
        }
        
        binding.cbTermsAccepted.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setTermsAccepted(isChecked)
            updateRegisterButtonState()
        }
    }
    
    private fun showTermsDialog() {
        val terms = viewModel.termsState.value
        if (terms is TermsState.Loaded) {
            val htmlContent = terms.terms.textHtml
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Terms & Conditions")
                .setMessage(Html.fromHtml(htmlContent, Html.FROM_HTML_MODE_COMPACT))
                .setPositiveButton("Close") { dialog, _ -> dialog.dismiss() }
                .setNeutralButton("Accept") { dialog, _ ->
                    binding.cbTermsAccepted.isChecked = true
                    dialog.dismiss()
                }
                .show()
        } else if (terms is TermsState.Error) {
            Snackbar.make(binding.root, terms.message, Snackbar.LENGTH_LONG)
                .setAction("Retry") { viewModel.loadTerms() }
                .show()
        }
    }
    
    private fun setupRegisterButton() {
        binding.btnRegister.setOnClickListener {
            submitRegistration()
        }
    }
    
    private fun submitRegistration() {
        val beds = binding.etBeds.text?.toString()?.toIntOrNull()
        val monthlyCharges = binding.etMonthlyCharges.text?.toString()?.toDoubleOrNull()
        
        viewModel.submit(
            name = binding.etName.text.toString(),
            address = binding.etAddress.text?.toString(),
            doctorName = binding.etDoctorName.text?.toString(),
            phone = binding.etPhone.text?.toString(),
            email = binding.etEmail.text?.toString(),
            panNo = binding.etPanNo.text?.toString(),
            gstNo = binding.etGstNo.text?.toString(),
            aadharNo = binding.etAadharNo.text?.toString(),
            pcbNo = binding.etPcbNo.text?.toString(),
            beds = beds,
            monthlyCharges = monthlyCharges,
            otherNotes = binding.etOtherNotes.text?.toString()
        )
    }
    
    private fun updateRegisterButtonState() {
        val hasName = binding.etName.text?.isNotBlank() == true
        val hasAddress = binding.etAddress.text?.isNotBlank() == true
        val hasDoctorName = binding.etDoctorName.text?.isNotBlank() == true
        val hasPhone = binding.etPhone.text?.isNotBlank() == true
        val gpsOk = viewModel.gpsState.value is GpsState.Captured
        val termsAccepted = binding.cbTermsAccepted.isChecked
        val termsLoaded = viewModel.termsState.value is TermsState.Loaded
        
        binding.btnRegister.isEnabled = hasName && hasAddress && hasDoctorName && 
                hasPhone && gpsOk && termsAccepted && termsLoaded
    }

    private fun observeStates() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collectLatest { state ->
                binding.progressBar.isVisible = state is RegistrationState.Loading
                binding.btnRegister.isEnabled = state !is RegistrationState.Loading && canSubmit()
                
                when (state) {
                    is RegistrationState.Success -> {
                        showSuccessDialog(state.response.agreementNumber, state.response.pdfUrl)
                    }
                    is RegistrationState.Error -> {
                        Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                    }
                    else -> Unit
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.gpsState.collectLatest { state ->
                updateGpsUI(state)
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.termsState.collectLatest { state ->
                updateTermsUI(state)
            }
        }
    }
    
    private fun updateGpsUI(state: GpsState) {
        when (state) {
            is GpsState.NotCaptured -> {
                binding.ivGpsStatus.setImageResource(R.drawable.ic_location_off)
                binding.ivGpsStatus.setColorFilter(ContextCompat.getColor(requireContext(), R.color.error))
                binding.tvGpsStatus.text = "GPS location required"
                binding.tvGpsCoordinates.isVisible = false
                binding.btnCaptureGps.isEnabled = true
                binding.btnCaptureGps.text = "Capture"
            }
            is GpsState.Capturing -> {
                binding.tvGpsStatus.text = "Capturing location..."
                binding.btnCaptureGps.isEnabled = false
            }
            is GpsState.Captured -> {
                binding.ivGpsStatus.setImageResource(R.drawable.ic_location_on)
                binding.ivGpsStatus.setColorFilter(ContextCompat.getColor(requireContext(), R.color.success))
                binding.tvGpsStatus.text = "Location captured"
                binding.tvGpsCoordinates.isVisible = true
                binding.tvGpsCoordinates.text = String.format(
                    Locale.US,
                    "Lat: %.6f, Lon: %.6f\nAccuracy: ±%.0fm",
                    state.latitude, state.longitude, state.accuracy
                )
                binding.btnCaptureGps.isEnabled = true
                binding.btnCaptureGps.text = "Recapture"
                updateRegisterButtonState()
            }
            is GpsState.Error -> {
                binding.ivGpsStatus.setImageResource(R.drawable.ic_location_off)
                binding.ivGpsStatus.setColorFilter(ContextCompat.getColor(requireContext(), R.color.error))
                binding.tvGpsStatus.text = state.message
                binding.tvGpsCoordinates.isVisible = false
                binding.btnCaptureGps.isEnabled = true
                binding.btnCaptureGps.text = "Retry"
            }
        }
    }
    
    private fun updateTermsUI(state: TermsState) {
        when (state) {
            is TermsState.NotLoaded -> {
                binding.tvTermsVersion.text = "Tap to load Terms & Conditions"
                binding.progressTerms.isVisible = false
                binding.cbTermsAccepted.isEnabled = false
            }
            is TermsState.Loading -> {
                binding.tvTermsVersion.text = "Loading..."
                binding.progressTerms.isVisible = true
                binding.cbTermsAccepted.isEnabled = false
            }
            is TermsState.Loaded -> {
                binding.tvTermsVersion.text = "Terms & Conditions"
                binding.progressTerms.isVisible = false
                binding.cbTermsAccepted.isEnabled = true
                updateRegisterButtonState()
            }
            is TermsState.Error -> {
                binding.tvTermsVersion.text = "Failed to load. Tap to retry."
                binding.progressTerms.isVisible = false
                binding.cbTermsAccepted.isEnabled = false
            }
        }
    }
    
    private fun canSubmit(): Boolean {
        val hasName = binding.etName.text?.isNotBlank() == true
        val hasAddress = binding.etAddress.text?.isNotBlank() == true
        val hasDoctorName = binding.etDoctorName.text?.isNotBlank() == true
        val hasPhone = binding.etPhone.text?.isNotBlank() == true
        val gpsOk = viewModel.gpsState.value is GpsState.Captured
        val termsAccepted = binding.cbTermsAccepted.isChecked
        val termsLoaded = viewModel.termsState.value is TermsState.Loaded
        
        return hasName && hasAddress && hasDoctorName && hasPhone && gpsOk && termsAccepted && termsLoaded
    }
    
    private fun showSuccessDialog(agreementNumber: String?, pdfUrl: String?) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Registration Successful")
            .setMessage("HCF registered successfully!\n\nAgreement Number: ${agreementNumber ?: "N/A"}")
            .setPositiveButton("Done") { _, _ ->
                viewModel.reset()
                findNavController().popBackStack()
            }
            .apply {
                if (!pdfUrl.isNullOrEmpty()) {
                    setNeutralButton("View PDF") { _, _ ->
                        openPdf(pdfUrl)
                        viewModel.reset()
                        findNavController().popBackStack()
                    }
                }
            }
            .setCancelable(false)
            .show()
    }
    
    private fun openPdf(pdfUrl: String) {
        try {
            // Construct full URL from base URL + pdf path
            val baseUrl = getString(R.string.base_url).trimEnd('/')
            val fullUrl = if (pdfUrl.startsWith("http")) pdfUrl else "$baseUrl$pdfUrl"
            
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse(fullUrl), "application/pdf")
                flags = Intent.FLAG_ACTIVITY_NO_HISTORY
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Unable to open PDF", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
