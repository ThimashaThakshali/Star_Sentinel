package com.example.starsentinel.presentation

import android.content.Context
import android.util.Log
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
import kotlin.math.abs


// The Class which for fear detection by combining sensor data and calling the ML model API

class FearDetector(private val context: Context) {
    private val tag = "FearDetector "

    // Backend API URL -  ngrok URL
    private val apiUrl = "https://cd68-34-16-204-224.ngrok-free.app/predict"

    // Flow to expose fear detection state
    private val _isFearDetected = MutableStateFlow(false)
    val isFearDetected: StateFlow<Boolean> = _isFearDetected.asStateFlow()

    // Create AlertService instance
    private val alertService = AlertService(context)

    // Track if alert has been sent for current fear state to avoid repeating
    private var alertSentForCurrentState = false

    // Feature buffer for smoothing predictions
    private val predictionBuffer = mutableListOf<Boolean>()
    private val bufferSize = 5

    // Heart rate: How does it increase during stress? (2023). Available at: https://www.medicalnewstoday.com/articles/average-heart-rate-when-stressed (Accessed: 3 July 2025).
    // Heart rate change detection
    private var previousHeartRate = 0
    private val heartRateIncreaseThreshold = 25 // Sudden increase of 15 BPM or more
    private val heartRateMinimum = 65 // Only consider increases above this base rate

    //Press, C. (no date) The emerging science of human screams. Available at: https://phys.org/news/2015-07-occupy-privileged-acoustic-niche-biological.html (Accessed: 3 July 2025).
    // Scream detection thresholds
    private var previousIntensity: Float = 0f
    private var previousIntensityTime: Long = 0
    private val intensityChangeThreshold = 1f // 1 dB change threshold
    private val maxTimeDiff = 1000L // 1 second window
    private val pitchScreamThreshold = 400f // High pitch threshold in Hz
    private val intensityScreamThreshold = 15f  // High intensity threshold in dB
    private val mfccDeviationThreshold = 5f // Threshold for MFCC deviation indicating scream

    // Fear state timeout
    private var fearDetectedTimestamp: Long = 0
    private val fearStateTimeout = 30000 // 30 seconds of fear state before resetting

    /*
        Process sensor data to detect fear
        heartRate- Current heart rate in BPM
        meanRR - Mean R-R interval in ms
        rmssd - RMSSD (Root Mean Square of Successive Differences) in ms
        sdnn - SDNN (Standard Deviation of Normal-to-Normal intervals) in ms
        mfccValues - MFCC audio features
        pitchMean - Mean pitch (fundamental frequency) in Hz
        intensityVar - Intensity (volume) variance in dB
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

        val intensitySpike = detectSuddenIntensityChange(intensityVar)


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

                if (modelPrediction) {
                    Log.d(tag, "Model prediction: Fear detected")
                } else {
                    Log.d(tag, "Model prediction: No fear detected")
                }
                if (heartRateIncrease) {
                    Log.d(tag, "Heart rate increase detected: $heartRate")
                } else {
                    Log.d(tag, "No significant heart rate change")
                }
                if (screamDetected) {
                    Log.d(tag, "Scream detected! Pitch: $pitchMean, Intensity: $intensityVar")
                } else {
                    Log.d(tag, "No scream detected")
                }
                if (intensitySpike) {
                    Log.d(tag, "Sudden intensity spike detected: $intensityVar dB")
                } else {
                    Log.d(tag, "No sudden intensity change detected")
                }

                // Combine with rule-based detections
                val fearDetected = modelPrediction || heartRateIncrease || screamDetected || intensitySpike

                // Add prediction to buffer
                predictionBuffer.add(fearDetected)
                if (predictionBuffer.size > bufferSize) {
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
                        Log.d(tag, "Alert sent to emergency contacts")
                    }

                    Log.d(tag, "Fear detected! HR: $heartRate, Pitch: $pitchMean, Intensity: $intensityVar")
                } else if (_isFearDetected.value) {
                    // Check if fear state should timeout
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - fearDetectedTimestamp > fearStateTimeout && !isFear) {
                        _isFearDetected.value = false
                        alertSentForCurrentState = false // Reset the alert sent flag
                        Log.d(tag, "Fear state reset after timeout")
                    }
                }

            } catch (e: Exception) {
                Log.e(tag, "Error in fear detection: ${e.message}")
            }
        }
    }

    /* Detect sudden increase in heart rate
        currentHeartRate - Current heart rate in BPM
        True if a sudden increase is detected
     */
    private fun detectHeartRateChange(currentHeartRate: Int): Boolean {
        val result = if ( previousHeartRate != 0 && previousHeartRate > heartRateMinimum &&
            currentHeartRate > previousHeartRate &&
            currentHeartRate - previousHeartRate >= heartRateIncreaseThreshold) {
            Log.d(tag, "Sudden heart rate increase detected: $previousHeartRate -> $currentHeartRate")
            true
        } else {
            false
        }

        previousHeartRate = currentHeartRate
        return result
    }

    /* Detect scream signature in audio features
        mfccValues - MFCC audio features
        pitchMean -  Mean pitch in Hz
        intensityVar -  Intensity variance in dB
        True if a scream is detected
     */
    private fun detectScream(mfccValues: List<Float>, pitchMean: Float, intensityVar: Float): Boolean {
        // Check for high pitch + high intensity (scream signature)
        val isPitchHigh = pitchMean > pitchScreamThreshold
        val isIntensityHigh = intensityVar > intensityScreamThreshold

        // Check for extreme MFCC deviation (another indicator of scream)
        var mfccDeviation = false
        if (mfccValues.isNotEmpty()) {
            // Calculate deviation of first MFCC coefficient (related to energy)
            mfccDeviation = abs(mfccValues[0]) > mfccDeviationThreshold
        }

        val screamDetected = (isPitchHigh && isIntensityHigh) || (isIntensityHigh && mfccDeviation)

        return screamDetected
    }

    /* Send features to the backend model and get fear prediction
      features List of features to send to the model
      True if fear is detected, false otherwise
     */
    private fun predictFear(features: List<Float>): Boolean {
        try {
            val url = URL(apiUrl)
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
                Log.e(tag, "HTTP error: $responseCode")
                return false
            }
        } catch (e: Exception) {
            Log.e(tag, "Error calling prediction API: ${e.message}")
            return false
        }
    }

    /* Detects sudden intensity changes (1dB+ within 1 second)
       Similar to heart rate sudden change detection
     */
    private fun detectSuddenIntensityChange(currentIntensity: Float): Boolean {
        val currentTime = System.currentTimeMillis()

        // Only check if we have a previous reading and it's within 1 second
        if (previousIntensity != 0f && (currentTime - previousIntensityTime) <= maxTimeDiff) {

            val intensityDiff = abs(currentIntensity - previousIntensity)
            if (intensityDiff >= intensityChangeThreshold) {
                Log.d(tag, "Sudden intensity change: ${previousIntensity}dB -> ${currentIntensity}dB")
                return true
            }
        }

        // Update previous values
        previousIntensity = currentIntensity
        previousIntensityTime = currentTime
        return false
    }

    // Reset the detector state
    fun reset() {
        predictionBuffer.clear()
        _isFearDetected.value = false
        alertSentForCurrentState = false
        previousHeartRate = 0
        fearDetectedTimestamp = 0
    }
}