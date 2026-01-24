package com.smartcbwtf.mobile.ui

import android.annotation.SuppressLint
import android.animation.ValueAnimator
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.smartcbwtf.mobile.R
import com.smartcbwtf.mobile.bluetooth.ConnectionState
import com.smartcbwtf.mobile.databinding.FragmentScanWeighBinding
import com.smartcbwtf.mobile.model.OperationMode
import com.smartcbwtf.mobile.utils.PermissionHelper
import com.smartcbwtf.mobile.viewmodel.GeofenceState
import com.smartcbwtf.mobile.viewmodel.ScanWeighViewModel
import com.smartcbwtf.mobile.viewmodel.SubmissionState
import com.smartcbwtf.mobile.viewmodel.LocationState
import com.smartcbwtf.mobile.ui.adapter.BagAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class ScanWeighFragment : Fragment(R.layout.fragment_scan_weigh) {

    // Use activityViewModels() so the ViewModel is shared with QrScannerFragment
    private val viewModel: ScanWeighViewModel by activityViewModels()
    private val args: ScanWeighFragmentArgs by navArgs()
    private var _binding: FragmentScanWeighBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var permissionHelper: PermissionHelper

    private var lastScanTime: Long = 0
    private var bluetoothDialog: AlertDialog? = null
    private var connectingDialog: AlertDialog? = null
    private var geofenceBlockingDialog: AlertDialog? = null
    private var connectionTimeoutJob: Job? = null
    private lateinit var bagAdapter: BagAdapter
    
    companion object {
        private const val CONNECTION_TIMEOUT_MS = 30_000L // 30 seconds
    }
    
    private val isVerificationMode: Boolean
        get() = args.eventType.equals("CBWTF_VERIFICATION", ignoreCase = true)
    
    // Launcher to enable Bluetooth
    private val enableBluetoothLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val bluetoothManager = requireContext().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        if (bluetoothManager.adapter?.isEnabled == true) {
            // Bluetooth is now enabled, proceed with device picker
            showBluetoothDevicePicker()
        } else {
            Toast.makeText(requireContext(), "Bluetooth is required to connect scale", Toast.LENGTH_SHORT).show()
        }
    }
    
    private val deviceId: String
        get() = Settings.Secure.getString(requireContext().contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"

    private val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (viewModel.sessionState.value.hasUnsavedBags) {
                showDiscardConfirmationDialog()
            } else {
                isEnabled = false
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }
    }

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentScanWeighBinding.bind(view)

        // Register back press callback
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backPressedCallback)

        // Set operation mode based on navigation args
        val mode = if (isVerificationMode) OperationMode.VERIFY else OperationMode.COLLECTION
        viewModel.setOperationMode(mode)

        setupUI()
        setupListeners()
        setupObservers()

        if (permissionHelper.hasLocationPermission()) {
            if (isVerificationMode) {
                // Initialize verification mode with GPS gating
                viewModel.initializeVerificationMode()
            } else {
                viewModel.refreshLocation()
            }
        }

        // Request permissions on start
        requestPermissionLauncher.launch(permissionHelper.getRequiredPermissions().toTypedArray())
    }

    private fun setupUI() {
        val isVerification = args.eventType.equals("CBWTF_VERIFICATION", ignoreCase = true)
        
        // Update toolbar title based on mode
        (activity as? AppCompatActivity)?.supportActionBar?.title = 
            if (isVerification) "Verify at CBWTF" else "Scan & Weigh"
        
        binding.tvModeTitle.text = if (isVerification) "Verify Waste" else "Pickup Waste"
        binding.tvModeSubtitle.text = if (isVerification) 
            "Scan bag QR and verify using the plant Bluetooth scale." 
        else 
            "Scan bag QR and weigh using the Bluetooth scale."

        // Update submit button text based on mode
        binding.btnSubmitAll.text = if (isVerification) "Verify" else "Submit"

        // Setup RecyclerView for bag list with delete functionality
        bagAdapter = BagAdapter { index ->
            viewModel.removeBag(index)
            Toast.makeText(requireContext(), "Bag removed. You can rescan this QR.", Toast.LENGTH_SHORT).show()
        }
        binding.recyclerBags.apply {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
            adapter = bagAdapter
        }

        // Initial session summary
        updateSessionSummary(0, 0.0)
    }

    private fun setupListeners() {
        // Connect/Disconnect Scale
        binding.btnConnectScale.setOnClickListener {
            animateButtonPress(it)
            if (permissionHelper.hasBluetoothPermissions()) {
                checkBluetoothAndShowPicker()
            } else {
                Toast.makeText(requireContext(), "Bluetooth permission required", Toast.LENGTH_SHORT).show()
                requestPermissionLauncher.launch(permissionHelper.getRequiredPermissions().toTypedArray())
            }
        }

        // QR Scan button (in prompt state)
        binding.btnScanQr.setOnClickListener {
            animateButtonPress(it)
            launchQrScanner()
        }

        // Scan again button (in scanned state)
        binding.btnScanAgain.setOnClickListener {
            animateButtonPress(it)
            launchQrScanner()
        }

        // Add Bag button
        binding.btnAddBag.setOnClickListener {
            animateButtonPress(it)
            if (viewModel.addBag()) {
                // Show quick feedback
                Snackbar.make(binding.root, "Bag added to session", Snackbar.LENGTH_SHORT)
                    .setAnchorView(binding.btnAddBag)
                    .show()
            }
        }

        // Submit All button
        binding.btnSubmitAll.setOnClickListener {
            animateButtonPress(it)
            if (isVerificationMode) {
                viewModel.verifyBag(deviceId)
            } else {
                val eventType = args.eventType.ifBlank { "HCF_COLLECTION" }
                viewModel.submitAll(eventType)
            }
        }
    }

    private fun launchQrScanner() {
        // CRITICAL: Capture weight BEFORE opening scanner
        // Camera can cause Bluetooth to disconnect on some devices
        val capturedWeight = viewModel.captureWeightForScanner()
        if (capturedWeight != null && capturedWeight > 0) {
            Toast.makeText(requireContext(), "Weight captured: %.2f kg".format(capturedWeight), Toast.LENGTH_SHORT).show()
        }
        
        // Navigate to the QR scanner fragment
        findNavController().navigate(R.id.action_scanWeighFragment_to_qrScannerFragment)
    }

    private fun setupObservers() {
        // Connection state
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.connectionState.collect { state ->
                updateBluetoothStatus(state)
                binding.btnConnectScale.text = when (state) {
                    ConnectionState.CONNECTED -> "Disconnect"
                    ConnectionState.CONNECTING -> "Connecting..."
                    ConnectionState.SCANNING -> "Scanning..."
                    else -> "Connect Scale"
                }
                binding.btnConnectScale.setOnClickListener {
                    animateButtonPress(it)
                    if (state == ConnectionState.CONNECTED) {
                        viewModel.disconnectScale()
                    } else if (state != ConnectionState.SCANNING && state != ConnectionState.CONNECTING) {
                        showBluetoothDevicePicker()
                    }
                }
                
                // Update scale status text
                binding.tvScaleStatus.text = when (state) {
                    ConnectionState.CONNECTED -> "Scale connected and ready"
                    ConnectionState.CONNECTING -> "Connecting to scale..."
                    ConnectionState.DISCONNECTED -> "Tap \"Connect Scale\" to start"
                    ConnectionState.ERROR -> "Connection failed. Try again."
                    ConnectionState.SCANNING -> "Scanning for nearby devices..."
                }
            }
        }

        // Weight
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.weight.collect { weight ->
                binding.tvWeight.text = weight?.toString() ?: "0.0"
            }
        }

        // Scanned QR
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.scannedQr.collect { qr ->
                val hasQr = !qr.isNullOrBlank()
                binding.qrScanPrompt.isVisible = !hasQr
                binding.qrScannedInfo.isVisible = hasQr
                
                if (hasQr) {
                    binding.tvQrCode.text = "QR: $qr"
                    binding.tvQrTimestamp.text = formatScanTime(lastScanTime)
                    
                    // Animate the success state
                    binding.qrScannedInfo.alpha = 0f
                    binding.qrScannedInfo.animate()
                        .alpha(1f)
                        .setDuration(200)
                        .start()
                }
            }
        }

        // QR Error
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.qrError.collect { message ->
                binding.tvQrCodeError.isVisible = !message.isNullOrBlank()
                binding.tvQrCodeError.text = message ?: ""
            }
        }

        // Location
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.location.collect { state ->
                updateGpsStatus(state)
            }
        }

        // Can Add Bag
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.canAddBag.collect { enabled ->
                binding.btnAddBag.isEnabled = enabled
                binding.btnAddBag.alpha = if (enabled) 1f else 0.6f
            }
        }

        // Can Submit All
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.canSubmitAll.collect { enabled ->
                binding.btnSubmitAll.isEnabled = enabled
                binding.btnSubmitAll.alpha = if (enabled) 1f else 0.6f
            }
        }

        // Session State
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.sessionState.collect { session ->
                updateSessionSummary(session.totalBags, session.totalWeight)
                // Button text stays as "Submit" - enabled state handled by canSubmitAll
                
                // Update back press callback based on unsaved bags
                backPressedCallback.isEnabled = session.hasUnsavedBags
                
                // Update bag list RecyclerView
                bagAdapter.submitList(session.bags.toList())
                binding.recyclerBags.isVisible = session.bags.isNotEmpty()
            }
        }

        // Submission state
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.submissionState.collect { state ->
                when (state) {
                    is SubmissionState.Loading -> {
                        binding.btnSubmitAll.isEnabled = false
                        binding.btnAddBag.isEnabled = false
                        binding.progressSubmit.isVisible = true
                        binding.btnSubmitAll.text = ""
                    }
                    is SubmissionState.BatchSuccess -> {
                        binding.progressSubmit.isVisible = false
                        
                        // Show success snackbar
                        Snackbar.make(
                            binding.root,
                            "${state.count} bags submitted successfully!",
                            Snackbar.LENGTH_LONG
                        ).setAction("Done") {
                            findNavController().popBackStack()
                        }.show()
                        
                        // Reset button text
                        binding.btnSubmitAll.text = if (isVerificationMode) "Verify" else "Submit"
                    }
                    is SubmissionState.VerifySuccess -> {
                        binding.progressSubmit.isVisible = false
                        binding.btnSubmitAll.text = "Verify"
                        
                        // Show verification result
                        val message = if (state.anomalyState == "MISMATCH") {
                            "Verified with MISMATCH! Weight delta: ${state.deltaKg?.let { "%.2f".format(it) } ?: "N/A"} kg"
                        } else {
                            "Bag verified successfully!"
                        }
                        
                        val snackbarColor = if (state.anomalyState == "MISMATCH") {
                            com.google.android.material.R.color.design_default_color_error
                        } else {
                            R.color.success
                        }
                        
                        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
                            .setAction("Next") {
                                viewModel.resetForNextScan()
                            }
                            .show()
                    }
                    is SubmissionState.Success -> {
                        binding.progressSubmit.isVisible = false
                        Toast.makeText(requireContext(), "Collection saved!", Toast.LENGTH_SHORT).show()
                        viewModel.resetForNextScan()
                    }
                    is SubmissionState.Error -> {
                        binding.progressSubmit.isVisible = false
                        binding.btnSubmitAll.text = if (isVerificationMode) "Verify" else "Submit"
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        binding.progressSubmit.isVisible = false
                    }
                }
            }
        }

        // General error
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.error.collect { err ->
                if (!err.isNullOrBlank()) {
                    Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Geofence state (for verification mode)
        if (isVerificationMode) {
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.geofenceState.collectLatest { state ->
                    handleGeofenceState(state)
                }
            }

            // Can Verify (overrides canSubmitAll for verification mode)
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.canVerify.collectLatest { enabled ->
                    binding.btnSubmitAll.isEnabled = enabled
                    binding.btnSubmitAll.alpha = if (enabled) 1f else 0.6f
                }
            }

            // Scanner enabled state
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.scannerEnabled.collectLatest { enabled ->
                    binding.btnScanQr.isEnabled = enabled
                    binding.btnScanQr.alpha = if (enabled) 1f else 0.6f
                    binding.btnScanAgain.isEnabled = enabled
                    binding.btnScanAgain.alpha = if (enabled) 1f else 0.6f
                }
            }
        }
    }

    private fun handleGeofenceState(state: GeofenceState) {
        when (state) {
            is GeofenceState.NotChecked -> {
                // Initial state, nothing to show
            }
            is GeofenceState.AcquiringGPS -> {
                binding.tvGpsStatus.text = "GPS: Acquiring for verification..."
                binding.gpsStatusDot.setBackgroundResource(R.drawable.bg_status_dot_yellow)
            }
            is GeofenceState.InsideGeofence -> {
                binding.tvGpsStatus.text = "GPS: Inside facility"
                binding.gpsStatusDot.setBackgroundResource(R.drawable.bg_status_dot_green)
                binding.tvLocationCoords.text = "Accuracy: ${state.accuracy?.toInt() ?: "--"}m"
                geofenceBlockingDialog?.dismiss()
                geofenceBlockingDialog = null
            }
            is GeofenceState.OutsideGeofence -> {
                binding.tvGpsStatus.text = "GPS: Outside facility"
                binding.gpsStatusDot.setBackgroundResource(R.drawable.bg_status_dot_gray)
                showGeofenceBlockingDialog(state.message)
            }
            is GeofenceState.GpsError -> {
                binding.tvGpsStatus.text = "GPS: Error"
                binding.gpsStatusDot.setBackgroundResource(R.drawable.bg_status_dot_gray)
                showGeofenceBlockingDialog(state.message)
            }
        }
    }

    private fun showGeofenceBlockingDialog(message: String) {
        geofenceBlockingDialog?.dismiss()
        geofenceBlockingDialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Location Required")
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("Retry Location") { dialog, _ ->
                dialog.dismiss()
                viewModel.retryGpsForVerification()
            }
            .setNegativeButton("Go Back") { dialog, _ ->
                dialog.dismiss()
                findNavController().popBackStack()
            }
            .show()
    }

    private fun updateBluetoothStatus(state: ConnectionState) {
        val isConnected = state == ConnectionState.CONNECTED
        val isConnecting = state == ConnectionState.CONNECTING || state == ConnectionState.SCANNING
        
        binding.tvBluetoothStatus.text = when (state) {
            ConnectionState.CONNECTED -> "Scale: Connected"
            ConnectionState.CONNECTING -> "Scale: Connecting..."
            ConnectionState.SCANNING -> "Scale: Scanning..."
            ConnectionState.ERROR -> "Scale: Error"
            ConnectionState.DISCONNECTED -> "Scale: Disconnected"
        }
        
        binding.bluetoothStatusDot.setBackgroundResource(
            when {
                isConnected -> R.drawable.bg_status_dot_green
                isConnecting -> R.drawable.bg_status_dot_yellow
                else -> R.drawable.bg_status_dot_gray
            }
        )
    }

    private fun updateGpsStatus(state: LocationState) {
        when (state) {
            is LocationState.Waiting -> {
                binding.tvGpsStatus.text = "GPS: Acquiring..."
                binding.gpsStatusDot.setBackgroundResource(R.drawable.bg_status_dot_yellow)
                binding.tvLocationCoords.text = "Accuracy: --"
            }
            is LocationState.Ready -> {
                binding.tvGpsStatus.text = "GPS: Ready"
                binding.gpsStatusDot.setBackgroundResource(R.drawable.bg_status_dot_green)
                binding.tvLocationCoords.text = "Accuracy: ${state.accuracy?.toInt() ?: "--"}m"
            }
            is LocationState.Error -> {
                binding.tvGpsStatus.text = "GPS: Error"
                binding.gpsStatusDot.setBackgroundResource(R.drawable.bg_status_dot_gray)
                binding.tvLocationCoords.text = state.message
            }
        }
    }

    private fun updateSessionSummary(bagCount: Int, totalWeight: Double) {
        // Animate count change
        val currentCount = binding.tvSessionBagCount.text.toString().toIntOrNull() ?: 0
        if (currentCount != bagCount) {
            animateTextChange(binding.tvSessionBagCount, bagCount.toString())
        } else {
            binding.tvSessionBagCount.text = bagCount.toString()
        }
        
        // Animate weight change
        binding.tvSessionTotalWeight.text = String.format(Locale.US, "%.1f", totalWeight)
        
        // Pulse animation on the card when bags are added
        if (bagCount > currentCount) {
            binding.cardSessionSummary.animate()
                .scaleX(1.02f)
                .scaleY(1.02f)
                .setDuration(100)
                .setInterpolator(OvershootInterpolator())
                .withEndAction {
                    binding.cardSessionSummary.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .start()
                }
                .start()
        }
    }

    private fun animateTextChange(textView: android.widget.TextView, newValue: String) {
        textView.animate()
            .scaleX(1.2f)
            .scaleY(1.2f)
            .setDuration(100)
            .withEndAction {
                textView.text = newValue
                textView.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .start()
            }
            .start()
    }

    private fun showDiscardConfirmationDialog() {
        val bagCount = viewModel.sessionState.value.totalBags
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Unsaved Bags")
            .setMessage("You have $bagCount unsaved bag${if (bagCount > 1) "s" else ""} in this session. Do you want to discard them?")
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton("Discard") { _, _ ->
                viewModel.clearSession()
                backPressedCallback.isEnabled = false
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
            .setNeutralButton("Submit First") { _, _ ->
                val eventType = args.eventType.ifBlank { "HCF_COLLECTION" }
                viewModel.submitAll(eventType)
            }
            .show()
    }

    private fun formatScanTime(timestamp: Long): String {
        if (timestamp == 0L) return "Scanned just now"
        val diff = System.currentTimeMillis() - timestamp
        return when {
            diff < 60_000 -> "Scanned just now"
            diff < 3600_000 -> "Scanned ${diff / 60_000} min ago"
            else -> {
                val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
                "Scanned at ${sdf.format(Date(timestamp))}"
            }
        }
    }

    private fun animateButtonPress(view: View) {
        view.animate()
            .scaleX(0.96f)
            .scaleY(0.96f)
            .setDuration(80)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(80)
                    .start()
            }
            .start()
    }

    /**
     * Check if Bluetooth is enabled and prompt user to enable if not
     */
    private fun checkBluetoothAndShowPicker() {
        val bluetoothManager = requireContext().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        
        when {
            bluetoothAdapter == null -> {
                // Device doesn't support Bluetooth
                Toast.makeText(requireContext(), "This device does not support Bluetooth", Toast.LENGTH_LONG).show()
            }
            !bluetoothAdapter.isEnabled -> {
                // Bluetooth is disabled, prompt user to enable
                showEnableBluetoothDialog()
            }
            else -> {
                // Bluetooth is enabled, proceed with device picker
                showBluetoothDevicePicker()
            }
        }
    }
    
    /**
     * Show dialog prompting user to enable Bluetooth
     */
    private fun showEnableBluetoothDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Bluetooth Required")
            .setMessage("Bluetooth is turned off. Please enable Bluetooth to connect to the scale.")
            .setPositiveButton("Enable") { dialog, _ ->
                dialog.dismiss()
                // Launch system Bluetooth settings
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                enableBluetoothLauncher.launch(enableBtIntent)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    @SuppressLint("MissingPermission")
    private fun showBluetoothDevicePicker() {
        // Start scanning for devices
        viewModel.connectScale()
        
        val deviceList = mutableListOf<BluetoothDevice>()
        val deviceNames = mutableListOf<String>()
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, deviceNames)
        
        // Calculate max height (roughly 5 items worth, ~300dp)
        val maxHeightPx = (300 * resources.displayMetrics.density).toInt()
        
        val listView = ListView(requireContext()).apply {
            this.adapter = adapter
            setPadding(32, 16, 32, 16)
        }
        
        // Wrap ListView in a ScrollView-like container with constrained height
        val container = android.widget.LinearLayout(requireContext()).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            addView(listView, android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                weight = 0f
            })
        }
        
        bluetoothDialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select Bluetooth Scale")
            .setMessage("Scanning for nearby devices...")
            .setView(container)
            .setNegativeButton("Cancel") { dialog, _ ->
                cancelBluetoothConnection()
                dialog.dismiss()
            }
            .setCancelable(false)
            .create()
        
        bluetoothDialog?.show()
        
        // Observe discovered devices
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.discoveredDevices.collect { devices ->
                deviceList.clear()
                deviceNames.clear()
                deviceList.addAll(devices)
                devices.forEach { device ->
                    // Safely get device name - requires BLUETOOTH_CONNECT permission on Android 12+
                    val name = try {
                        device.name?.takeIf { it.isNotBlank() } ?: "BLE Device"
                    } catch (e: SecurityException) {
                        "BLE Device"
                    }
                    val address = device.address
                    // Show name prominently, with address in smaller text below
                    deviceNames.add("$name\n$address")
                }
                adapter.notifyDataSetChanged()
                
                // Constrain list height after update if too many devices
                listView.post {
                    // Measure ListView's desired height
                    var totalHeight = 0
                    for (i in 0 until adapter.count) {
                        val listItem = adapter.getView(i, null, listView)
                        listItem.measure(
                            android.view.View.MeasureSpec.makeMeasureSpec(listView.width.takeIf { it > 0 } ?: 500, android.view.View.MeasureSpec.AT_MOST),
                            android.view.View.MeasureSpec.UNSPECIFIED
                        )
                        totalHeight += listItem.measuredHeight
                    }
                    totalHeight += listView.dividerHeight * (adapter.count - 1).coerceAtLeast(0)
                    
                    val params = listView.layoutParams
                    params.height = if (totalHeight > maxHeightPx) maxHeightPx else android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                    listView.layoutParams = params
                }
                
                // Update message based on found devices
                if (devices.isEmpty()) {
                    bluetoothDialog?.setMessage("Scanning for nearby devices...")
                } else {
                    bluetoothDialog?.setMessage("${devices.size} device(s) found. Tap to connect:")
                }
            }
        }
        
        // Handle device selection
        listView.setOnItemClickListener { _, _, position, _ ->
            val selectedDevice = deviceList.getOrNull(position)
            if (selectedDevice != null) {
                bluetoothDialog?.dismiss()
                bluetoothDialog = null
                showConnectingDialog(selectedDevice)
            }
        }
        
        // Auto-close dialog when connected
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.connectionState.collect { state ->
                when (state) {
                    ConnectionState.CONNECTED -> {
                        bluetoothDialog?.dismiss()
                        bluetoothDialog = null
                    }
                    ConnectionState.ERROR -> {
                        bluetoothDialog?.setMessage("Error scanning. Please try again.")
                    }
                    ConnectionState.DISCONNECTED -> {
                        // Dialog might have been dismissed, no action needed
                    }
                    else -> { /* Keep scanning */ }
                }
            }
        }
    }
    
    /**
     * Show connecting dialog with cancel button and timeout
     */
    @SuppressLint("MissingPermission")
    private fun showConnectingDialog(device: BluetoothDevice) {
        val deviceName = try {
            device.name?.takeIf { it.isNotBlank() } ?: "BLE Device (${device.address.takeLast(8)})"
        } catch (e: SecurityException) {
            "BLE Device (${device.address.takeLast(8)})"
        }
        
        connectingDialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Connecting...")
            .setMessage("Connecting to $deviceName...\n\nThis may take a few seconds.")
            .setNegativeButton("Cancel") { dialog, _ ->
                cancelBluetoothConnection()
                dialog.dismiss()
            }
            .setCancelable(false)
            .create()
        
        connectingDialog?.show()
        
        // Start connection
        viewModel.connectToDevice(device)
        
        // Start connection timeout
        connectionTimeoutJob?.cancel()
        connectionTimeoutJob = viewLifecycleOwner.lifecycleScope.launch {
            delay(CONNECTION_TIMEOUT_MS)
            // If still connecting after timeout, cancel
            if (viewModel.connectionState.value == ConnectionState.CONNECTING) {
                cancelBluetoothConnection()
                connectingDialog?.dismiss()
                connectingDialog = null
                Toast.makeText(requireContext(), "Connection timed out. Please try again.", Toast.LENGTH_LONG).show()
            }
        }
        
        // Observe connection state
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.connectionState.collect { state ->
                when (state) {
                    ConnectionState.CONNECTED -> {
                        connectionTimeoutJob?.cancel()
                        connectingDialog?.dismiss()
                        connectingDialog = null
                        Snackbar.make(binding.root, "Scale connected successfully", Snackbar.LENGTH_SHORT).show()
                    }
                    ConnectionState.ERROR -> {
                        connectionTimeoutJob?.cancel()
                        connectingDialog?.dismiss()
                        connectingDialog = null
                        Toast.makeText(requireContext(), "Failed to connect. Please try again.", Toast.LENGTH_LONG).show()
                    }
                    ConnectionState.DISCONNECTED -> {
                        // If we were connecting and got disconnected, it's a failure
                        if (connectingDialog?.isShowing == true) {
                            connectionTimeoutJob?.cancel()
                            connectingDialog?.dismiss()
                            connectingDialog = null
                        }
                    }
                    else -> { /* Keep waiting */ }
                }
            }
        }
    }
    
    /**
     * Cancel any ongoing Bluetooth connection process
     */
    private fun cancelBluetoothConnection() {
        connectionTimeoutJob?.cancel()
        connectionTimeoutJob = null
        viewModel.stopScanning()
        viewModel.disconnectScale()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Cancel any ongoing Bluetooth connection when leaving the page
        cancelBluetoothConnection()
        bluetoothDialog?.dismiss()
        bluetoothDialog = null
        connectingDialog?.dismiss()
        connectingDialog = null
        geofenceBlockingDialog?.dismiss()
        geofenceBlockingDialog = null
        _binding = null
    }
}
