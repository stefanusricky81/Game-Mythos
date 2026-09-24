package com.example.audio

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class SoundManager(private val context: Context) {

    private var toneGenerator: ToneGenerator? = null
    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    var soundEnabled: Boolean = true
    var hapticsEnabled: Boolean = true

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 70)
        } catch (_: Exception) {
            toneGenerator = null
        }
    }

    fun playCardSound() {
        if (soundEnabled) {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 100)
        }
        vibrate(35)
    }

    fun playAttackSound() {
        if (soundEnabled) {
            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_PIP, 140)
        }
        vibrate(60)
    }

    fun playShieldSound() {
        if (soundEnabled) {
            toneGenerator?.startTone(ToneGenerator.TONE_SUP_CONFIRM, 120)
        }
        vibrate(40)
    }

    fun playUltimateSound() {
        if (soundEnabled) {
            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 400)
        }
        vibrateHeavy()
    }

    fun playVictorySound() {
        if (soundEnabled) {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 350)
        }
        vibrate(100)
    }

    fun playDefeatSound() {
        if (soundEnabled) {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_NACK, 300)
        }
        vibrate(150)
    }

    private fun vibrate(durationMs: Long) {
        if (!hapticsEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (_: Exception) {}
    }

    private fun vibrateHeavy() {
        if (!hapticsEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 80, 50, 150, 50, 250)
                val amplitudes = intArrayOf(0, 180, 0, 220, 0, 255)
                vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(400)
            }
        } catch (_: Exception) {}
    }

    fun release() {
        try {
            toneGenerator?.release()
            toneGenerator = null
        } catch (_: Exception) {}
    }
}
