package com.example.jobtown.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jobtown.data.SupabaseClient
import com.example.jobtown.ui.theme.*
import com.example.jobtown.utils.ValidationUtils
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.launch

// Shown when the app is opened via the "Forgot Password" email link
// (jobtown://reset-password -- see AndroidManifest.xml + NavGraph.kt).
// By the time this screen is reached, handleDeeplinks(intent) in
// MainActivity has already parsed the recovery token from the link and
// established a temporary recovery session, so modifyUser() below is
// allowed to go through without the user needing to log in again first.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResetPasswordScreen(
    onPasswordUpdated: () -> Unit,
    onCancel: () -> Unit
) {
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    var newPasswordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundWhite)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .background(brush = Brush.verticalGradient(colors = listOf(SageGreenMain, SageGreenLight)))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Set New Password", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = DeepGreenDark)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Choose a new password for your account.",
                fontSize = 13.sp,
                color = DeepGreenDark.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = {
                            newPassword = it.take(ValidationUtils.PASSWORD_MAX_LENGTH)
                            newPasswordError = null
                            errorMessage = ""
                        },
                        label = { Text("New Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = SageGreenDark) },
                        trailingIcon = {
                            val image = if (newPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                            IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                                Icon(imageVector = image, contentDescription = "Toggle password visibility", tint = Color.Gray)
                            }
                        },
                        singleLine = true,
                        isError = newPasswordError != null,
                        supportingText = {
                            newPasswordError?.let { Text(text = it, color = Color.Red, fontSize = 12.sp) }
                        },
                        visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it.take(ValidationUtils.PASSWORD_MAX_LENGTH)
                            confirmPasswordError = null
                            errorMessage = ""
                        },
                        label = { Text("Confirm New Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = SageGreenDark) },
                        trailingIcon = {
                            val image = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(imageVector = image, contentDescription = "Toggle password visibility", tint = Color.Gray)
                            }
                        },
                        singleLine = true,
                        isError = confirmPasswordError != null,
                        supportingText = {
                            confirmPasswordError?.let { Text(text = it, color = Color.Red, fontSize = 12.sp) }
                        },
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )

                    if (errorMessage.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = errorMessage, color = Color.Red, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            // Basic local checks -- kept independent of
                            // ValidationUtils' signup/login-specific
                            // validators since we're not certain those cover
                            // this exact "new password" case.
                            val trimmedNew = newPassword.trim()
                            val trimmedConfirm = confirmPassword.trim()

                            newPasswordError = if (trimmedNew.length < 6) "Password must be at least 6 characters" else null
                            confirmPasswordError = if (trimmedConfirm != trimmedNew) "Passwords do not match" else null

                            if (newPasswordError != null || confirmPasswordError != null) {
                                return@Button
                            }

                            isSaving = true
                            errorMessage = ""
                            scope.launch {
                                try {
                                    SupabaseClient.client.auth.modifyUser {
                                        password = trimmedNew
                                    }
                                    isSaving = false
                                    onPasswordUpdated()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    isSaving = false
                                    errorMessage = "Couldn't update your password. The reset link may have expired -- please request a new one."
                                }
                            }
                        },
                        enabled = !isSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DeepGreenDark)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text("Update Password", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(
                        onClick = onCancel,
                        enabled = !isSaving,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancel", color = TextDark.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}