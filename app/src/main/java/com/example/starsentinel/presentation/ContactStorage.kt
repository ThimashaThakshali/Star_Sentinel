package com.example.starsentinel.presentation

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import androidx.core.content.edit

class ContactStorage(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("ContactsPrefs", Context.MODE_PRIVATE)

    private val gson = Gson()

    fun saveContact(contact: Contact) {
        val contactList = getContacts().toMutableList()
        contactList.add(contact)
        saveContacts(contactList)
    }

    fun getContacts(): List<Contact> {
        val jsonString = sharedPreferences.getString("savedContacts", null)
        if (jsonString.isNullOrEmpty()) return emptyList()

        return try {
            val type = object : TypeToken<List<Contact>>() {}.type
            gson.fromJson(jsonString, type) ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun saveContacts(contacts: List<Contact>) {
        val jsonString = gson.toJson(contacts)
        sharedPreferences.edit { putString("savedContacts", jsonString) }
    }

    fun getContactByPhone(phone: String): Contact? {
        return getContacts().firstOrNull { it.phone == phone }
    }
}