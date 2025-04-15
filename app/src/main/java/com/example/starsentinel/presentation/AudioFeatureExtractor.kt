package com.example.starsentinel.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * Class to extract audio features from raw audio data
 */
class AudioFeatureExtractor {
    private val TAG = "AudioFeatureExtractor"

    // MFCC features
    private val _mfccValues = MutableStateFlow(List(13) { 0f })
    val mfccValues: StateFlow<List<Float>> = _mfccValues.asStateFlow()

    // Pitch (fundamental frequency)
    private val _pitchMean = MutableStateFlow(0f)
    val pitchMean: StateFlow<Float> = _pitchMean.asStateFlow()

    // Intensity (volume) variance
    private val _intensityVar = MutableStateFlow(0f)
    val intensityVar: StateFlow<Float> = _intensityVar.asStateFlow()

    // Buffer for intensity values to calculate variance
    private val intensityBuffer = mutableListOf<Float>()
    private val MAX_BUFFER_SIZE = 50

    /**
     * Process a buffer of audio samples
     * @param buffer The audio sample buffer
     * @param sampleRate The audio sample rate in Hz
     */
    fun processAudioBuffer(buffer: ShortArray, sampleRate: Int) {
        try {
            // Calculate signal intensity (RMS)
            val rms = calculateRMS(buffer)

            // Add to intensity buffer
            intensityBuffer.add(rms)
            if (intensityBuffer.size > MAX_BUFFER_SIZE) {
                intensityBuffer.removeAt(0)
            }

            // Calculate intensity variance
            calculateIntensityVariance()

            // Extract pitch (simplified zero-crossing method)
            val pitch = estimatePitch(buffer, sampleRate)
            if (pitch > 0) {
                _pitchMean.value = pitch
            }

            // Extract MFCC (simplified placeholder implementation)
            extractMFCC(buffer, sampleRate)

        } catch (e: Exception) {
            Log.e(TAG, "Error processing audio buffer: ${e.message}")
        }
    }

    /**
     * Calculate RMS (Root Mean Square) of audio buffer
     */
    private fun calculateRMS(buffer: ShortArray): Float {
        var sum = 0.0
        for (sample in buffer) {
            sum += sample.toDouble() * sample.toDouble()
        }
        val mean = sum / buffer.size
        return sqrt(mean).toFloat()
    }

    /**
     * Calculate intensity variance from buffer of RMS values
     */
    private fun calculateIntensityVariance() {
        if (intensityBuffer.size < 2) return

        val mean = intensityBuffer.average().toFloat()
        var sumSquaredDiff = 0f

        for (intensity in intensityBuffer) {
            val diff = intensity - mean
            sumSquaredDiff += diff * diff
        }

        val variance = sumSquaredDiff / intensityBuffer.size

        // Convert to decibels
        _intensityVar.value = 20 * log10(variance + 1) // +1 to avoid log(0)
    }

    /**
     * Estimate pitch using zero-crossing rate
     * This is a simplified method - actual pitch detection would use more sophisticated algorithms
     */
    private fun estimatePitch(buffer: ShortArray, sampleRate: Int): Float {
        var zeroCrossings = 0
        for (i in 1 until buffer.size) {
            if ((buffer[i] > 0 && buffer[i-1] <= 0) ||
                (buffer[i] <= 0 && buffer[i-1] > 0)) {
                zeroCrossings++
            }
        }

        // Compute frequency from zero-crossing rate
        val duration = buffer.size.toFloat() / sampleRate
        val frequency = zeroCrossings / (2 * duration)

        // Human voice range filter (80-255 Hz for adults)
        return if (frequency in 80f..400f) frequency else 0f
    }

    /**
     * Extract MFCC features (simplified)
     * Note: Real MFCC extraction is complex and typically uses libraries like TarsosDSP
     * This is a placeholder implementation
     */
    private fun extractMFCC(buffer: ShortArray, sampleRate: Int) {
        // This is a simplified placeholder - doesn't actually compute real MFCCs
        // In a real implementation, you would:
        // 1. Apply pre-emphasis filter
        // 2. Frame the signal
        // 3. Apply window function (e.g., Hamming)
        // 4. Compute FFT
        // 5. Apply Mel filterbank
        // 6. Take log
        // 7. Apply DCT

        // Generate placeholder MFCC values that respond to audio energy
        val energy = calculateRMS(buffer)
        val normalizedEnergy = energy / 10000f // Normalize by typical maximum

        // Create simple placeholder MFCC values that vary with signal energy
        val newMfccs = List(13) { i ->
            val baseValue = when (i) {
                0 -> normalizedEnergy * 10 // First coefficient relates to energy
                else -> (normalizedEnergy * 5 * (13 - i) / 13f) *
                        if (i % 2 == 0) 1 else -1 // Alternating signs with decreasing magnitude
            }
            baseValue
        }

        _mfccValues.value = newMfccs
    }

    /**
     * Reset the extractor state
     */
    fun reset() {
        intensityBuffer.clear()
        _pitchMean.value = 0f
        _intensityVar.value = 0f
        _mfccValues.value = List(13) { 0f }
    }
}