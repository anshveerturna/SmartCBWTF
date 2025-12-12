package com.smartcbwtf.mobile.ui.scanner

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.smartcbwtf.mobile.R
import com.smartcbwtf.mobile.bluetooth.ConnectionState
import com.smartcbwtf.mobile.databinding.FragmentQrScannerBinding
import com.smartcbwtf.mobile.viewmodel.LocationState
import com.smartcbwtf.mobile.viewmodel.ScanWeighViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Production-grade QR Scanner Fragment with:
 * - CameraX for camera lifecycle management
 * - ML Kit for fast barcode detection
 * - Premium viewfinder overlay with scanning animation
 * - Torch toggle for low-light conditions
 * - Haptic feedback on successful scan
 * - Integration with ScanWeighViewModel for weight/GPS capture
 */
@AndroidEntryPoint
class QrScannerFragment : Fragment(R.layout.fragment_qr_scanner) {

    companion object {
        private const val TAG = "QrScannerFragment"
        private const val SUCCESS_DISPLAY_DURATION = 1200L
    }

    private var _binding: FragmentQrScannerBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ScanWeighViewModel by activityViewModels()

    // CameraX
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private lateinit var cameraExecutor: ExecutorService

    // QR Analyzer
    private var qrCodeAnalyzer: QrCodeAnalyzer? = null

    // State
    private var isTorchOn = false
    private var hasTorch = false
    private var scannedQrCode: String? = null
    private var scanTimestamp: Long = 0L

    // Permission launcher
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startCamera()
        } else {
            showPermissionDeniedUI()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentQrScannerBinding.bind(view)

        cameraExecutor = Executors.newSingleThreadExecutor()

        hideActionBar()
        setupWindowInsets()
        setupUI()
        setupListeners()
        setupObservers()
        checkCameraPermission()
    }

    /**
     * Hide the ActionBar for full-bleed scanner experience
     */
    private fun hideActionBar() {
        (requireActivity() as? AppCompatActivity)?.supportActionBar?.hide()
    }

    /**
     * Show the ActionBar when leaving this fragment
     */
    private fun showActionBar() {
        (requireActivity() as? AppCompatActivity)?.supportActionBar?.show()
    }

    /**
     * Handle window insets for proper status bar padding on floating controls
     */
    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navigationBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            
            // Apply status bar padding to the top bar
            binding.topBar.updatePadding(
                top = statusBarInsets.top + resources.getDimensionPixelSize(R.dimen.scanner_top_padding)
            )
            
            // Apply navigation bar padding to the bottom controls
            binding.bottomControls.updatePadding(
                bottom = navigationBarInsets.bottom + resources.getDimensionPixelSize(R.dimen.scanner_bottom_padding)
            )
            
            insets
        }
    }

    private fun setupUI() {
        // Initially show the scanning state
        binding.layoutInitialState.isVisible = true
        binding.layoutScanResult.isVisible = false
        binding.cardScanSuccess.isVisible = false
    }

    private fun setupListeners() {
        // Back button
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        // Torch toggle
        binding.btnTorch.setOnClickListener {
            toggleTorch()
        }

        // Scan Again button
        binding.btnScanAgain.setOnClickListener {
            resetScanner()
        }

        // Add Bag button
        binding.btnAddBag.setOnClickListener {
            addBagAndReturn()
        }

        // Open settings button (for permission denied state)
        binding.btnOpenSettings.setOnClickListener {
            openAppSettings()
        }

        // Tap-to-focus on preview
        setupTapToFocus()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupTapToFocus() {
        binding.previewView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                camera?.let { cam ->
                    val factory = binding.previewView.meteringPointFactory
                    val point = factory.createPoint(event.x, event.y)
                    val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
                        .setAutoCancelDuration(3, java.util.concurrent.TimeUnit.SECONDS)
                        .build()
                    cam.cameraControl.startFocusAndMetering(action)
                }
            }
            true
        }
    }

    private fun setupObservers() {
        // Observe weight
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.weight.collect { weight ->
                updateWeightDisplay(weight)
            }
        }

        // Observe connection state
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.connectionState.collect { state ->
                updateScaleConnectionUI(state)
            }
        }

        // Observe location
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.location.collect { location ->
                updateLocationDisplay(location)
            }
        }

        // Update Add Bag button state
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.canAddBag.collect { canAdd ->
                binding.btnAddBag.isEnabled = canAdd
                binding.btnAddBag.alpha = if (canAdd) 1f else 0.6f
            }
        }
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                startCamera()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                showPermissionDeniedUI()
            }
            else -> {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun startCamera() {
        binding.layoutPermissionDenied.isVisible = false
        
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases()
            } catch (e: Exception) {
                Log.e(TAG, "Camera initialization failed", e)
                Toast.makeText(requireContext(), "Camera initialization failed", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    @Suppress("DEPRECATION")
    private fun bindCameraUseCases() {
        val cameraProvider = cameraProvider ?: return

        // Unbind previous use cases
        cameraProvider.unbindAll()

        // Camera selector - prefer back camera
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        // Preview use case
        preview = Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .build()
            .also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

        // Image analysis use case
        qrCodeAnalyzer = QrCodeAnalyzer(
            onQrCodeDetected = { qrCode ->
                requireActivity().runOnUiThread {
                    handleQrCodeDetected(qrCode)
                }
            },
            onError = { error ->
                Log.e(TAG, "QR detection error", error)
            }
        )

        imageAnalyzer = ImageAnalysis.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor, qrCodeAnalyzer!!)
            }

        try {
            camera = cameraProvider.bindToLifecycle(
                viewLifecycleOwner,
                cameraSelector,
                preview,
                imageAnalyzer
            )

            // Check if torch is available
            hasTorch = camera?.cameraInfo?.hasFlashUnit() ?: false
            binding.btnTorch.isVisible = hasTorch

            // Set viewfinder rect for analyzer (after layout is ready)
            binding.viewfinderOverlay.post {
                val rect = binding.viewfinderOverlay.getViewfinderRect()
                qrCodeAnalyzer?.setViewfinderRect(rect)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Camera binding failed", e)
            Toast.makeText(requireContext(), "Camera setup failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleQrCodeDetected(qrCode: String) {
        // Prevent duplicate processing
        if (scannedQrCode == qrCode && System.currentTimeMillis() - scanTimestamp < SUCCESS_DISPLAY_DURATION) {
            return
        }

        scannedQrCode = qrCode
        scanTimestamp = System.currentTimeMillis()

        // Pause scanning
        qrCodeAnalyzer?.setPaused(true)

        // Haptic feedback
        triggerHapticFeedback()

        // Update viewfinder to success state
        binding.viewfinderOverlay.setSuccessState(true)

        // Show success overlay
        showSuccessOverlay()

        // Update ViewModel
        viewModel.onQrScanned(qrCode)

        // Show scan result UI
        showScanResultUI(qrCode)
    }

    private fun triggerHapticFeedback() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = requireContext().getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                manager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                requireContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(100)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Haptic feedback failed", e)
        }
    }

    private fun showSuccessOverlay() {
        binding.cardScanSuccess.alpha = 0f
        binding.cardScanSuccess.isVisible = true
        binding.cardScanSuccess.animate()
            .alpha(1f)
            .setDuration(200)
            .start()

        // Auto-hide after delay
        viewLifecycleOwner.lifecycleScope.launch {
            delay(SUCCESS_DISPLAY_DURATION)
            binding.cardScanSuccess.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction {
                    binding.cardScanSuccess.isVisible = false
                }
                .start()
        }
    }

    private fun showScanResultUI(qrCode: String) {
        // Hide initial state
        binding.layoutInitialState.isVisible = false
        binding.layoutScanResult.isVisible = true

        // Update QR display
        binding.tvScannedQr.text = qrCode

        // Try to extract category from QR
        val parts = qrCode.split("|")
        if (parts.size >= 3) {
            binding.layoutCategory.isVisible = true
            binding.tvCategory.text = "Category: ${parts[2]}"
        } else {
            binding.layoutCategory.isVisible = false
        }

        // Update timestamp
        val timeFormat = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
        binding.tvTimestamp.text = timeFormat.format(Date())

        // Animate in
        binding.layoutScanResult.alpha = 0f
        binding.layoutScanResult.animate()
            .alpha(1f)
            .setDuration(300)
            .start()
    }

    private fun updateWeightDisplay(weight: Double?) {
        if (weight != null && weight > 0) {
            binding.tvWeight.text = String.format(Locale.US, "%.2f kg", weight)
            binding.layoutWeightWarning.isVisible = false
        } else {
            binding.tvWeight.text = "—"
            binding.layoutWeightWarning.isVisible = scannedQrCode != null
        }
    }

    private fun updateScaleConnectionUI(state: ConnectionState) {
        val isConnected = state == ConnectionState.CONNECTED
        binding.layoutWeightWarning.isVisible = !isConnected && scannedQrCode != null
    }

    private fun updateLocationDisplay(location: LocationState) {
        when (location) {
            is LocationState.Ready -> {
                binding.tvGps.text = "Captured"
                binding.tvGps.isVisible = true
                binding.tvGpsWaiting.isVisible = false
            }
            is LocationState.Waiting -> {
                binding.tvGps.isVisible = false
                binding.tvGpsWaiting.isVisible = true
            }
            is LocationState.Error -> {
                binding.tvGps.text = "Error"
                binding.tvGps.isVisible = true
                binding.tvGpsWaiting.isVisible = false
            }
        }
    }

    private fun toggleTorch() {
        camera?.let { cam ->
            isTorchOn = !isTorchOn
            cam.cameraControl.enableTorch(isTorchOn)
            
            binding.btnTorch.setImageResource(
                if (isTorchOn) R.drawable.ic_flashlight_on else R.drawable.ic_flashlight_off
            )
        }
    }

    private fun resetScanner() {
        // Clear state
        scannedQrCode = null
        scanTimestamp = 0L

        // Reset viewfinder
        binding.viewfinderOverlay.setSuccessState(false)

        // Hide scan result, show initial state
        binding.layoutScanResult.isVisible = false
        binding.layoutInitialState.isVisible = true

        // Reset analyzer
        qrCodeAnalyzer?.resetCooldown()
        qrCodeAnalyzer?.setPaused(false)

        // Reset ViewModel
        viewModel.resetForNextScan()
    }

    private fun addBagAndReturn() {
        if (viewModel.addBag()) {
            // Show brief feedback
            Toast.makeText(requireContext(), "Bag added to session", Toast.LENGTH_SHORT).show()
            
            // Return to ScanWeigh fragment
            findNavController().popBackStack()
        } else {
            Toast.makeText(requireContext(), "Cannot add bag - check weight and GPS", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showPermissionDeniedUI() {
        binding.layoutPermissionDenied.isVisible = true
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", requireContext().packageName, null)
        }
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        // Re-check permission when returning from settings
        if (binding.layoutPermissionDenied.isVisible) {
            checkCameraPermission()
        }
        // Resume scanning if previously paused
        qrCodeAnalyzer?.setPaused(false)
    }

    override fun onPause() {
        super.onPause()
        // Pause scanning when fragment is not visible
        qrCodeAnalyzer?.setPaused(true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Restore the ActionBar for other screens
        showActionBar()
        cameraExecutor.shutdown()
        qrCodeAnalyzer?.release()
        _binding = null
    }
}
