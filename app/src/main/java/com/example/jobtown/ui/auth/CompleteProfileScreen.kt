package com.example.jobtown.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jobtown.data.User

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompleteProfileScreen(
    user: User?,
    onComplete: (User) -> Unit
) {
    var phone by remember { mutableStateOf(user?.phone ?: "") }
    var location by remember { mutableStateOf(user?.location ?: "") }
    var skills by remember { mutableStateOf("") }
    var experienceLevel by remember { mutableStateOf("Junior") }
    var portfolioUrl by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf(user?.bio ?: "") }

    var expandedExperience by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Complete Your Profile") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Almost Done, ${user?.name ?: "User"}! ✨",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Provide additional details to enhance your profile and connect with the right opportunities.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone Number") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Location (City, Country)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            OutlinedTextField(
                value = skills,
                onValueChange = { skills = it },
                label = { Text("Key Skills (e.g., Kotlin, React, UI/UX)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            ExposedDropdownMenuBox(
                expanded = expandedExperience,
                onExpandedChange = { expandedExperience = !expandedExperience },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = experienceLevel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Experience Level") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedExperience) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                ExposedDropdownMenu(
                    expanded = expandedExperience,
                    onDismissRequest = { expandedExperience = false }
                ) {
                    listOf("Student / Entry", "Junior (1-2 yrs)", "Mid-Level (3-5 yrs)", "Senior (5+ yrs)").forEach { level ->
                        DropdownMenuItem(
                            text = { Text(level) },
                            onClick = {
                                experienceLevel = level
                                expandedExperience = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = portfolioUrl,
                onValueChange = { portfolioUrl = it },
                label = { Text("Portfolio / LinkedIn / GitHub URL") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it },
                label = { Text("Professional Summary / Bio") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    val updatedUser = (user ?: User()).copy(
                        phone = phone.trim(),
                        location = location.trim(),
                        bio = "Skills: $skills | Exp: $experienceLevel | Portfolio: $portfolioUrl\n$bio".trim()
                    )
                    onComplete(updatedUser)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = "Save & Continue to Home", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}