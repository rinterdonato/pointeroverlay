package com.example.pointeroverlay

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.example.pointeroverlay.databinding.ActivityMainBinding
import java.nio.ByteBuffer
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    // === Tuned to your readings: covered ~30, uncovered ~103 ===
    private val fadeDurationMs = 1000L
    private val baseAlpha = 0.2f
    private val lowThreshold = 50.0   // <= 50 => obscured (finger)
    private val highThreshold = 90.0  // >= 90 => NOT obscured
    private val emaAlpha = 0.3        // a bit more responsive
    private val maxFps = 10

    private var emaLuma: Double = 0.0
    private var lastObscured: Boolean? = null
    private var lastAnalyzedNs: Long = 0L

    private val analysisExecutor: Executor by lazy { Executors.newSingleThreadExecutor() }

    private val cameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            Log.d(TAG, "Camera permission result: $granted")
            if (granted) startAnalysis() else stopAnalysis()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.circleOverlay.setBaseAlpha(baseAlpha)
        binding.btnFadeIn.setOnClickListener { binding.circleOverlay.fadeToFull(fadeDurationMs) }
        binding.btnFadeOut.setOnClickListener { binding.circleOverlay.fadeToBase(fadeDurationMs) }
    }

    override fun onResume() { super.onResume(); ensureCameraPermissionAndStart() }
    override fun onPause() { super.onPause(); stopAnalysis() }

    private fun ensureCameraPermissionAndStart() {
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (granted) startAnalysis() else cameraPermission.launch(Manifest.permission.CAMERA)
    }

    private fun startAnalysis() {
        val providerFuture = ProcessCameraProvider.getInstance(this)
        providerFuture.addListener({
            val provider = providerFuture.get()
            provider.unbindAll()

            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            analysis.setAnalyzer(analysisExecutor) { image ->
                val now = System.nanoTime()
                val minDeltaNs = 1_000_000_000L / maxFps
                if (lastAnalyzedNs != 0L && now - lastAnalyzedNs < minDeltaNs) {
                    image.close(); return@setAnalyzer
                }
                lastAnalyzedNs = now

                try {
                    val yPlane = image.planes[0].buffer
                    val avg = averageLuma(yPlane, image.planes[0].rowStride, image.width, image.height)
                    emaLuma = if (emaLuma == 0.0) avg else (emaAlpha * avg + (1 - emaAlpha) * emaLuma)

                    val obscured = when {
                        emaLuma <= lowThreshold -> true
                        emaLuma >= highThreshold -> false
                        else -> lastObscured // hysteresis
                    }

                    runOnUiThread {
                        // Update HUD if present
                        val hud = binding.root.findViewById<android.widget.TextView>(R.id.debugText)
                        hud?.text = "Luma: ${"%.1f".format(emaLuma)}  Obscured: ${obscured ?: "--"}  (lo=$lowThreshold hi=$highThreshold)"

                        if (obscured != lastObscured && obscured != null) {
                            Log.d(TAG, "EMA luma=${"%.1f".format(emaLuma)} obscured=$obscured")
                            if (obscured) binding.circleOverlay.fadeToFull(fadeDurationMs)
                            else binding.circleOverlay.fadeToBase(fadeDurationMs)
                            lastObscured = obscured
                        }
                    }
                } catch (t: Throwable) {
                    Log.e(TAG, "Analyzer error", t)
                } finally {
                    image.close()
                }
            }

            provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, analysis)
        }, mainExecutor())
    }

    private fun stopAnalysis() {
        runCatching { ProcessCameraProvider.getInstance(this).get().unbindAll() }
        lastObscured = null
        lastAnalyzedNs = 0L
    }

    private fun mainExecutor(): Executor = ContextCompat.getMainExecutor(this)

    private fun averageLuma(buffer: ByteBuffer, rowStride: Int, width: Int, height: Int): Double {
        buffer.rewind()
        var sum = 0L
        var count = 0
        val step = 4
        val limit = buffer.limit()
        for (y in 0 until height step step) {
            val rowStart = y * rowStride
            var x = 0
            while (x < width) {
                val index = rowStart + x
                if (index < limit) {
                    sum += (buffer.get(index).toInt() and 0xFF)
                    count++
                }
                x += step
            }
        }
        return if (count == 0) 0.0 else sum.toDouble() / count
    }

    companion object { private const val TAG = "PointerOverlay" }
}