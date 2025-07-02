package com.example.starsentinel.presentation

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.pow
import kotlin.math.sqrt

// The Class which calculates the Heart Rate Variability (HRV) metrics from heart beat timestamps

class HeartRateProcessor {
    private val tag = "HeartRateProcessor"

    // Window size for RR intervals (beats)
    private val windowsize = 60

    // RR intervals list (in milliseconds)
    private val rrIntervals = mutableListOf<Long>()
    private val _heartRate = MutableStateFlow(0)
    private val _meanRR = MutableStateFlow(0f)
    private val _rmssd = MutableStateFlow(0f)
    private val _sdnn = MutableStateFlow(0f)

    val heartRate: StateFlow<Int> = _heartRate.asStateFlow()
    val meanRR: StateFlow<Float> = _meanRR.asStateFlow()
    val rmssd: StateFlow<Float> = _rmssd.asStateFlow()
    val sdnn: StateFlow<Float> = _sdnn.asStateFlow()

    // Last timestamp in milliseconds
    private var lastBeatTimestamp: Long = 0

    /**
     * Process a new heart beat
     * @param timestamp The timestamp in milliseconds when the beat occurred
     */
    fun processBeat(timestamp: Long) {
        if (lastBeatTimestamp > 0) {
            val rrInterval = timestamp - lastBeatTimestamp

            // Filter out physiologically impossible RR intervals
            if (rrInterval in 300..1500) {
                rrIntervals.add(rrInterval)

                // Keep only the last WINDOW_SIZE intervals
                if (rrIntervals.size > windowsize) {
                    rrIntervals.removeAt(0)
                }

                // Calculate metrics if we have enough samples
                if (rrIntervals.size >= 3) {
                    calculateMetrics()
                }
            }
        }

        lastBeatTimestamp = timestamp
    }

    /**
     * Process a direct heart rate reading in BPM
     */
    fun processHeartRate(bpm: Int) {
        _heartRate.value = bpm

        // Approximate RR interval from BPM (not as accurate as actual beat timestamps)
        if (bpm > 0) {
            val approximateRR = 60000f / bpm

            // If we don't have real RR intervals yet, use this approximation
            if (rrIntervals.isEmpty()) {
                _meanRR.value = approximateRR
            }
        }
    }

    /**
     * Calculate HRV metrics from the collected RR intervals
     */
    private fun calculateMetrics() {
        try {
            // Mean RR interval (ms)
            val mean = rrIntervals.average().toFloat()
            _meanRR.value = mean

            // Instantaneous heart rate based on most recent RR interval
            if (rrIntervals.isNotEmpty()) {
                val instantHR = (60000f / rrIntervals.last()).toInt()
                _heartRate.value = instantHR
            }

            // SDNN (ms) - Standard deviation of NN intervals
            var sumSquaredDiff = 0.0
            for (rr in rrIntervals) {
                sumSquaredDiff += (rr - mean).toDouble().pow(2)
            }
            val sdnn = sqrt(sumSquaredDiff / rrIntervals.size)
            _sdnn.value = sdnn.toFloat()

            // RMSSD (ms) - Root mean square of successive differences
            if (rrIntervals.size > 1) {
                var sumSquaredSuccessiveDiff = 0.0
                for (i in 1 until rrIntervals.size) {
                    val diff = (rrIntervals[i] - rrIntervals[i-1]).toDouble()
                    sumSquaredSuccessiveDiff += diff.pow(2)
                }
                val rmssd = sqrt(sumSquaredSuccessiveDiff / (rrIntervals.size - 1))
                _rmssd.value = rmssd.toFloat()
            }
        } catch (e: Exception) {
            Log.e(tag, "Error calculating HRV metrics: ${e.message}")
        }
    }

    /**
     * Reset the processor state
     */
    fun reset() {
        rrIntervals.clear()
        lastBeatTimestamp = 0
        _heartRate.value = 0
        _meanRR.value = 0f
        _rmssd.value = 0f
        _sdnn.value = 0f
    }
}