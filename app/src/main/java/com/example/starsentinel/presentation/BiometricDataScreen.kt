package com.example.starsentinel.presentation

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.starsentinel.R
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import android.content.Intent
import android.net.Uri
import android.provider.Settings

@SuppressLint("WearRecents")
@Composable
fun BiometricDataScreen(navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    // Create and remember sensor instances
    val heartRateSensor = remember { HeartRateSensor(context) }
    val speechDetector = remember { SpeechDetector(context) }

    // Permission states
    var showPermissionDialog by remember { mutableStateOf(false) }
    var missingPermissions by remember { mutableStateOf<List<String>>(emptyList()) }

    // Heart rate state
    val heartRate by heartRateSensor.heartRate.collectAsState(initial = 0)
    val meanRR by heartRateSensor.meanRR.collectAsState(initial = 0f)
    val rmssd by heartRateSensor.rmssd.collectAsState(initial = 0f)
    val sdnn by heartRateSensor.sdnn.collectAsState(initial = 0f)

    // Audio state
    val isSpeechDetected by speechDetector.isSpeechDetected.collectAsState(initial = false)
    val mfccValues by speechDetector.mfccValues.collectAsState(initial = List(13) { 0f })
    val pitchMean by speechDetector.pitchMean.collectAsState(initial = 0f)
    val intensityVar by speechDetector.intensityVar.collectAsState(initial = 0f)

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
                        "to monitor your biometric data.")
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
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Text(
                text = "Biometric Data",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            // Heart Rate Section
            BiometricSection(
                title = "Heart Rate Metrics",
                iconRes = R.drawable.heart_rate_icon
            ) {
                BiometricDataRow("Heart Rate", if (heartRate > 0) "$heartRate bpm" else "-- bpm")
                BiometricDataRow("Mean RR Interval", if (meanRR > 0) "${meanRR.toInt()} ms" else "-- ms")
                BiometricDataRow("RMSSD", if (rmssd > 0) "${rmssd.toInt()} ms" else "-- ms")
                BiometricDataRow("SDNN", if (sdnn > 0) "${sdnn.toInt()} ms" else "-- ms")

                if (!heartRateSensor.hasPermission()) {
                    Text(
                        text = "Body sensors permission required for heart rate monitoring",
                        fontSize = 12.sp,
                        color = Color.Red,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                } else if (heartRate == 0) {
                    Text(
                        text = "Waiting for heart rate data...",
                        fontSize = 12.sp,
                        color = Color.Yellow,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Audio Features Section
            BiometricSection(
                title = "Audio Analysis",
                iconRes = R.drawable.mic_icon
            ) {
                BiometricDataRow("Speech Detected", if (isSpeechDetected) "Yes" else "No")
                BiometricDataRow("Pitch Mean", if (pitchMean > 0) "${pitchMean.toInt()} Hz" else "-- Hz")
                BiometricDataRow("Intensity Variance", if (intensityVar > 0) "${"%.2f".format(intensityVar)} dB" else "-- dB")

                Text(
                    text = "MFCC Features",
                    fontSize = 14.sp,
                    color = Color.White,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                mfccValues.forEachIndexed { index, value ->
                    if (index < 5) {  // Show only the first 5 coefficients to save space
                        BiometricDataRow("MFCC $index", if (value != 0f) "%.2f".format(value) else "--")
                    }
                }

                if (!speechDetector.hasPermission()) {
                    Text(
                        text = "Microphone permission required for audio analysis",
                        fontSize = 12.sp,
                        color = Color.Red,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                } else if (!isSpeechDetected) {
                    Text(
                        text = "Start speaking to analyze audio features",
                        fontSize = 12.sp,
                        color = Color.Yellow,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { navController.popBackStack() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2196F3)
                ),
                modifier = Modifier.padding(8.dp)
            ) {
                Text("Back")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun BiometricSection(
    title: String,
    iconRes: Int,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF242424))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = title,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(bottom = 12.dp),
                thickness = 1.dp,
                color = Color.DarkGray
            )

            content()
        }
    }
}

@Composable
fun BiometricDataRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color.LightGray
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = Color.White,
            fontWeight = FontWeight.Medium
        )
    }
}