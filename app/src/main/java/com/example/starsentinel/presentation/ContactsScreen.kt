// ContactsScreen.kt
package com.example.starsentinel.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.starsentinel.R

@Composable
fun ContactsScreen(navController: NavController, onContactUpdated: () -> Unit ) {
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

    Box(
        modifier = Modifier
        .fillMaxSize()
        .background(Color.Black)) {
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
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFFBDC1C6),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.extraLarge) // Use one consistent shape
                        .background(Color(0xFF303030))
                        .border(
                            width = 1.dp,
                            color = Color(0xFF5F6368), // Use the gray color from the image
                            shape = MaterialTheme.shapes.extraLarge // Match the clip shape
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Search icon
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )

                    // Search text field
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White
                        ),
                        decorationBox = { innerTextField ->
                            Box {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Search",
                                        color = Color.LightGray,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp) // Add padding here
                                    )
                                }
                                innerTextField()
                            }
                        },
                        singleLine = true,
                    )

                    // X button to clear search (only shows when there's text)
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = Color.White
                            )
                        }
                    }
                }
            }

            // Contacts list or empty state
            if (filteredContacts.isEmpty()) {
                // Empty state - just display empty space
                Spacer(modifier = Modifier.weight(1f))
            } else {
                // Contacts list (scrolling) - only show if we have contacts
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f) // Takes remaining space
                ) {
                    items(
                        items = filteredContacts,
                        key = { it.hashCode() } // Stable keys for performance
                    ) { contact ->
                        ContactItem(
                            contact = contact,
                            onClick= {
                                // Navigate to edit screen with contact details
                                navController.navigate("editContact/${contact.phone}")
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp)) // Add spacing between contacts
                    }
                    item {
                        Spacer(modifier = Modifier.height(26.dp))
                    }
                }
            }
        }

        // Floating Action Button (positioned absolutely) - made smaller with new color
        FloatingActionButton(
            onClick = { navController.navigate("createContact") },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(25.dp)
                .size(40.dp), // Make it smaller
            containerColor = Color(0xFF0891B2)
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Add Contact",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

    }
}

@Composable
fun ContactItem(
    contact: Contact,
    onClick: () -> Unit ) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(24.dp)) // Rounded corners to match image
            .background(Color(0xFF49454F)) // Set the background color as requested
            .padding(vertical = 12.dp, horizontal = 16.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar placeholder circle
        Box(
            modifier = Modifier
                .size(25.dp)
                .clip(MaterialTheme.shapes.extraLarge)
                .background(Color.Gray.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.crontacts_icon), // Make sure you have this resource
                contentDescription = "Contact",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Contact name - display the actual contact name
        Text(
            text = "${contact.firstName} ${contact.lastName}",
            style = MaterialTheme.typography.bodyLarge.copy(
                color = Color.White,
                fontSize = 13.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,

        )
    }
}