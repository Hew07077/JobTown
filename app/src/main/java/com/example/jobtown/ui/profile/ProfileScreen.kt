package com.example.jobtown.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.jobtown.data.User
import com.example.jobtown.data.UserRole
import com.example.jobtown.data.repository.UserRepository
import com.example.jobtown.ui.theme.*
import com.example.jobtown.utils.ValidationUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    currentUser: User? = null,
    onLogout: () -> Unit = {},
    onProfileUpdated: (User) -> Unit = {}
) {
    // Local copy so edits reflect immediately without requiring the parent
    // screen / nav graph to be touched.
    var displayedUser by remember(currentUser) { mutableStateOf(currentUser) }
    val isEmployer = displayedUser?.role == UserRole.EMPLOYER

    var isEditing by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var saveErrorMessage by remember { mutableStateOf("") }

    // Editable fields, seeded from the current user each time edit mode opens.
    var editName by remember { mutableStateOf(displayedUser?.name ?: "") }
    var editPhone by remember { mutableStateOf(displayedUser?.phone ?: "") }
    var editLocation by remember { mutableStateOf(displayedUser?.location ?: "") }

    // Field-level validation errors, shown inline right under each input.
    var nameError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var locationError by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    fun startEditing() {
        editName = displayedUser?.name ?: ""
        editPhone = displayedUser?.phone ?: ""
        editLocation = displayedUser?.location ?: ""
        nameError = null
        phoneError = null
        locationError = null
        saveErrorMessage = ""
        isEditing = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Profile",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = DeepGreenDark
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SageGreenMain)
            )
        },
        containerColor = BackgroundWhite
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            Surface(
                color = SageGreenMain,
                shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(SageGreenLight)
                            .border(2.dp, SageGreenDark, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isEmployer) Icons.Default.Business else Icons.Default.Person,
                            contentDescription = "Avatar",
                            tint = DeepGreenDark,
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = displayedUser?.name ?: "User Name",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = displayedUser?.email ?: "user@example.com",
                        fontSize = 14.sp,
                        color = DeepGreenDark
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        color = DeepGreenDark,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (isEmployer) "Employer Account" else "Job Seeker",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (isEditing) {
                // ---- Edit Profile form -------------------------------------------
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Edit Profile",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepGreenDark
                    )

                    OutlinedTextField(
                        value = editName,
                        onValueChange = {
                            editName = ValidationUtils.filterNameInput(it)
                            nameError = null
                            saveErrorMessage = ""
                        },
                        label = { Text("Full Name") },
                        singleLine = true,
                        isError = nameError != null,
                        supportingText = {
                            nameError?.let { Text(text = it, color = Color.Red, fontSize = 12.sp) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )

                    OutlinedTextField(
                        value = editPhone,
                        onValueChange = {
                            editPhone = ValidationUtils.filterPhoneInput(it)
                            phoneError = null
                            saveErrorMessage = ""
                        },
                        label = { Text("Phone Number") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        isError = phoneError != null,
                        supportingText = {
                            phoneError?.let { Text(text = it, color = Color.Red, fontSize = 12.sp) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )

                    OutlinedTextField(
                        value = editLocation,
                        onValueChange = {
                            editLocation = it.take(ValidationUtils.LOCATION_MAX_LENGTH)
                            locationError = null
                            saveErrorMessage = ""
                        },
                        label = { Text("Location (City, Country)") },
                        singleLine = true,
                        isError = locationError != null,
                        supportingText = {
                            locationError?.let { Text(text = it, color = Color.Red, fontSize = 12.sp) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )

                    if (saveErrorMessage.isNotBlank()) {
                        Text(text = saveErrorMessage, color = Color.Red, fontSize = 12.sp)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { isEditing = false },
                            enabled = !isSaving,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Cancel", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val currentUserSafe = displayedUser
                                if (currentUserSafe == null) {
                                    saveErrorMessage = "User information is missing."
                                    return@Button
                                }

                                // Validate every field before hitting the network.
                                val nameValidation = ValidationUtils.validateFullName(editName)
                                val phoneValidation = ValidationUtils.validatePhone(editPhone, required = false)
                                val locationValidation = ValidationUtils.validateLocation(editLocation, required = false)

                                nameError = nameValidation
                                phoneError = phoneValidation
                                locationError = locationValidation

                                if (nameValidation != null || phoneValidation != null || locationValidation != null) {
                                    saveErrorMessage = "Please fix the highlighted fields before saving."
                                    return@Button
                                }

                                isSaving = true
                                saveErrorMessage = ""

                                scope.launch {
                                    try {
                                        val updatedUser = currentUserSafe.copy(
                                            name = editName.trim(),
                                            phone = editPhone.trim(),
                                            location = editLocation.trim()
                                        )

                                        val isSaved = UserRepository.updateUserInSupabase(updatedUser)

                                        isSaving = false
                                        if (isSaved) {
                                            displayedUser = updatedUser
                                            onProfileUpdated(updatedUser)
                                            isEditing = false
                                        } else {
                                            saveErrorMessage = "Failed to save profile. Please try again."
                                        }
                                    } catch (e: Exception) {
                                        isSaving = false
                                        saveErrorMessage = e.message ?: "An unexpected error occurred."
                                    }
                                }
                            },
                            enabled = !isSaving,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DeepGreenDark)
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                            } else {
                                Text("Save", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            } else {
                // ---- Options list -------------------------------------------------
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ProfileOptionItem(
                        icon = Icons.Default.Edit,
                        title = "Edit Profile",
                        onClick = { startEditing() }
                    )

                    if (!isEmployer) {
                        ProfileOptionItem(
                            icon = Icons.Default.Description,
                            title = "Resume / CV",
                            onClick = { }
                        )
                    }

                    ProfileOptionItem(
                        icon = Icons.Default.Notifications,
                        title = "Notifications",
                        onClick = { }
                    )

                    ProfileOptionItem(
                        icon = Icons.Default.Settings,
                        title = "Settings",
                        onClick = { }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = onLogout,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Logout",
                            tint = Color.Red
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Log Out",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Red
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun ProfileOptionItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SageGreenLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = DeepGreenDark,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = TextDark,
                modifier = Modifier.weight(1f)
            )

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = SageGreenDark
            )
        }
    }
}
