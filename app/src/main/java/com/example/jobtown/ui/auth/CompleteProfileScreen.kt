package com.example.jobtown.ui.auth

import androidx.compose.foundation.background
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
import androidx.navigation.NavController
import com.example.jobtown.Screen
import com.example.jobtown.data.User
import com.example.jobtown.ui.theme.DarkTextPurple
import com.example.jobtown.ui.theme.DeepGreenDark
import com.example.jobtown.ui.theme.SageGreenMain

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompleteProfileScreen(
    navController: NavController,
    currentUser: User?,
    onUpdateUser: (User) -> Unit
) {
    var phone by remember(currentUser) { mutableStateOf(currentUser?.phone ?: "") }
    var skillsOrIndustry by remember(currentUser) {
        mutableStateOf(if (currentUser?.role == "company") currentUser?.industry ?: "" else currentUser?.skills ?: "")
    }
    var bio by remember(currentUser) { mutableStateOf(currentUser?.bio ?: "") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Complete Your Profile",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepGreenDark
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (currentUser?.role == "company") "Set up your company overview" else "Tell recruiters a bit more about yourself",
                    fontSize = 14.sp,
                    color = DarkTextPurple
                )

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = skillsOrIndustry,
                    onValueChange = { skillsOrIndustry = it },
                    label = { Text(if (currentUser?.role == "company") "Industry / Company Type" else "Key Skills") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text(if (currentUser?.role == "company") "Company Overview" else "Short Bio") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (currentUser != null) {
                            val updatedUser = if (currentUser.role == "company") {
                                currentUser.copy(
                                    phone = phone.trim(),
                                    industry = skillsOrIndustry.trim(),
                                    bio = bio.trim()
                                )
                            } else {
                                currentUser.copy(
                                    phone = phone.trim(),
                                    skills = skillsOrIndustry.trim(),
                                    bio = bio.trim()
                                )
                            }
                            onUpdateUser(updatedUser)
                        }
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.CompleteProfile.route) { inclusive = true }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SageGreenMain)
                ) {
                    Text("Save & Continue", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DeepGreenDark)
                }
            }
        }
    }
}