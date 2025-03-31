// ContactsScreen.kt
package com.example.starsentinel.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.starsentinel.R

@Composable
fun ContactsScreen(navController: NavController) {
    val context = LocalContext.current
    val contactStorage = remember { ContactStorage(context) }
    var searchQuery by remember { mutableStateOf("") }
    var contacts by remember { mutableStateOf(contactStorage.getContacts()) }

    // Filter contacts in real-time
    val filteredContacts by remember(contacts, searchQuery) {
        derivedStateOf {
            if (searchQuery.isEmpty()) contacts
            else contacts.filter {
                it.firstName.contains(searchQuery, true) ||
                        it.lastName.contains(searchQuery, true) ||
                        it.phone.contains(searchQuery)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header section (non-scrolling)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()), // Enables scrolling
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Contacts",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Search bar
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    shape = MaterialTheme.shapes.small,

                    leadingIcon = {
                        Image(
                            painter = painterResource(R.drawable.ic_search),
                            contentDescription = "Search",
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    placeholder = { Text("Search", style = MaterialTheme.typography.bodyMedium) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedIndicatorColor = Color.LightGray,
                        unfocusedIndicatorColor = Color.LightGray
                    )
                )
            }

            // Contacts list (scrolling)
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // Takes remaining space
            ) {
                items(
                    items = filteredContacts,
                    key = { it.hashCode() } // Stable keys for performance
                ) { contact ->
                    ContactItem(contact)
                    HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray)
                }
            }
        }

        // Floating Action Button (positioned absolutely)
        FloatingActionButton(
            onClick = { navController.navigate("createContact") },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = Color(0xFF0891B2)
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Add Contact",
                tint = Color.White
            )
        }
    }
}

@Composable
fun ContactItem(contact: Contact) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${contact.firstName} ${contact.lastName}",
            style = MaterialTheme.typography.bodyLarge.copy(
                color = Color.Black,
                fontSize = 16.sp
            )
        )
    }
}