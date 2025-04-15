package com.example.starsentinel.alert

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.example.starsentinel.location.LocationService
import com.example.starsentinel.presentation.Contact
import com.example.starsentinel.presentation.ContactStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Service responsible for sending WhatsApp alerts to emergency contacts
 * Optimized for Wear OS on Samsung Galaxy Watch 5
 */
class WhatsAppAlertService(private val context: Context) {
    private val TAG = "WhatsAppAlertService"

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
     * Send alert messages to all emergency contacts via WhatsApp
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

        // Send WhatsApp message to each contact
        CoroutineScope(Dispatchers.Main).launch {
            try {
                sendWhatsAppToContacts(contacts, fullMessage)
                lastAlertTimestamp = currentTime
                Log.d(TAG, "WhatsApp alerts attempted for ${contacts.size} contacts")
            } catch (e: Exception) {
                Log.e(TAG, "Error sending WhatsApp alerts: ${e.message}")
                // Show toast on error
                Toast.makeText(context, "Failed to send WhatsApp alerts", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Send WhatsApp messages to list of contacts using Wear OS compatible method
     */
    private fun sendWhatsAppToContacts(contacts: List<Contact>, message: String) {
        for (contact in contacts) {
            if (contact.phone.isNotBlank()) {
                try {
                    // Format phone number correctly (no additional country code)
                    val formattedPhone = cleanPhoneNumber(contact.phone)

                    // Try direct WhatsApp chat intent first (Wear OS compatible)
                    try {
                        // This uses the WhatsApp companion app directly
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setPackage("com.whatsapp")
                            data = Uri.parse("content://com.android.contacts/data/$formattedPhone")
                            putExtra("jid", "$formattedPhone@s.whatsapp.net")
                            putExtra("message", message)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                        Log.d(TAG, "Direct WhatsApp intent for ${contact.firstName}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Direct intent failed: ${e.message}")

                        // Fallback to universal WhatsApp URL method
                        try {
                            val uri = Uri.parse("https://wa.me/$formattedPhone/?text=${Uri.encode(message)}")
                            val fallbackIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(fallbackIntent)
                            Log.d(TAG, "Fallback WhatsApp web for ${contact.firstName}")
                        } catch (e2: Exception) {
                            Log.e(TAG, "Fallback also failed: ${e2.message}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to send WhatsApp to ${contact.phone}: ${e.message}")
                }
            }
        }
    }

    /**
     * Clean phone number for WhatsApp by removing non-digits but preserving the valid country code
     */
    private fun cleanPhoneNumber(phone: String): String {
        // Remove all non-digit characters except the + at the beginning
        val cleanedPhone = phone.replace(Regex("[^0-9+]"), "")

        // If the number already has a country code (starts with +), just remove the +
        return if (cleanedPhone.startsWith("+")) {
            cleanedPhone.substring(1)
        } else {
            // If no country code, keep as is - don't add default country code
            // The user needs to enter correct numbers with country code
            cleanedPhone
        }
    }

    /**
     * Get the currently set alert message from SharedPreferences
     */
    private fun getAlertMessage(): String {
        return sharedPreferences.getString("alertMessage", "I might be in danger...") ?: "I might be in danger..."
    }
}