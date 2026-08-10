package com.example.jobtown.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
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
import com.example.jobtown.data.SupabaseClient
import com.example.jobtown.data.User
import com.example.jobtown.data.repository.UserRepository
import com.example.jobtown.ui.theme.*
import com.example.jobtown.utils.ValidationUtils
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: (User) -> Unit,
    onSignUpClick: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // Field-level validation errors, shown inline right under each input.
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

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

            Text("Welcome Back", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = DeepGreenDark)

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
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

                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it.take(ValidationUtils.PASSWORD_MAX_LENGTH)
                            passwordError = null
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
                            passwordError?.let { Text(text = it, color = Color.Red, fontSize = 12.sp) }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
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
                            // Validate every field before we ever touch the network.
                            val emailValidation = ValidationUtils.validateEmail(email)
                            val passwordValidation = ValidationUtils.validateLoginPassword(password)
                            emailError = emailValidation
                            passwordError = passwordValidation

                            if (emailValidation != null || passwordValidation != null) {
                                return@Button
                            }

                            isLoading = true
                            errorMessage = ""
                            scope.launch {
                                try {
                                    // 1. Authenticate with Supabase
                                    SupabaseClient.client.auth.signInWith(Email) {
                                        this.email = email.trim()
                                        this.password = password.trim()
                                    }

                                    // 2. Fetch authenticated user details safely
                                    val currentAuthUser = SupabaseClient.client.auth.currentUserOrNull()

                                    // 3. Pull the real profile row (name, phone, location, bio,
                                    //    role, etc.) from the "users" table instead of guessing
                                    //    one locally -- this is what was previously missing.
                                    val storedUser = UserRepository.findUserByEmail(email.trim())

                                    val authenticatedUser = storedUser?.copy(
                                        id = currentAuthUser?.id ?: storedUser.id,
                                        password = password
                                    ) ?: User(
                                        // Fallback only if no matching row exists yet in the
                                        // database (e.g. account created outside the normal
                                        // signup flow).
                                        id = currentAuthUser?.id ?: "user_${email.hashCode()}",
                                        name = email.substringBefore("@"),
                                        email = email.trim(),
                                        password = password
                                    )

                                    isLoading = false
                                    onLoginSuccess(authenticatedUser)

                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    isLoading = false
                                    // 3. User-friendly error message (prevents raw stack traces)
                                    errorMessage = "Invalid email or password. Please try again."
                                }
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DeepGreenDark)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text("Log In", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row {
                Text(text = "Don't have an account? ", color = TextDark.copy(alpha = 0.7f), fontSize = 14.sp)
                Text(
                    text = "Sign Up",
                    color = DeepGreenDark,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onSignUpClick() }
                )
            }
        }
    }
}
