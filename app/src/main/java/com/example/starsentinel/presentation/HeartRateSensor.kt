package com.example.starsentinel.presentation

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.StateFlow

/* The class which  handles the  heart rate sensor readings from the watch
    and calculates heart rate variability metrics
 */

class HeartRateSensor(private val context: Context) : SensorEventListener {
    private val tag = "HeartRateSensor"
    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val heartRateSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)

    // Create heart rate processor for HRV calculations
    private val heartRateProcessor = HeartRateProcessor()

    // Expose StateFlows from the processor
    val heartRate: StateFlow<Int> = heartRateProcessor.heartRate
    val meanRR: StateFlow<Float> = heartRateProcessor.meanRR
    val rmssd: StateFlow<Float> = heartRateProcessor.rmssd
    val sdnn: StateFlow<Float> = heartRateProcessor.sdnn

    // Last heart beat timestamp
    private var lastTimestamp: Long = 0

    @SuppressLint("MissingPermission")
    fun startListening(): Boolean {
        // First check if we have permission
        if (!hasPermission()) {
            Log.w(tag, "No body sensors permission")
            return false
        }

        // Check if sensor exists on this device
        if (!hasHeartRateSensor()) {
            Log.w(tag, "No heart rate sensor found")
            return false
        }

        try {
            // Register listener with fastest rate for better accuracy
            sensorManager.registerListener(
                this,
                heartRateSensor,
                SensorManager.SENSOR_DELAY_FASTEST
            )
            Log.d(tag, "Started listening to heart rate sensor")
            return true
        } catch (e: Exception) {
            Log.e(tag, "Error starting sensor: ${e.message}")
            return false
        }
    }

    fun stopListening() {
        try {
            sensorManager.unregisterListener(this)
            heartRateProcessor.reset()
        } catch (exception: Exception) {
            Log.e(tag, "Exception when unregistering heart rate sensor: ${exception.message}")
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_HEART_RATE) {
            val heartRateValue = event.values[0].toInt()
            val currentTimestamp = System.currentTimeMillis()

            // Process the heart rate value
            heartRateProcessor.processHeartRate(heartRateValue)

            // If this appears to be a new beat (value within normal range)
            if (heartRateValue in 30..220) {
                // Simple beat detection: if time since last beat is reasonable
                // (at least 300ms, which corresponds to 200 BPM max)
                if (lastTimestamp == 0L || currentTimestamp - lastTimestamp >= 300) {
                    heartRateProcessor.processBeat(currentTimestamp)
                    lastTimestamp = currentTimestamp
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // Handle accuracy changes if needed
        when (accuracy) {
            SensorManager.SENSOR_STATUS_ACCURACY_LOW ->
                Log.w(tag, "Heart rate sensor accuracy LOW")
            SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM ->
                Log.d(tag, "Heart rate sensor accuracy MEDIUM")
            SensorManager.SENSOR_STATUS_ACCURACY_HIGH ->
                Log.d(tag, "Heart rate sensor accuracy HIGH")
        }
    }

    fun hasHeartRateSensor(): Boolean {
        return heartRateSensor != null
    }

    fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BODY_SENSORS
        ) == PackageManager.PERMISSION_GRANTED
    }
}