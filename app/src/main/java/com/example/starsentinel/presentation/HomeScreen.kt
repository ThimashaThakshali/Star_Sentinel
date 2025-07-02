package com.example.starsentinel.presentation

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.starsentinel.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin

@SuppressLint("WearRecents")
@Composable
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    // Create and remember sensor instances
    val heartRateSensor = remember { HeartRateSensor(context) }
    val speechDetector = remember { SpeechDetector(context) }

    // Create AlertService instance
    val alertService = remember { AlertService(context) }

    // Permission states
    var showPermissionDialog by remember { mutableStateOf(false) }
    var missingPermissions by remember { mutableStateOf<List<String>>(emptyList()) }

    // Create and remember detector instance
    val fearDetector = remember { FearDetector(context) }

    // Collect all sensor data for fear detection
    val meanRR by heartRateSensor.meanRR.collectAsState(initial = 0f)
    val rmssd by heartRateSensor.rmssd.collectAsState(initial = 0f)
    val sdnn by heartRateSensor.sdnn.collectAsState(initial = 0f)
    val mfccValues by speechDetector.mfccValues.collectAsState(initial = List(13) { 0f })
    val pitchMean by speechDetector.pitchMean.collectAsState(initial = 0f)
    val intensityVar by speechDetector.intensityVar.collectAsState(initial = 0f)

    // Track sensor availability state
    var hasHeartRateSensor by remember { mutableStateOf(false) }

    // State variables
    val isFearDetected by fearDetector.isFearDetected.collectAsState(initial = false)
    val heartRate by heartRateSensor.heartRate.collectAsState(initial = 0)
    val isSpeechDetected by speechDetector.isSpeechDetected.collectAsState(initial = false)

    // Track manual alert button press with animation
    var isButtonPressed by remember { mutableStateOf(false) }

    // Process sensor data for fear detection
    LaunchedEffect(heartRate, isSpeechDetected, mfccValues, pitchMean, intensityVar) {
        // Only process if we have valid heart rate and audio data
        if (heartRate > 0 && meanRR > 0) {
            fearDetector.processData(
                heartRate = heartRate,
                meanRR = meanRR,
                rmssd = rmssd,
                sdnn = sdnn,
                mfccValues = mfccValues,
                pitchMean = pitchMean,
                intensityVar = intensityVar
            )
        }
    }

    // Start/stop sensors based on lifecycle
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {

                    // Check sensor availability first
                    hasHeartRateSensor = heartRateSensor.hasHeartRateSensor()

                    if (heartRateSensor.hasPermission() && hasHeartRateSensor) {
                        val started = heartRateSensor.startListening()
                        if (!started) {
                            Log.w(TAG, "Failed to start heart rate sensor")
                        }
                    }

                    // Check permissions and start sensors
                    val neededPermissions = mutableListOf<String>()

                    if (!heartRateSensor.hasPermission()) {
                        neededPermissions.add("Body Sensors")
                    } else {
                        heartRateSensor.startListening()
                    }

                    if (!speechDetector.hasPermission()) {
                        neededPermissions.add("Microphone")
                    } else {
                        speechDetector.startListening()
                    }

                    if (neededPermissions.isNotEmpty()) {
                        missingPermissions = neededPermissions
                        showPermissionDialog = true
                    }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    heartRateSensor.stopListening()
                    speechDetector.stopListening()
                    fearDetector.reset()
                }
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            heartRateSensor.stopListening()
            speechDetector.stopListening()
            fearDetector.reset()
        }
    }

    // Permission dialog
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = {
                Text(
                    text = "Permission Required",
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = "This app needs ${missingPermissions.joinToString(" and ")} permissions " +
                            "to monitor your health and detect voice commands.",
                    color = Color.White
                )
            },
            confirmButton = {
                Button(onClick = {
                    showPermissionDialog = false
                    // Open app settings
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }) {
                    Text(
                        text = "Open Settings",
                        color = Color.White
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text(text = "Cancel", color = Color.White)
                }
            },
            containerColor = Color.Black,
            textContentColor = Color.White
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        // Main watch face
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(240.dp)
                .clip(CircleShape)
                .background(Color.Black)
                .border(
                    width = 10.dp,
                    color = if (isFearDetected) Color.Red else Color.Green,
                    shape = CircleShape
                )
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Bell Icon (now clickable)
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .clip(CircleShape)
                        .background(if (isButtonPressed) Color.Red else Color.White)
                        .clickable {
                            // Send alert when bell icon is clicked
                            alertService.sendAlerts()

                            // Show visual feedback
                            isButtonPressed = true

                            // Reset visual feedback after short delay
                            coroutineScope.launch {
                                delay(500)
                                isButtonPressed = false
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.bell_icon_white),
                        contentDescription = "Send Emergency Alert",
                        modifier = Modifier.size(55.dp),
                        tint = if (isButtonPressed) Color.White else Color.Black
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Heart rate and speech detection indicators row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.heart_rate_icon),
                            contentDescription = "Heart Rate",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (heartRateSensor.hasPermission()) "$heartRate bpm" else "-- bpm",
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }

                    // Speech detection indicator
                    SpeechDetectionIndicator(
                        isDetecting = speechDetector.hasPermission() && isSpeechDetected
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Settings button
                IconButton(
                    onClick = { navController.navigate("settingsScreen") },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.settings_icon),
                        contentDescription = "Settings",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun SpeechDetectionIndicator(
    isDetecting: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
    ) {
        // Microphone icon
        Icon(
            painter = painterResource(id = R.drawable.mic_icon),
            contentDescription = "Voice Analysis",
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )

        // Audio waveform when speech is detected
        if (isDetecting) {
            AudioWaveform(
                modifier = Modifier
                    .width(24.dp)
                    .height(16.dp)
            )
        } else {
            // Flat line when no speech is detected
            Canvas(
                modifier = Modifier
                    .width(24.dp)
                    .height(16.dp)
            ) {
                val centerY = size.height / 2
                drawLine(
                    color = Color.White,
                    start = Offset(0f, centerY),
                    end = Offset(size.width, centerY),
                    strokeWidth = 2f,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
fun AudioWaveform(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val animationProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "waveform"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerY = height / 2

        // Draw the waveform
        for (i in 0 until width.toInt() step 4) {
            val x = i.toFloat()
            val amplitude = (sin((x / width * 4 + animationProgress) * 2 * Math.PI) * 0.5 + 0.5).toFloat()
            val lineHeight = height * amplitude

            drawLine(
                color = Color.White,
                start = Offset(x, centerY - lineHeight / 2),
                end = Offset(x, centerY + lineHeight / 2),
                strokeWidth = 2f,
                cap = StrokeCap.Round
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    HomeScreen(navController = rememberNavController())
}