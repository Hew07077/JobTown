package com.example.jobtown.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.jobtown.data.UserRole
import com.example.jobtown.data.repository.UserRepository
import com.example.jobtown.ui.theme.*
import com.example.jobtown.utils.ValidationUtils
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompleteProfileScreen(
    user: User?,
    draft: SignUpFields,
    onDraftChange: (SignUpFields) -> Unit,
    onBack: () -> Unit,
    onComplete: (User) -> Unit
) {
    var expandedExperience by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // Field-level validation errors, shown inline right under each input.
    var phoneError by remember { mutableStateOf<String?>(null) }
    var locationError by remember { mutableStateOf<String?>(null) }
    var skillsError by remember { mutableStateOf<String?>(null) }
    var portfolioUrlError by remember { mutableStateOf<String?>(null) }
    var bioError by remember { mutableStateOf<String?>(null) }

    val isEmployer = user?.role == UserRole.EMPLOYER
    val displayName = (if (isEmployer) user?.companyName else user?.name)?.ifBlank { null } ?: "User"

    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Complete Your Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    // Goes back to SignUpScreen. The shared draft isn't touched,
                    // so anything already typed on this screen is still there
                    // if the user comes forward again.
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = DeepGreenDark
                        )
                    }
                },
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
                text = "Almost Done, $displayName! ✨",
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

            // Phone Input -- digits only (optional leading +), 7-15 digits
            OutlinedTextField(
                value = draft.phone,
                onValueChange = {
                    onDraftChange(draft.copy(phone = ValidationUtils.filterPhoneInput(it)))
                    phoneError = null
                    errorMessage = ""
                },
                label = { Text("Phone Number") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                isError = phoneError != null,
                supportingText = {
                    phoneError?.let { Text(text = it, color = Color.Red, fontSize = 12.sp) }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Location Input
            OutlinedTextField(
                value = draft.location,
                onValueChange = {
                    onDraftChange(draft.copy(location = it.take(ValidationUtils.LOCATION_MAX_LENGTH)))
                    locationError = null
                    errorMessage = ""
                },
                label = { Text(if (isEmployer) "Company Location (City, Country)" else "Location (City, Country)") },
                isError = locationError != null,
                supportingText = {
                    locationError?.let { Text(text = it, color = Color.Red, fontSize = 12.sp) }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Skills Input
            OutlinedTextField(
                value = draft.skills,
                onValueChange = {
                    onDraftChange(draft.copy(skills = it.take(ValidationUtils.SKILLS_MAX_LENGTH)))
                    skillsError = null
                    errorMessage = ""
                },
                label = { Text(if (isEmployer) "Key Skills Needed (e.g., Kotlin, React, UI/UX)" else "Key Skills (e.g., Kotlin, React, UI/UX)") },
                isError = skillsError != null,
                supportingText = {
                    Text(
                        text = skillsError ?: "${draft.skills.length}/${ValidationUtils.SKILLS_MAX_LENGTH}",
                        color = if (skillsError != null) Color.Red else TextDark.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )
                },
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
                    value = draft.experienceLevel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(if (isEmployer) "Minimum Experience Level" else "Experience Level") },
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
                                onDraftChange(draft.copy(experienceLevel = level))
                                expandedExperience = false
                            }
                        )
                    }
                }
            }

            // Portfolio URL Input -- optional, but must be a valid URL if provided
            OutlinedTextField(
                value = draft.portfolioUrl,
                onValueChange = {
                    onDraftChange(draft.copy(portfolioUrl = it.take(ValidationUtils.URL_MAX_LENGTH)))
                    portfolioUrlError = null
                    errorMessage = ""
                },
                label = { Text(if (isEmployer) "Company Website / LinkedIn URL" else "Portfolio / LinkedIn / GitHub URL") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                isError = portfolioUrlError != null,
                supportingText = {
                    portfolioUrlError?.let { Text(text = it, color = Color.Red, fontSize = 12.sp) }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Bio Summary Input
            OutlinedTextField(
                value = draft.bio,
                onValueChange = {
                    onDraftChange(draft.copy(bio = it.take(ValidationUtils.BIO_MAX_LENGTH)))
                    bioError = null
                    errorMessage = ""
                },
                label = { Text(if (isEmployer) "Company Description" else "Professional Summary / Bio") },
                isError = bioError != null,
                supportingText = {
                    Text(
                        text = bioError ?: "${draft.bio.length}/${ValidationUtils.BIO_MAX_LENGTH}",
                        color = if (bioError != null) Color.Red else TextDark.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )
                },
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

                    // Validate every field before hitting the network.
                    val phoneValidation = ValidationUtils.validatePhone(draft.phone, required = true)
                    val locationValidation = ValidationUtils.validateLocation(draft.location, required = true)
                    val skillsValidation = ValidationUtils.validateSkills(draft.skills)
                    val portfolioValidation = ValidationUtils.validatePortfolioUrl(draft.portfolioUrl)
                    val bioValidation = ValidationUtils.validateBio(draft.bio)

                    phoneError = phoneValidation
                    locationError = locationValidation
                    skillsError = skillsValidation
                    portfolioUrlError = portfolioValidation
                    bioError = bioValidation

                    if (phoneValidation != null || locationValidation != null ||
                        skillsValidation != null || portfolioValidation != null || bioValidation != null
                    ) {
                        errorMessage = "Please fix the highlighted fields before continuing."
                        return@Button
                    }

                    isLoading = true
                    errorMessage = ""

                    scope.launch {
                        try {
                            // 1. Sign up the user into Supabase Auth. If this email was
                            //    already registered in Auth from a previous attempt that
                            //    crashed/errored before the profile row got saved, fall
                            //    back to signing in instead of hard-failing here.
                            try {
                                SupabaseClient.client.auth.signUpWith(Email) {
                                    email = user.email
                                    password = user.password
                                }
                            } catch (signUpError: Exception) {
                                val message = signUpError.message ?: ""
                                val alreadyRegistered = message.contains("already registered", ignoreCase = true) ||
                                        message.contains("already exists", ignoreCase = true)

                                if (alreadyRegistered) {
                                    SupabaseClient.client.auth.signInWith(Email) {
                                        email = user.email
                                        password = user.password
                                    }
                                } else {
                                    throw signUpError
                                }
                            }

                            // 2. Fetch authenticated Supabase user ID or fall back to user email
                            val currentAuthUser = SupabaseClient.client.auth.currentUserOrNull()
                            val finalUserId = currentAuthUser?.id ?: user.id

                            // 3. Construct formatted user object
                            val compiledBio = buildString {
                                if (draft.skills.isNotBlank()) append("Skills: ${draft.skills.trim()}\n")
                                if (draft.experienceLevel.isNotBlank()) append("Exp: ${draft.experienceLevel}\n")
                                if (draft.portfolioUrl.isNotBlank()) append("Portfolio: ${draft.portfolioUrl.trim()}\n")
                                if (draft.bio.isNotBlank()) append("\n${draft.bio.trim()}")
                            }.trim()

                            val newUser = user.copy(
                                id = finalUserId,
                                phone = draft.phone.trim(),
                                location = draft.location.trim(),
                                bio = compiledBio,
                                // The "created_at" column is timestamptz -- an empty
                                // string (the User model's default) would fail to
                                // insert, so stamp it with the real current time here.
                                createdAt = Clock.System.now().toString()
                            )

                            // 4. Persist user data in PostgreSQL database table
                            val isSaved = UserRepository.saveUserToSupabase(newUser)

                            isLoading = false
                            if (isSaved) {
                                onComplete(newUser)
                            } else {
                                errorMessage = "Failed to save profile details. This is often caused by a Row Level Security (RLS) policy on the \"users\" table blocking the write — check Supabase Logs for the exact error, or ask a teammate with dashboard access."
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