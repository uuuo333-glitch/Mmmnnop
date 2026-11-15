package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.example.data.model.SoundEffectType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Real-time procedural PCM audio synthesizer via Android AudioTrack.
 * Generates crisp, distinctive alert & vehicle/FX sounds without external audio file dependencies.
 */
class SoundEffectEngine {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var activeJob: Job? = null

    fun play(effect: SoundEffectType) {
        activeJob?.cancel()
        activeJob = scope.launch {
            val samples = synthesize(effect)
            playPcm(samples)
        }
    }

    private fun playPcm(samples: ShortArray) {
        val minBuffer = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(samples.size * 2)

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(minBuffer)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        try {
            track.write(samples, 0, samples.size)
            track.play()
            val durationMs = (samples.size * 1000L) / SAMPLE_RATE
            Thread.sleep(durationMs + 80L)
        } catch (_: InterruptedException) {
            // Cancelled by next sound trigger
        } catch (_: Exception) {
            // Safe audio hardware guard in headless environments
        } finally {
            try {
                track.stop()
            } catch (_: Exception) {
            }
            track.release()
        }
    }

    private fun synthesize(effect: SoundEffectType): ShortArray = when (effect) {
        SoundEffectType.TRUCK_HORN -> synthTruckHorn()
        SoundEffectType.SPEEDING_CAR -> synthSpeedingCar()
        SoundEffectType.MOTORCYCLE -> synthMotorcycle()
        SoundEffectType.POLICE_SIREN -> synthPoliceSiren()
        SoundEffectType.FLY_BUZZ -> synthFlyBuzz()
        SoundEffectType.GLASS_SHATTER -> synthGlassShatter()
        SoundEffectType.TURBO_SPOOL -> synthTurboSpool()
        SoundEffectType.EXHAUST_POPS -> synthExhaustPops()
        SoundEffectType.ENGINE_REV -> synthEngineRev()
        SoundEffectType.PASSING_ROCKET -> synthPassingRocket()
        SoundEffectType.JET_AIRPLANE -> synthJetAirplane()
    }

    private fun synthTruckHorn(): ShortArray {
        val durationSec = 1.35
        val n = (SAMPLE_RATE * durationSec).toInt()
        val out = ShortArray(n)
        for (i in 0 until n) {
            val t = i.toDouble() / SAMPLE_RATE
            // Dual-blast air horn + subtle Doppler drop
            val env = when {
                t < 0.05 -> t / 0.05
                t < 0.38 -> 1.0
                t < 0.48 -> 0.15
                t < 0.54 -> 1.0
                t < 1.15 -> 1.0 - 0.15 * (t - 0.54)
                else -> (1.35 - t) / 0.20
            }.coerceIn(0.0, 1.0)
            val doppler = 1.0 - 0.08 * (t / durationSec)
            val f1 = 185.0 * doppler
            val f2 = 233.0 * doppler
            val s = 0.45 * saw(f1, t) + 0.40 * saw(f2, t) + 0.15 * sin(2 * PI * f1 * 2 * t)
            out[i] = (s * env * 25000).toInt().coerceIn(-32767, 32767).toShort()
        }
        return out
    }

    private fun synthSpeedingCar(): ShortArray {
        val durationSec = 1.4
        val n = (SAMPLE_RATE * durationSec).toInt()
        val out = ShortArray(n)
        var phase = 0.0
        for (i in 0 until n) {
            val t = i.toDouble() / SAMPLE_RATE
            val passPos = (t - 0.65)
            val gain = exp(-passPos * passPos * 4.5)
            val freq = 140.0 + 180.0 * (1.0 / (1.0 + exp(passPos * 5.0)))
            phase += 2 * PI * freq / SAMPLE_RATE
            val roar = 0.55 * sin(phase) + 0.3 * sin(phase * 2.0) + 0.15 * sin(phase * 3.0)
            val wind = (Random.nextDouble() * 2 - 1) * 0.12
            out[i] = ((roar + wind) * gain * 26000).toInt().coerceIn(-32767, 32767).toShort()
        }
        return out
    }

    private fun synthMotorcycle(): ShortArray {
        val durationSec = 1.4
        val n = (SAMPLE_RATE * durationSec).toInt()
        val out = ShortArray(n)
        var phase = 0.0
        for (i in 0 until n) {
            val t = i.toDouble() / SAMPLE_RATE
            val gearShift = if (t < 0.55) t / 0.55 else (t - 0.55) / 0.85
            val baseFreq = if (t < 0.55) 180.0 + 360.0 * gearShift else 240.0 + 420.0 * gearShift
            val env = sin(PI * (t / durationSec)).coerceIn(0.1, 1.0)
            phase += 2 * PI * baseFreq / SAMPLE_RATE
            val buzz = 0.5 * sawPhase(phase) + 0.35 * sin(phase * 2.0) + 0.15 * sin(phase * 0.5)
            out[i] = (buzz * env * 22000).toInt().coerceIn(-32767, 32767).toShort()
        }
        return out
    }

    private fun synthPoliceSiren(): ShortArray {
        val durationSec = 1.4
        val n = (SAMPLE_RATE * durationSec).toInt()
        val out = ShortArray(n)
        var phase = 0.0
        for (i in 0 until n) {
            val t = i.toDouble() / SAMPLE_RATE
            val sweep = 0.5 * (1.0 - kotlin.math.cos(2 * PI * 1.45 * t))
            val freq = 620.0 + 580.0 * sweep
            phase += 2 * PI * freq / SAMPLE_RATE
            val env = if (t > 1.25) (1.4 - t) / 0.15 else 1.0
            val s = 0.7 * sin(phase) + 0.3 * sin(phase * 3.0) * 0.33
            out[i] = (s * env * 24000).toInt().coerceIn(-32767, 32767).toShort()
        }
        return out
    }

    private fun synthFlyBuzz(): ShortArray {
        val durationSec = 1.1
        val n = (SAMPLE_RATE * durationSec).toInt()
        val out = ShortArray(n)
        for (i in 0 until n) {
            val t = i.toDouble() / SAMPLE_RATE
            val am = 0.5 + 0.5 * sin(2 * PI * 14.0 * t)
            val env = sin(PI * (t / durationSec)).coerceIn(0.0, 1.0) * am
            val freq = 195.0 + 28.0 * sin(2 * PI * 5.0 * t)
            val s = 0.5 * saw(freq, t) + 0.35 * sin(2 * PI * freq * 2.0 * t) + 0.15 * sin(2 * PI * freq * 3.0 * t)
            out[i] = (s * env * 19000).toInt().coerceIn(-32767, 32767).toShort()
        }
        return out
    }

    private fun synthGlassShatter(): ShortArray {
        val durationSec = 0.9
        val n = (SAMPLE_RATE * durationSec).toInt()
        val out = ShortArray(n)
        val shardFreqs = doubleArrayOf(2450.0, 3180.0, 4120.0, 5290.0, 6410.0)
        for (i in 0 until n) {
            val t = i.toDouble() / SAMPLE_RATE
            val impact = (Random.nextDouble() * 2 - 1) * exp(-t * 14.0)
            var shards = 0.0
            for (k in shardFreqs.indices) {
                val delay = k * 0.045
                if (t >= delay) {
                    val dt = t - delay
                    shards += sin(2 * PI * shardFreqs[k] * dt) * exp(-dt * 9.0) * 0.25
                }
            }
            out[i] = ((impact * 0.7 + shards) * 26000).toInt().coerceIn(-32767, 32767).toShort()
        }
        return out
    }

    private fun synthTurboSpool(): ShortArray {
        val durationSec = 1.3
        val n = (SAMPLE_RATE * durationSec).toInt()
        val out = ShortArray(n)
        var phase = 0.0
        for (i in 0 until n) {
            val t = i.toDouble() / SAMPLE_RATE
            val sample = if (t < 0.78) {
                // Turbine spool whistle 900Hz -> 3400Hz
                val p = t / 0.78
                val freq = 900.0 + 2500.0 * p * p
                phase += 2 * PI * freq / SAMPLE_RATE
                val air = (Random.nextDouble() * 2 - 1) * 0.15 * p
                (0.75 * sin(phase) + air) * (0.3 + 0.7 * p)
            } else {
                // Blow-off valve flutter "stu-tu-tu-tu"
                val dt = t - 0.78
                val flutter = 0.5 + 0.5 * sin(2 * PI * 18.0 * dt)
                val decay = exp(-dt * 5.2)
                val hiss = (Random.nextDouble() * 2 - 1) * 0.6
                val whistle = sin(2 * PI * (2400.0 - 900.0 * dt) * dt) * 0.4
                (hiss + whistle) * flutter * decay
            }
            out[i] = (sample * 23000).toInt().coerceIn(-32767, 32767).toShort()
        }
        return out
    }

    private fun synthExhaustPops(): ShortArray {
        val durationSec = 1.35
        val n = (SAMPLE_RATE * durationSec).toInt()
        val out = ShortArray(n)
        val popTimes = doubleArrayOf(0.32, 0.48, 0.62, 0.74, 0.90, 1.05)
        for (i in 0 until n) {
            val t = i.toDouble() / SAMPLE_RATE
            // Deep exhaust rumble
            val rumble = 0.35 * sin(2 * PI * 72.0 * t) + 0.20 * sin(2 * PI * 108.0 * t)
            var pops = 0.0
            for (pt in popTimes) {
                if (t >= pt) {
                    val dt = t - pt
                    val burst = (Random.nextDouble() * 2 - 1) * 0.9 + sin(2 * PI * 145.0 * dt) * 0.6
                    pops += burst * exp(-dt * 28.0)
                }
            }
            val env = if (t > 1.2) (1.35 - t) / 0.15 else 1.0
            out[i] = ((rumble + pops) * env * 22000).toInt().coerceIn(-32767, 32767).toShort()
        }
        return out
    }

    private fun synthEngineRev(): ShortArray {
        val durationSec = 1.6
        val n = (SAMPLE_RATE * durationSec).toInt()
        val out = ShortArray(n)
        var phase = 0.0
        for (i in 0 until n) {
            val t = i.toDouble() / SAMPLE_RATE
            val throttle = when {
                t < 0.25 -> 0.2
                t < 0.90 -> 0.2 + 0.8 * ((t - 0.25) / 0.65)
                else -> 1.0 - 0.75 * ((t - 0.90) / 0.70)
            }.coerceIn(0.15, 1.0)
            val baseFreq = 62.0 + 185.0 * throttle
            phase += 2 * PI * baseFreq / SAMPLE_RATE
            val v8 = 0.42 * sin(phase) +
                0.30 * sin(phase * 2.0) +
                0.18 * sawPhase(phase * 1.5) +
                0.10 * sin(phase * 4.0)
            val env = when {
                t < 0.06 -> t / 0.06
                t > 1.42 -> (1.6 - t) / 0.18
                else -> 1.0
            }.coerceIn(0.0, 1.0)
            out[i] = (v8 * env * 25000).toInt().coerceIn(-32767, 32767).toShort()
        }
        return out
    }

    private fun synthPassingRocket(): ShortArray {
        val durationSec = 1.6
        val n = (SAMPLE_RATE * durationSec).toInt()
        val out = ShortArray(n)
        var phase = 0.0
        var lowNoise = 0.0
        for (i in 0 until n) {
            val t = i.toDouble() / SAMPLE_RATE
            val passCenter = t - 0.72
            val dopplerGain = exp(-passCenter * passCenter * 5.2)
            val whistleFreq = 460.0 + 620.0 * (1.0 / (1.0 + exp(passCenter * 6.5)))
            phase += 2 * PI * whistleFreq / SAMPLE_RATE
            val white = Random.nextDouble() * 2.0 - 1.0
            lowNoise = lowNoise * 0.82 + white * 0.18
            val crackle = if (t > 0.72 && Random.nextDouble() > 0.92) white * 0.65 else 0.0
            val sample = (lowNoise * 1.15 + 0.35 * sin(phase) + crackle) * dopplerGain
            out[i] = (sample * 27000).toInt().coerceIn(-32767, 32767).toShort()
        }
        return out
    }

    private fun synthJetAirplane(): ShortArray {
        val durationSec = 1.75
        val n = (SAMPLE_RATE * durationSec).toInt()
        val out = ShortArray(n)
        var turbinePhase = 0.0
        var lowRoar = 0.0
        for (i in 0 until n) {
            val t = i.toDouble() / SAMPLE_RATE
            val passPos = t - 0.85
            val jetGain = exp(-passPos * passPos * 3.0)
            val turbineFreq = 1150.0 + 950.0 * (1.0 / (1.0 + exp(passPos * 4.5)))
            turbinePhase += 2 * PI * turbineFreq / SAMPLE_RATE
            val white = Random.nextDouble() * 2.0 - 1.0
            lowRoar = lowRoar * 0.88 + white * 0.12
            val roar = lowRoar * 1.2 + 0.25 * sin(turbinePhase) + 0.12 * sin(turbinePhase * 2.0)
            out[i] = (roar * jetGain * 26500).toInt().coerceIn(-32767, 32767).toShort()
        }
        return out
    }

    private fun saw(freq: Double, t: Double): Double {
        val frac = (freq * t) % 1.0
        return 2.0 * frac - 1.0
    }

    private fun sawPhase(phase: Double): Double {
        val cycles = phase / (2 * PI)
        val frac = cycles - kotlin.math.floor(cycles)
        return 2.0 * frac - 1.0
    }

    private companion object {
        const val SAMPLE_RATE = 22050
    }
}
