package com.example.jobtown.ui.profile

import android.util.Patterns
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.jobtown.Screen
import com.example.jobtown.data.User
import com.example.jobtown.ui.theme.DeepGreenDark
import com.example.jobtown.ui.theme.SageGreenMain

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    currentUser: User?,
    onUpdateUser: (User) -> Unit,
    onLogout: () -> Unit
) {
    var nameInput by remember { mutableStateOf(currentUser?.name ?: "") }
    var emailInput by remember { mutableStateOf(currentUser?.email ?: "") }
    var feedbackMsg by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("User Profile", fontWeight = FontWeight.Bold, color = DeepGreenDark) },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back to Home", tint = DeepGreenDark)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SageGreenMain)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFFAFAFA))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // User Initial Icon
            Surface(
                shape = RoundedCornerShape(50.dp),
                color = SageGreenMain,
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = if (nameInput.isNotEmpty()) nameInput.take(1).uppercase() else "U",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepGreenDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it; feedbackMsg = "" },
                label = { Text("Display Name") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = emailInput,
                onValueChange = { emailInput = it; feedbackMsg = "" },
                label = { Text("Email Address") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Read-only Account Role Field
            OutlinedTextField(
                value = currentUser?.role?.replaceFirstChar { it.uppercase() } ?: "Seeker",
                onValueChange = {},
                readOnly = true,
                label = { Text("Account Role (Fixed)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = Color.Gray,
                    disabledBorderColor = Color.LightGray,
                    disabledLabelColor = Color.Gray
                ),
                enabled = false
            )

            if (feedbackMsg.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = feedbackMsg,
                    color = if (isError) Color.Red else Color(0xFF2E7D32),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val trimmedName = nameInput.trim()
                    val trimmedEmail = emailInput.trim()

                    if (trimmedName.isBlank() || trimmedEmail.isBlank()) {
                        isError = true
                        feedbackMsg = "Fields cannot be blank."
                    } else if (!Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
                        isError = true
                        feedbackMsg = "Please enter a valid email format."
                    } else if (currentUser != null) {
                        val updated = currentUser.copy(
                            name = trimmedName,
                            email = trimmedEmail
                        )
                        onUpdateUser(updated)
                        isError = false
                        feedbackMsg = "Profile successfully updated!"
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SageGreenMain)
            ) {
                Text("Save Changes", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DeepGreenDark)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.1f))
            ) {
                Text("Log Out", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Red)
            }
        }
    }
}