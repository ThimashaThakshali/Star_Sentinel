package com.example.starsentinel.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.starsentinel.R

@Composable
fun CreateContactScreen(navController: NavController) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Back button and title
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Add spacer to center the title
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Create Contact",
                style = MaterialTheme.typography.titleLarge
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Add Photo section
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Add Photo", style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(8.dp))
            IconButton(
                onClick = { /* TODO: Add photo logic */ },
                modifier = Modifier.size(80.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.add_photo_icon),
                    contentDescription = "Add Photo"
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // First Name field
        Text(text = "First Name", style = MaterialTheme.typography.labelMedium)
        TextField(
            value = firstName,
            onValueChange = { firstName = it },
            placeholder = { Text("Enter First Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Last Name field
        Text(text = "Last Name", style = MaterialTheme.typography.labelMedium)
        TextField(
            value = lastName,
            onValueChange = { lastName = it },
            placeholder = { Text("Enter Last Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Phone field
        Text(text = "Phone (Mobile)", style = MaterialTheme.typography.labelMedium)
        TextField(
            value = phone,
            onValueChange = { phone = it },
            placeholder = { Text("Enter Phone Number") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Email field
        Text(text = "Add Email", style = MaterialTheme.typography.labelMedium)
        TextField(
            value = email,
            onValueChange = { email = it },
            placeholder = { Text("Enter Email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.weight(1f))

        // Save button
        Button(
            onClick = {
                // Simple validation and save logic
                if (firstName.isNotBlank() && phone.isNotBlank()) {
                    // TODO: Save contact to SharedPreferences or database later
                    navController.popBackStack()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = firstName.isNotBlank() && phone.isNotBlank()
        ) {
            Text("Save")
        }
    }
}