package com.example.pointeroverlay

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.pointeroverlay.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Always-visible baseline (can tune later via settings/slider)
        binding.circleOverlay.setBaseAlpha(0.2f)

        // Test buttons: fade to full and back to baseline (1s)
        binding.btnFadeIn.setOnClickListener {
            binding.circleOverlay.fadeTo(targetAlpha = 1.0f, durationMs = 1000L)
        }
        binding.btnFadeOut.setOnClickListener {
            binding.circleOverlay.fadeTo(targetAlpha = 0.2f, durationMs = 1000L)
        }
    }
}