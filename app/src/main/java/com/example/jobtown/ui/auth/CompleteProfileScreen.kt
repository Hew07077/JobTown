package com.example.jobtown.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jobtown.data.SupabaseClient
import com.example.jobtown.data.User
import com.example.jobtown.data.repository.UserRepository
import com.example.jobtown.ui.theme.*
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompleteProfileScreen(
    user: User?,
    onComplete: (User) -> Unit
) {
    var phone by remember { mutableStateOf(user?.phone ?: "") }
    var location by remember { mutableStateOf(user?.location ?: "") }
    var skills by remember { mutableStateOf("") }
    var experienceLevel by remember { mutableStateOf("Junior (1-2 yrs)") }
    var portfolioUrl by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf(user?.bio ?: "") }

    var expandedExperience by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Complete Your Profile", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SageGreenMain,
                    titleContentColor = DeepGreenDark
                )
            )
        },
        containerColor = BackgroundWhite
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
                fontWeight = FontWeight.Bold,
                color = DeepGreenDark
            )

            Text(
                text = "Provide additional details to enhance your profile and connect with the right opportunities.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Phone Input
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it; errorMessage = "" },
                label = { Text("Phone Number") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Location Input
            OutlinedTextField(
                value = location,
                onValueChange = { location = it; errorMessage = "" },
                label = { Text("Location (City, Country)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Skills Input
            OutlinedTextField(
                value = skills,
                onValueChange = { skills = it; errorMessage = "" },
                label = { Text("Key Skills (e.g., Kotlin, React, UI/UX)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Experience Dropdown Box
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

            // Portfolio URL Input
            OutlinedTextField(
                value = portfolioUrl,
                onValueChange = { portfolioUrl = it; errorMessage = "" },
                label = { Text("Portfolio / LinkedIn / GitHub URL") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Bio Summary Input
            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it; errorMessage = "" },
                label = { Text("Professional Summary / Bio") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                shape = RoundedCornerShape(12.dp)
            )

            if (errorMessage.isNotBlank()) {
                Text(text = errorMessage, color = Color.Red, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Complete Registration Button
            Button(
                onClick = {
                    if (user == null) {
                        errorMessage = "User information is missing. Please restart signup."
                        return@Button
                    }

                    isLoading = true
                    errorMessage = ""

                    scope.launch {
                        try {
                            // 1. Sign up the user into Supabase Auth
                            SupabaseClient.client.auth.signUpWith(Email) {
                                email = user.email
                                password = user.password
                            }

                            // 2. Fetch authenticated Supabase user ID or fall back to user email
                            val currentAuthUser = SupabaseClient.client.auth.currentUserOrNull()
                            val finalUserId = currentAuthUser?.id ?: user.id

                            // 3. Construct formatted user object
                            val compiledBio = buildString {
                                if (skills.isNotBlank()) append("Skills: ${skills.trim()}\n")
                                if (experienceLevel.isNotBlank()) append("Exp: $experienceLevel\n")
                                if (portfolioUrl.isNotBlank()) append("Portfolio: ${portfolioUrl.trim()}\n")
                                if (bio.isNotBlank()) append("\n${bio.trim()}")
                            }.trim()

                            val newUser = user.copy(
                                id = finalUserId,
                                phone = phone.trim(),
                                location = location.trim(),
                                bio = compiledBio
                            )

                            // 4. Persist user data in PostgreSQL database table
                            val isSaved = UserRepository.saveUserToSupabase(newUser)

                            isLoading = false
                            if (isSaved) {
                                onComplete(newUser)
                            } else {
                                errorMessage = "Failed to save profile details. Please try again."
                            }
                        } catch (e: Exception) {
                            isLoading = false
                            errorMessage = e.message ?: "An unexpected error occurred."
                        }
                    }
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DeepGreenDark)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text(text = "Save & Continue to Home", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}