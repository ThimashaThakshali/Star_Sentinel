package com.example.starsentinel.presentation

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

// class which is responsible for getting and tracking device location

@Suppress("DEPRECATION")
class LocationService(private val context: Context) {
    private val tag = "LocationService"

    // FusedLocationProviderClient - Main API for location services
    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

    // Location request configuration
    private val locationRequest = LocationRequest.create().apply {
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        interval = 10000 // 10 seconds
        fastestInterval = 5000 // 5 seconds
    }

    // LocationCallback to receive location updates
    private var locationCallback: LocationCallback? = null

    // Flow to expose location data
    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()

    // Google Maps URL for sharing location
    private val _locationUrl = MutableStateFlow("")
    val locationUrl: StateFlow<String> = _locationUrl.asStateFlow()

    // Address information
    private val _currentAddress = MutableStateFlow("Fetching address...")
    val currentAddress: StateFlow<String> = _currentAddress.asStateFlow()

    // Start location updates

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        try {
            // Create location callback
            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    locationResult.lastLocation?.let { location ->
                        _currentLocation.value = location
                        updateLocationUrl(location)
                        Log.d(tag, "Location updated: ${location.latitude}, ${location.longitude}")
                    }
                }
            }

            // Request location updates
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )

            // Get last known location immediately
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    _currentLocation.value = it
                    updateLocationUrl(it)
                    Log.d(tag, "Last known location: ${it.latitude}, ${it.longitude}")
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error starting location updates: ${e.message}")
        }
    }

    // Stop location updates

    fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
            locationCallback = null
            Log.d(tag, "Location updates stopped")
        }
    }

    // Get location as formatted string

    @SuppressLint("DefaultLocale")
    fun getLocationString(): String {
        val location = _currentLocation.value
        return if (location != null) {
            String.format("%.6f, %.6f", location.latitude, location.longitude)
        } else {
            "Unknown location"
        }
    }

    // Update Google Maps URL for the location

    private fun updateLocationUrl(location: Location) {
        val url = "https://www.google.com/maps?q=${location.latitude},${location.longitude}"
        _locationUrl.value = url

        // Get address for the location
        updateAddressFromLocation(location)
    }

    // Get address information from location coordinates

    private fun updateAddressFromLocation(location: Location) {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())

            // For Android 13+ (API 33+)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocation(location.latitude, location.longitude, 1) { addresses ->
                    if (addresses.isNotEmpty()) {
                        val address = addresses[0]
                        val addressText = buildString {
                            // Street address
                            if (!address.thoroughfare.isNullOrEmpty()) {
                                append(address.thoroughfare)
                                if (!address.subThoroughfare.isNullOrEmpty()) {
                                    append(" ").append(address.subThoroughfare)
                                }
                                append(", ")
                            }

                            // City/locality
                            if (!address.locality.isNullOrEmpty()) {
                                append(address.locality)
                            } else if (!address.subAdminArea.isNullOrEmpty()) {
                                append(address.subAdminArea)
                            }

                            // Add postal code if available
                            if (!address.postalCode.isNullOrEmpty()) {
                                append(", ").append(address.postalCode)
                            }
                        }

                        _currentAddress.value = addressText.ifEmpty { "Address unavailable" }
                    } else {
                        _currentAddress.value = "Address unavailable"
                    }
                }
            } else {
                // For older Android versions
                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    val addressText = buildString {
                        // Street address
                        if (!address.thoroughfare.isNullOrEmpty()) {
                            append(address.thoroughfare)
                            if (!address.subThoroughfare.isNullOrEmpty()) {
                                append(" ").append(address.subThoroughfare)
                            }
                            append(", ")
                        }

                        // City/locality
                        if (!address.locality.isNullOrEmpty()) {
                            append(address.locality)
                        } else if (!address.subAdminArea.isNullOrEmpty()) {
                            append(address.subAdminArea)
                        }

                        // Add postal code if available
                        if (!address.postalCode.isNullOrEmpty()) {
                            append(", ").append(address.postalCode)
                        }
                    }

                    _currentAddress.value = addressText.ifEmpty { "Address unavailable" }
                } else {
                    _currentAddress.value = "Address unavailable"
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error getting address: ${e.message}")
            _currentAddress.value = "Address unavailable"
        }
    }

    // Get location text for alert message

    fun getLocationForAlert(): String {
        val location = _currentLocation.value
        val address = _currentAddress.value

        return if (location != null) {
            "My current location: $address\n${_locationUrl.value}"
        } else {
            "Location unavailable"
        }
    }
}