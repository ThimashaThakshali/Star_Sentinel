package com.example.starsentinel.presentation

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.starsentinel.R
import com.example.starsentinel.location.LocationService
import com.example.starsentinel.alert.AlertService

@SuppressLint("DefaultLocale")
@Composable
fun LocationScreen(navController: NavController) {
    val context = LocalContext.current
    val locationService = remember { LocationService(context) }
    val currentLocation by locationService.currentLocation.collectAsState()
    val locationUrl by locationService.locationUrl.collectAsState()
    val currentAddress by locationService.currentAddress.collectAsState()

    // Start location updates
    LaunchedEffect(key1 = Unit) {
        locationService.startLocationUpdates()
    }

    // Clean up location updates when leaving screen
    DisposableEffect(key1 = Unit) {
        onDispose {
            locationService.stopLocationUpdates()
        }
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
            // Title
            Text(
                text = "Current Location",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFBDC1C6),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Location icon
            Icon(
                painter = painterResource(id = R.drawable.safe_zone_icon),
                contentDescription = "Location",
                tint = Color.White,
                modifier = Modifier
                    .size(64.dp)
                    .padding(8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Location coordinates
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                color = Color(0xFF242424),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Coordinates:",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (currentLocation != null) {
                            String.format("%.6f, %.6f",
                                currentLocation!!.latitude,
                                currentLocation!!.longitude)
                        } else "Waiting for location...",
                        color = Color.White,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Address display
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                color = Color(0xFF242424),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Address:",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = currentAddress,
                        color = Color.White,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Display status
            Text(
                text = if (currentLocation != null)
                    "Location will be shared with emergency contacts"
                else "Acquiring location...",
                color = Color.Gray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Test button
            Button(
                onClick = {
                    val alertService = AlertService(context)
                    alertService.sendAlerts()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2196F3)
                )
            ) {
                Text("Test Alert")
            }
        }

        /*
         Box(
             modifier = Modifier
                 .align(Alignment.BottomCenter)
                 .padding(bottom = 16.dp)
         ) {
             Button(
                 onClick = { navController.popBackStack() },
                 colors = ButtonDefaults.buttonColors(
                     containerColor = Color.DarkGray
                 )
             ) {
                 Text("Back")
             }
         } */
    }
}