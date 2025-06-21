package com.example.starsentinel.alert

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import com.example.starsentinel.presentation.Contact
import com.example.starsentinel.presentation.ContactStorage

class AlertService(private val context: Context) {
    private val TAG = "AlertService"
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
    private val contactStorage = ContactStorage(context)

    fun sendAlerts() {
        val alertMessage = getAlertMessage()
        if (alertMessage.isEmpty()) {
            Log.e(TAG, "No alert message found")
            return
        }

        val contacts = contactStorage.getContacts()
        if (contacts.isEmpty()) {
            Log.e(TAG, "No emergency contacts found")
            return
        }

        // Send SMS to each contact using the simpler approach
        for (contact in contacts) {
            if (contact.phone.isNotBlank()) {
                sendSms(contact.phone, alertMessage)
            }
        }
    }

    private fun sendSms(phoneNumber: String, message: String) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$phoneNumber")
                putExtra("sms_body", message)
            }

            if (intent.resolveActivity(context.packageManager) != null) {
                startActivity(context, intent, null)
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