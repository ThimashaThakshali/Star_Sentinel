package com.example.starsentinel.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import java.util.UUID

@Composable
fun CreateContactScreen(
    navController: NavController,
    phone: String? = null
) {
    val context = LocalContext.current
    val contactStorage = remember { ContactStorage(context) }
    var contacts by remember { mutableStateOf(contactStorage.getContacts()) }

    // Load existing contact if in edit mode
    val existingContact = remember(phone) {
        phone?.let { contactStorage.getContactByPhone(it) }
    }

    val id  = UUID.randomUUID().toString()
    var firstName by remember { mutableStateOf(existingContact?.firstName ?: "") }
    var lastName by remember { mutableStateOf(existingContact?.lastName ?: "") }
    var phoneNumber by remember { mutableStateOf(existingContact?.phone ?: "") }
    var email by remember { mutableStateOf(existingContact?.email ?: "") }
    var isPhoneValid by remember { mutableStateOf(true) }
    var isEmailValid by remember { mutableStateOf(true) }

    val isEditMode = existingContact != null
    val phoneRegex = Regex("^[0-9]*$")
    val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Header - changes based on mode
            Text(
                text = if (isEditMode) "Edit Contact" else "Create Contact",
                color = Color(0xFFBDC1C6),
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )

        /*    // Add Photo section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .clip(CircleShape)
                        .border(2.dp, Color(0xFF2563EB), CircleShape)
                        .clickable {
                            Toast.makeText(context, "Add photo logic", Toast.LENGTH_SHORT).show()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.add_photo_icon),
                        contentDescription = "Add Photo",
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Add Photo",
                    color = Color.White,
                    fontSize = 14.sp
                )
            }    */

            // Input fields
            InputFieldSection(
                label = "First Name",
                value = firstName,
                onValueChange = { firstName = it },
                placeholder = "Enter First Name"
            )

            InputFieldSection(
                label = "Last Name",
                value = lastName,
                onValueChange = { lastName = it },
                placeholder = "Enter Last Name"
            )

            InputFieldSection(
                label = "Phone Number",
                value = phoneNumber,
                onValueChange = {
                    phoneNumber = it
                    isPhoneValid = phoneRegex.matches(it)
                },
                placeholder = "Enter Phone Number",
                isValid = isPhoneValid,
                errorMessage = "Phone number can only contain digits"
            )

            InputFieldSection(
                label = "Email",
                value = email,
                onValueChange = {
                    email = it
                    isEmailValid = emailRegex.matches(it)
                },
                placeholder = "Enter Email",
                isValid = isEmailValid,
                errorMessage = "Invalid email address"
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Save/Update button
            Button(
                onClick = {
                    if (firstName.isNotBlank() && phoneNumber.isNotBlank() && isPhoneValid && isEmailValid) {
                        val newContact = Contact(id,firstName, lastName, phoneNumber, email)

                        if (isEditMode) {
                            // Remove old contact if editing
                            val updatedContacts = contacts.toMutableList().apply {
                                removeIf { it.phone == existingContact?.phone }
                                add(newContact)
                            }
                            contactStorage.saveContacts(updatedContacts)
                        } else {
                            contactStorage.saveContact(newContact)
                        }

                        // Refresh contacts and navigate back
                        contacts = contactStorage.getContacts()
                        navController.popBackStack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(40.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2563EB)
                )
            ) {
                Text(
                    text = "Save",
                    color = Color.White,
                    fontSize = 16.sp
                )
            }

            // Delete button (only shown in edit mode)
            if (isEditMode) {
                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        // Remove the contact
                        val updatedContacts = contacts.toMutableList().apply {
                            removeIf { it.id == existingContact?.id }
                        }
                        contactStorage.saveContacts(updatedContacts)
                        contacts = contactStorage.getContacts()
                        navController.popBackStack()
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(40.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red
                    )
                ) {
                    Text(
                        text = "Delete Contact",
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun InputFieldSection(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isValid: Boolean = true,
    errorMessage: String = ""
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = if (isValid) Color(0xFF333333) else Color.Red,
                    shape = RoundedCornerShape(8.dp)
                )
                .background(
                    color = Color.Black,
                    shape = RoundedCornerShape(8.dp)
                )
                .height(40.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(
                    color = Color.White,
                    fontSize = 14.sp
                ),
                singleLine = true,
                cursorBrush = SolidColor(Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                decorationBox = { innerTextField ->
                    Box {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                color = Color(0xFF666666),
                                fontSize = 14.sp
                            )
                        }
                        innerTextField()
                    }
                }
            )

            // Clear button
            if (value.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 8.dp)
                        .size(20.dp)
                        .clickable { onValueChange("") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = Color(0xFF666666),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        if (!isValid) {
            Text(
                text = errorMessage,
                color = Color.Red,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}