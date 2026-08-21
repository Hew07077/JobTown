package com.example.jobtown.ui.profile

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.jobtown.data.User
import com.example.jobtown.data.UserRole
import com.example.jobtown.data.repository.UserRepository
import com.example.jobtown.ui.theme.*
import com.example.jobtown.utils.ValidationUtils
import kotlinx.coroutines.launch
import java.util.UUID

private val INDUSTRY_OPTIONS = listOf(
    "Technology / IT",
    "Finance / BaAnking",
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

private data class ProfileEntry(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val subtitle: String,
    val period: String,
    val description: String
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    currentUser: User? = null,
    onLogout: () -> Unit = {},
    onProfileUpdated: (User) -> Unit = {}
) {
    var displayedUser by remember(currentUser) { mutableStateOf(currentUser) }
    val isEmployer = displayedUser?.role == UserRole.EMPLOYER
    val displayName = (if (isEmployer) displayedUser?.companyName else displayedUser?.name)
        ?.ifBlank { null } ?: "User Name"
    val memberSince = displayedUser?.createdAt?.takeIf { it.isNotBlank() }?.substringBefore("T")

    // Extract lists/URLs directly from dedicated User model fields safely
    val skillsList = remember(displayedUser?.skills) {
        displayedUser?.skills
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: emptyList()
    }
    val websiteOrPortfolioUrl = remember(displayedUser?.websiteUrl, displayedUser?.portfolioUrl, isEmployer) {
        if (isEmployer) displayedUser?.websiteUrl.orEmpty() else displayedUser?.portfolioUrl.orEmpty()
    }

    var isEditing by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var saveErrorMessage by remember { mutableStateOf("") }

    var editName by remember { mutableStateOf("") }
    var editPhone by remember { mutableStateOf(displayedUser?.phone ?: "") }
    var editLocation by remember { mutableStateOf(displayedUser?.location ?: "") }
    var editIndustry by remember { mutableStateOf(displayedUser?.industry ?: "") }
    var editCompanySize by remember { mutableStateOf(displayedUser?.companySize ?: "") }

    var nameError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var locationError by remember { mutableStateOf<String?>(null) }

    var selectedTab by remember { mutableStateOf("Overview") }
    val experienceEntries = remember { mutableStateListOf<ProfileEntry>() }
    val educationEntries = remember { mutableStateListOf<ProfileEntry>() }
    val certificationEntries = remember { mutableStateListOf<ProfileEntry>() }
    var addEntryDialogFor by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val listState = rememberLazyListState()

    fun jumpTo(index: Int, tab: String) {
        selectedTab = tab
        scope.launch { listState.animateScrollToItem(index) }
    }

    fun startEditing() {
        editName = if (isEmployer) displayedUser?.companyName ?: "" else displayedUser?.name ?: ""
        editPhone = displayedUser?.phone ?: ""
        editLocation = displayedUser?.location ?: ""
        editIndustry = displayedUser?.industry ?: ""
        editCompanySize = displayedUser?.companySize ?: ""
        nameError = null
        phoneError = null
        locationError = null
        saveErrorMessage = ""
        isEditing = true
    }

    Scaffold(containerColor = BackgroundWhite) { innerPadding ->
        if (isEditing) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
            ) {
                ProfileHeader(
                    navController = navController,
                    isEmployer = isEmployer,
                    isEditing = true,
                    displayName = displayName,
                    email = displayedUser?.email,
                    memberSince = memberSince,
                    avatarUrl = displayedUser?.avatarUrl,
                    onEditClick = { }
                )

                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(text = "Edit Profile", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DeepGreenDark)

                    OutlinedTextField(
                        value = editName,
                        onValueChange = {
                            editName = if (isEmployer) it.take(ValidationUtils.NAME_MAX_LENGTH) else ValidationUtils.filterNameInput(it)
                            nameError = null
                            saveErrorMessage = ""
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
                        supportingText = { nameError?.let { Text(text = it, color = Color.Red, fontSize = 12.sp) } },
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
                        supportingText = { phoneError?.let { Text(text = it, color = Color.Red, fontSize = 12.sp) } },
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
                        label = { Text(if (isEmployer) "Company Location (City, Country)" else "Location (City, Country)") },
                        singleLine = true,
                        isError = locationError != null,
                        supportingText = { locationError?.let { Text(text = it, color = Color.Red, fontSize = 12.sp) } },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )

                    if (isEmployer) {
                        var expandedIndustry by remember { mutableStateOf(false) }
                        var expandedCompanySize by remember { mutableStateOf(false) }

                        ExposedDropdownMenuBox(
                            expanded = expandedIndustry,
                            onExpandedChange = { expandedIndustry = !expandedIndustry },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = editIndustry,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Industry") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedIndustry) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            )
                            ExposedDropdownMenu(expanded = expandedIndustry, onDismissRequest = { expandedIndustry = false }) {
                                INDUSTRY_OPTIONS.forEach { option ->
                                    DropdownMenuItem(text = { Text(option) }, onClick = { editIndustry = option; expandedIndustry = false })
                                }
                            }
                        }

                        ExposedDropdownMenuBox(
                            expanded = expandedCompanySize,
                            onExpandedChange = { expandedCompanySize = !expandedCompanySize },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = editCompanySize,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Company Size") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCompanySize) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            )
                            ExposedDropdownMenu(expanded = expandedCompanySize, onDismissRequest = { expandedCompanySize = false }) {
                                COMPANY_SIZE_OPTIONS.forEach { option ->
                                    DropdownMenuItem(text = { Text(option) }, onClick = { editCompanySize = option; expandedCompanySize = false })
                                }
                            }
                        }
                    }

                    if (saveErrorMessage.isNotBlank()) {
                        Text(text = saveErrorMessage, color = Color.Red, fontSize = 12.sp)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = { isEditing = false },
                            enabled = !isSaving,
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) { Text("Cancel", fontWeight = FontWeight.Bold) }

                        Button(
                            onClick = {
                                val currentUserSafe = displayedUser
                                if (currentUserSafe == null) {
                                    saveErrorMessage = "User information is missing."
                                    return@Button
                                }
                                val nameValidation = if (isEmployer) ValidationUtils.validateCompanyName(editName) else ValidationUtils.validateFullName(editName)
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
                                            name = if (isEmployer) currentUserSafe.name else editName.trim(),
                                            companyName = if (isEmployer) editName.trim() else currentUserSafe.companyName,
                                            phone = editPhone.trim(),
                                            location = editLocation.trim(),
                                            industry = if (isEmployer) editIndustry.trim() else currentUserSafe.industry,
                                            companySize = if (isEmployer) editCompanySize.trim() else currentUserSafe.companySize
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
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DeepGreenDark)
                        ) {
                            if (isSaving) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                            else Text("Save", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        } else if (isEmployer) {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                item { ProfileHeader(navController, isEmployer, false, displayName, displayedUser?.email, memberSince, displayedUser?.avatarUrl, onEditClick = { startEditing() }) }
                item { ContactCard(displayedUser?.phone, displayedUser?.email, displayedUser?.location) }

                stickyHeader {
                    ProfileTabBar(
                        tabs = listOf("Overview", "Open Positions"),
                        selectedTab = selectedTab,
                        onSelect = { tab -> jumpTo(if (tab == "Overview") 3 else 4, tab) }
                    )
                }

                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp).padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        ProfileSectionCard(title = "About the Company", icon = Icons.Default.Info) {
                            val bioText = displayedUser?.bio.orEmpty().ifBlank { "This company hasn't added a description yet. Tap the pencil icon above to add one." }
                            Text(
                                text = bioText,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                color = TextDark.copy(alpha = if (displayedUser?.bio.isNullOrBlank()) 0.5f else 0.9f)
                            )
                            if (!displayedUser?.tagline.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = displayedUser?.tagline.orEmpty(),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = DeepGreenDark
                                )
                            }
                        }

                        if (!displayedUser?.perks.isNullOrEmpty()) {
                            ProfileSectionCard(title = "Perks & Benefits", icon = Icons.Default.Star) {
                                @OptIn(ExperimentalLayoutApi::class)
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    displayedUser?.perks.orEmpty().forEach { perk ->
                                        Surface(
                                            color = SageGreenLight,
                                            shape = RoundedCornerShape(20.dp)
                                        ) {
                                            Text(
                                                text = perk,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = DeepGreenDark,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (!displayedUser?.industry.isNullOrBlank() || !displayedUser?.companySize.isNullOrBlank()) {
                            ProfileSectionCard(title = "Company Details", icon = Icons.Default.Business) {
                                if (!displayedUser?.industry.isNullOrBlank()) {
                                    InfoRow(icon = Icons.Default.Category, label = "Industry", value = displayedUser?.industry.orEmpty())
                                }
                                if (!displayedUser?.companySize.isNullOrBlank()) {
                                    InfoRow(icon = Icons.Default.Groups, label = "Company Size", value = displayedUser?.companySize.orEmpty())
                                }
                            }
                        }

                        if (websiteOrPortfolioUrl.isNotBlank()) {
                            ProfileSectionCard(title = "Website", icon = Icons.Default.Language) {
                                InfoRow(
                                    icon = Icons.Default.Language,
                                    label = "Company website",
                                    value = websiteOrPortfolioUrl,
                                    valueColor = DeepGreenDark,
                                    onClick = {
                                        val url = websiteOrPortfolioUrl.let { if (it.startsWith("http")) it else "https://$it" }
                                        uriHandler.openUri(url)
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp).padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        SectionHeading("Open Positions")
                        Spacer(modifier = Modifier.height(10.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(modifier = Modifier.size(60.dp).clip(CircleShape).background(SageGreenLight), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Work, contentDescription = null, tint = DeepGreenDark, modifier = Modifier.size(26.dp))
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Manage your listings", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextDark)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Head to your home feed to see and manage every job you've posted.", fontSize = 12.sp, color = TextDark.copy(alpha = 0.55f), lineHeight = 16.sp, textAlign = TextAlign.Center)
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { navController.popBackStack() },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = DeepGreenDark)
                                ) {
                                    Icon(Icons.Default.Work, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("View Your Posted Jobs", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }

                item { Column(modifier = Modifier.padding(horizontal = 20.dp).padding(top = 16.dp)) { ProfileAccountActions(isEmployer = true, onLogout = onLogout) } }
                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        } else {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                item { ProfileHeader(navController, isEmployer, false, displayName, displayedUser?.email, memberSince, displayedUser?.avatarUrl, onEditClick = { startEditing() }) }
                item { ContactCard(displayedUser?.phone, displayedUser?.email, displayedUser?.location) }

                stickyHeader {
                    ProfileTabBar(
                        tabs = listOf("Overview", "Experience", "Education", "Certifications"),
                        selectedTab = selectedTab,
                        onSelect = { tab ->
                            val index = when (tab) {
                                "Overview" -> 3
                                "Experience" -> 4
                                "Education" -> 5
                                else -> 6
                            }
                            jumpTo(index, tab)
                        }
                    )
                }

                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp).padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        ProfileSectionCard(title = "Professional Summary", icon = Icons.Default.Info) {
                            val bioText = displayedUser?.bio.orEmpty().ifBlank { "You haven't added a summary yet. Tap the pencil icon above to add one." }
                            Text(
                                text = bioText,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                color = TextDark.copy(alpha = if (displayedUser?.bio.isNullOrBlank()) 0.5f else 0.9f)
                            )
                            if (!displayedUser?.experienceLevel.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Surface(color = SageGreenLight, shape = RoundedCornerShape(20.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                                        Icon(Icons.Default.TrendingUp, contentDescription = null, tint = DeepGreenDark, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(displayedUser?.experienceLevel.orEmpty(), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = DeepGreenDark)
                                    }
                                }
                            }
                        }

                        if (skillsList.isNotEmpty()) {
                            ProfileSectionCard(title = "Key Skills", icon = Icons.Default.Star) {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(skillsList) { skill ->
                                        Surface(
                                            color = Color.White,
                                            shape = RoundedCornerShape(20.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, SageGreenDark.copy(alpha = 0.35f))
                                        ) {
                                            Text(text = skill, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = DeepGreenDark, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                                        }
                                    }
                                }
                            }
                        }

                        if (websiteOrPortfolioUrl.isNotBlank()) {
                            ProfileSectionCard(title = "Portfolio", icon = Icons.Default.Language) {
                                InfoRow(
                                    icon = Icons.Default.Language,
                                    label = "Portfolio / LinkedIn / GitHub",
                                    value = websiteOrPortfolioUrl,
                                    valueColor = DeepGreenDark,
                                    onClick = {
                                        val url = websiteOrPortfolioUrl.let { if (it.startsWith("http")) it else "https://$it" }
                                        uriHandler.openUri(url)
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp).padding(top = 14.dp)) {
                        SectionHeading("Experience")
                        Spacer(modifier = Modifier.height(10.dp))
                        ProfileEntrySection(
                            emptyIcon = Icons.Default.Work,
                            entryIcon = Icons.Default.Work,
                            emptyTitle = "No work experience yet",
                            emptyText = "Showcase your work history to stand out to employers.",
                            addLabel = "Add Experience",
                            entries = experienceEntries,
                            onAddClick = { addEntryDialogFor = "Experience" },
                            onRemove = { experienceEntries.remove(it) }
                        )
                    }
                }

                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp).padding(top = 14.dp)) {
                        SectionHeading("Education")
                        Spacer(modifier = Modifier.height(10.dp))
                        ProfileEntrySection(
                            emptyIcon = Icons.Default.School,
                            entryIcon = Icons.Default.School,
                            emptyTitle = "No education added yet",
                            emptyText = "Add your qualifications so employers know your background.",
                            addLabel = "Add Education",
                            entries = educationEntries,
                            onAddClick = { addEntryDialogFor = "Education" },
                            onRemove = { educationEntries.remove(it) }
                        )
                    }
                }

                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp).padding(top = 14.dp)) {
                        SectionHeading("Certifications")
                        Spacer(modifier = Modifier.height(10.dp))
                        ProfileEntrySection(
                            emptyIcon = Icons.Default.WorkspacePremium,
                            entryIcon = Icons.Default.WorkspacePremium,
                            emptyTitle = "No certifications added yet",
                            emptyText = "Certifications help you stand out from other candidates.",
                            addLabel = "Add Certification",
                            entries = certificationEntries,
                            onAddClick = { addEntryDialogFor = "Certification" },
                            onRemove = { certificationEntries.remove(it) }
                        )
                    }
                }

                item { Column(modifier = Modifier.padding(horizontal = 20.dp).padding(top = 16.dp)) { ProfileAccountActions(isEmployer = false, onLogout = onLogout) } }
                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }

    addEntryDialogFor?.let { category ->
        AddEntryDialog(
            category = category,
            onDismiss = { addEntryDialogFor = null },
            onSave = { entry ->
                when (category) {
                    "Experience" -> experienceEntries.add(entry)
                    "Education" -> educationEntries.add(entry)
                    "Certification" -> certificationEntries.add(entry)
                }
                addEntryDialogFor = null
            }
        )
    }
}

@Composable
private fun ProfileHeader(
    navController: NavController,
    isEmployer: Boolean,
    isEditing: Boolean,
    displayName: String,
    email: String?,
    memberSince: String?,
    avatarUrl: String? = null,
    onEditClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(colors = listOf(SageGreenMain, SageGreenLight)),
                shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
            )
            .padding(top = 8.dp, bottom = 44.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = DeepGreenDark)
            }
            Spacer(modifier = Modifier.weight(1f))
        }

        Box(contentAlignment = Alignment.BottomEnd) {
            Surface(modifier = Modifier.size(96.dp), shape = CircleShape, color = Color.White, shadowElevation = 8.dp) {
                Box(contentAlignment = Alignment.Center) {
                    if (!avatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = "Profile Logo",
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = if (isEmployer) Icons.Default.Business else Icons.Default.Person,
                            contentDescription = "Avatar",
                            tint = DeepGreenDark,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }
            }
            if (!isEditing) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(DeepGreenDark)
                        .border(2.dp, Color.White, CircleShape)
                        .clickable { onEditClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = Color.White, modifier = Modifier.size(15.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(text = displayName, fontSize = 21.sp, fontWeight = FontWeight.Bold, color = TextDark)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = email ?: "user@example.com", fontSize = 13.sp, color = DeepGreenDark)
        Spacer(modifier = Modifier.height(10.dp))

        Surface(color = DeepGreenDark, shape = RoundedCornerShape(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = if (isEmployer) "Employer Account" else "Job Seeker",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        if (!memberSince.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = "Member since $memberSince", fontSize = 11.sp, color = DeepGreenDark.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun ContactCard(phone: String?, email: String?, location: String?) {
    Card(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .fillMaxWidth()
            .offset(y = (-26).dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ContactDisplayItem(icon = Icons.Default.Phone, label = "Phone", value = phone, modifier = Modifier.weight(1f))
            ContactDisplayItem(icon = Icons.Default.Email, label = "Email", value = email, modifier = Modifier.weight(1f))
            ContactDisplayItem(icon = Icons.Default.LocationOn, label = "Location", value = location, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ContactDisplayItem(icon: ImageVector, label: String, value: String?, modifier: Modifier = Modifier) {
    val hasValue = !value.isNullOrBlank()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier.size(42.dp).clip(CircleShape).background(if (hasValue) SageGreenLight else SageGreenLight.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (hasValue) DeepGreenDark else DeepGreenDark.copy(alpha = 0.35f),
                modifier = Modifier.size(19.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, fontSize = 10.sp, color = TextDark.copy(alpha = 0.45f))
        Text(
            text = value?.takeIf { it.isNotBlank() } ?: "Not specified",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = if (hasValue) TextDark else TextDark.copy(alpha = 0.35f),
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 13.sp
        )
    }
}

@Composable
private fun ProfileTabBar(tabs: List<String>, selectedTab: String, onSelect: (String) -> Unit) {
    Surface(color = BackgroundWhite, modifier = Modifier.fillMaxWidth()) {
        Surface(
            color = SageGreenLight.copy(alpha = 0.35f),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.padding(horizontal = 20.dp).padding(vertical = 10.dp)
        ) {
            LazyRow(contentPadding = PaddingValues(6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(tabs) { tab ->
                    val isSelected = selectedTab == tab
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelect(tab) },
                        label = { Text(text = tab, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                        shape = RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DeepGreenDark,
                            selectedLabelColor = Color.White,
                            containerColor = Color.Transparent,
                            labelColor = TextDark
                        ),
                        border = null
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeading(title: String) {
    Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DeepGreenDark)
}

@Composable
private fun ProfileSectionCard(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(SageGreenLight), contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = DeepGreenDark, modifier = Modifier.size(15.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DeepGreenDark)
            }
            content()
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String, valueColor: Color = TextDark, onClick: (() -> Unit)? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick) else Modifier)
    ) {
        Box(modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(SageGreenLight), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = DeepGreenDark, modifier = Modifier.size(17.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, fontSize = 11.sp, color = TextDark.copy(alpha = 0.5f))
            Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = valueColor)
        }
        if (onClick != null) {
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = SageGreenDark, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun ProfileEntrySection(
    emptyIcon: ImageVector,
    entryIcon: ImageVector,
    emptyTitle: String,
    emptyText: String,
    addLabel: String,
    entries: List<ProfileEntry>,
    onAddClick: () -> Unit,
    onRemove: (ProfileEntry) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (entries.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.size(60.dp).clip(CircleShape).background(SageGreenLight), contentAlignment = Alignment.Center) {
                        Icon(emptyIcon, contentDescription = null, tint = DeepGreenDark, modifier = Modifier.size(26.dp))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = emptyTitle, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = emptyText, fontSize = 12.sp, color = TextDark.copy(alpha = 0.55f), lineHeight = 16.sp, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onAddClick, shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = DeepGreenDark)) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(addLabel, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                    }
                }
            }
        } else {
            entries.forEach { entry ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.Top) {
                        Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(SageGreenLight), contentAlignment = Alignment.Center) {
                            Icon(entryIcon, contentDescription = null, tint = DeepGreenDark, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = entry.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextDark)
                            if (entry.subtitle.isNotBlank()) Text(text = entry.subtitle, fontSize = 13.sp, color = DeepGreenDark)
                            if (entry.period.isNotBlank()) Text(text = entry.period, fontSize = 11.sp, color = TextDark.copy(alpha = 0.5f))
                            if (entry.description.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(text = entry.description, fontSize = 13.sp, color = TextDark.copy(alpha = 0.8f), lineHeight = 18.sp)
                            }
                        }
                        IconButton(onClick = { onRemove(entry) }, modifier = Modifier.size(26.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Remove", tint = TextDark.copy(alpha = 0.35f), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            OutlinedButton(onClick = onAddClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = DeepGreenDark)) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add another", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun ProfileAccountActions(isEmployer: Boolean, onLogout: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = "ACCOUNT", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDark.copy(alpha = 0.4f))

        if (!isEmployer) {
            ProfileOptionItem(icon = Icons.Default.Description, title = "Resume / CV", onClick = { })
        }
        ProfileOptionItem(icon = Icons.Default.Notifications, title = "Notifications", onClick = { })
        ProfileOptionItem(icon = Icons.Default.Settings, title = "Settings", onClick = { })

        Spacer(modifier = Modifier.height(4.dp))

        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
        ) {
            Icon(imageVector = Icons.Default.ExitToApp, contentDescription = "Logout", tint = Color.Red)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Log Out", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.Red)
        }
    }
}

@Composable
private fun AddEntryDialog(category: String, onDismiss: () -> Unit, onSave: (ProfileEntry) -> Unit) {
    var title by remember { mutableStateOf("") }
    var subtitle by remember { mutableStateOf("") }
    var period by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var titleError by remember { mutableStateOf<String?>(null) }

    val (titleLabel, subtitleLabel) = when (category) {
        "Experience" -> "Job Title" to "Company"
        "Education" -> "Degree / Qualification" to "Institution"
        else -> "Certification Name" to "Issued By"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add $category", fontWeight = FontWeight.Bold, color = DeepGreenDark) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(100); titleError = null },
                    label = { Text(titleLabel) },
                    singleLine = true,
                    isError = titleError != null,
                    supportingText = { titleError?.let { Text(it, color = Color.Red, fontSize = 12.sp) } },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = subtitle,
                    onValueChange = { subtitle = it.take(100) },
                    label = { Text(subtitleLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = period,
                    onValueChange = { period = it.take(50) },
                    label = { Text("Period (e.g. 2022 - Present)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it.take(300) },
                    label = { Text("Description (optional)") },
                    modifier = Modifier.fillMaxWidth().height(90.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (title.trim().isBlank()) {
                    titleError = "This field is required."
                    return@TextButton
                }
                onSave(ProfileEntry(title = title.trim(), subtitle = subtitle.trim(), period = period.trim(), description = description.trim()))
            }) {
                Text("Add", color = DeepGreenDark, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextDark.copy(alpha = 0.6f)) }
        }
    )
}

@Composable
fun ProfileOptionItem(icon: ImageVector, title: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        onClick = onClick
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(SageGreenLight), contentAlignment = Alignment.Center) {
                Icon(imageVector = icon, contentDescription = null, tint = DeepGreenDark, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextDark, modifier = Modifier.weight(1f))
            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = SageGreenDark)
        }
    }
}