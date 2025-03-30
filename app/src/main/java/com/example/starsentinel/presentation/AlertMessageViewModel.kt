package com.example.starsentinel.presentation

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData

class AlertMessageViewModel(application: Application) : AndroidViewModel(application) {
    private val sharedPreferences: SharedPreferences =
        application.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)

    val currentMessage = MutableLiveData<String>()
    val predefinedMessages = MutableLiveData<List<String>>() // Use List<String> instead of MutableList

    init {
        // Load saved message or use default
        currentMessage.value = sharedPreferences.getString("alertMessage", "I might be in danger...")

        // Load predefined messages (ensure mutable conversion)
        predefinedMessages.value = sharedPreferences
            .getStringSet("predefinedMessages", setOf())?.toList() ?: listOf()
    }

    fun updateMessage(newMessage: String) {
        currentMessage.value = newMessage
        sharedPreferences.edit().putString("alertMessage", newMessage).apply()

        // Add new message to predefined messages only if not present
        val updatedMessages = predefinedMessages.value?.toMutableList() ?: mutableListOf()
        if (!updatedMessages.contains(newMessage)) {
            updatedMessages.add(newMessage)
            // Keep only the last 5 messages
            if (updatedMessages.size > 5) updatedMessages.removeAt(0)

            predefinedMessages.value = updatedMessages //  Assign a NEW LIST
            sharedPreferences.edit().putStringSet("predefinedMessages", updatedMessages.toSet()).apply()
        }
    }
}