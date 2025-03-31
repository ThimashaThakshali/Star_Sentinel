package com.example.starsentinel.presentation

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.starsentinel.R

@Composable
fun CreateContactScreen(navController: NavController, onContactAdded: () -> Unit) {
    val context = LocalContext.current
    val contactStorage = remember { ContactStorage(context) }

    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()), // Enables scrolling
        verticalArrangement = Arrangement.Top,
    ) {

        Spacer(modifier = Modifier.height(15.dp))

        // Title
        Text(
            text = "Create Contact",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Add Photo section
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Add Photo", style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(10.dp))
            IconButton(
                onClick = {
                    // TODO: Add photo logic (e.g., image picker)
                    Toast.makeText(context, "Add photo logic", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.size(80.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.add_photo_icon),
                    contentDescription = "Add Photo"
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // First Name Field
        Text(text = "First Name", style = MaterialTheme.typography.labelMedium)
        TextField(
            value = firstName,
            onValueChange = { firstName = it },
            placeholder = { Text("Enter First Name" , style = MaterialTheme.typography.bodySmall) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(10.dp))

        // Last Name Field
        Text(text = "Last Name", style = MaterialTheme.typography.labelMedium)
        TextField(
            value = lastName,
            onValueChange = { lastName = it },
            placeholder = { Text("Enter Last Name",  style = MaterialTheme.typography.bodySmall) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(10.dp))

        // Phone Field
        Text(text = "Phone Number", style = MaterialTheme.typography.labelMedium)
        TextField(
            value = phone,
            onValueChange = { phone = it },
            placeholder = { Text("Enter Phone Number",  style = MaterialTheme.typography.bodySmall) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(10.dp))

        // Email Field
        Text(text = "Email", style = MaterialTheme.typography.labelMedium)
        TextField(
            value = email,
            onValueChange = { email = it },
            placeholder = { Text("Enter Email",  style = MaterialTheme.typography.bodySmall) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Save Button
        Button(
            onClick = {
                if (firstName.isNotBlank() && phone.isNotBlank()) {
                    val newContact = Contact(firstName, lastName, phone, email)
                    contactStorage.saveContact(newContact)
                    onContactAdded() // Refresh the contact list

                    // ✅ Navigate to ContactsScreen instead of just popping back
                    navController.navigate("contactsScreen") {
                        popUpTo("createContact") { inclusive = true } // Remove createContact from backstack
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Contact")
        }
        Spacer(modifier = Modifier.height(16.dp))

    }
}
