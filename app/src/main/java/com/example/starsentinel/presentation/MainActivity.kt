package com.example.starsentinel.presentation

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.starsentinel.presentation.theme.StarSentinelTheme
import androidx.compose.material3.*
import androidx.core.content.edit

class MainActivity : ComponentActivity() {
 override fun onCreate(savedInstanceState: Bundle?) {
     super.onCreate(savedInstanceState)
     setContent {
         val isFirstLaunch = remember { mutableStateOf(checkFirstLaunch()) }
         val navController = rememberNavController()
         AppNavigation(navController, isFirstLaunch.value)
     }
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
fun AppNavigation(navController: NavHostController, isFirstLaunch: Boolean) {
 NavHost(navController = navController, startDestination = if (isFirstLaunch) "welcomeScreen" else "setupScreen") {
     composable("welcomeScreen") { WelcomeScreen(navController) }
     composable("setupScreen") { SetupScreen(navController) }
     composable("alertMessageScreen") { AlertMessageScreen(navController) }
     composable("createContact") { CreateContactScreen(navController) }
 }
}

@Composable
fun WelcomeScreen(navController: NavController) {
 StarSentinelTheme {
     Column(
         modifier = Modifier
             .fillMaxSize()
             .verticalScroll(rememberScrollState()),  // Enables scrolling
         verticalArrangement = Arrangement.Center,
         horizontalAlignment = Alignment.CenterHorizontally
     ) {
         Text(text = "Welcome to Star Sentinel!")

         Spacer(modifier = Modifier.height(20.dp))

         Button(onClick = { navController.navigate("setupScreen") }) {
             Text("Next")
         }
     }
 }
}