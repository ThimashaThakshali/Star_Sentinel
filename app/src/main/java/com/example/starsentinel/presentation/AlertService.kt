package com.example.starsentinel.alert

import android.content.Context
import android.content.SharedPreferences
import android.telephony.SmsManager
import android.util.Log
import com.example.starsentinel.location.LocationService
import com.example.starsentinel.presentation.Contact
import com.example.starsentinel.presentation.ContactStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager

/**
 * Service responsible for sending alert messages to emergency contacts
 */
class AlertService(private val context: Context) {
    private val TAG = "AlertService"

    // Shared preferences for accessing saved alert message
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)

    // Contact storage for accessing emergency contacts
    private val contactStorage = ContactStorage(context)

    // Location service for accessing device location
    private val locationService = LocationService(context)

    // Track last alert time to prevent spam
    private var lastAlertTimestamp = 0L
    private val ALERT_COOLDOWN_MS = 60000 // 1 minute cooldown between alerts

    init {
        // Start location updates when service is created
        locationService.startLocationUpdates()
    }

    /**
     * Send alert messages to all emergency contacts
     */
    fun sendAlerts() {
        val currentTime = System.currentTimeMillis()

        // Check cooldown to prevent rapid repeated alerts
        if (currentTime - lastAlertTimestamp < ALERT_COOLDOWN_MS) {
            Log.d(TAG, "Alert on cooldown, skipping")
            return
        }

        // Get the current alert message
        val alertMessage = getAlertMessage()
        if (alertMessage.isEmpty()) {
            Log.e(TAG, "No alert message found")
            return
        }

        // Get all contacts
        val contacts = contactStorage.getContacts()
        if (contacts.isEmpty()) {
            Log.e(TAG, "No emergency contacts found")
            return
        }

        // Get location information and append to message
        val locationInfo = locationService.getLocationForAlert()
        val fullMessage = "$alertMessage\n\n$locationInfo"

        // Send SMS to each contact
        CoroutineScope(Dispatchers.IO).launch {
            try {
                sendSmsToContacts(contacts, fullMessage)
                lastAlertTimestamp = currentTime
                Log.d(TAG, "Alert messages sent successfully to ${contacts.size} contacts")
            } catch (e: Exception) {
                Log.e(TAG, "Error sending alert messages: ${e.message}")
            }
        }
    }

    /**
     * Send SMS to list of contacts
     */
    private fun sendSmsToContacts(contacts: List<Contact>, message: String) {
        // First verify permission is actually granted
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.SEND_SMS
            ) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "SMS permission not granted")
            return
        }

        val smsManager = SmsManager.getDefault()

        for (contact in contacts) {
            if (contact.phone.isNotBlank()) {
                try {
                    // Split message if too long
                    val parts = smsManager.divideMessage(message)
                    smsManager.sendMultipartTextMessage(
                        contact.phone,
                        null,
                        parts,
                        null,
                        null
                    )
                    Log.d(TAG, "Message sent to ${contact.firstName} ${contact.lastName}")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to send SMS to ${contact.phone}: ${e.message}")
                }
            }
        }
    }

    /**
     * Get the currently set alert message from SharedPreferences
     */
    private fun getAlertMessage(): String {
        return sharedPreferences.getString("alertMessage", "I might be in danger...") ?: "I might be in danger..."
    }
}