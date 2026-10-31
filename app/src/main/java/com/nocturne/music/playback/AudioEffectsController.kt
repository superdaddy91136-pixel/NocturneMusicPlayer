package com.nocturne.music.playback

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import com.nocturne.music.model.EqualizerBand
import com.nocturne.music.model.EqualizerState
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class AudioEffectsController @Inject constructor() {

    private var currentSessionId: Int = 0
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null

    private var lastAppliedState: EqualizerState = EqualizerState()
    private var normalizationEnabled: Boolean = false
    private var normalizationPreampDb: Float = 0f
    private var trackReplayGainDb: Float = 0f

    @Synchronized
    fun bindToAudioSession(audioSessionId: Int, initialState: EqualizerState): EqualizerState {
        if (audioSessionId <= 0) return initialState
        if (currentSessionId == audioSessionId && equalizer != null) {
            applyEqualizerState(initialState)
            return readHardwareState(initialState)
        }

        releaseEffects()
        currentSessionId = audioSessionId

        return try {
            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = initialState.enabled
            }
            runCatching {
                bassBoost = BassBoost(0, audioSessionId).apply {
                    enabled = initialState.enabled && initialState.bassBoostStrength > 0
                }
            }
            runCatching {
                virtualizer = Virtualizer(0, audioSessionId).apply {
                    enabled = initialState.enabled && initialState.virtualizerStrength > 0
                }
            }
            runCatching {
                loudnessEnhancer = LoudnessEnhancer(audioSessionId)
            }
            applyEqualizerState(initialState)
            updateNormalizationGain()
            readHardwareState(initialState)
        } catch (_: Throwable) {
            initialState.copy(hardwareAvailable = false)
        }
    }

    @Synchronized
    fun applyEqualizerState(state: EqualizerState) {
        lastAppliedState = state
        try {
            val eq = equalizer
            if (eq != null) {
                eq.enabled = state.enabled
                val numBands = eq.numberOfBands.toInt()
                val range = eq.bandLevelRange
                val minLevel = range.getOrElse(0) { -1500 }.toInt()
                val maxLevel = range.getOrElse(1) { 1500 }.toInt()

                for (i in 0 until minOf(numBands, state.bands.size)) {
                    val targetMb = state.bands[i].levelMillibels.coerceIn(minLevel, maxLevel).toShort()
                    eq.setBandLevel(i.toShort(), targetMb)
                }
            }

            bassBoost?.let { bb ->
                bb.enabled = state.enabled && state.bassBoostStrength > 0
                if (bb.strengthSupported) {
                    bb.setStrength(state.bassBoostStrength.coerceIn(0, 1000).toShort())
                }
            }

            virtualizer?.let { virt ->
                virt.enabled = state.enabled && state.virtualizerStrength > 0
                if (virt.strengthSupported) {
                    virt.setStrength(state.virtualizerStrength.coerceIn(0, 1000).toShort())
                }
            }

            updateNormalizationGain()
        } catch (_: Throwable) {
        }
    }

    @Synchronized
    fun configureVolumeNormalization(
        enabled: Boolean,
        preampDb: Float,
        currentTrackReplayGainDb: Float
    ) {
        normalizationEnabled = enabled
        normalizationPreampDb = preampDb
        trackReplayGainDb = currentTrackReplayGainDb
        updateNormalizationGain()
    }

    private fun updateNormalizationGain() {
        try {
            val enhancer = loudnessEnhancer ?: return
            val eqGainMb = if (lastAppliedState.enabled) lastAppliedState.loudnessGainMb else 0
            val normGainMb = if (normalizationEnabled) {
                ((normalizationPreampDb + trackReplayGainDb) * 100f).roundToInt()
            } else {
                0
            }
            val totalGainMb = (eqGainMb + normGainMb).coerceIn(-1200, 1800)
            enhancer.enabled = totalGainMb != 0
            if (enhancer.enabled) {
                enhancer.setTargetGain(totalGainMb)
            }
        } catch (_: Throwable) {
        }
    }

    private fun readHardwareState(desired: EqualizerState): EqualizerState {
        val eq = equalizer ?: return desired
        return try {
            val numBands = eq.numberOfBands.toInt().coerceAtLeast(1)
            val range = eq.bandLevelRange
            val minLevel = range.getOrElse(0) { -1500 }.toInt()
            val maxLevel = range.getOrElse(1) { 1500 }.toInt()
            val bands = (0 until numBands).map { idx ->
                val centerFreqMilliHz = eq.getCenterFreq(idx.toShort())
                val centerHz = (centerFreqMilliHz / 1000).coerceAtLeast(20)
                val level = desired.bands.getOrNull(idx)?.levelMillibels
                    ?: eq.getBandLevel(idx.toShort()).toInt()
                EqualizerBand(
                    index = idx,
                    centerFreqHz = centerHz,
                    levelMillibels = level.coerceIn(minLevel, maxLevel),
                    minLevelMillibels = minLevel,
                    maxLevelMillibels = maxLevel
                )
            }
            desired.copy(bands = bands, hardwareAvailable = true)
        } catch (_: Throwable) {
            desired
        }
    }

    @Synchronized
    fun releaseEffects() {
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        runCatching { virtualizer?.release() }
        runCatching { loudnessEnhancer?.release() }
        equalizer = null
        bassBoost = null
        virtualizer = null
        loudnessEnhancer = null
        currentSessionId = 0
    }
}
