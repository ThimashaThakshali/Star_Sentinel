package com.example.starsentinel.presentation

import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.starsentinel.R
import com.example.starsentinel.sensor.HeartRateSensor
import com.example.starsentinel.audio.SpeechDetector
import kotlin.math.sin

@Composable
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Create and remember sensor instances
    val heartRateSensor = remember { HeartRateSensor(context) }
    val speechDetector = remember { SpeechDetector(context) }

    // Permission states
    var showPermissionDialog by remember { mutableStateOf(false) }
    var missingPermissions by remember { mutableStateOf<List<String>>(emptyList()) }

    // State variables
    var isFearDetected by remember { mutableStateOf(true) }
    val heartRate by heartRateSensor.heartRate.collectAsState(initial = 0)
    val isSpeechDetected by speechDetector.isSpeechDetected.collectAsState(initial = false)

    // Start/stop sensors based on lifecycle
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
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
                }
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            heartRateSensor.stopListening()
            speechDetector.stopListening()
        }
    }

    // Permission dialog
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Permission Required") },
            text = {
                Text("This app needs ${missingPermissions.joinToString(" and ")} permissions " +
                        "to monitor your health and detect voice commands.")
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
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        // Main watch face - same UI, but now with real data
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

                // Bell Icon
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.bell_icon_white),
                        contentDescription = "Notification Bell",
                        modifier = Modifier.size(55.dp)
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

// The rest of the code remains the same
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