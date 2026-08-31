package com.example.jobtown.ui.auth

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.jobtown.data.SupabaseClient
import com.example.jobtown.data.model.User
import com.example.jobtown.data.model.UserRole
import com.example.jobtown.data.repository.UserRepository
import com.example.jobtown.ui.theme.*
import com.example.jobtown.utils.ValidationUtils
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

private val INDUSTRY_OPTIONS = listOf(
    "Technology / IT",
    "Finance / Banking",
    "Healthcare",
    "Retail / E-commerce",
    "Manufacturing",
    "Education",
    "Hospitality / F&B",
    "Construction / Real Estate",
    "Logistics / Transportation",
    "Media / Marketing",
    "Other"
)

private val COMPANY_SIZE_OPTIONS = listOf(
    "1-10 employees",
    "11-50 employees",
    "51-200 employees",
    "201-500 employees",
    "501-1000 employees",
    "1000+ employees"
)

private val DEFAULT_PERKS_OPTIONS = listOf(
    "Remote Friendly",
    "Health Insurance",
    "Flexible Hours",
    "401(k) Matching",
    "Learning Stipend",
    "Paid Time Off",
    "Parental Leave",
    "Performance Bonus"
)

// Uploads logo image bytes to the 'avatars' bucket in Supabase Storage
private suspend fun uploadCompanyLogoToSupabase(
    context: Context,
    userId: String,
    imageUri: Uri
): String? = withContext(Dispatchers.IO) {
    try {
        val inputStream = context.contentResolver.openInputStream(imageUri)
        val bytes = inputStream?.use { it.readBytes() } ?: return@withContext null
        val filePath = "logos/$userId.jpg"

        // Upload file to 'avatars' storage bucket
        SupabaseClient.client.storage.from("avatars").upload(filePath, bytes, upsert = true)

        // Retrieve public URL
        SupabaseClient.client.storage.from("avatars").publicUrl(filePath)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun isRateLimited(message: String): Boolean =
    message.contains("rate limit", ignoreCase = true) ||
        message.contains("over_email_send_rate_limit", ignoreCase = true)

private fun isAlreadyRegistered(message: String): Boolean =
    message.contains("already registered", ignoreCase = true) ||
        message.contains("already exists", ignoreCase = true) ||
        message.contains("user already", ignoreCase = true)

private fun isEmailNotConfirmed(message: String): Boolean =
    message.contains("email not confirmed", ignoreCase = true)

private fun friendlyAuthError(error: Throwable): String {
    val message = error.message.orEmpty()
    return when {
        isRateLimited(message) ->
            "Too many confirmation emails were sent for this address. Wait a few minutes, then tap Save again. If you already have an account, go back and log in."
        isEmailNotConfirmed(message) ->
            "Please open the confirmation link we emailed you, then tap Save again."
        isAlreadyRegistered(message) ->
            "This email is already registered. Go back and log in."
        message.contains("invalid login", ignoreCase = true) ||
            message.contains("invalid credentials", ignoreCase = true) ->
            "Could not sign in with this email and password. Go back and check your signup details."
        // Keep short messages we threw ourselves; never dump the raw HTTP request
        // (it includes API keys and is what showed up as the red wall of text).
        message.isNotBlank() &&
            message.length < 220 &&
            !message.contains("Request:", ignoreCase = true) &&
            !message.contains("Headers:", ignoreCase = true) ->
            message
        else -> "Could not create your account. Please try again."
    }
}

/**
 * Creates or restores the Auth session without sending extra confirmation emails.
 *
 * Save used to call signUpWith() on every tap. Each call emails the user, and
 * after a few taps Supabase returns "email rate limit exceeded". Retrying Save
 * (or a previous attempt that created Auth but failed later) must sign in first.
 */
private suspend fun ensureAuthSession(email: String, password: String) {
    val current = SupabaseClient.client.auth.currentUserOrNull()
    if (current != null && current.email.equals(email, ignoreCase = true)) {
        return
    }

    try {
        SupabaseClient.client.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
        return
    } catch (signInError: Exception) {
        val message = signInError.message.orEmpty()
        if (isEmailNotConfirmed(message)) {
            throw Exception(
                "Please open the confirmation link we emailed you, then tap Save again.",
                signInError
            )
        }
        // Invalid credentials usually means the account does not exist yet.
        // Fall through to signup. Do not signup again if the email is confirmed
        // as already taken — that would send another email.
        if (isAlreadyRegistered(message) || isRateLimited(message)) {
            throw signInError
        }
    }

    try {
        SupabaseClient.client.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
    } catch (signUpError: Exception) {
        val message = signUpError.message.orEmpty()
        if (!isAlreadyRegistered(message) && !isRateLimited(message)) {
            throw signUpError
        }
        try {
            SupabaseClient.client.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
        } catch (retryError: Exception) {
            if (isRateLimited(message) || isRateLimited(retryError.message.orEmpty())) {
                throw Exception(
                    "Too many confirmation emails were sent for this address. Wait a few minutes, then tap Save again. If you already have an account, go back and log in.",
                    retryError
                )
            }
            throw retryError
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CompleteProfileScreen(
    user: User?,
    draft: SignUpFields,
    onDraftChange: (SignUpFields) -> Unit,
    onBack: () -> Unit,
    onComplete: (User) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var expandedExperience by remember { mutableStateOf(false) }
    var expandedCompanySize by remember { mutableStateOf(false) }
    var expandedIndustry by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // Employer Specific Local States
    var companyTagline by remember { mutableStateOf("") }
    var selectedPerks by remember { mutableStateOf(setOf<String>()) }
    var logoUri by remember { mutableStateOf<Uri?>(null) }

    val logoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        logoUri = uri
    }

    // Validation Errors
    var phoneError by remember { mutableStateOf<String?>(null) }
    var locationError by remember { mutableStateOf<String?>(null) }
    var skillsError by remember { mutableStateOf<String?>(null) }
    var portfolioUrlError by remember { mutableStateOf<String?>(null) }
    var bioError by remember { mutableStateOf<String?>(null) }

    val isEmployer = user?.role == UserRole.EMPLOYER
    val displayName = (if (isEmployer) user?.companyName else user?.name)?.ifBlank { null } ?: "User"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Complete Your Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
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
                text = if (isEmployer)
                    "Set up your official company profile to start posting jobs and connecting with top candidates."
                else
                    "Provide additional details to enhance your profile and connect with the right opportunities.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Company Logo Upload Section (Employer Only)
            if (isEmployer) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Company Logo",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Box(
                            contentAlignment = Alignment.BottomEnd,
                            modifier = Modifier.size(90.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = SageGreenLight,
                                border = androidx.compose.foundation.BorderStroke(2.dp, SageGreenDark),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable { logoPickerLauncher.launch("image/*") }
                            ) {
                                if (logoUri != null) {
                                    AsyncImage(
                                        model = logoUri,
                                        contentDescription = "Company Logo",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.Business,
                                            contentDescription = null,
                                            tint = DeepGreenDark,
                                            modifier = Modifier.size(40.dp)
                                        )
                                    }
                                }
                            }

                            Surface(
                                shape = CircleShape,
                                color = DeepGreenDark,
                                modifier = Modifier
                                    .size(28.dp)
                                    .clickable {
                                        if (logoUri != null) logoUri = null else logoPickerLauncher.launch("image/*")
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (logoUri != null) Icons.Filled.Close else Icons.Filled.AddPhotoAlternate,
                                        contentDescription = "Upload Logo",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (logoUri != null) "Tap to change logo" else "Tap to upload company logo",
                            fontSize = 11.sp,
                            color = TextDark.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // Contact Phone Input
            OutlinedTextField(
                value = draft.phone,
                onValueChange = {
                    onDraftChange(draft.copy(phone = ValidationUtils.filterPhoneInput(it)))
                    phoneError = null
                    errorMessage = ""
                },
                label = { Text(if (isEmployer) "Company Contact Number" else "Phone Number") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                isError = phoneError != null,
                supportingText = {
                    phoneError?.let { Text(text = it, color = Color.Red, fontSize = 12.sp) }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Location Input - Country + City picker. Employers can add more than one
            // branch/office address; all are stored together in draft.location.
            Text(
                text = if (isEmployer) "Company Location" else "Location",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = TextDark.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxWidth()
            )
            com.example.jobtown.ui.components.LocationPicker(
                locationString = draft.location,
                onLocationStringChange = {
                    onDraftChange(draft.copy(location = it))
                    locationError = null
                    errorMessage = ""
                },
                allowMultipleBranches = isEmployer,
                errorText = locationError,
                modifier = Modifier.fillMaxWidth()
            )

            if (isEmployer) {
                // Tagline
                OutlinedTextField(
                    value = companyTagline,
                    onValueChange = { companyTagline = it.take(100) },
                    label = { Text("Company Tagline / One-liner") },
                    placeholder = { Text("e.g. Building the next generation of mobile experiences") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Industry Dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedIndustry,
                    onExpandedChange = { expandedIndustry = !expandedIndustry },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = draft.industry,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Industry") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedIndustry) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(expanded = expandedIndustry, onDismissRequest = { expandedIndustry = false }) {
                        INDUSTRY_OPTIONS.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    onDraftChange(draft.copy(industry = option))
                                    expandedIndustry = false
                                }
                            )
                        }
                    }
                }

                // Company Size Dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedCompanySize,
                    onExpandedChange = { expandedCompanySize = !expandedCompanySize },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = draft.companySize,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Company Size") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCompanySize) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(expanded = expandedCompanySize, onDismissRequest = { expandedCompanySize = false }) {
                        COMPANY_SIZE_OPTIONS.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    onDraftChange(draft.copy(companySize = option))
                                    expandedCompanySize = false
                                }
                            )
                        }
                    }
                }

                // Perks Chips Selection
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Company Perks & Benefits",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Text(
                            text = "Select perks to highlight on your company profile:",
                            fontSize = 12.sp,
                            color = TextDark.copy(alpha = 0.6f)
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            DEFAULT_PERKS_OPTIONS.forEach { perk ->
                                val isSelected = selectedPerks.contains(perk)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedPerks = if (isSelected) selectedPerks - perk else selectedPerks + perk
                                    },
                                    label = { Text(perk, fontSize = 12.sp) },
                                    leadingIcon = if (isSelected) {
                                        { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                    } else null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = SageGreenMain,
                                        selectedLabelColor = DeepGreenDark
                                    )
                                )
                            }
                        }
                    }
                }
            } else {
                // Job Seeker Skills
                OutlinedTextField(
                    value = draft.skills,
                    onValueChange = {
                        onDraftChange(draft.copy(skills = it.take(ValidationUtils.SKILLS_MAX_LENGTH)))
                        skillsError = null
                        errorMessage = ""
                    },
                    label = { Text("Key Skills (e.g., Kotlin, React, UI/UX)") },
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

                // Job Seeker Experience Level
                ExposedDropdownMenuBox(
                    expanded = expandedExperience,
                    onExpandedChange = { expandedExperience = !expandedExperience },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = draft.experienceLevel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Experience Level") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedExperience) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
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
            }

            // Portfolio / Website URL
            OutlinedTextField(
                value = draft.portfolioUrl,
                onValueChange = {
                    onDraftChange(draft.copy(portfolioUrl = it.take(ValidationUtils.URL_MAX_LENGTH)))
                    portfolioUrlError = null
                    errorMessage = ""
                },
                label = { Text(if (isEmployer) "Company Website URL" else "Portfolio / LinkedIn / GitHub URL") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                isError = portfolioUrlError != null,
                supportingText = {
                    portfolioUrlError?.let { Text(text = it, color = Color.Red, fontSize = 12.sp) }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Bio / Description
            OutlinedTextField(
                value = draft.bio,
                onValueChange = {
                    onDraftChange(draft.copy(bio = it.take(ValidationUtils.BIO_MAX_LENGTH)))
                    bioError = null
                    errorMessage = ""
                },
                label = { Text(if (isEmployer) "Company Description & Mission" else "Professional Summary / Bio") },
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
                    .height(120.dp),
                shape = RoundedCornerShape(12.dp)
            )

            if (errorMessage.isNotBlank()) {
                Text(text = errorMessage, color = Color.Red, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Save / Submit Button
            Button(
                onClick = {
                    if (user == null) {
                        errorMessage = "User information is missing. Please restart signup."
                        return@Button
                    }

                    val phoneValidation = ValidationUtils.validatePhone(draft.phone, required = true)
                    val locationValidation = ValidationUtils.validateLocation(draft.location, required = true)
                    val skillsValidation = if (!isEmployer) ValidationUtils.validateSkills(draft.skills) else null
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
                            // 1. Sign in if the Auth user already exists (from a previous
                            // Save tap). Only sign up when there is no account yet — this
                            // avoids "email rate limit exceeded" from extra confirmation emails.
                            ensureAuthSession(user.email.trim().lowercase(), user.password)

                            val currentAuthUser = SupabaseClient.client.auth.currentUserOrNull()
                            val finalUserId = currentAuthUser?.id.orEmpty()
                            if (finalUserId.isBlank()) {
                                isLoading = false
                                errorMessage = "Could not create your account session. Please try again, or confirm your email first."
                                return@launch
                            }

                            var uploadedLogoUrl: String? = null
                            if (isEmployer && logoUri != null) {
                                uploadedLogoUrl = uploadCompanyLogoToSupabase(context, finalUserId, logoUri!!)
                            }

                            val newUser = user.copy(
                                id = finalUserId,
                                email = user.email.trim().lowercase(),
                                phone = draft.phone.trim(),
                                location = draft.location.trim(),
                                bio = draft.bio.trim(),
                                companySize = draft.companySize.trim(),
                                industry = draft.industry.trim(),
                                tagline = companyTagline.trim(),
                                websiteUrl = draft.portfolioUrl.trim(),
                                perks = selectedPerks.toList(),
                                skills = draft.skills.trim(),
                                experienceLevel = draft.experienceLevel.trim(),
                                portfolioUrl = draft.portfolioUrl.trim(),
                                avatarUrl = uploadedLogoUrl ?: user.avatarUrl,
                                createdAt = Clock.System.now().toString()
                            )

                            // 6. Persist everything to the 'users' table (which already contains profile fields)
                            val isUserSaved = UserRepository.saveUserToSupabase(newUser)

                            isLoading = false
                            if (isUserSaved) {
                                onComplete(newUser)
                            } else {
                                errorMessage = UserRepository.lastUserSaveError ?: "Failed to save profile details. Please try again."
                            }
                        } catch (e: Exception) {
                            isLoading = false
                            errorMessage = friendlyAuthError(e)
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