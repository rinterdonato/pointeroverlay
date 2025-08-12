package com.example.pointeroverlay

import android.content.Context
import android.util.Size
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import java.nio.ByteBuffer
import java.util.concurrent.Executor

/**
 * Simple CameraX image analyzer that computes average luma (Y plane) and
 * calls [onObscured] when the smoothed luma stays below a threshold.
 */
class CameraObstructionDetector(
    private val context: Context,
    private val lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    private val onObscured: (Boolean, Double) -> Unit,
) {
    private var cameraProvider: ProcessCameraProvider? = null
    private var analysis: ImageAnalysis? = null

    private var emaLuma: Double = 0.0

    fun start(
        targetResolution: Size = Size(640, 480),
        analyzerExecutor: Executor = ContextCompat.getMainExecutor(context),
        lowThreshold: Double = 20.0,    // below => obscured (after smoothing)
        highThreshold: Double = 30.0,   // above => not obscured (hysteresis)
        emaAlpha: Double = 0.2,         // smoothing factor (0..1)
        maxFps: Int = 10
    ) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            cameraProvider = providerFuture.get()
            bindAnalysis(targetResolution, analyzerExecutor, lowThreshold, highThreshold, emaAlpha, maxFps)
        }, ContextCompat.getMainExecutor(context))
    }

    fun stop() {
        cameraProvider?.unbindAll()
        analysis = null
    }

    private fun bindAnalysis(
        targetResolution: Size,
        analyzerExecutor: Executor,
        lowT: Double,
        highT: Double,
        emaAlpha: Double,
        maxFps: Int
    ) {
        val provider = cameraProvider ?: return
        provider.unbindAll()

        val selector = CameraSelector.DEFAULT_BACK_CAMERA

        val analysisUseCase = ImageAnalysis.Builder()
            .setTargetResolution(targetResolution)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        var lastTs = 0L
        analysisUseCase.setAnalyzer(analyzerExecutor) { image ->
            val now = System.nanoTime()
            if (lastTs != 0L) {
                val minDeltaNs = 1_000_000_000L / maxFps
                if (now - lastTs < minDeltaNs) { image.close(); return@setAnalyzer }
            }
            lastTs = now

            val yPlane = image.planes[0].buffer
            val avg = average(yPlane, image.planes[0].rowStride, image.width, image.height)
            emaLuma = if (emaLuma == 0.0) avg else (emaAlpha * avg + (1 - emaAlpha) * emaLuma)

            val obscured = when {
                emaLuma <= lowT -> true
                emaLuma >= highT -> false
                else -> null // hysteresis: keep previous
            }

            if (obscured != null) onObscured(obscured, emaLuma)
            image.close()
        }

        provider.bindToLifecycle(lifecycleOwner, selector, analysisUseCase)
        analysis = analysisUseCase
    }

    private fun average(buffer: ByteBuffer, rowStride: Int, width: Int, height: Int): Double {
        buffer.rewind()
        var sum = 0L
        var count = 0
        // Sample every few pixels for speed (stride 4)
        val step = 4
        for (y in 0 until height step step) {
            val rowStart = y * rowStride
            for (x in 0 until width step step) {
                val index = rowStart + x
                if (index < buffer.limit()) {
                    val v = buffer.get(index)
                    sum += (v.toInt() and 0xFF)
                    count++
                }
            }
        }
        return if (count == 0) 0.0 else sum.toDouble() / count
    }
}
