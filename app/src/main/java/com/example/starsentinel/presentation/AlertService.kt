package com.example.starsentinel.alert

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.example.starsentinel.location.LocationService
import com.example.starsentinel.presentation.Contact
import com.example.starsentinel.presentation.ContactStorage

class AlertService(private val context: Context) {
    private val TAG = "AlertService"
    private val sharedPreferences = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
    private val contactStorage = ContactStorage(context)
    private val locationService = LocationService(context)

    // Track last alert time to prevent spam
    private var lastAlertTimestamp = 0L
    private val ALERT_COOLDOWN_MS = 60000 // 1 minute cooldown between alerts

    init {
        // Start location updates when service is created
        locationService.startLocationUpdates()
    }

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
        contacts.forEach { contact ->
            if (contact.phone.isNotBlank()) {
                sendSms(contact.phone, fullMessage)
            }
        }

        lastAlertTimestamp = currentTime
    }

    private fun sendSms(phoneNumber: String, message: String) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$phoneNumber")
                putExtra("sms_body", message)
            }

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                Toast.makeText(context, "No messaging app found", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send SMS: ${e.message}")
            Toast.makeText(context, "Failed to send SMS", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getAlertMessage(): String {
        return sharedPreferences.getString("alertMessage", "I might be in danger...")
            ?: "I might be in danger..."
    }
}