package com.example.starsentinel.presentation

import android.Manifest
import android.widget.Toast
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import java.util.*

@Composable
fun SetGeofenceScreen(navController: NavController) {
    val context = LocalContext.current
    val geofenceStorage = remember { GeofenceStorage(context) }

    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var radius by remember { mutableStateOf(100f) }
    var location by remember { mutableStateOf<LatLng?>(null) }
    var geofenceId by remember { mutableStateOf(UUID.randomUUID().toString()) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(37.42, -122.08), 10f)
    }

    LaunchedEffect(Unit) {
        if (hasLocationPermission(context)) {
            try {
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    fusedLocationClient.lastLocation.addOnSuccessListener { lastLocation ->
                        lastLocation?.let {
                            val currentLatLng = LatLng(it.latitude, it.longitude)
                            location = currentLatLng
                            cameraPositionState.position = CameraPosition.fromLatLngZoom(currentLatLng, 15f)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val uiSettings by remember {
        mutableStateOf(MapUiSettings(
            zoomControlsEnabled = true,
            compassEnabled = true,
            myLocationButtonEnabled = hasLocationPermission(context)
        ))
    }

    val mapProperties by remember {
        mutableStateOf(MapProperties(
            mapType = MapType.NORMAL,
            isMyLocationEnabled = hasLocationPermission(context)
        ))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(vertical = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Set Safe Zone",
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFFBDC1C6),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(6.dp))

        if (!hasLocationPermission(context)) {
            Text(
                text = "Location permissions required",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(8.dp)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = uiSettings,
                properties = mapProperties,
                onMapClick = { latLng ->
                    if (hasLocationPermission(context)) {
                        location = latLng
                        cameraPositionState.position = CameraPosition.fromLatLngZoom(latLng, 15f)
                    }
                }
            ) {
                location?.let { loc ->
                    Circle(
                        center = loc,
                        radius = radius.toDouble(),
                        fillColor = Color(0x220891B2),
                        strokeColor = Color(0xFF0891B2),
                        strokeWidth = 2f
                    )
                    Marker(
                        state = MarkerState(position = loc),
                        title = name.ifEmpty { "Safe Zone" }
                    )
                }
            }
        }

        Text(
            text = "Geofence Name",
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .padding(top = 20.dp, bottom = 4.dp, start = 16.dp)
                .align(Alignment.Start)
        )
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            placeholder = {
                Text(
                    text = "Enter Zone Name",
                    color = Color.Gray,
                    fontSize = 12.sp,
                ) },
            singleLine = true,
            maxLines = 1,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedBorderColor = Color.White,
                unfocusedBorderColor = Color.Gray,
                cursorColor = Color.White,
                focusedPlaceholderColor = Color.Gray,
                unfocusedPlaceholderColor = Color.Gray
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 15.dp, start = 11.dp, end = 11.dp),
            trailingIcon = {
                if (name.isNotEmpty()) {
                    IconButton(
                        onClick = { name = "" },
                                modifier = Modifier.size(20.dp)) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = Color.Gray
                        )
                    }
                }
            }
        )

        Text(
            text = "Address",
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .padding(top = 18.dp, bottom = 4.dp, start = 16.dp)
                .align(Alignment.Start)
        )

        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            placeholder = {
                Text(
                    text = "Enter Address",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            },
            singleLine = true,
            maxLines = 1,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedBorderColor = Color.White,
                unfocusedBorderColor = Color.Gray,
                cursorColor = Color.White,
                focusedPlaceholderColor = Color.Gray,
                unfocusedPlaceholderColor = Color.Gray
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 15.dp, start = 11.dp, end = 11.dp),
            trailingIcon = {
                Row {
                    Spacer(modifier = Modifier.width(4.dp))
                    if (address.isNotEmpty()) {
                        IconButton(
                            onClick = { address = "" },
                            modifier = Modifier.size(20.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = Color.Gray
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                searchAddress(context, address) { latLng ->
                                    location = latLng
                                    cameraPositionState.position = CameraPosition.fromLatLngZoom(latLng, 15f)
                                }
                            },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Color(0xFFFFFFFF)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
            }
        )

        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 6.dp)
            ) {
                Text(
                    text = "Radius:",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "${radius.toInt()}m",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Slider(
                value = radius,
                onValueChange = { radius = it },
                valueRange = 50f..500f,
                steps = 9,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                location?.let { latLng ->
                    if (hasLocationPermission(context)) {
                        saveAndCreateGeofence(context, geofenceStorage, geofenceId, name, latLng, radius, address)
                        navController.popBackStack()
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2563EB),
                disabledContainerColor = Color.DarkGray,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .width(100.dp)
                .height(48.dp),
            enabled = name.isNotEmpty() && location != null && hasLocationPermission(context)
        ) {
            Text(
                text = "Save",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun hasLocationPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
}

private fun searchAddress(context: Context, address: String, onLocationFound: (LatLng) -> Unit) {
    val geocoder = Geocoder(context, Locale.getDefault())
    try {
        @Suppress("DEPRECATION")
        val addresses = geocoder.getFromLocationName(address, 1)
        addresses?.takeIf { it.isNotEmpty() }?.let {
            val foundLocation = it[0]
            val latLng = LatLng(foundLocation.latitude, foundLocation.longitude)
            onLocationFound(latLng)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Could not find address", Toast.LENGTH_SHORT).show()
    }
}

private fun saveAndCreateGeofence(
    context: Context,
    storage: GeofenceStorage,
    id: String,
    name: String,
    latLng: LatLng,
    radius: Float,
    address: String
) {
    val geofenceData = GeofenceData().apply {
        this.id = id
        this.name = if (name.isEmpty()) "Safe Zone" else name
        this.latitude = latLng.latitude
        this.longitude = latLng.longitude
        this.radius = radius
        this.address = address
    }

    storage.saveGeofence(geofenceData)
    createGeofence(context, id, geofenceData.name, latLng, radius)
}

private fun createGeofence(
    context: Context,
    id: String,
    name: String,
    latLng: LatLng,
    radius: Float
) {
    val validRadius = radius.coerceIn(50f, 500f)
    val geofence = Geofence.Builder()
        .setRequestId(id)
        .setCircularRegion(latLng.latitude, latLng.longitude, validRadius)
        .setExpirationDuration(Geofence.NEVER_EXPIRE)
        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
        .setLoiteringDelay(5000)
        .build()

    val geofencingRequest = GeofencingRequest.Builder()
        .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
        .addGeofence(geofence)
        .build()

    val geofencingClient = LocationServices.getGeofencingClient(context)

    val intent = Intent(context, GeofenceBroadcastReceiver::class.java).apply {
        action = "com.example.starsentinel.ACTION_GEOFENCE_EVENT"
    }
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        (System.currentTimeMillis() % Integer.MAX_VALUE).toInt(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    if (ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    ) {
        geofencingClient.addGeofences(geofencingRequest, pendingIntent).run {
            addOnSuccessListener {
                Toast.makeText(
                    context,
                    "Safe zone '${name.ifEmpty { "Unnamed" }}' added",
                    Toast.LENGTH_SHORT
                ).show()
            }
            addOnFailureListener { e ->
                val errorMessage = when {
                    e is ApiException -> "Error: ${getGeofenceErrorString(e.statusCode)}"
                    else -> "Failed to add safe zone: ${e.message}"
                }
                Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }
}

private fun getGeofenceErrorString(errorCode: Int): String {
    return when (errorCode) {
        GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE ->
            "Geofence service is not available on this device"
        GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES ->
            "Too many geofences registered"
        GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS ->
            "Too many pending intents"
        else -> "Unknown error: $errorCode"
    }
}