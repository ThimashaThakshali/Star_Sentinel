package com.example.starsentinel.presentation

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.core.content.edit

class AlertMessageViewModel(application: Application) : AndroidViewModel(application) {
    private val sharedPreferences = application.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)

    val currentMessage = MutableLiveData<String>().apply {
        value = sharedPreferences.getString("alertMessage", "I might be in danger...")
    }

    val predefinedMessages = MutableLiveData<List<String>>().apply {
        value = sharedPreferences.getStringSet("predefinedMessages", setOf("I'm in trouble", "Help Me!"))?.toList()
            ?: listOf("I'm in trouble", "Help Me!")
    }

    fun updateMessage(newMessage: String) {
        // Update current message
        currentMessage.value = newMessage
        sharedPreferences.edit {
            putString("alertMessage", newMessage)
        }

        // Update predefined messages
        val updatedMessages = predefinedMessages.value?.toMutableList() ?: mutableListOf()
        if (!updatedMessages.contains(newMessage)) {
            updatedMessages.add(newMessage)
            if (updatedMessages.size > 5) {
                updatedMessages.removeAt(0)
            }
            predefinedMessages.value = updatedMessages
            sharedPreferences.edit {
                putStringSet("predefinedMessages", updatedMessages.toSet())
            }
        }
    }
}