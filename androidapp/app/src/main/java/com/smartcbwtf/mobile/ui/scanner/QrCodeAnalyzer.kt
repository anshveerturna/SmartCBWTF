package com.smartcbwtf.mobile.ui.scanner

import android.annotation.SuppressLint
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.RectF
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit

/**
 * Image analyzer for QR code detection using ML Kit with ZXing fallback.
 * Features:
 * - ML Kit primary detection for speed and accuracy
 * - ZXing fallback for edge cases
 * - Cooldown period to prevent duplicate scans
 * - Optional viewfinder bounds filtering
 */
class QrCodeAnalyzer(
    private val onQrCodeDetected: (String) -> Unit,
    private val onError: (Exception) -> Unit
) : ImageAnalysis.Analyzer {

    companion object {
        private const val TAG = "QrCodeAnalyzer"
        private const val SCAN_COOLDOWN_MS = 1200L // Cooldown after successful scan
    }

    // ML Kit scanner
    private val mlKitScanner: BarcodeScanner by lazy {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        BarcodeScanning.getClient(options)
    }

    // ZXing reader for fallback
    private val zxingReader: MultiFormatReader by lazy {
        MultiFormatReader().apply {
            setHints(
                mapOf(
                    DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
                    DecodeHintType.TRY_HARDER to true
                )
            )
        }
    }

    // State
    private var lastScanTime = 0L
    private var isProcessing = false
    private var viewfinderRect: RectF? = null
    private var isPaused = false

    /**
     * Set the viewfinder bounds for filtering detections
     */
    fun setViewfinderRect(rect: RectF?) {
        viewfinderRect = rect
    }

    /**
     * Pause/resume scanning
     */
    fun setPaused(paused: Boolean) {
        isPaused = paused
    }

    /**
     * Check if scanner is in cooldown period
     */
    fun isInCooldown(): Boolean {
        return System.currentTimeMillis() - lastScanTime < SCAN_COOLDOWN_MS
    }

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        // Skip if paused, in cooldown, or already processing
        if (isPaused || isInCooldown() || isProcessing) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        isProcessing = true

        try {
            // Create InputImage for ML Kit
            val inputImage = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
            )

            // Try ML Kit first
            mlKitScanner.process(inputImage)
                .addOnSuccessListener { barcodes ->
                    val validBarcode = findValidBarcode(barcodes, imageProxy)
                    if (validBarcode != null) {
                        handleSuccessfulScan(validBarcode)
                    } else if (barcodes.isEmpty()) {
                        // Try ZXing fallback
                        tryZxingFallback(imageProxy)
                    }
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "ML Kit failed, trying ZXing fallback", e)
                    tryZxingFallback(imageProxy)
                }
                .addOnCompleteListener {
                    isProcessing = false
                    imageProxy.close()
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing image", e)
            isProcessing = false
            imageProxy.close()
        }
    }

    private fun findValidBarcode(barcodes: List<Barcode>, imageProxy: ImageProxy): String? {
        for (barcode in barcodes) {
            val rawValue = barcode.rawValue ?: continue
            
            // Check if barcode is within viewfinder bounds (if set)
            val boundingBox = barcode.boundingBox
            if (viewfinderRect != null && boundingBox != null) {
                // Convert barcode bounds to preview coordinates
                val barcodeRect = scaleRect(
                    boundingBox,
                    imageProxy.width,
                    imageProxy.height
                )
                
                // Check if barcode center is within viewfinder
                val centerX = barcodeRect.centerX()
                val centerY = barcodeRect.centerY()
                if (!viewfinderRect!!.contains(centerX, centerY)) {
                    continue // Skip barcodes outside viewfinder
                }
            }

            // Validate QR format (expecting pipe-delimited format)
            if (isValidQrFormat(rawValue)) {
                return rawValue
            }
        }
        return null
    }

    @Suppress("UNUSED_PARAMETER")
    private fun scaleRect(rect: Rect, imageWidth: Int, imageHeight: Int): RectF {
        // This is a simplified scaling - in production you'd account for 
        // preview view dimensions and rotation
        return RectF(
            rect.left.toFloat(),
            rect.top.toFloat(),
            rect.right.toFloat(),
            rect.bottom.toFloat()
        )
    }

    private fun tryZxingFallback(imageProxy: ImageProxy) {
        try {
            val buffer = imageProxy.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)

            val source = PlanarYUVLuminanceSource(
                bytes,
                imageProxy.width,
                imageProxy.height,
                0, 0,
                imageProxy.width,
                imageProxy.height,
                false
            )

            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            val result = zxingReader.decode(binaryBitmap)
            
            if (result != null && isValidQrFormat(result.text)) {
                handleSuccessfulScan(result.text)
            }
        } catch (e: NotFoundException) {
            // No QR code found - this is normal
        } catch (e: Exception) {
            Log.w(TAG, "ZXing fallback failed", e)
        }
    }

    private fun handleSuccessfulScan(qrCode: String) {
        lastScanTime = System.currentTimeMillis()
        onQrCodeDetected(qrCode)
    }

    /**
     * Validate QR code format
     * Expected format: "TYPE|HCF_ID|CATEGORY|SERIAL" e.g., "GUT|HCF123|YELLOW|00001234"
     */
    private fun isValidQrFormat(qr: String): Boolean {
        val parts = qr.split("|")
        return parts.size >= 4 && parts.all { it.isNotBlank() }
    }

    /**
     * Reset the cooldown timer to allow immediate scanning
     */
    fun resetCooldown() {
        lastScanTime = 0L
    }

    /**
     * Release resources
     */
    fun release() {
        mlKitScanner.close()
    }
}
