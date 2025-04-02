package com.example.starsentinel.presentation

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class GeofenceStorage(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("GeofencePrefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveGeofence(geofence: GeofenceData) {
        val geofenceList = getGeofences().toMutableList()
        val existingIndex = geofenceList.indexOfFirst { it.id == geofence.id }
        if (existingIndex >= 0) {
            geofenceList[existingIndex] = geofence
        } else {
            geofenceList.add(geofence)
        }
        val jsonString = gson.toJson(geofenceList)
        sharedPreferences.edit { putString("savedGeofences", jsonString) }
    }

    fun getGeofences(): List<GeofenceData> {
        val jsonString = sharedPreferences.getString("savedGeofences", null)
        return if (jsonString.isNullOrEmpty()) emptyList() else try {
            val type = object : TypeToken<List<GeofenceData>>() {}.type
            gson.fromJson(jsonString, type) ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun removeGeofence(id: String) {
        val geofenceList = getGeofences().toMutableList()
        geofenceList.removeIf { it.id == id }
        val jsonString = gson.toJson(geofenceList)
        sharedPreferences.edit { putString("savedGeofences", jsonString) }
    }
}