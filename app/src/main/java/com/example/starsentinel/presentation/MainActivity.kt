package com.example.starsentinel.presentation

import SettingsScreen
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.starsentinel.presentation.theme.StarSentinelTheme

class MainActivity : ComponentActivity() {
    private lateinit var contactStorage: ContactStorage
    private var contacts = mutableStateOf(listOf<Contact>())
    private var showPermissionDialog = mutableStateOf(false)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (!permissions.all { it.value }) {
            showPermissionDialog.value = true
            Toast.makeText(this, "Location permissions required", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        contactStorage = ContactStorage(this)
        contacts.value = contactStorage.getContacts()

        setContent {
            StarSentinelTheme {
                val context = LocalContext.current
                val isFirstLaunch = remember { mutableStateOf(checkFirstLaunch()) }
                val navController = rememberNavController()

                // Request permissions when UI is ready
                LaunchedEffect(Unit) {
                    if (!hasRequiredPermissions()) {
                        requestLocationPermissions()
                    }
                }

                // Permission dialog
                if (showPermissionDialog.value) {
                    AlertDialog(
                        onDismissRequest = { showPermissionDialog.value = false },
                        title = { Text("Permission Required") },
                        text = { Text("Background location access is required for safety features.")
                        },
                        confirmButton = {
                            Button(onClick = {
                                showPermissionDialog.value = false
                                openAppSettings(context)
                            }) {
                                Text("Open Settings")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showPermissionDialog.value = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }

                AppNavigation(navController, isFirstLaunch.value, contactStorage, contacts)
            }
        }
    }

    @SuppressLint("WearRecents")
    private fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    private fun hasRequiredPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermissions() {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        )
    }

    private fun checkFirstLaunch(): Boolean {
        val sharedPreferences: SharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val isFirstLaunch = sharedPreferences.getBoolean("firstLaunch", true)
        if (isFirstLaunch) {
            sharedPreferences.edit { putBoolean("firstLaunch", false) }
        }
        return isFirstLaunch
    }
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    isFirstLaunch: Boolean,
    contactStorage: ContactStorage,
    contacts: MutableState<List<Contact>>
) {
    NavHost(
        navController = navController,
        startDestination = if (isFirstLaunch) "welcomeScreen" else "homeScreen",
    ) {
        composable("welcomeScreen") { WelcomeScreen(navController) }
        composable("setupScreen") { SetupScreen(navController) }
        composable("alertMessageScreen") { AlertMessageScreen(navController) }
        composable("createContact") {
            CreateContactScreen(
                navController = navController,
                phone = null
            )
        }
        composable("editContact/{phone}") { backStackEntry ->
            val phone = backStackEntry.arguments?.getString("phone")
            CreateContactScreen(
                navController = navController,
                phone = phone
            )
        }
        composable("contactsScreen") {
            ContactsScreen(
                navController = navController,
                onContactUpdated = { contacts.value = contactStorage.getContacts() }
            )
        }
        composable("setGeofence") { SetGeofenceScreen(navController) }
        composable("manageGeofences") { ManageGeofencesScreen(navController) }
        composable("homeScreen") { HomeScreen(navController) }
        composable("settingsScreen") { SettingsScreen(navController) }
    }
}

@Composable
fun WelcomeScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(30.dp))
        Text(
            text = "Welcome to Star Sentinel!" ,
            color = Color(0xFFFFFFFF),
        )
        Spacer(modifier = Modifier.height(30.dp))
        Button(onClick = {
            navController.navigate("setupScreen")
        }) {
            Text(
                text="Next",
                fontSize = 14.sp,
                modifier = Modifier.padding(vertical = 5.dp, horizontal = 15.dp)
                )
        }
    }
}