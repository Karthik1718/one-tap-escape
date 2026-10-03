package com.karthik.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.*
import kotlin.math.*

object AudioManager {
    private var isMuted = false
    private var isPlaying = false
    private var currentJob: Job? = null
    private val audioScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private const val SAMPLE_RATE = 44100
    private var coinSamples: ShortArray? = null
    private var swerveSamples: ShortArray? = null
    private var crashSamples: ShortArray? = null
    private var nearMissSamples: ShortArray? = null
    private var powerupSamples: ShortArray? = null
    private var buttonSamples: ShortArray? = null
    private var sirenSamples: ShortArray? = null

    fun init(context: Context) {
        audioScope.launch {
            generateSfxBuffers()
        }
    }

    private fun generateSfxBuffers() {
        // 1. Coin: Bright 2-tone chime (1046Hz -> 1318Hz, 120ms)
        val coinLen = (SAMPLE_RATE * 0.12).toInt()
        val cSamples = ShortArray(coinLen)
        val halfCoin = coinLen / 2
        for (i in 0 until coinLen) {
            val freq = if (i < halfCoin) 1046.5 else 1318.5
            val angle = 2.0 * PI * i * freq / SAMPLE_RATE
            val env = 1.0 - (i.toDouble() / coinLen)
            cSamples[i] = (sin(angle) * 11000 * env).toInt().coerceIn(-32768, 32767).toShort()
        }
        coinSamples = cSamples

        // 2. Swerve: Quick whoosh (480Hz -> 180Hz, 70ms)
        val swerveLen = (SAMPLE_RATE * 0.07).toInt()
        val sSamples = ShortArray(swerveLen)
        for (i in 0 until swerveLen) {
            val progress = i.toDouble() / swerveLen
            val freq = 480.0 - (progress * 300.0)
            val angle = 2.0 * PI * i * freq / SAMPLE_RATE
            val env = sin(progress * PI)
            sSamples[i] = (sin(angle) * 8000 * env).toInt().coerceIn(-32768, 32767).toShort()
        }
        swerveSamples = sSamples

        // 3. Crash: Low impact thud + crunchy noise burst (260ms)
        val crashLen = (SAMPLE_RATE * 0.26).toInt()
        val crSamples = ShortArray(crashLen)
        val random = java.util.Random(42)
        for (i in 0 until crashLen) {
            val progress = i.toDouble() / crashLen
            val env = (1.0 - progress).pow(2.0)
            val lowRumble = sin(2.0 * PI * i * 85.0 / SAMPLE_RATE) * 12000
            val noise = (random.nextDouble() * 2.0 - 1.0) * 14000
            crSamples[i] = ((lowRumble * 0.5 + noise * 0.5) * env).toInt().coerceIn(-32768, 32767).toShort()
        }
        crashSamples = crSamples

        // 4. Near Miss: Energetic rising sparkle (950Hz -> 1600Hz, 130ms)
        val nearLen = (SAMPLE_RATE * 0.13).toInt()
        val nmSamples = ShortArray(nearLen)
        for (i in 0 until nearLen) {
            val progress = i.toDouble() / nearLen
            val freq = 950.0 + (progress * 650.0)
            val angle1 = 2.0 * PI * i * freq / SAMPLE_RATE
            val angle2 = 2.0 * PI * i * (freq * 1.5) / SAMPLE_RATE
            val env = 1.0 - progress
            val wave = sin(angle1) * 0.7 + sin(angle2) * 0.3
            nmSamples[i] = (wave * 12000 * env).toInt().coerceIn(-32768, 32767).toShort()
        }
        nearMissSamples = nmSamples

        // 5. Powerup: Ascending 4-tone fan-fare (240ms)
        val powLen = (SAMPLE_RATE * 0.24).toInt()
        val pSamples = ShortArray(powLen)
        val notes = doubleArrayOf(523.25, 659.25, 783.99, 1046.5)
        for (i in 0 until powLen) {
            val noteIdx = min((i * 4 / powLen), 3)
            val freq = notes[noteIdx]
            val angle = 2.0 * PI * i * freq / SAMPLE_RATE
            val subI = i % (powLen / 4)
            val subEnv = 1.0 - (subI.toDouble() / (powLen / 4))
            pSamples[i] = (sin(angle) * 10000 * subEnv).toInt().coerceIn(-32768, 32767).toShort()
        }
        powerupSamples = pSamples

        // 6. Button Click (30ms)
        val btnLen = (SAMPLE_RATE * 0.03).toInt()
        val bSamples = ShortArray(btnLen)
        for (i in 0 until btnLen) {
            val progress = i.toDouble() / btnLen
            val angle = 2.0 * PI * i * (750.0 - progress * 350.0) / SAMPLE_RATE
            val env = 1.0 - progress
            bSamples[i] = (sin(angle) * 9000 * env).toInt().coerceIn(-32768, 32767).toShort()
        }
        buttonSamples = bSamples

        // 7. Police Siren: Wailing emergency siren sound (450ms, 600Hz <-> 1300Hz wail)
        val sirenLen = (SAMPLE_RATE * 0.45).toInt()
        val srSamples = ShortArray(sirenLen)
        for (i in 0 until sirenLen) {
            val progress = i.toDouble() / sirenLen
            val wail = sin(progress * PI * 2.0)
            val freq = 900.0 + (wail * 350.0)
            val angle = 2.0 * PI * i * freq / SAMPLE_RATE
            val env = sin(progress * PI)
            srSamples[i] = (sin(angle) * 11000 * env).toInt().coerceIn(-32768, 32767).toShort()
        }
        sirenSamples = srSamples
    }

    fun startMusic() {
        if (isPlaying || isMuted) return
        isPlaying = true
        currentJob = audioScope.launch {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
                
            val audioFormat = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(SAMPLE_RATE)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()

            // 130 BPM Arcade Driving Groove
            val bpm = 132.0
            val beatSec = 60.0 / bpm
            val stepSec = beatSec / 4.0 // 16th note duration (~0.1136s)
            val stepSamples = (SAMPLE_RATE * stepSec).toInt()
            val totalSteps = 64 // 4 bars of 16 steps
            val loopTotalSamples = stepSamples * totalSteps

            // Synthesize the 4-bar driving track into a seamless loop
            val musicBuffer = ShortArray(loopTotalSamples)
            val random = java.util.Random(1337)

            // D-minor Bass frequencies (Hz) for 64 steps
            val bassPattern = doubleArrayOf(
                73.42, 73.42, 73.42, 73.42, 73.42, 73.42, 87.31, 98.00,
                98.00, 98.00, 98.00, 98.00, 110.0, 110.0, 130.81, 73.42,
                73.42, 73.42, 73.42, 73.42, 87.31, 87.31, 98.00, 110.0,
                130.81, 130.81, 110.0, 110.0, 98.00, 87.31, 82.41, 73.42,
                // Second half variations
                73.42, 73.42, 146.83, 73.42, 73.42, 87.31, 98.00, 110.0,
                98.00, 98.00, 196.00, 98.00, 110.0, 110.0, 130.81, 73.42,
                87.31, 87.31, 174.61, 87.31, 98.00, 98.00, 110.0, 130.81,
                146.83, 130.81, 110.0, 98.00, 87.31, 82.41, 73.42, 73.42
            )

            // Lead Melody frequencies (Hz) for 64 steps (0.0 = rest)
            val leadPattern = doubleArrayOf(
                293.66, 0.0, 349.23, 0.0, 440.00, 0.0, 587.33, 587.33,
                523.25, 0.0, 440.00, 0.0, 392.00, 392.00, 440.00, 0.0,
                349.23, 0.0, 392.00, 0.0, 440.00, 0.0, 523.25, 0.0,
                587.33, 523.25, 440.00, 392.00, 349.23, 329.63, 293.66, 0.0,
                // Variation part 2
                293.66, 349.23, 440.00, 587.33, 659.25, 0.0, 587.33, 0.0,
                523.25, 0.0, 440.00, 523.25, 392.00, 0.0, 440.00, 0.0,
                440.00, 523.25, 587.33, 659.25, 698.46, 659.25, 587.33, 523.25,
                440.00, 392.00, 349.23, 329.63, 293.66, 261.63, 293.66, 293.66
            )

            var phaseBass = 0.0
            var phaseLead = 0.0

            for (step in 0 until totalSteps) {
                val bassFreq = bassPattern[step]
                val leadFreq = leadPattern[step]
                val isBeat = (step % 4 == 0) // Kick on quarter notes
                val isOffbeat = (step % 2 == 1) // Hi-hat on 8th offbeats

                val offset = step * stepSamples
                for (i in 0 until stepSamples) {
                    val sampleIdx = offset + i
                    val tInStep = i.toDouble() / stepSamples

                    // 1. Synth Bass (Snappy punch with rich 2nd and 3rd harmonics)
                    val bassEnv = (1.0 - tInStep).pow(1.5)
                    phaseBass += 2.0 * PI * bassFreq / SAMPLE_RATE
                    val bassWave = sin(phaseBass) + 0.45 * sin(phaseBass * 2.0) + 0.2 * sin(phaseBass * 3.0)
                    val bassSample = bassWave * bassEnv * 6500.0

                    // 2. Lead Synth (Bright analog saw/sine blend with vibrato)
                    var leadSample = 0.0
                    if (leadFreq > 0.0) {
                        val vibrato = 1.0 + 0.015 * sin(2.0 * PI * 6.0 * (sampleIdx.toDouble() / SAMPLE_RATE))
                        phaseLead += 2.0 * PI * (leadFreq * vibrato) / SAMPLE_RATE
                        val leadEnv = (1.0 - tInStep * 0.4).coerceIn(0.0, 1.0)
                        val leadWave = sin(phaseLead) * 0.65 + sin(phaseLead * 2.0) * 0.35
                        leadSample = leadWave * leadEnv * 5500.0
                    }

                    // 3. Electronic Kick (Pitch drop 140Hz -> 45Hz)
                    var kickSample = 0.0
                    if (isBeat && i < stepSamples * 0.75) {
                        val kProgress = i.toDouble() / (stepSamples * 0.75)
                        val kFreq = 140.0 - (kProgress * 95.0)
                        val kAngle = 2.0 * PI * i * kFreq / SAMPLE_RATE
                        val kEnv = (1.0 - kProgress).pow(2.0)
                        kickSample = sin(kAngle) * 7500.0 * kEnv
                    }

                    // 4. Hi-Hat (Crisp noise tick)
                    var hatSample = 0.0
                    if (isOffbeat && i < stepSamples * 0.35) {
                        val hProgress = i.toDouble() / (stepSamples * 0.35)
                        val hEnv = (1.0 - hProgress).pow(2.5)
                        hatSample = (random.nextDouble() * 2.0 - 1.0) * 3200.0 * hEnv
                    }

                    val total = bassSample + leadSample + kickSample + hatSample
                    musicBuffer[sampleIdx] = total.toInt().coerceIn(-32768, 32767).toShort()
                }
            }

            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(audioAttributes)
                .setAudioFormat(audioFormat)
                .setBufferSizeInBytes(loopTotalSamples * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(musicBuffer, 0, loopTotalSamples)
            audioTrack.setLoopPoints(0, loopTotalSamples, -1) // Infinite smooth loop
            audioTrack.play()

            try {
                while (isActive && isPlaying && !isMuted) {
                    delay(1000)
                }
            } finally {
                try {
                    audioTrack.stop()
                    audioTrack.release()
                } catch (_: Exception) {}
            }
        }
    }

    fun stopMusic() {
        isPlaying = false
        currentJob?.cancel()
    }

    fun toggleMute(): Boolean {
        isMuted = !isMuted
        if (isMuted) {
            stopMusic()
        } else {
            startMusic()
        }
        return isMuted
    }

    fun playSfx(context: Context? = null, type: String) {
        if (isMuted) return
        val buffer = when (type) {
            "coin" -> coinSamples
            "swerve" -> swerveSamples
            "crash" -> crashSamples
            "near_miss" -> nearMissSamples
            "powerup" -> powerupSamples
            "button" -> buttonSamples
            "siren" -> sirenSamples
            else -> buttonSamples
        } ?: return

        audioScope.launch {
            try {
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()

                val audioFormat = AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()

                val track = AudioTrack.Builder()
                    .setAudioAttributes(audioAttributes)
                    .setAudioFormat(audioFormat)
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(buffer, 0, buffer.size)
                track.play()
                delay((buffer.size.toDouble() / SAMPLE_RATE * 1000).toLong() + 30)
                track.stop()
                track.release()
            } catch (_: Exception) {}
        }
    }

    fun vibrate(context: Context, type: String = "light") {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            } ?: return

            if (!vibrator.hasVibrator()) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                when (type) {
                    "light" -> vibrator.vibrate(VibrationEffect.createOneShot(20, 90))
                    "medium" -> vibrator.vibrate(VibrationEffect.createOneShot(45, 170))
                    "heavy" -> vibrator.vibrate(VibrationEffect.createOneShot(180, 255))
                    "double" -> vibrator.vibrate(
                        VibrationEffect.createWaveform(
                            longArrayOf(0, 30, 40, 45),
                            intArrayOf(0, 160, 0, 255),
                            -1
                        )
                    )
                    else -> vibrator.vibrate(VibrationEffect.createOneShot(30, 110))
                }
            } else {
                @Suppress("DEPRECATION")
                when (type) {
                    "light" -> vibrator.vibrate(20)
                    "medium" -> vibrator.vibrate(45)
                    "heavy" -> vibrator.vibrate(180)
                    "double" -> vibrator.vibrate(longArrayOf(0, 30, 40, 45), -1)
                    else -> vibrator.vibrate(30)
                }
            }
        } catch (_: Exception) {}
    }

    fun isMuted() = isMuted
}
