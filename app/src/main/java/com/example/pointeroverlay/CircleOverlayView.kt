package com.example.pointeroverlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.core.graphics.withTranslation

class CircleOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0xFFFF0000.toInt() // Red
        alpha = (0.2f * 255).toInt() // Base visual opacity (20%)
    }

    private var radiusPx: Float = dp(24f)

    /** Set the base paint alpha (0f..1f). View's own alpha is animated via [fadeTo]. */
    fun setBaseAlpha(alphaFraction: Float) {
        val clamped = alphaFraction.coerceIn(0f, 1f)
        paint.alpha = (clamped * 255).toInt()
        invalidate()
    }

    /** Animate this view's overall alpha to [targetAlpha] over [durationMs]. */
    fun fadeTo(targetAlpha: Float, durationMs: Long) {
        val clamped = targetAlpha.coerceIn(0f, 1f)
        animate().alpha(clamped).setDuration(durationMs).start()
    }

    fun setRadiusDp(r: Float) {
        radiusPx = dp(r)
        invalidate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // Start at overall alpha = 1 so base paint alpha (20%) is visible initially
        alpha = 1f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        canvas.withTranslation(cx, cy) {
            drawCircle(0f, 0f, radiusPx, paint)
        }
    }

    private fun dp(v: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, resources.displayMetrics)
}