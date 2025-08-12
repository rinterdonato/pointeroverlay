package com.example.pointeroverlay

import android.animation.ValueAnimator
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
    }

    private var radiusPx: Float = dp(24f)
    private var baseAlphaFraction: Float = 0.2f
    private var currentAlphaFraction: Float = baseAlphaFraction

    /** Sets the baseline alpha (always visible level). */
    fun setBaseAlpha(alphaFraction: Float) {
        baseAlphaFraction = alphaFraction.coerceIn(0f, 1f)
        currentAlphaFraction = baseAlphaFraction
        invalidate()
    }

    /** Fades to an arbitrary alpha fraction over the given duration. */
    fun fadeTo(targetAlpha: Float, durationMs: Long) {
        val startAlpha = currentAlphaFraction
        val endAlpha = targetAlpha.coerceIn(0f, 1f)
        ValueAnimator.ofFloat(startAlpha, endAlpha).apply {
            duration = durationMs
            addUpdateListener { animator ->
                currentAlphaFraction = animator.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    /** Fades to full opacity. */
    fun fadeToFull(durationMs: Long) {
        fadeTo(1.0f, durationMs)
    }

    /** Fades back to baseline opacity. */
    fun fadeToBase(durationMs: Long) {
        fadeTo(baseAlphaFraction, durationMs)
    }

    /** Sets the circle radius in dp. */
    fun setRadiusDp(r: Float) {
        radiusPx = dp(r)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        paint.alpha = (currentAlphaFraction * 255).toInt()
        canvas.withTranslation(cx, cy) {
            drawCircle(0f, 0f, radiusPx, paint)
        }
    }

    private fun dp(v: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, resources.displayMetrics)
}