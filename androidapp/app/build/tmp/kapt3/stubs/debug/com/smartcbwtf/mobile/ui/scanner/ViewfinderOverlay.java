package com.smartcbwtf.mobile.ui.scanner;

/**
 * Custom overlay view for QR scanner with:
 * - Dimmed scrim around viewfinder
 * - Rounded rectangle viewfinder with corner brackets
 * - Animated scanning line
 * - Success/error state indication
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000T\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0007\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0012\u0018\u0000 /2\u00020\u0001:\u0001/B%\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\n\b\u0002\u0010\u0004\u001a\u0004\u0018\u00010\u0005\u0012\b\b\u0002\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\u0002\u0010\bJ\b\u0010\u001a\u001a\u00020\u001bH\u0002J\u0010\u0010\u001c\u001a\u00020\u001b2\u0006\u0010\u001d\u001a\u00020\u001eH\u0002J\u0006\u0010\u001f\u001a\u00020\u0019J\b\u0010 \u001a\u00020\u001bH\u0014J\u0010\u0010!\u001a\u00020\u001b2\u0006\u0010\u001d\u001a\u00020\u001eH\u0014J(\u0010\"\u001a\u00020\u001b2\u0006\u0010#\u001a\u00020\u00072\u0006\u0010$\u001a\u00020\u00072\u0006\u0010%\u001a\u00020\u00072\u0006\u0010&\u001a\u00020\u0007H\u0014J\u0006\u0010\'\u001a\u00020\u001bJ\u000e\u0010(\u001a\u00020\u001b2\u0006\u0010)\u001a\u00020\u000eJ\u000e\u0010*\u001a\u00020\u001b2\u0006\u0010+\u001a\u00020\u000eJ\b\u0010,\u001a\u00020\u001bH\u0002J\b\u0010-\u001a\u00020\u001bH\u0002J\u0006\u0010.\u001a\u00020\u001bR\u000e\u0010\t\u001a\u00020\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\f\u001a\u00020\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\r\u001a\u00020\u000eX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000f\u001a\u00020\u000eX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0010\u001a\u0004\u0018\u00010\u0011X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0012\u001a\u00020\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0013\u001a\u00020\u0014X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0015\u001a\u00020\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0016\u001a\u00020\u000eX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0017\u001a\u00020\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0018\u001a\u00020\u0019X\u0082\u000e\u00a2\u0006\u0002\n\u0000\u00a8\u00060"}, d2 = {"Lcom/smartcbwtf/mobile/ui/scanner/ViewfinderOverlay;", "Landroid/view/View;", "context", "Landroid/content/Context;", "attrs", "Landroid/util/AttributeSet;", "defStyleAttr", "", "(Landroid/content/Context;Landroid/util/AttributeSet;I)V", "borderPaint", "Landroid/graphics/Paint;", "clearPaint", "cornerPaint", "isAnimating", "", "isSuccess", "scanLineAnimator", "Landroid/animation/ValueAnimator;", "scanLinePaint", "scanLineY", "", "scrimPaint", "showScanLine", "successPaint", "viewfinderRect", "Landroid/graphics/RectF;", "calculateViewfinderRect", "", "drawCornerBrackets", "canvas", "Landroid/graphics/Canvas;", "getViewfinderRect", "onDetachedFromWindow", "onDraw", "onSizeChanged", "w", "h", "oldw", "oldh", "resumeScanLineAnimation", "setShowScanLine", "show", "setSuccessState", "success", "setupScanLineGradient", "startScanLineAnimation", "stopScanLineAnimation", "Companion", "app_debug"})
public final class ViewfinderOverlay extends android.view.View {
    private static final float VIEWFINDER_WIDTH_RATIO = 0.75F;
    private static final float CORNER_RADIUS = 24.0F;
    private static final float CORNER_LENGTH = 40.0F;
    private static final float CORNER_STROKE_WIDTH = 4.0F;
    private static final float BORDER_STROKE_WIDTH = 2.0F;
    private static final float SCAN_LINE_HEIGHT = 3.0F;
    @org.jetbrains.annotations.NotNull()
    private final android.graphics.Paint scrimPaint = null;
    @org.jetbrains.annotations.NotNull()
    private final android.graphics.Paint borderPaint = null;
    @org.jetbrains.annotations.NotNull()
    private final android.graphics.Paint cornerPaint = null;
    @org.jetbrains.annotations.NotNull()
    private final android.graphics.Paint scanLinePaint = null;
    @org.jetbrains.annotations.NotNull()
    private final android.graphics.Paint successPaint = null;
    @org.jetbrains.annotations.NotNull()
    private android.graphics.RectF viewfinderRect;
    @org.jetbrains.annotations.NotNull()
    private final android.graphics.Paint clearPaint = null;
    private float scanLineY = 0.0F;
    @org.jetbrains.annotations.Nullable()
    private android.animation.ValueAnimator scanLineAnimator;
    private boolean isAnimating = false;
    private boolean isSuccess = false;
    private boolean showScanLine = true;
    @org.jetbrains.annotations.NotNull()
    public static final com.smartcbwtf.mobile.ui.scanner.ViewfinderOverlay.Companion Companion = null;
    
    @kotlin.jvm.JvmOverloads()
    public ViewfinderOverlay(@org.jetbrains.annotations.NotNull()
    android.content.Context context, @org.jetbrains.annotations.Nullable()
    android.util.AttributeSet attrs, int defStyleAttr) {
        super(null);
    }
    
    @java.lang.Override()
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    }
    
    private final void calculateViewfinderRect() {
    }
    
    private final void setupScanLineGradient() {
    }
    
    private final void startScanLineAnimation() {
    }
    
    public final void stopScanLineAnimation() {
    }
    
    public final void resumeScanLineAnimation() {
    }
    
    @java.lang.Override()
    protected void onDraw(@org.jetbrains.annotations.NotNull()
    android.graphics.Canvas canvas) {
    }
    
    private final void drawCornerBrackets(android.graphics.Canvas canvas) {
    }
    
    /**
     * Get the viewfinder bounds for limiting barcode detection area
     */
    @org.jetbrains.annotations.NotNull()
    public final android.graphics.RectF getViewfinderRect() {
        return null;
    }
    
    /**
     * Set success state (green corners, no scan line)
     */
    public final void setSuccessState(boolean success) {
    }
    
    /**
     * Show/hide the scanning line
     */
    public final void setShowScanLine(boolean show) {
    }
    
    @java.lang.Override()
    protected void onDetachedFromWindow() {
    }
    
    @kotlin.jvm.JvmOverloads()
    public ViewfinderOverlay(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        super(null);
    }
    
    @kotlin.jvm.JvmOverloads()
    public ViewfinderOverlay(@org.jetbrains.annotations.NotNull()
    android.content.Context context, @org.jetbrains.annotations.Nullable()
    android.util.AttributeSet attrs) {
        super(null);
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u0007\n\u0002\b\u0006\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\n"}, d2 = {"Lcom/smartcbwtf/mobile/ui/scanner/ViewfinderOverlay$Companion;", "", "()V", "BORDER_STROKE_WIDTH", "", "CORNER_LENGTH", "CORNER_RADIUS", "CORNER_STROKE_WIDTH", "SCAN_LINE_HEIGHT", "VIEWFINDER_WIDTH_RATIO", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}