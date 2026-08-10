package com.example.jobtown.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jobtown.R
import com.example.jobtown.data.UserRole
import com.example.jobtown.ui.theme.*
import com.example.jobtown.utils.ValidationUtils

@Composable
fun SignUpScreen(
    onNextClick: (String, String, String, UserRole) -> Unit,
    onLoginClick: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var selectedRole by remember { mutableStateOf(UserRole.JOB_SEEKER) }
    val isEmployer = selectedRole == UserRole.EMPLOYER
    var errorMessage by remember { mutableStateOf("") }

    // Field-level validation errors, shown inline right under each input.
    var nameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundWhite)
    ) {
        // Top Gradient Header Background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .background(brush = Brush.verticalGradient(colors = listOf(SageGreenMain, SageGreenLight)))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            Surface(
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(22.dp)),
                color = Color.White,
                shadowElevation = 6.dp
            ) {
                Box(modifier = Modifier.padding(14.dp), contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_jobtown_logo),
                        contentDescription = "Logo",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Create Account",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = DeepGreenDark
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Full Name (Job Seeker) / Company Name (Employer) -- label,
                    // icon, keystroke filter and validation rule all switch
                    // based on the role picked below.
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = if (isEmployer) it.take(ValidationUtils.NAME_MAX_LENGTH) else ValidationUtils.filterNameInput(it)
                            nameError = null
                            errorMessage = ""
                        },
                        label = { Text(if (isEmployer) "Company Name" else "Full Name") },
                        leadingIcon = {
                            Icon(
                                imageVector = if (isEmployer) Icons.Default.Business else Icons.Default.Person,
                                contentDescription = null,
                                tint = SageGreenDark
                            )
                        },
                        singleLine = true,
                        isError = nameError != null,
                        supportingText = {
                            nameError?.let { Text(text = it, color = Color.Red, fontSize = 12.sp) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Email Input
                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it.take(ValidationUtils.EMAIL_MAX_LENGTH)
                            emailError = null
                            errorMessage = ""
                        },
                        label = { Text("Email Address") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = SageGreenDark) },
                        singleLine = true,
                        isError = emailError != null,
                        supportingText = {
                            emailError?.let { Text(text = it, color = Color.Red, fontSize = 12.sp) }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Password Input
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it.take(ValidationUtils.PASSWORD_MAX_LENGTH)
                            passwordError = null
                            // Re-check the confirm field live so a stale "match" error clears too.
                            if (confirmPassword.isNotEmpty()) {
                                confirmPasswordError = ValidationUtils.validateConfirmPassword(password, confirmPassword)
                            }
                            errorMessage = ""
                        },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = SageGreenDark) },
                        trailingIcon = {
                            val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(imageVector = image, contentDescription = "Toggle password visibility", tint = Color.Gray)
                            }
                        },
                        singleLine = true,
                        isError = passwordError != null,
                        supportingText = {
                            Text(
                                text = passwordError ?: "At least ${ValidationUtils.PASSWORD_MIN_LENGTH} characters, with a letter and a number.",
                                color = if (passwordError != null) Color.Red else TextDark.copy(alpha = 0.5f),
                                fontSize = 12.sp
                            )
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Confirm Password Input
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it.take(ValidationUtils.PASSWORD_MAX_LENGTH)
                            confirmPasswordError = null
                            errorMessage = ""
                        },
                        label = { Text("Confirm Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = SageGreenDark) },
                        trailingIcon = {
                            val image = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(imageVector = image, contentDescription = "Toggle confirm password visibility", tint = Color.Gray)
                            }
                        },
                        singleLine = true,
                        isError = confirmPasswordError != null,
                        supportingText = {
                            confirmPasswordError?.let { Text(text = it, color = Color.Red, fontSize = 12.sp) }
                        },
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Role Selector Header
                    Text("I am a:", fontWeight = FontWeight.Bold, color = DeepGreenDark, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))

                    // Role Selection Radio Options
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedRole = UserRole.JOB_SEEKER; nameError = null }
                                .padding(4.dp)
                        ) {
                            RadioButton(
                                selected = selectedRole == UserRole.JOB_SEEKER,
                                onClick = { selectedRole = UserRole.JOB_SEEKER; nameError = null },
                                colors = RadioButtonDefaults.colors(selectedColor = DeepGreenDark)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Job Seeker", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedRole = UserRole.EMPLOYER; nameError = null }
                                .padding(4.dp)
                        ) {
                            RadioButton(
                                selected = selectedRole == UserRole.EMPLOYER,
                                onClick = { selectedRole = UserRole.EMPLOYER; nameError = null },
                                colors = RadioButtonDefaults.colors(selectedColor = DeepGreenDark)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Employer", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                    }

                    if (errorMessage.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = errorMessage, color = Color.Red, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            val cleanName = name.trim()
                            val cleanEmail = email.trim().lowercase()

                            // Validate every field before advancing to the next step.
                            val nameValidation = if (isEmployer) {
                                ValidationUtils.validateCompanyName(cleanName)
                            } else {
                                ValidationUtils.validateFullName(cleanName)
                            }
                            val emailValidation = ValidationUtils.validateEmail(cleanEmail)
                            val passwordValidation = ValidationUtils.validateNewPassword(password)
                            val confirmValidation = ValidationUtils.validateConfirmPassword(password, confirmPassword)

                            nameError = nameValidation
                            emailError = emailValidation
                            passwordError = passwordValidation
                            confirmPasswordError = confirmValidation

                            if (nameValidation != null || emailValidation != null ||
                                passwordValidation != null || confirmValidation != null
                            ) {
                                errorMessage = "Please fix the highlighted fields before continuing."
                                return@Button
                            }

                            errorMessage = ""
                            onNextClick(cleanName, cleanEmail, password, selectedRole)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DeepGreenDark)
                    ) {
                        Text(text = "Next: Complete Profile", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.padding(bottom = 16.dp)) {
                Text(text = "Already have an account? ", color = TextDark.copy(alpha = 0.7f), fontSize = 14.sp)
                Text(
                    text = "Log In",
                    color = DeepGreenDark,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onLoginClick() }
                )
            }
        }
    }
}
