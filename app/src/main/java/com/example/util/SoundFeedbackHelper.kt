package com.example.util

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

/**
 * Helper class for zero-latency acoustic chimes and haptic feedback during parcel processing.
 * Uses Android system ToneGenerator for instant offline sound confirmation.
 */
class SoundFeedbackHelper(private val context: Context) {

    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 85)
        } catch (e: Exception) {
            Log.w("SoundFeedbackHelper", "Failed to initialize ToneGenerator: ${e.message}")
        }
    }

    /**
     * Crisp, pleasant chime on successfully verified barcode (5/5 frames) or saved parcel.
     */
    fun playSuccessFeedback(soundEnabled: Boolean = true, hapticEnabled: Boolean = true) {
        if (soundEnabled) {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 120)
            } catch (e: Exception) {
                // Ignore audio errors gracefully
            }
        }
        if (hapticEnabled) {
            triggerVibration(50)
        }
    }

    /**
     * Alert double-tone for duplicate barcode detection or input error.
     */
    fun playWarningFeedback(soundEnabled: Boolean = true, hapticEnabled: Boolean = true) {
        if (soundEnabled) {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_NACK, 250)
            } catch (e: Exception) {
                // Ignore audio errors gracefully
            }
        }
        if (hapticEnabled) {
            triggerVibration(150)
        }
    }

    /**
     * Gentle chime when OCR captures an address and auto-fills values.
     */
    fun playOcrCaptureFeedback(soundEnabled: Boolean = true, hapticEnabled: Boolean = true) {
        if (soundEnabled) {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 80)
            } catch (e: Exception) {
                // Ignore audio errors gracefully
            }
        }
        if (hapticEnabled) {
            triggerVibration(40)
        }
    }

    private fun triggerVibration(durationMillis: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(durationMillis, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(
                        VibrationEffect.createOneShot(durationMillis, VibrationEffect.DEFAULT_AMPLITUDE)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(durationMillis)
                }
            }
        } catch (e: Exception) {
            // Ignore vibration hardware failures
        }
    }

    fun release() {
        try {
            toneGenerator?.release()
            toneGenerator = null
        } catch (e: Exception) {
            // Ignore
        }
    }
}
