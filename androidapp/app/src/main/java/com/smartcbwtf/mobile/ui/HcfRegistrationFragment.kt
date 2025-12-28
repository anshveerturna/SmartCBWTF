package com.smartcbwtf.mobile.ui

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.util.Patterns
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.smartcbwtf.mobile.R
import com.smartcbwtf.mobile.databinding.FragmentHcfRegistrationBinding
import com.smartcbwtf.mobile.viewmodel.GpsState
import com.smartcbwtf.mobile.viewmodel.HcfRegistrationViewModel
import com.smartcbwtf.mobile.viewmodel.RegistrationState
import com.smartcbwtf.mobile.viewmodel.TermsState
import com.smartcbwtf.mobile.viewmodel.RentAgreementState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale

@AndroidEntryPoint
class HcfRegistrationFragment : Fragment(R.layout.fragment_hcf_registration) {

    private val viewModel: HcfRegistrationViewModel by viewModels()
    private var _binding: FragmentHcfRegistrationBinding? = null
    private val binding get() = _binding!!
    
    // PDF picker - specific MIME type
    private val pdfPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { handleFileSelected(it) }
    }
    
    // Image picker - PhotoPicker for modern Android
    private val imagePicker = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { handleFileSelected(it) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHcfRegistrationBinding.bind(view)

        // Hide keyboard when tapping outside inputs
        setupHideKeyboardOnTouch(binding.root)

        setupFormFields()
        setupOwnershipType()
        setupGpsCapture()
        setupTermsCard()
        setupRegisterButton()
        observeStates()
    }

    private fun setupHideKeyboardOnTouch(root: View) {
        root.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                v.clearFocus()
                hideKeyboard(v)
            }
            false
        }
    }

    private fun hideKeyboard(view: View) {
        val imm = ContextCompat.getSystemService(view.context, InputMethodManager::class.java)
        imm?.hideSoftInputFromWindow(view.windowToken, 0)
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
        binding.etPincode.doAfterTextChanged { textWatcher() }
        binding.etState.doAfterTextChanged { textWatcher() }
        binding.etDoctorName.doAfterTextChanged { textWatcher() }
        binding.etPhone.doAfterTextChanged { textWatcher(); validatePhoneField(showError = true) }
        binding.etEmail.doAfterTextChanged { textWatcher(); validateEmailField(showError = true) }
        binding.etAadharNo.doAfterTextChanged { textWatcher(); validateAadharField(showError = true) }

        // PAN: uppercase and validate
        binding.etPanNo.doAfterTextChanged {
            val upper = it?.toString()?.uppercase(Locale.US).orEmpty()
            if (upper != it?.toString()) binding.etPanNo.setText(upper)
            binding.etPanNo.setSelection(binding.etPanNo.text?.length ?: 0)
            textWatcher(); validatePanField(showError = true)
        }

        // GST: uppercase and validate
        binding.etGstNo.doAfterTextChanged {
            val upper = it?.toString()?.uppercase(Locale.US).orEmpty()
            if (upper != it?.toString()) binding.etGstNo.setText(upper)
            binding.etGstNo.setSelection(binding.etGstNo.text?.length ?: 0)
            textWatcher(); validateGstField(showError = true)
        }
    }
    
    private fun setupOwnershipType() {
        // RadioGroup for ownership type
        binding.rgOwnershipType.setOnCheckedChangeListener { _, checkedId ->
            val isRented = checkedId == R.id.rbRented
            binding.cardRentAgreement.isVisible = isRented
            viewModel.setOwnershipType(if (isRented) "RENTED" else "OWNED")
            updateRegisterButtonState()
        }
        
        // Upload button - show bottom sheet with options
        binding.btnUploadRentAgreement.setOnClickListener {
            showDocumentPickerBottomSheet()
        }
    }
    
    private fun showDocumentPickerBottomSheet() {
        val bottomSheet = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.bottom_sheet_document_picker, null)
        
        view.findViewById<View>(R.id.optionPdf).setOnClickListener {
            pdfPicker.launch(arrayOf("application/pdf"))
            bottomSheet.dismiss()
        }
        
        view.findViewById<View>(R.id.optionPhoto).setOnClickListener {
            imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            bottomSheet.dismiss()
        }
        
        bottomSheet.setContentView(view)
        bottomSheet.show()
    }
    
    private fun handleFileSelected(uri: Uri) {
        // Take persistent permission for the URI
        try {
            requireContext().contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (e: SecurityException) {
            // May not be grantable for all URIs, continue anyway
        }
        viewModel.uploadRentAgreement(uri)
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
            if (validateFields(showError = true)) {
                submitRegistration()
            }
        }
    }

    private fun isValidPhone(phone: String?): Boolean = phone?.matches(Regex("^[6-9]\\d{9}$")) == true
    private fun isValidPan(pan: String?): Boolean = pan?.matches(Regex("^[A-Z]{5}[0-9]{4}[A-Z]$")) == true
    private fun isValidGst(gst: String?): Boolean = gst?.matches(Regex("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$")) == true
    private fun isValidAadhar(aadhar: String?): Boolean = aadhar?.matches(Regex("^\\d{12}$")) == true
    private fun isValidEmail(email: String?): Boolean = !email.isNullOrBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()

    private fun validateFields(showError: Boolean): Boolean {
        val ok = validateAllFields(showError)
        if (!ok && showError) {
            Snackbar.make(binding.root, "Please fix highlighted fields before submitting", Snackbar.LENGTH_LONG).show()
        }
        return ok
    }

    private fun validateAllFields(showError: Boolean): Boolean {
        var ok = true
        ok = validatePhoneField(showError) && ok
        ok = validatePanField(showError) && ok
        ok = validateGstField(showError) && ok
        ok = validateAadharField(showError) && ok
        ok = validateEmailField(showError) && ok
        return ok
    }

    private fun validatePhoneField(showError: Boolean): Boolean {
        val phoneVal = binding.etPhone.text?.toString().orEmpty()
        val valid = isValidPhone(phoneVal)
        if (showError) {
            binding.tilPhone.error = if (valid) null else "Enter 10-digit phone starting with 6/7/8/9"
        }
        return valid
    }

    private fun validatePanField(showError: Boolean): Boolean {
        val panVal = binding.etPanNo.text?.toString()?.uppercase(Locale.US).orEmpty()
        val valid = panVal.isEmpty() || isValidPan(panVal)
        if (showError) {
            binding.tilPanNo.error = if (valid) null else "Invalid PAN format (ABCDE1234F)"
        }
        return valid
    }

    private fun validateGstField(showError: Boolean): Boolean {
        val gstVal = binding.etGstNo.text?.toString()?.uppercase(Locale.US).orEmpty()
        val valid = gstVal.isEmpty() || isValidGst(gstVal)
        if (showError) {
            binding.tilGstNo.error = if (valid) null else "Invalid GSTIN format"
        }
        return valid
    }

    private fun validateAadharField(showError: Boolean): Boolean {
        val aadharVal = binding.etAadharNo.text?.toString().orEmpty()
        val valid = aadharVal.isEmpty() || isValidAadhar(aadharVal)
        if (showError) {
            binding.tilAadharNo.error = if (valid) null else "Aadhaar must be 12 digits"
        }
        return valid
    }

    private fun validateEmailField(showError: Boolean): Boolean {
        val emailVal = binding.etEmail.text?.toString().orEmpty()
        val valid = emailVal.isEmpty() || isValidEmail(emailVal)
        if (showError) {
            binding.tilEmail.error = if (valid) null else "Enter a valid email"
        }
        return valid
    }
    
    private fun submitRegistration() {
        val beds = binding.etBeds.text?.toString()?.toIntOrNull()
        val monthlyCharges = binding.etMonthlyCharges.text?.toString()?.toDoubleOrNull()
        
        viewModel.submit(
            name = binding.etName.text.toString(),
            address = binding.etAddress.text?.toString(),
            pincode = binding.etPincode.text?.toString(),
            state = binding.etState.text?.toString(),
            doctorName = binding.etDoctorName.text?.toString(),
            phone = binding.etPhone.text?.toString(),
            email = binding.etEmail.text?.toString(),
            panNo = binding.etPanNo.text?.toString(),
            gstNo = binding.etGstNo.text?.toString(),
            aadharNo = binding.etAadharNo.text?.toString(),
            beds = beds,
            monthlyCharges = monthlyCharges,
            otherNotes = binding.etOtherNotes.text?.toString()
        )
    }
    
    private fun updateRegisterButtonState() {
        val hasName = binding.etName.text?.isNotBlank() == true
        val hasAddress = binding.etAddress.text?.isNotBlank() == true
        val pincodeText = binding.etPincode.text?.toString()
        val pincodeOk = pincodeText.isNullOrBlank() || pincodeText.matches(Regex("^\\d{6}$"))
        val hasDoctorName = binding.etDoctorName.text?.isNotBlank() == true
        val hasPhone = binding.etPhone.text?.isNotBlank() == true && isValidPhone(binding.etPhone.text?.toString())
        val gpsOk = viewModel.gpsState.value is GpsState.Captured
        val termsAccepted = binding.cbTermsAccepted.isChecked
        val termsLoaded = viewModel.termsState.value is TermsState.Loaded

        val panOk = binding.etPanNo.text.isNullOrBlank() || isValidPan(binding.etPanNo.text?.toString()?.uppercase(Locale.US))
        val gstOk = binding.etGstNo.text.isNullOrBlank() || isValidGst(binding.etGstNo.text?.toString()?.uppercase(Locale.US))
        val aadharOk = binding.etAadharNo.text.isNullOrBlank() || isValidAadhar(binding.etAadharNo.text?.toString())
        val emailOk = binding.etEmail.text.isNullOrBlank() || isValidEmail(binding.etEmail.text?.toString())

        binding.btnRegister.isEnabled = hasName && hasAddress && pincodeOk && hasDoctorName && hasPhone &&
            gpsOk && termsAccepted && termsLoaded && panOk && gstOk && aadharOk && emailOk
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
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.rentAgreementState.collectLatest { state ->
                updateRentAgreementUI(state)
            }
        }
    }
    
    private fun updateRentAgreementUI(state: RentAgreementState) {
        when (state) {
            is RentAgreementState.None -> {
                binding.ivRentAgreementStatus.setImageResource(R.drawable.ic_file)
                binding.ivRentAgreementStatus.setColorFilter(ContextCompat.getColor(requireContext(), R.color.error))
                binding.tvRentAgreementStatus.text = "Upload rent agreement (PDF/Photo)"
                binding.progressRentAgreement.isVisible = false
                binding.btnUploadRentAgreement.isEnabled = true
                binding.btnUploadRentAgreement.text = "Upload"
            }
            is RentAgreementState.Uploading -> {
                binding.tvRentAgreementStatus.text = "Uploading..."
                binding.progressRentAgreement.isVisible = true
                binding.btnUploadRentAgreement.isEnabled = false
            }
            is RentAgreementState.Uploaded -> {
                binding.ivRentAgreementStatus.setImageResource(R.drawable.ic_check)
                binding.ivRentAgreementStatus.setColorFilter(ContextCompat.getColor(requireContext(), R.color.success))
                binding.tvRentAgreementStatus.text = "Uploaded: ${state.fileName}"
                binding.progressRentAgreement.isVisible = false
                binding.btnUploadRentAgreement.isEnabled = true
                binding.btnUploadRentAgreement.text = "Change"
                updateRegisterButtonState()
            }
            is RentAgreementState.Error -> {
                binding.ivRentAgreementStatus.setImageResource(R.drawable.ic_file)
                binding.ivRentAgreementStatus.setColorFilter(ContextCompat.getColor(requireContext(), R.color.error))
                binding.tvRentAgreementStatus.text = "Upload failed: ${state.message}"
                binding.progressRentAgreement.isVisible = false
                binding.btnUploadRentAgreement.isEnabled = true
                binding.btnUploadRentAgreement.text = "Retry"
                Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
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
        val hasPhone = binding.etPhone.text?.isNotBlank() == true && isValidPhone(binding.etPhone.text?.toString())
        val gpsOk = viewModel.gpsState.value is GpsState.Captured
        val termsAccepted = binding.cbTermsAccepted.isChecked
        val termsLoaded = viewModel.termsState.value is TermsState.Loaded

        val panOk = binding.etPanNo.text.isNullOrBlank() || isValidPan(binding.etPanNo.text?.toString()?.uppercase(Locale.US))
        val gstOk = binding.etGstNo.text.isNullOrBlank() || isValidGst(binding.etGstNo.text?.toString()?.uppercase(Locale.US))
        val aadharOk = binding.etAadharNo.text.isNullOrBlank() || isValidAadhar(binding.etAadharNo.text?.toString())
        val emailOk = binding.etEmail.text.isNullOrBlank() || isValidEmail(binding.etEmail.text?.toString())

        return hasName && hasAddress && hasDoctorName && hasPhone && gpsOk && termsAccepted && termsLoaded && panOk && gstOk && aadharOk && emailOk
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
