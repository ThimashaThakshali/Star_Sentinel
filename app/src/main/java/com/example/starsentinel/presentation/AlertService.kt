package com.example.starsentinel.presentation

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.net.toUri

class AlertService(private val context: Context) {
    private val tag = "AlertService"
    private val sharedPreferences = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
    private val contactStorage = ContactStorage(context)
    private val locationService = LocationService(context)

    // Track last alert time to prevent spam
    private var lastAlertTimestamp = 0L
    private val alertCoolDownMs = 60000 // 1 minute cooldown between alerts

    init {
        // Start location updates when service is created
        locationService.startLocationUpdates()
    }

    fun sendAlerts() {
        val currentTime = System.currentTimeMillis()

        // Check cooldown to prevent rapid repeated alerts
        if (currentTime - lastAlertTimestamp < alertCoolDownMs) {
            Log.d(tag, "Alert on cooldown, skipping")
            return
        }

        // Get the current alert message
        val alertMessage = getAlertMessage()
        if (alertMessage.isEmpty()) {
            Log.e(tag, "No alert message found")
            return
        }

        // Get all contacts
        val contacts = contactStorage.getContacts()
        if (contacts.isEmpty()) {
            Log.e(tag, "No emergency contacts found")
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
                data = "smsto:$phoneNumber".toUri()
                putExtra("sms_body", message)
            }

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                Toast.makeText(context, "No messaging app found", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to send SMS: ${e.message}")
            Toast.makeText(context, "Failed to send SMS", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getAlertMessage(): String {
        return sharedPreferences.getString("alertMessage", "I might be in danger...")
            ?: "I might be in danger..."
    }
}