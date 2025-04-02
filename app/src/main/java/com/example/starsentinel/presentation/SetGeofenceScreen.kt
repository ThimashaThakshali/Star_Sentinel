package com.example.starsentinel.presentation

import android.Manifest
import android.widget.Toast
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Set Safe Zone",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(vertical = 8.dp)
        )

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

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Zone Name") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        )

        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("Address") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            trailingIcon = {
                if (address.isNotEmpty()) {
                    IconButton(onClick = {
                        searchAddress(context, address) { latLng ->
                            location = latLng
                            cameraPositionState.position = CameraPosition.fromLatLngZoom(latLng, 15f)
                        }
                    }) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
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
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Radius:")
                Text("${radius.toInt()}m")
            }
            Slider(
                value = radius,
                onValueChange = { radius = it },
                valueRange = 50f..500f,
                steps = 9,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        Button(
            onClick = {
                location?.let { latLng ->
                    if (hasLocationPermission(context)) {
                        saveAndCreateGeofence(context, geofenceStorage, geofenceId, name, latLng, radius, address)
                        navController.popBackStack()
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            enabled = name.isNotEmpty() && location != null && hasLocationPermission(context)
        ) {
            Text("Save Safe Zone")
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