package com.example.jobtown.ui.profile

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.jobtown.data.model.ProfileEntry
import com.example.jobtown.data.model.User
import com.example.jobtown.data.model.UserRole
import com.example.jobtown.data.repository.AvatarHistoryItem
import com.example.jobtown.ui.theme.*
import com.example.jobtown.utils.ValidationUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    currentUser: User? = null,
    onLogout: () -> Unit = {},
    onProfileUpdated: (User) -> Unit = {},
    viewModel: ProfileViewModel = viewModel()
) {
    LaunchedEffect(currentUser) {
        viewModel.bind(currentUser)
    }

    val displayedUser = viewModel.user ?: currentUser
    val isEmployer = viewModel.isEmployer || displayedUser?.role == UserRole.EMPLOYER
    val displayName = (if (isEmployer) displayedUser?.companyName else displayedUser?.name)
        ?.ifBlank { null } ?: "User Name"
    val memberSince = displayedUser?.createdAt?.takeIf { it.isNotBlank() }?.substringBefore("T")

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

    val isEditing = viewModel.isEditing
    val isSaving = viewModel.isSaving
    val saveErrorMessage = viewModel.saveErrorMessage
    val selectedTab = viewModel.selectedTab
    val experienceEntries = viewModel.experienceEntries
    val educationEntries = viewModel.educationEntries
    val certificationEntries = viewModel.certificationEntries
    val isUploadingAvatar = viewModel.isUploadingAvatar
    val isUploadingResume = viewModel.isUploadingResume

    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val listState = rememberLazyListState()
    val context = LocalContext.current

    LaunchedEffect(viewModel.avatarError) {
        viewModel.avatarError?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.consumeAvatarError()
        }
    }

    LaunchedEffect(viewModel.resumeError) {
        viewModel.resumeError?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.consumeResumeError()
        }
    }

    LaunchedEffect(viewModel.certificateError) {
        viewModel.certificateError?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.consumeCertificateError()
        }
    }

    val avatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val bytes = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            }
            if (bytes == null) {
                viewModel.avatarError = "Couldn't read the selected image."
                return@launch
            }
            val mimeType = context.contentResolver.getType(uri)
            val extension = when {
                mimeType?.contains("png") == true -> "png"
                mimeType?.contains("webp") == true -> "webp"
                else -> "jpg"
            }
            viewModel.uploadAvatar(bytes, extension, onProfileUpdated)
        }
    }

    fun pickAvatar() {
        avatarPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    LaunchedEffect(viewModel.showAvatarManager) {
        if (viewModel.showAvatarManager) {
            viewModel.loadAvatarHistory()
        }
    }

    fun jumpTo(index: Int, tab: String) {
        viewModel.selectedTab = tab
        scope.launch { listState.animateScrollToItem(index) }
    }

    val resumePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val bytes = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            }
            if (bytes == null) {
                viewModel.resumeError = "Couldn't read the selected file."
                return@launch
            }
            viewModel.uploadResume(bytes, onProfileUpdated)
        }
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
                    isUploadingAvatar = isUploadingAvatar,
                    onAvatarClick = { viewModel.showAvatarManager = true },
                    onEditClick = { }
                )

                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(text = "Edit Profile", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DeepGreenDark)

                    OutlinedTextField(
                        value = viewModel.editName,
                        onValueChange = {
                            viewModel.editName = if (isEmployer) it.take(ValidationUtils.NAME_MAX_LENGTH) else ValidationUtils.filterNameInput(it)
                            viewModel.nameError = null
                            viewModel.saveErrorMessage = ""
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
                        isError = viewModel.nameError != null,
                        supportingText = { viewModel.nameError?.let { Text(text = it, color = Color.Red, fontSize = 12.sp) } },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )

                    OutlinedTextField(
                        value = viewModel.editPhone,
                        onValueChange = {
                            viewModel.editPhone = ValidationUtils.filterPhoneInput(it)
                            viewModel.phoneError = null
                            viewModel.saveErrorMessage = ""
                        },
                        label = { Text("Phone Number") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        isError = viewModel.phoneError != null,
                        supportingText = { viewModel.phoneError?.let { Text(text = it, color = Color.Red, fontSize = 12.sp) } },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )

                    Text(
                        text = if (isEmployer) "Company Location" else "Location",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextDark.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth()
                    )
                    com.example.jobtown.ui.components.LocationPicker(
                        locationString = viewModel.editLocation,
                        onLocationStringChange = {
                            viewModel.editLocation = it
                            viewModel.locationError = null
                            viewModel.saveErrorMessage = ""
                        },
                        allowMultipleBranches = isEmployer,
                        errorText = viewModel.locationError,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (isEmployer) {
                        ProfileDropdownField(
                            label = "Industry",
                            value = viewModel.editIndustry,
                            options = ProfileOptions.INDUSTRIES,
                            onSelect = { viewModel.editIndustry = it }
                        )
                        ProfileDropdownField(
                            label = "Company Size",
                            value = viewModel.editCompanySize,
                            options = ProfileOptions.COMPANY_SIZES,
                            onSelect = { viewModel.editCompanySize = it }
                        )
                        OutlinedTextField(
                            value = viewModel.editTagline,
                            onValueChange = { viewModel.editTagline = it.take(80) },
                            label = { Text("Tagline") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        )
                        OutlinedTextField(
                            value = viewModel.editWebsiteUrl,
                            onValueChange = {
                                viewModel.editWebsiteUrl = it.take(ValidationUtils.URL_MAX_LENGTH)
                                viewModel.urlError = null
                            },
                            label = { Text("Company Website URL") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                            isError = viewModel.urlError != null,
                            supportingText = { viewModel.urlError?.let { Text(it, color = Color.Red, fontSize = 12.sp) } },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        )
                        Text("Perks & Benefits", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextDark.copy(alpha = 0.7f))
                        ProfileOptions.PERKS.forEach { perk ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.togglePerk(perk) },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = viewModel.editPerks.contains(perk),
                                    onCheckedChange = { viewModel.togglePerk(perk) },
                                    colors = CheckboxDefaults.colors(checkedColor = DeepGreenDark)
                                )
                                Text(perk, fontSize = 14.sp, color = TextDark)
                            }
                        }
                    } else {
                        Text("Experience Level", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextDark.copy(alpha = 0.7f))
                        ProfileOptions.EXPERIENCE_LEVELS.forEach { level ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.editExperienceLevel = level },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = viewModel.editExperienceLevel == level,
                                    onClick = { viewModel.editExperienceLevel = level },
                                    colors = RadioButtonDefaults.colors(selectedColor = DeepGreenDark)
                                )
                                Text(level, fontSize = 14.sp, color = TextDark)
                            }
                        }
                        OutlinedTextField(
                            value = viewModel.editSkills,
                            onValueChange = { viewModel.editSkills = it.take(ValidationUtils.SKILLS_MAX_LENGTH) },
                            label = { Text("Key Skills (comma separated)") },
                            supportingText = {
                                Text("${viewModel.editSkills.length}/${ValidationUtils.SKILLS_MAX_LENGTH}", fontSize = 12.sp)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        )
                        OutlinedTextField(
                            value = viewModel.editPortfolioUrl,
                            onValueChange = {
                                viewModel.editPortfolioUrl = it.take(ValidationUtils.URL_MAX_LENGTH)
                                viewModel.urlError = null
                            },
                            label = { Text("Portfolio / LinkedIn / GitHub URL") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                            isError = viewModel.urlError != null,
                            supportingText = { viewModel.urlError?.let { Text(it, color = Color.Red, fontSize = 12.sp) } },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        )
                    }

                    OutlinedTextField(
                        value = viewModel.editBio,
                        onValueChange = { viewModel.editBio = it.take(ValidationUtils.BIO_MAX_LENGTH) },
                        label = { Text(if (isEmployer) "About the Company" else "Professional Summary") },
                        supportingText = {
                            Text("${viewModel.editBio.length}/${ValidationUtils.BIO_MAX_LENGTH}", fontSize = 12.sp)
                        },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        shape = RoundedCornerShape(14.dp)
                    )

                    if (saveErrorMessage.isNotBlank()) {
                        Text(text = saveErrorMessage, color = Color.Red, fontSize = 12.sp)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = { viewModel.cancelEditing() },
                            enabled = !isSaving,
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) { Text("Cancel", fontWeight = FontWeight.Bold) }

                        Button(
                            onClick = { viewModel.saveProfile(onProfileUpdated) },
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
                item { ProfileHeader(navController, isEmployer, false, displayName, displayedUser?.email, memberSince, displayedUser?.avatarUrl, isUploadingAvatar, onEditClick = { viewModel.startEditing() }) }
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
                item { ProfileHeader(navController, isEmployer, false, displayName, displayedUser?.email, memberSince, displayedUser?.avatarUrl, isUploadingAvatar, onEditClick = { viewModel.startEditing() }) }
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
                            onAddClick = { viewModel.addEntryDialogFor = "Experience" },
                            onRemove = { viewModel.removeEntry("Experience", it) }
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
                            onAddClick = { viewModel.addEntryDialogFor = "Education" },
                            onRemove = { viewModel.removeEntry("Education", it) }
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
                            onAddClick = { viewModel.addEntryDialogFor = "Certification" },
                            onRemove = { viewModel.removeEntry("Certification", it) },
                            onViewFile = { url -> uriHandler.openUri(url) }
                        )
                    }
                }

                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp).padding(top = 16.dp)) {
                        ProfileAccountActions(
                            isEmployer = false,
                            resumeUrl = displayedUser?.resumeUrl,
                            onResumeClick = { viewModel.showResumeDialog = true },
                            onLogout = onLogout
                        )
                    }
                }
                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }

    viewModel.addEntryDialogFor?.let { category ->
        AddEntryDialog(
            category = category,
            isUploadingFile = viewModel.isUploadingCertificate,
            onDismiss = { viewModel.addEntryDialogFor = null },
            onSave = { entry -> viewModel.addEntry(category, entry) },
            onUploadCertificate = { bytes, extension, onDone ->
                viewModel.uploadCertificate(bytes, extension, onDone)
            }
        )
    }

    if (viewModel.showAvatarManager) {
        AvatarManagerSheet(
            isLoadingHistory = viewModel.isLoadingAvatarHistory,
            historyItems = viewModel.avatarHistory,
            currentAvatarUrl = displayedUser?.avatarUrl,
            onDismiss = { viewModel.showAvatarManager = false },
            onUploadNewClick = {
                viewModel.showAvatarManager = false
                pickAvatar()
            },
            onSelect = { viewModel.selectAvatarFromHistory(it, onProfileUpdated) },
            onDelete = { viewModel.deleteAvatarFromHistory(it, onProfileUpdated) }
        )
    }

    if (viewModel.showResumeDialog) {
        ResumeDialog(
            resumeUrl = displayedUser?.resumeUrl,
            isUploading = isUploadingResume,
            onDismiss = { viewModel.showResumeDialog = false },
            onUploadClick = { resumePickerLauncher.launch("application/pdf") },
            onViewClick = {
                displayedUser?.resumeUrl?.takeIf { it.isNotBlank() }?.let { uriHandler.openUri(it) }
            },
            onRemoveClick = { viewModel.removeResume(onProfileUpdated) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AvatarManagerSheet(
    isLoadingHistory: Boolean,
    historyItems: List<AvatarHistoryItem>,
    currentAvatarUrl: String?,
    onDismiss: () -> Unit,
    onUploadNewClick: () -> Unit,
    onSelect: (AvatarHistoryItem) -> Unit,
    onDelete: (AvatarHistoryItem) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val currentUrlWithoutCacheBust = currentAvatarUrl?.substringBefore("?")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(text = "Profile Photo", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DeepGreenDark)
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onUploadNewClick,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DeepGreenDark)
            ) {
                Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Upload New Photo", fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(text = "PREVIOUS PHOTOS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDark.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(10.dp))

            when {
                isLoadingHistory -> {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = DeepGreenDark, modifier = Modifier.size(24.dp))
                    }
                }
                historyItems.isEmpty() -> {
                    Text(
                        text = "No previous photos yet -- they'll show up here once you upload one.",
                        fontSize = 12.sp,
                        color = TextDark.copy(alpha = 0.5f),
                        lineHeight = 16.sp
                    )
                }
                else -> {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        items(historyItems, key = { it.path }) { item ->
                            val isCurrent = item.url == currentUrlWithoutCacheBust
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(contentAlignment = Alignment.TopEnd) {
                                    Surface(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(CircleShape)
                                            .clickable { onSelect(item) },
                                        shape = CircleShape,
                                        border = if (isCurrent) BorderStroke(2.dp, DeepGreenDark) else null
                                    ) {
                                        AsyncImage(
                                            model = item.url,
                                            contentDescription = "Previous profile photo",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                            .border(1.dp, SageGreenLight, CircleShape)
                                            .clickable { onDelete(item) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Delete photo", tint = Color.Red, modifier = Modifier.size(13.dp))
                                    }
                                }
                                if (isCurrent) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "Current", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = DeepGreenDark)
                                }
                            }
                        }
                    }
                }
            }
        }
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
    isUploadingAvatar: Boolean = false,
    onAvatarClick: () -> Unit = {},
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
            Surface(
                modifier = Modifier.size(96.dp).then(
                    if (isEditing) Modifier.clickable(onClick = onAvatarClick) else Modifier
                ),
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 8.dp
            ) {
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
                    if (isUploadingAvatar) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(28.dp)
                            )
                        }
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
    onRemove: (ProfileEntry) -> Unit,
    onViewFile: ((String) -> Unit)? = null
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
                            if (entry.fileUrl.isNotBlank() && onViewFile != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(onClick = { onViewFile(entry.fileUrl) }, contentPadding = PaddingValues(0.dp)) {
                                    Icon(Icons.Default.AttachFile, contentDescription = null, tint = DeepGreenDark, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("View certificate", color = DeepGreenDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
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
private fun ProfileAccountActions(
    isEmployer: Boolean,
    resumeUrl: String? = null,
    onResumeClick: () -> Unit = {},
    onLogout: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = "ACCOUNT", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDark.copy(alpha = 0.4f))

        if (!isEmployer) {
            ProfileOptionItem(
                icon = Icons.Default.Description,
                title = if (!resumeUrl.isNullOrBlank()) "Resume / CV (uploaded)" else "Resume / CV",
                onClick = onResumeClick
            )
        }
        ProfileOptionItem(icon = Icons.Default.Notifications, title = "Notifications", onClick = { })

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEntryDialog(
    category: String,
    isUploadingFile: Boolean,
    onDismiss: () -> Unit,
    onSave: (ProfileEntry) -> Unit,
    onUploadCertificate: (ByteArray, String, (String?) -> Unit) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var subtitle by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var titleError by remember { mutableStateOf<String?>(null) }
    var fileError by remember { mutableStateOf<String?>(null) }
    var employmentType by remember { mutableStateOf(ProfileOptions.EMPLOYMENT_TYPES.first()) }
    var educationLevel by remember { mutableStateOf(ProfileOptions.EDUCATION_LEVELS[2]) }
    var issuer by remember { mutableStateOf(ProfileOptions.CERTIFICATE_ISSUERS.first()) }
    var customIssuer by remember { mutableStateOf("") }
    var startYear by remember { mutableStateOf(ProfileOptions.YEARS.getOrElse(1) { "" }) }
    var endYear by remember { mutableStateOf("Present") }
    var year by remember { mutableStateOf(ProfileOptions.YEARS.getOrElse(1) { "" }) }
    var fileUrl by remember { mutableStateOf("") }
    var fileName by remember { mutableStateOf("") }

    val certificatePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val mimeType = context.contentResolver.getType(uri).orEmpty()
        val extension = when {
            mimeType.contains("pdf") -> "pdf"
            mimeType.contains("png") -> "png"
            mimeType.contains("webp") -> "webp"
            mimeType.contains("jpeg") || mimeType.contains("jpg") -> "jpg"
            else -> null
        }
        if (extension == null) {
            fileError = "Please upload a PDF or image file."
            return@rememberLauncherForActivityResult
        }
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        if (bytes == null) {
            fileError = "Couldn't read the selected file."
            return@rememberLauncherForActivityResult
        }
        fileError = null
        onUploadCertificate(bytes, extension) { url ->
            if (url != null) {
                fileUrl = url
                fileName = uri.lastPathSegment?.substringAfterLast("/") ?: "certificate.$extension"
            } else {
                fileError = "Failed to upload certificate. Please try again."
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isUploadingFile) onDismiss() },
        title = { Text("Add $category", fontWeight = FontWeight.Bold, color = DeepGreenDark) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (category) {
                    "Experience" -> {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it.take(100); titleError = null },
                            label = { Text("Job Title") },
                            singleLine = true,
                            isError = titleError != null,
                            supportingText = { titleError?.let { Text(it, color = Color.Red, fontSize = 12.sp) } },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = subtitle,
                            onValueChange = { subtitle = it.take(100) },
                            label = { Text("Company") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("Employment type", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextDark.copy(alpha = 0.7f))
                        ProfileOptions.EMPLOYMENT_TYPES.forEach { type ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { employmentType = type },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = employmentType == type,
                                    onClick = { employmentType = type },
                                    colors = RadioButtonDefaults.colors(selectedColor = DeepGreenDark)
                                )
                                Text(type, fontSize = 14.sp, color = TextDark)
                            }
                        }
                        ProfileDropdownField(label = "Start year", value = startYear, options = ProfileOptions.YEARS.filter { it != "Present" }, onSelect = { startYear = it })
                        ProfileDropdownField(label = "End year", value = endYear, options = ProfileOptions.YEARS, onSelect = { endYear = it })
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it.take(300) },
                            label = { Text("Description (optional)") },
                            modifier = Modifier.fillMaxWidth().height(90.dp)
                        )
                    }
                    "Education" -> {
                        ProfileDropdownField(
                            label = "Qualification",
                            value = educationLevel,
                            options = ProfileOptions.EDUCATION_LEVELS,
                            onSelect = { educationLevel = it; titleError = null }
                        )
                        OutlinedTextField(
                            value = subtitle,
                            onValueChange = { subtitle = it.take(100) },
                            label = { Text("Institution") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        ProfileDropdownField(label = "Start year", value = startYear, options = ProfileOptions.YEARS.filter { it != "Present" }, onSelect = { startYear = it })
                        ProfileDropdownField(label = "End year", value = endYear, options = ProfileOptions.YEARS, onSelect = { endYear = it })
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it.take(300) },
                            label = { Text("Field of study (optional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    else -> {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it.take(100); titleError = null },
                            label = { Text("Certification name") },
                            singleLine = true,
                            isError = titleError != null,
                            supportingText = { titleError?.let { Text(it, color = Color.Red, fontSize = 12.sp) } },
                            modifier = Modifier.fillMaxWidth()
                        )
                        ProfileDropdownField(
                            label = "Issued by",
                            value = issuer,
                            options = ProfileOptions.CERTIFICATE_ISSUERS,
                            onSelect = { issuer = it }
                        )
                        if (issuer == "Other") {
                            OutlinedTextField(
                                value = customIssuer,
                                onValueChange = { customIssuer = it.take(100) },
                                label = { Text("Issuer name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        ProfileDropdownField(
                            label = "Year",
                            value = year,
                            options = ProfileOptions.YEARS.filter { it != "Present" },
                            onSelect = { year = it }
                        )
                        OutlinedButton(
                            onClick = { certificatePicker.launch(arrayOf("application/pdf", "image/*")) },
                            enabled = !isUploadingFile,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            if (isUploadingFile) {
                                CircularProgressIndicator(color = DeepGreenDark, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Uploading...")
                            } else {
                                Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (fileUrl.isBlank()) "Upload certificate file" else "Replace file")
                            }
                        }
                        if (fileName.isNotBlank()) {
                            Text(fileName, fontSize = 12.sp, color = DeepGreenDark)
                        }
                        fileError?.let { Text(it, color = Color.Red, fontSize = 12.sp) }
                        Text("PDF or image. Required.", fontSize = 12.sp, color = TextDark.copy(alpha = 0.5f))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isUploadingFile,
                onClick = {
                    when (category) {
                        "Experience" -> {
                            if (title.trim().isBlank()) {
                                titleError = "This field is required."
                                return@TextButton
                            }
                            onSave(
                                ProfileEntry(
                                    title = title.trim(),
                                    subtitle = subtitle.trim(),
                                    period = "$startYear - $endYear · $employmentType",
                                    description = description.trim()
                                )
                            )
                        }
                        "Education" -> {
                            onSave(
                                ProfileEntry(
                                    title = educationLevel,
                                    subtitle = subtitle.trim(),
                                    period = "$startYear - $endYear",
                                    description = description.trim()
                                )
                            )
                        }
                        else -> {
                            if (title.trim().isBlank()) {
                                titleError = "This field is required."
                                return@TextButton
                            }
                            if (fileUrl.isBlank()) {
                                fileError = "Please upload a certificate file."
                                return@TextButton
                            }
                            val issuerName = if (issuer == "Other") customIssuer.trim().ifBlank { "Other" } else issuer
                            onSave(
                                ProfileEntry(
                                    title = title.trim(),
                                    subtitle = issuerName,
                                    period = year,
                                    fileUrl = fileUrl
                                )
                            )
                        }
                    }
                }
            ) {
                Text("Add", color = DeepGreenDark, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isUploadingFile) {
                Text("Cancel", color = TextDark.copy(alpha = 0.6f))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileDropdownField(
    label: String,
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
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

@Composable
private fun ResumeDialog(
    resumeUrl: String?,
    isUploading: Boolean,
    onDismiss: () -> Unit,
    onUploadClick: () -> Unit,
    onViewClick: () -> Unit,
    onRemoveClick: () -> Unit
) {
    val hasResume = !resumeUrl.isNullOrBlank()

    AlertDialog(
        onDismissRequest = { if (!isUploading) onDismiss() },
        title = { Text("Resume / CV", fontWeight = FontWeight.Bold, color = DeepGreenDark) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier.size(60.dp).clip(CircleShape).background(SageGreenLight),
                    contentAlignment = Alignment.Center
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(color = DeepGreenDark, strokeWidth = 3.dp, modifier = Modifier.size(26.dp))
                    } else {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = DeepGreenDark,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = when {
                        isUploading -> "Uploading..."
                        hasResume -> "Resume uploaded"
                        else -> "No resume uploaded yet"
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (hasResume) "PDF" else "Upload a PDF so employers can see your resume.",
                    fontSize = 12.sp,
                    color = TextDark.copy(alpha = 0.55f),
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onUploadClick, enabled = !isUploading) {
                Text(if (hasResume) "Replace" else "Upload", color = DeepGreenDark, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row {
                if (hasResume) {
                    TextButton(onClick = onViewClick, enabled = !isUploading) {
                        Text("View", color = DeepGreenDark)
                    }
                    TextButton(onClick = onRemoveClick, enabled = !isUploading) {
                        Text("Remove", color = Color.Red)
                    }
                } else {
                    TextButton(onClick = onDismiss, enabled = !isUploading) {
                        Text("Cancel", color = TextDark.copy(alpha = 0.6f))
                    }
                }
            }
        }
    )
}