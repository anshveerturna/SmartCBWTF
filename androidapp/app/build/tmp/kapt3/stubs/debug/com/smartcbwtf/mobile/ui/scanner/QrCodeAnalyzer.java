package com.smartcbwtf.mobile.ui.scanner;

/**
 * Image analyzer for QR code detection using ML Kit with ZXing fallback.
 * Features:
 * - ML Kit primary detection for speed and accuracy
 * - ZXing fallback for edge cases
 * - Cooldown period to prevent duplicate scans
 * - Optional viewfinder bounds filtering
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000j\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\u000e\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\t\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\t\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0002\b\u0007\u0018\u0000 42\u00020\u0001:\u00014B1\u0012\u0012\u0010\u0002\u001a\u000e\u0012\u0004\u0012\u00020\u0004\u0012\u0004\u0012\u00020\u00050\u0003\u0012\u0016\u0010\u0006\u001a\u0012\u0012\b\u0012\u00060\u0007j\u0002`\b\u0012\u0004\u0012\u00020\u00050\u0003\u00a2\u0006\u0002\u0010\tJ\u0010\u0010\u001c\u001a\u00020\u00052\u0006\u0010\u001d\u001a\u00020\u001eH\u0017J \u0010\u001f\u001a\u0004\u0018\u00010\u00042\f\u0010 \u001a\b\u0012\u0004\u0012\u00020\"0!2\u0006\u0010\u001d\u001a\u00020\u001eH\u0002J\u0010\u0010#\u001a\u00020\u00052\u0006\u0010$\u001a\u00020\u0004H\u0002J\u0006\u0010%\u001a\u00020\u000bJ\u0010\u0010&\u001a\u00020\u000b2\u0006\u0010\'\u001a\u00020\u0004H\u0002J\u0006\u0010(\u001a\u00020\u0005J\u0006\u0010)\u001a\u00020\u0005J \u0010*\u001a\u00020\u00162\u0006\u0010+\u001a\u00020,2\u0006\u0010-\u001a\u00020.2\u0006\u0010/\u001a\u00020.H\u0002J\u000e\u00100\u001a\u00020\u00052\u0006\u00101\u001a\u00020\u000bJ\u0010\u00102\u001a\u00020\u00052\b\u0010+\u001a\u0004\u0018\u00010\u0016J\u0010\u00103\u001a\u00020\u00052\u0006\u0010\u001d\u001a\u00020\u001eH\u0002R\u000e\u0010\n\u001a\u00020\u000bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\f\u001a\u00020\u000bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\r\u001a\u00020\u000eX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u001b\u0010\u000f\u001a\u00020\u00108BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\u0013\u0010\u0014\u001a\u0004\b\u0011\u0010\u0012R\u001e\u0010\u0006\u001a\u0012\u0012\b\u0012\u00060\u0007j\u0002`\b\u0012\u0004\u0012\u00020\u00050\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u0002\u001a\u000e\u0012\u0004\u0012\u00020\u0004\u0012\u0004\u0012\u00020\u00050\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0015\u001a\u0004\u0018\u00010\u0016X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u001b\u0010\u0017\u001a\u00020\u00188BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\u001b\u0010\u0014\u001a\u0004\b\u0019\u0010\u001a\u00a8\u00065"}, d2 = {"Lcom/smartcbwtf/mobile/ui/scanner/QrCodeAnalyzer;", "Landroidx/camera/core/ImageAnalysis$Analyzer;", "onQrCodeDetected", "Lkotlin/Function1;", "", "", "onError", "Ljava/lang/Exception;", "Lkotlin/Exception;", "(Lkotlin/jvm/functions/Function1;Lkotlin/jvm/functions/Function1;)V", "isPaused", "", "isProcessing", "lastScanTime", "", "mlKitScanner", "Lcom/google/mlkit/vision/barcode/BarcodeScanner;", "getMlKitScanner", "()Lcom/google/mlkit/vision/barcode/BarcodeScanner;", "mlKitScanner$delegate", "Lkotlin/Lazy;", "viewfinderRect", "Landroid/graphics/RectF;", "zxingReader", "Lcom/google/zxing/MultiFormatReader;", "getZxingReader", "()Lcom/google/zxing/MultiFormatReader;", "zxingReader$delegate", "analyze", "imageProxy", "Landroidx/camera/core/ImageProxy;", "findValidBarcode", "barcodes", "", "Lcom/google/mlkit/vision/barcode/common/Barcode;", "handleSuccessfulScan", "qrCode", "isInCooldown", "isValidQrFormat", "qr", "release", "resetCooldown", "scaleRect", "rect", "Landroid/graphics/Rect;", "imageWidth", "", "imageHeight", "setPaused", "paused", "setViewfinderRect", "tryZxingFallback", "Companion", "app_debug"})
public final class QrCodeAnalyzer implements androidx.camera.core.ImageAnalysis.Analyzer {
    @org.jetbrains.annotations.NotNull()
    private final kotlin.jvm.functions.Function1<java.lang.String, kotlin.Unit> onQrCodeDetected = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.jvm.functions.Function1<java.lang.Exception, kotlin.Unit> onError = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String TAG = "QrCodeAnalyzer";
    private static final long SCAN_COOLDOWN_MS = 1200L;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy mlKitScanner$delegate = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy zxingReader$delegate = null;
    private long lastScanTime = 0L;
    private boolean isProcessing = false;
    @org.jetbrains.annotations.Nullable()
    private android.graphics.RectF viewfinderRect;
    private boolean isPaused = false;
    @org.jetbrains.annotations.NotNull()
    public static final com.smartcbwtf.mobile.ui.scanner.QrCodeAnalyzer.Companion Companion = null;
    
    public QrCodeAnalyzer(@org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> onQrCodeDetected, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.Exception, kotlin.Unit> onError) {
        super();
    }
    
    private final com.google.mlkit.vision.barcode.BarcodeScanner getMlKitScanner() {
        return null;
    }
    
    private final com.google.zxing.MultiFormatReader getZxingReader() {
        return null;
    }
    
    /**
     * Set the viewfinder bounds for filtering detections
     */
    public final void setViewfinderRect(@org.jetbrains.annotations.Nullable()
    android.graphics.RectF rect) {
    }
    
    /**
     * Pause/resume scanning
     */
    public final void setPaused(boolean paused) {
    }
    
    /**
     * Check if scanner is in cooldown period
     */
    public final boolean isInCooldown() {
        return false;
    }
    
    @java.lang.Override()
    @android.annotation.SuppressLint(value = {"UnsafeOptInUsageError"})
    public void analyze(@org.jetbrains.annotations.NotNull()
    androidx.camera.core.ImageProxy imageProxy) {
    }
    
    private final java.lang.String findValidBarcode(java.util.List<? extends com.google.mlkit.vision.barcode.common.Barcode> barcodes, androidx.camera.core.ImageProxy imageProxy) {
        return null;
    }
    
    @kotlin.Suppress(names = {"UNUSED_PARAMETER"})
    private final android.graphics.RectF scaleRect(android.graphics.Rect rect, int imageWidth, int imageHeight) {
        return null;
    }
    
    private final void tryZxingFallback(androidx.camera.core.ImageProxy imageProxy) {
    }
    
    private final void handleSuccessfulScan(java.lang.String qrCode) {
    }
    
    /**
     * Validate QR code format
     * Expected format: "TYPE|HCF_ID|CATEGORY|SERIAL" e.g., "GUT|HCF123|YELLOW|00001234"
     */
    private final boolean isValidQrFormat(java.lang.String qr) {
        return false;
    }
    
    /**
     * Reset the cooldown timer to allow immediate scanning
     */
    public final void resetCooldown() {
    }
    
    /**
     * Release resources
     */
    public final void release() {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0018\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\t\n\u0000\n\u0002\u0010\u000e\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0007"}, d2 = {"Lcom/smartcbwtf/mobile/ui/scanner/QrCodeAnalyzer$Companion;", "", "()V", "SCAN_COOLDOWN_MS", "", "TAG", "", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}