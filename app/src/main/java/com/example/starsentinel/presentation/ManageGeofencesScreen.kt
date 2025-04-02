package com.example.starsentinel.presentation

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
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
            TopAppBar(
                title = { Text("Safe Zones") },
                actions = {
                    IconButton(onClick = { navController.navigate("setGeofence") }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Safe Zone")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(8.dp)
        ) {
            if (geofences.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No safe zones set")
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { navController.navigate("setGeofence") }) {
                            Text("Add Safe Zone")
                        }
                    }
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
                        Divider()
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog && selectedGeofence != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Safe Zone") },
            text = {
                Text("Are you sure you want to delete the safe zone '${selectedGeofence?.name}'?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            selectedGeofence?.let { geofence ->
                                // Remove from storage
                                geofenceStorage.removeGeofence(geofence.id)

                                // Remove from active geofences
                                geofencingClient.removeGeofences(listOf(geofence.id))
                                    .addOnSuccessListener {
                                        Toast.makeText(
                                            context,
                                            "Safe zone removed",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }

                                // Refresh the list
                                geofences = geofenceStorage.getGeofences()
                            }
                            showDeleteDialog = false
                            selectedGeofence = null
                        }
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun GeofenceItem(
    geofence: GeofenceData,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
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
                .padding(end = 8.dp)
        ) {
            Text(
                text = geofence.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = if (geofence.address.isNotEmpty()) {
                    geofence.address
                } else {
                    "${geofence.latitude.format(4)}, ${geofence.longitude.format(4)}"
                },
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "Radius: ${geofence.radius.toInt()}m",
                style = MaterialTheme.typography.bodySmall
            )
        }

        IconButton(onClick = onEdit) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Edit"
            )
        }

        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

// Helper function to format doubles
private fun Double.format(digits: Int) = "%.${digits}f".format(this)