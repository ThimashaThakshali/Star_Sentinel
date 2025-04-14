package com.example.starsentinel.sensor

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A class to handle heart rate sensor readings from Samsung Galaxy Watch
 */
class HeartRateSensor(private val context: Context) : SensorEventListener {
    private val TAG = "HeartRateSensor"
    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val heartRateSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)

    private val _heartRate = MutableStateFlow(0)
    val heartRate: StateFlow<Int> = _heartRate.asStateFlow()

    fun startListening(): Boolean {
        // Explicitly check for permission before accessing the sensor
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BODY_SENSORS
            ) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Body sensors permission not granted")
            return false
        }

        try {
            heartRateSensor?.let {
                sensorManager.registerListener(
                    this,
                    it,
                    SensorManager.SENSOR_DELAY_NORMAL
                )
                return true
            }
        } catch (securityException: SecurityException) {
            Log.e(TAG, "Security exception when registering heart rate sensor: ${securityException.message}")
        } catch (exception: Exception) {
            Log.e(TAG, "Exception when registering heart rate sensor: ${exception.message}")
        }

        return false
    }

    fun stopListening() {
        try {
            sensorManager.unregisterListener(this)
        } catch (exception: Exception) {
            Log.e(TAG, "Exception when unregistering heart rate sensor: ${exception.message}")
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_HEART_RATE) {
            val heartRateValue = event.values[0].toInt()
            _heartRate.value = heartRateValue
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // Handle accuracy changes if needed
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