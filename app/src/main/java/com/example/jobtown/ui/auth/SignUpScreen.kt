package com.example.jobtown.ui.auth

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RadioButtonUnchecked
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
import com.example.jobtown.data.model.UserRole
import com.example.jobtown.ui.theme.*
import com.example.jobtown.utils.ValidationUtils

data class SignUpFields(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val role: UserRole = UserRole.JOB_SEEKER,
    val phone: String = "",
    val location: String = "",
    val skills: String = "",
    val experienceLevel: String = "Junior (1-2 yrs)",
    val portfolioUrl: String = "",
    val bio: String = "",
    val companySize: String = "",
    val industry: String = ""
)

enum class PasswordStrength(val label: String, val color: Color, val progress: Float) {
    EMPTY("", Color.Transparent, 0f),
    WEAK("Weak", Color(0xFFE53935), 0.33f),
    MEDIUM("Medium", Color(0xFFFB8C00), 0.66f),
    STRONG("Strong", Color(0xFF43A047), 1.0f)
}

private fun calculatePasswordStrength(password: String): PasswordStrength {
    if (password.isEmpty()) return PasswordStrength.EMPTY

    var score = 0
    if (password.length >= ValidationUtils.PASSWORD_MIN_LENGTH) score++
    if (password.any { it.isDigit() }) score++
    if (password.any { it.isLetter() }) score++
    if (password.any { !it.isLetterOrDigit() }) score++

    return when {
        score <= 2 -> PasswordStrength.WEAK
        score == 3 -> PasswordStrength.MEDIUM
        else -> PasswordStrength.STRONG
    }
}

@Composable
fun SignUpScreen(
    draft: SignUpFields,
    onDraftChange: (SignUpFields) -> Unit,
    onNextClick: () -> Unit,
    onLoginClick: () -> Unit
) {
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // Field-level validation errors, shown inline right under each input.
    var nameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }

    val isEmployer = draft.role == UserRole.EMPLOYER
    val scrollState = rememberScrollState()

    val passwordStrength = remember(draft.password) {
        calculatePasswordStrength(draft.password)
    }

    val animatedProgress by animateFloatAsState(
        targetValue = passwordStrength.progress,
        label = "PasswordStrengthProgress"
    )
    val animatedStrengthColor by animateColorAsState(
        targetValue = passwordStrength.color,
        label = "PasswordStrengthColor"
    )

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
                    // Full Name / Company Name Input
                    OutlinedTextField(
                        value = draft.name,
                        onValueChange = {
                            val filtered = if (isEmployer) it.take(ValidationUtils.NAME_MAX_LENGTH) else ValidationUtils.filterNameInput(it)
                            onDraftChange(draft.copy(name = filtered))
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
                        value = draft.email,
                        onValueChange = {
                            onDraftChange(draft.copy(email = it.take(ValidationUtils.EMAIL_MAX_LENGTH)))
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
                        value = draft.password,
                        onValueChange = {
                            val newPassword = it.take(ValidationUtils.PASSWORD_MAX_LENGTH)
                            onDraftChange(draft.copy(password = newPassword))
                            passwordError = null
                            if (draft.confirmPassword.isNotEmpty()) {
                                confirmPasswordError = ValidationUtils.validateConfirmPassword(newPassword, draft.confirmPassword)
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
                            if (passwordError != null) {
                                Text(text = passwordError!!, color = Color.Red, fontSize = 12.sp)
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )

                    // Password Strength Indicator & Tips Section
                    if (draft.password.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Password Strength:",
                                fontSize = 12.sp,
                                color = TextDark.copy(alpha = 0.7f)
                            )
                            Text(
                                text = passwordStrength.label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = animatedStrengthColor
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = animatedStrengthColor,
                            trackColor = Color.LightGray.copy(alpha = 0.4f)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Password Requirement Tips Checklist
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(start = 4.dp)
                        ) {
                            PasswordTipItem(
                                text = "At least ${ValidationUtils.PASSWORD_MIN_LENGTH} characters",
                                isMet = draft.password.length >= ValidationUtils.PASSWORD_MIN_LENGTH,
                                activeColor = animatedStrengthColor
                            )
                            PasswordTipItem(
                                text = "Contains a letter and a number",
                                isMet = draft.password.any { it.isLetter() } && draft.password.any { it.isDigit() },
                                activeColor = animatedStrengthColor
                            )
                            PasswordTipItem(
                                text = "Contains a special character (!@#$%...)",
                                isMet = draft.password.any { !it.isLetterOrDigit() },
                                activeColor = animatedStrengthColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Confirm Password Input
                    OutlinedTextField(
                        value = draft.confirmPassword,
                        onValueChange = {
                            onDraftChange(draft.copy(confirmPassword = it.take(ValidationUtils.PASSWORD_MAX_LENGTH)))
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
                                .clickable { onDraftChange(draft.copy(role = UserRole.JOB_SEEKER)); nameError = null }
                                .padding(4.dp)
                        ) {
                            RadioButton(
                                selected = draft.role == UserRole.JOB_SEEKER,
                                onClick = { onDraftChange(draft.copy(role = UserRole.JOB_SEEKER)); nameError = null },
                                colors = RadioButtonDefaults.colors(selectedColor = DeepGreenDark)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Job Seeker", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onDraftChange(draft.copy(role = UserRole.EMPLOYER)); nameError = null }
                                .padding(4.dp)
                        ) {
                            RadioButton(
                                selected = draft.role == UserRole.EMPLOYER,
                                onClick = { onDraftChange(draft.copy(role = UserRole.EMPLOYER)); nameError = null },
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
                            val cleanName = draft.name.trim()
                            val cleanEmail = draft.email.trim().lowercase()

                            val nameValidation = if (isEmployer) {
                                ValidationUtils.validateCompanyName(cleanName)
                            } else {
                                ValidationUtils.validateFullName(cleanName)
                            }
                            val emailValidation = ValidationUtils.validateEmail(cleanEmail)
                            val passwordValidation = ValidationUtils.validateNewPassword(draft.password)
                            val confirmValidation = ValidationUtils.validateConfirmPassword(draft.password, draft.confirmPassword)

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
                            onDraftChange(draft.copy(name = cleanName, email = cleanEmail))
                            onNextClick()
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

@Composable
private fun PasswordTipItem(text: String, isMet: Boolean, activeColor: Color) {
    val icon = if (isMet) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked
    val color = if (isMet) activeColor else TextDark.copy(alpha = 0.4f)

    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            fontSize = 11.sp,
            color = color
        )
    }
}