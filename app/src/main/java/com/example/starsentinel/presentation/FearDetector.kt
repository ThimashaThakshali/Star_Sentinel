package com.example.starsentinel.detection

import android.content.Context
import android.util.Log
import com.example.starsentinel.alert.AlertService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Class responsible for fear detection by combining sensor data and calling the ML model API
 */
class FearDetector(private val context: Context) {
    private val TAG = "FearDetector"

    // Backend API URL - replace with your ngrok URL
    private val API_URL = "https://f4c5-34-23-184-195.ngrok-free.app/predict"

    // Flow to expose fear detection state
    private val _isFearDetected = MutableStateFlow(false)
    val isFearDetected: StateFlow<Boolean> = _isFearDetected.asStateFlow()

    // Create AlertService instance
    private val alertService = AlertService(context)

    // Track if alert has been sent for current fear state to avoid repeating
    private var alertSentForCurrentState = false

    // Feature buffer for smoothing predictions
    private val predictionBuffer = mutableListOf<Boolean>()
    private val BUFFER_SIZE = 5

    // Heart rate change detection
    private var previousHeartRate = 0
    private val HEART_RATE_INCREASE_THRESHOLD = 15 // Sudden increase of 15 BPM or more
    private val HEART_RATE_MINIMUM = 65 // Only consider increases above this base rate

    // Scream detection thresholds
    private val PITCH_SCREAM_THRESHOLD = 300f // High pitch threshold in Hz
    private val INTENSITY_SCREAM_THRESHOLD = 10f // High intensity threshold in dB
    private val MFCC_DEVIATION_THRESHOLD = 5f // Threshold for MFCC deviation indicating scream

    // Fear state timeout
    private var fearDetectedTimestamp: Long = 0
    private val FEAR_STATE_TIMEOUT_MS = 30000 // 30 seconds of fear state before resetting

    /**
     * Process sensor data to detect fear
     * @param heartRate Current heart rate in BPM
     * @param meanRR Mean R-R interval in ms
     * @param rmssd RMSSD (Root Mean Square of Successive Differences) in ms
     * @param sdnn SDNN (Standard Deviation of Normal-to-Normal intervals) in ms
     * @param mfccValues MFCC audio features
     * @param pitchMean Mean pitch (fundamental frequency) in Hz
     * @param intensityVar Intensity (volume) variance in dB
     */
    fun processData(
        heartRate: Int,
        meanRR: Float,
        rmssd: Float,
        sdnn: Float,
        mfccValues: List<Float>,
        pitchMean: Float,
        intensityVar: Float
    ) {
        // Only process if we have valid heart rate data
        if (heartRate <= 0 || meanRR <= 0) {
            return
        }

        // Check for sudden heart rate increase
        val heartRateIncrease = detectHeartRateChange(heartRate)

        // Check for scream signature in audio
        val screamDetected = detectScream(mfccValues, pitchMean, intensityVar)

        // Create feature vector similar to the model training data
        val hrFeatures = listOf(
            heartRate.toFloat(),
            meanRR,
            rmssd,
            sdnn
        )

        // Audio features - use only the first 13 MFCC
        val audioFeatures = mfccValues.take(13) + listOf(pitchMean, intensityVar)

        // Combine all features into one vector
        val allFeatures = hrFeatures + audioFeatures

        // Make prediction using the model API
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Get prediction from model
                val modelPrediction = predictFear(allFeatures)

                // Combine with rule-based detections
                val fearDetected = modelPrediction || heartRateIncrease || screamDetected

                // Add prediction to buffer
                predictionBuffer.add(fearDetected)
                if (predictionBuffer.size > BUFFER_SIZE) {
                    predictionBuffer.removeAt(0)
                }

                // Use majority voting from the buffer to smooth predictions
                val isFear = predictionBuffer.count { it } > predictionBuffer.size / 2

                // Update fear state and timestamp
                if (isFear && !_isFearDetected.value) {
                    fearDetectedTimestamp = System.currentTimeMillis()
                    _isFearDetected.value = true

                    // Send alert message when fear is first detected
                    if (!alertSentForCurrentState) {
                        alertService.sendAlerts()
                        alertSentForCurrentState = true
                        Log.d(TAG, "Alert sent to emergency contacts")
                    }

                    Log.d(TAG, "Fear detected! HR: $heartRate, Pitch: $pitchMean, Intensity: $intensityVar")
                } else if (_isFearDetected.value) {
                    // Check if fear state should timeout
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - fearDetectedTimestamp > FEAR_STATE_TIMEOUT_MS && !isFear) {
                        _isFearDetected.value = false
                        alertSentForCurrentState = false // Reset the alert sent flag
                        Log.d(TAG, "Fear state reset after timeout")
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error in fear detection: ${e.message}")
            }
        }
    }

    /**
     * Detect sudden increase in heart rate
     * @param currentHeartRate Current heart rate in BPM
     * @return True if a sudden increase is detected
     */
    private fun detectHeartRateChange(currentHeartRate: Int): Boolean {
        val result = if (previousHeartRate > HEART_RATE_MINIMUM &&
            currentHeartRate > previousHeartRate &&
            currentHeartRate - previousHeartRate >= HEART_RATE_INCREASE_THRESHOLD) {
            Log.d(TAG, "Sudden heart rate increase detected: $previousHeartRate -> $currentHeartRate")
            true
        } else {
            false
        }

        previousHeartRate = currentHeartRate
        return result
    }

    /**
     * Detect scream signature in audio features
     * @param mfccValues MFCC audio features
     * @param pitchMean Mean pitch in Hz
     * @param intensityVar Intensity variance in dB
     * @return True if a scream is detected
     */
    private fun detectScream(mfccValues: List<Float>, pitchMean: Float, intensityVar: Float): Boolean {
        // Check for high pitch + high intensity (scream signature)
        val isPitchHigh = pitchMean > PITCH_SCREAM_THRESHOLD
        val isIntensityHigh = intensityVar > INTENSITY_SCREAM_THRESHOLD

        // Check for extreme MFCC deviation (another indicator of scream)
        var mfccDeviation = false
        if (mfccValues.isNotEmpty()) {
            // Calculate deviation of first MFCC coefficient (related to energy)
            mfccDeviation = Math.abs(mfccValues[0]) > MFCC_DEVIATION_THRESHOLD
        }

        val screamDetected = (isPitchHigh && isIntensityHigh) || (isIntensityHigh && mfccDeviation)

        if (screamDetected) {
            Log.d(TAG, "Scream detected! Pitch: $pitchMean, Intensity: $intensityVar")
        }

        return screamDetected
    }

    /**
     * Send features to the backend model and get fear prediction
     * @param features List of features to send to the model
     * @return True if fear is detected, false otherwise
     */
    private fun predictFear(features: List<Float>): Boolean {
        try {
            val url = URL(API_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")

            // Create JSON payload
            val jsonPayload = JSONObject()
            jsonPayload.put("input", JSONObject.wrap(features))

            // Send request
            BufferedOutputStream(connection.outputStream).use { os ->
                os.write(jsonPayload.toString().toByteArray())
                os.flush()
            }

            // Read response
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                    val response = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }

                    // Parse response
                    val jsonResponse = JSONObject(response.toString())
                    val prediction = jsonResponse.getInt("prediction")
                    return prediction == 1
                }
            } else {
                Log.e(TAG, "HTTP error: $responseCode")
                return false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calling prediction API: ${e.message}")
            return false
        }
    }

    /**
     * Reset the detector state
     */
    fun reset() {
        predictionBuffer.clear()
        _isFearDetected.value = false
        alertSentForCurrentState = false
        previousHeartRate = 0
        fearDetectedTimestamp = 0
    }
}