package com.example.starsentinel.presentation

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.core.content.edit

// AlertMessageViewModel.kt manages the message storage
class AlertMessageViewModel(application: Application) : AndroidViewModel(application) {
    private val sharedPreferences = application.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)

    val currentMessage = MutableLiveData<String>()
    val predefinedMessages = MutableLiveData<List<String>>()

    fun updateMessage(newMessage: String) {
        // Updates both current message and predefined messages list
        currentMessage.value = newMessage
        sharedPreferences.edit { putString("alertMessage", newMessage) }

        // Add to predefined messages if not already present
        val updatedMessages = predefinedMessages.value?.toMutableList() ?: mutableListOf()
        if (!updatedMessages.contains(newMessage)) {
            updatedMessages.add(newMessage)
            if (updatedMessages.size > 5) updatedMessages.removeAt(0)
            predefinedMessages.value = updatedMessages
            sharedPreferences.edit { putStringSet("predefinedMessages", updatedMessages.toSet()) }
        }
    }
}