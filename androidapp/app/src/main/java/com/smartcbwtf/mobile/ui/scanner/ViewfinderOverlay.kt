package com.smartcbwtf.mobile.ui.scanner

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import com.smartcbwtf.mobile.R

/**
 * Custom overlay view for QR scanner with:
 * - Dimmed scrim around viewfinder
 * - Rounded rectangle viewfinder with corner brackets
 * - Animated scanning line
 * - Success/error state indication
 */
class ViewfinderOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val VIEWFINDER_WIDTH_RATIO = 0.75f // 75% of screen width
        private const val CORNER_RADIUS = 24f
        private const val CORNER_LENGTH = 40f
        private const val CORNER_STROKE_WIDTH = 4f
        private const val BORDER_STROKE_WIDTH = 2f
        private const val SCAN_LINE_HEIGHT = 3f
    }

    // Paints
    private val scrimPaint = Paint().apply {
        color = Color.parseColor("#99000000") // Semi-transparent black
        style = Paint.Style.FILL
    }

    private val borderPaint = Paint().apply {
        color = Color.parseColor("#40FFFFFF")
        style = Paint.Style.STROKE
        strokeWidth = BORDER_STROKE_WIDTH
        isAntiAlias = true
    }

    private val cornerPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.primary_color)
        style = Paint.Style.STROKE
        strokeWidth = CORNER_STROKE_WIDTH
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
    }

    private val scanLinePaint = Paint().apply {
        isAntiAlias = true
    }

    private val successPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.successAccent)
        style = Paint.Style.STROKE
        strokeWidth = CORNER_STROKE_WIDTH
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
    }

    // Viewfinder rect
    private var viewfinderRect = RectF()
    private val clearPaint = Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    // Animation
    private var scanLineY = 0f
    private var scanLineAnimator: ValueAnimator? = null
    private var isAnimating = false

    // State
    private var isSuccess = false
    private var showScanLine = true

    init {
        // Enable hardware layer for better performance with xfermode
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        calculateViewfinderRect()
        setupScanLineGradient()
        startScanLineAnimation()
    }

    private fun calculateViewfinderRect() {
        val viewfinderSize = (width * VIEWFINDER_WIDTH_RATIO).toInt()
        val left = (width - viewfinderSize) / 2f
        val top = (height - viewfinderSize) / 2f - (height * 0.05f) // Slightly above center
        viewfinderRect = RectF(left, top, left + viewfinderSize, top + viewfinderSize)
    }

    private fun setupScanLineGradient() {
        val gradient = LinearGradient(
            viewfinderRect.left, 0f, viewfinderRect.right, 0f,
            intArrayOf(
                Color.TRANSPARENT,
                ContextCompat.getColor(context, R.color.primary_color),
                ContextCompat.getColor(context, R.color.primary_color),
                Color.TRANSPARENT
            ),
            floatArrayOf(0f, 0.2f, 0.8f, 1f),
            Shader.TileMode.CLAMP
        )
        scanLinePaint.shader = gradient
    }

    private fun startScanLineAnimation() {
        if (isAnimating) return
        
        scanLineAnimator?.cancel()
        scanLineAnimator = ValueAnimator.ofFloat(viewfinderRect.top, viewfinderRect.bottom).apply {
            duration = 2500
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            interpolator = LinearInterpolator()
            addUpdateListener { animator ->
                scanLineY = animator.animatedValue as Float
                invalidate()
            }
        }
        scanLineAnimator?.start()
        isAnimating = true
    }

    fun stopScanLineAnimation() {
        scanLineAnimator?.cancel()
        isAnimating = false
    }

    fun resumeScanLineAnimation() {
        if (!isAnimating && showScanLine) {
            startScanLineAnimation()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw scrim (dimmed overlay)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), scrimPaint)

        // Clear the viewfinder area
        canvas.drawRoundRect(viewfinderRect, CORNER_RADIUS, CORNER_RADIUS, clearPaint)

        // Draw border
        val paint = if (isSuccess) successPaint else borderPaint
        canvas.drawRoundRect(viewfinderRect, CORNER_RADIUS, CORNER_RADIUS, paint)

        // Draw corner brackets
        drawCornerBrackets(canvas)

        // Draw scan line (if not in success state)
        if (showScanLine && !isSuccess && scanLineY >= viewfinderRect.top && scanLineY <= viewfinderRect.bottom) {
            canvas.drawRect(
                viewfinderRect.left + 20f,
                scanLineY - SCAN_LINE_HEIGHT / 2,
                viewfinderRect.right - 20f,
                scanLineY + SCAN_LINE_HEIGHT / 2,
                scanLinePaint
            )
        }
    }

    private fun drawCornerBrackets(canvas: Canvas) {
        val paint = if (isSuccess) successPaint else cornerPaint
        val r = viewfinderRect
        val len = CORNER_LENGTH
        val offset = CORNER_STROKE_WIDTH / 2

        // Top-left corner
        canvas.drawLine(r.left + offset, r.top + len, r.left + offset, r.top + CORNER_RADIUS, paint)
        canvas.drawLine(r.left + CORNER_RADIUS, r.top + offset, r.left + len, r.top + offset, paint)

        // Top-right corner
        canvas.drawLine(r.right - offset, r.top + len, r.right - offset, r.top + CORNER_RADIUS, paint)
        canvas.drawLine(r.right - CORNER_RADIUS, r.top + offset, r.right - len, r.top + offset, paint)

        // Bottom-left corner
        canvas.drawLine(r.left + offset, r.bottom - len, r.left + offset, r.bottom - CORNER_RADIUS, paint)
        canvas.drawLine(r.left + CORNER_RADIUS, r.bottom - offset, r.left + len, r.bottom - offset, paint)

        // Bottom-right corner
        canvas.drawLine(r.right - offset, r.bottom - len, r.right - offset, r.bottom - CORNER_RADIUS, paint)
        canvas.drawLine(r.right - CORNER_RADIUS, r.bottom - offset, r.right - len, r.bottom - offset, paint)
    }

    /**
     * Get the viewfinder bounds for limiting barcode detection area
     */
    fun getViewfinderRect(): RectF = viewfinderRect

    /**
     * Set success state (green corners, no scan line)
     */
    fun setSuccessState(success: Boolean) {
        isSuccess = success
        if (success) {
            stopScanLineAnimation()
        } else {
            resumeScanLineAnimation()
        }
        invalidate()
    }

    /**
     * Show/hide the scanning line
     */
    fun setShowScanLine(show: Boolean) {
        showScanLine = show
        if (show) {
            resumeScanLineAnimation()
        } else {
            stopScanLineAnimation()
        }
        invalidate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        scanLineAnimator?.cancel()
    }
}
