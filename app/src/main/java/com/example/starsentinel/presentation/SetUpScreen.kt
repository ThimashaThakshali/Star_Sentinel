package com.example.starsentinel.presentation

import androidx.compose.runtime.Composable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.rememberNavController
import com.example.starsentinel.R

@Composable
fun SetupScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(5.dp)
            .verticalScroll(rememberScrollState()), // Enables scrolling
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // make the font black of the title Set Up Screen
        androidx.compose.material3.Text(
            text = "Set Up Screen",
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
            color = Color(0xFFBDC1C6),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )


        Spacer(modifier = Modifier.height(16.dp))

        SetupOption(
            iconRes = R.drawable.alert_icon,
            text = "Add Alerts",
            onClick = { navController.navigate("alertMessageScreen") /* Navigate to Add Alerts Screen */ }
        )

        SetupOption(
            iconRes = R.drawable.add_contact_icon,
            text = "Add Contacts",
            onClick = { navController.navigate("createContact") } // Navigate to Add Contacts Screen
        )

        SetupOption(
            iconRes = R.drawable.safe_zone_icon,
            text = "Add Safe Zone",
            onClick = { navController.navigate("setGeofence")}
        )

        Spacer(modifier = Modifier.height(10.dp))

        Button(onClick = { navController.navigate("homeScreen") }) {
            androidx.compose.material3.Text("Continue")
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun SetupOption(iconRes: Int, text: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp, horizontal = 15.dp )
            .clickable { onClick() },
        shape = RoundedCornerShape(25.dp), // Rounded corners with a radius of 12dp
        color = Color(0xFF0891B2) // Background color
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = text,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            androidx.compose.material3.Text(
                text = text,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SetupScreenPreview() {
    SetupScreen(navController = rememberNavController())
}