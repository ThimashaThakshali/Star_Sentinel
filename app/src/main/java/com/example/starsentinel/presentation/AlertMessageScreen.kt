package com.example.starsentinel.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp

@Composable
fun AlertMessageScreen(navController: NavController, viewModel: AlertMessageViewModel = viewModel()) {
    // Get the current message and predefined messages from view model
    val currentMessage by viewModel.currentMessage.observeAsState("I might be in danger...")
    val predefinedMessages by viewModel.predefinedMessages.observeAsState(listOf("I'm in trouble", "Help Me!"))

    // Local state for text field
    var textState by remember { mutableStateOf(TextFieldValue(currentMessage)) }

    // Update textState when currentMessage changes
    LaunchedEffect(currentMessage) {
        textState = TextFieldValue(currentMessage)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Alert Messages",
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFFBDC1C6),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Current Message Section
        Text(
            text = "Currently Set to",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            textAlign = TextAlign.Left,
        )

        Spacer(modifier = Modifier.height(15.dp))

        // Editable current message with edit icon
        OutlinedTextField(
            value = textState.text,
            onValueChange = { textState = TextFieldValue(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            shape = RoundedCornerShape(4.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color.Gray,
                focusedBorderColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedTextColor = Color.White,
            ),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(
                fontSize = 12.sp,
            ),
            trailingIcon = {
                IconButton(onClick = { /* Enable editing */ }) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Message",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Predefined Messages Section
        Text(
            text = "Select Message",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Display predefined messages as buttons with gray background
        predefinedMessages.forEach { message ->
            Button(
                onClick = {
                    textState = TextFieldValue(message)
                    viewModel.updateMessage(message)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF363739),
                ),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = message,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Normal,
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Save Button
        Button(
            onClick = {
                viewModel.updateMessage(textState.text)
                navController.popBackStack()
            },
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .padding(vertical = 8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2563EB)
            ),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text("Save")
        }
    }
}