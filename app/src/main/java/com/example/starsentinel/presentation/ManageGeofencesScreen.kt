package com.example.starsentinel.presentation

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch

@Composable
fun ManageGeofencesScreen(navController: NavController) {
    val context = LocalContext.current
    val geofenceStorage = remember { GeofenceStorage(context) }
    val geofencingClient = remember { LocationServices.getGeofencingClient(context) }
    var geofences by remember { mutableStateOf(geofenceStorage.getGeofences()) }
    var selectedGeofence by remember { mutableStateOf<GeofenceData?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Refresh geofences when screen is shown
    LaunchedEffect(Unit) {
        geofences = geofenceStorage.getGeofences()
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black)
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Safe Zones",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFFBDC1C6),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("setGeofence") },
                containerColor = Color(0xFF2563EB),
                contentColor = Color.White,
                modifier = Modifier
                    .padding(top = 25.dp, bottom = 16.dp, start = 25.dp, end = 25.dp)
                    .size(40.dp), // Make it smaller
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Safe Zone",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(paddingValues)
                .padding(8.dp)
        ) {
            if (geofences.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No safe zones set",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFFFFFFF)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(geofences) { geofence ->
                        GeofenceItem(
                            geofence = geofence,
                            onEdit = {
                                navController.navigate("setGeofence?id=${geofence.id}")
                            },
                            onDelete = {
                                selectedGeofence = geofence
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog && selectedGeofence != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Delete Safe Zone",
                        color = Color.White
                    )
                }
            },
            text = {
                Text(
                    text = "Are you sure you want to delete the safe zone '${selectedGeofence?.name}'?",
                    color = Color.LightGray
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            selectedGeofence?.let { geofence ->
                                geofenceStorage.removeGeofence(geofence.id)
                                geofencingClient.removeGeofences(listOf(geofence.id))
                                    .addOnSuccessListener {
                                        Toast.makeText(
                                            context,
                                            "Safe zone removed",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                geofences = geofenceStorage.getGeofences()
                            }
                            showDeleteDialog = false
                            selectedGeofence = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Gray)
                ) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.height(16.dp))
            },
            containerColor = Color.Black
        )
    }
}

@Composable
fun GeofenceItem(
    geofence: GeofenceData,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .padding(horizontal = 4.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF242424))
            .fillMaxWidth()
    ){
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onEdit() },

        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 4.dp, start = 8.dp)
        ) {

            Text(
                text = geofence.name,
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFFFFFFF),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis

            )


        }

        IconButton(
            onClick = onEdit,
            modifier = Modifier.size(24.dp) // Adjust size if needed


        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                tint = Color.White,
                contentDescription = "Edit" ,
            )
        }

        Spacer(modifier = Modifier.width(4.dp)) // Add a small spacer to control the gap

        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(24.dp)) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete",
                tint = Color.White,
            )
        }
    }
    }
}

// Helper function to format doubles
private fun Double.format(digits: Int) = "%.${digits}f".format(this)