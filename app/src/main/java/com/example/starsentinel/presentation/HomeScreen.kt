package com.example.starsentinel.presentation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaRecorder
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.starsentinel.R
import java.io.IOException
import kotlin.math.abs
import kotlin.math.sqrt

@Composable
fun HomeScreen(navController: NavController) {
    // State for fear detection
    var isFearDetected by remember { mutableStateOf(false) }

    // Sensor states
    val (heartRate, setHeartRate) = remember { mutableStateOf(0) }
    val (isSpeaking, setIsSpeaking) = remember { mutableStateOf(false) }

    // Get sensor data
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
        val audioRecorder = AudioRecorder(context)

        val sensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    if (it.sensor.type == Sensor.TYPE_HEART_RATE) {
                        setHeartRate(it.values[0].toInt())
                        // Check for elevated heart rate (threshold can be adjusted)
                        if (it.values[0] > 100) { // Threshold for elevated heart rate
                            isFearDetected = true
                        }
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        // Start listening
        sensorManager.registerListener(
            sensorListener,
            heartRateSensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )

        audioRecorder.startListening { isSpeakingDetected ->
            setIsSpeaking(isSpeakingDetected)
            if (isSpeakingDetected) {
                // Additional fear detection logic when speaking
                if (heartRate > 90) { // Combined threshold
                    isFearDetected = true
                }
            }
        }

        onDispose {
            sensorManager.unregisterListener(sensorListener)
            audioRecorder.stopListening()
        }
    }

    // Main UI
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        // Watch face with fear indicator
        Box(
            modifier = Modifier
                .size(240.dp)
                .clip(CircleShape)
                .background(Color.Black)
                .border(
                    width = 10.dp,
                    color = if (isFearDetected) Color.Red else Color.Green,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Heart rate display
                Text(
                    text = "$heartRate BPM",
                    color = Color.White,
                    fontSize = 18.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Speech detection indicator
                SpeechDetectionIndicator(isDetecting = isSpeaking)

                Spacer(modifier = Modifier.height(16.dp))

                // Fear status
                Text(
                    text = if (isFearDetected) "FEAR DETECTED" else "SAFE",
                    color = if (isFearDetected) Color.Red else Color.Green,
                    fontSize = 16.sp
                )
            }
        }
    }
}

// Audio Recorder class for speech detection
class AudioRecorder(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var isListening = false
    private var callback: ((Boolean) -> Unit)? = null

    fun startListening(onSpeechDetected: (Boolean) -> Unit) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        callback = onSpeechDetected
        isListening = true

        try {
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile("/dev/null") // We don't need to save the recording

                setOnErrorListener { _, _, _ ->
                    stopListening()
                }

                prepare()
                start()

                // Start amplitude monitoring
                Thread {
                    while (isListening) {
                        val amplitude = mediaRecorder?.maxAmplitude ?: 0
                        val isSpeaking = amplitude > 5000 // Threshold for speech detection
                        callback?.invoke(isSpeaking)
                        Thread.sleep(100) // Check every 100ms
                    }
                }.start()
            }
        } catch (e: IOException) {
            stopListening()
        }
    }

    fun stopListening() {
        isListening = false
        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
        } catch (e: Exception) {
            // Ignore
        }
        mediaRecorder = null
    }
}

@Composable
fun SpeechDetectionIndicator(isDetecting: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.mic_icon),
            contentDescription = "Microphone",
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )

        if (isDetecting) {
            // Animated waveform when speaking
            AudioWaveform()
        } else {
            // Flat line when silent
            Box(
                modifier = Modifier
                    .width(24.dp)
                    .height(2.dp)
                    .background(Color.White)
            )
        }
    }
}

@Composable
fun AudioWaveform() {
    // Simple animated waveform
    val infiniteTransition = rememberInfiniteTransition()
    val animValue by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Canvas(modifier = Modifier.size(24.dp, 16.dp)) {
        val height = size.height
        val width = size.width

        // Draw 3 bars with animation
        val barWidth = width / 4
        val maxBarHeight = height

        // Left bar
        val leftHeight = maxBarHeight * (0.3f + animValue * 0.7f)
        drawRect(
            color = Color.White,
            topLeft = Offset(0f, height - leftHeight),
            size = Size(barWidth, leftHeight)
        )

        // Middle bar
        val middleHeight = maxBarHeight * (0.5f + animValue * 0.5f)
        drawRect(
            color = Color.White,
            topLeft = Offset(barWidth * 2, height - middleHeight),
            size = Size(barWidth, middleHeight)
        )

        // Right bar
        val rightHeight = maxBarHeight * (0.2f + animValue * 0.8f)
        drawRect(
            color = Color.White,
            topLeft = Offset(barWidth * 3, height - rightHeight),
            size = Size(barWidth, rightHeight)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    HomeScreen(navController = rememberNavController())
}