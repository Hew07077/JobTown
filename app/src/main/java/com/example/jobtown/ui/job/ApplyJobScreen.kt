package com.example.jobtown.ui.job

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.example.jobtown.data.model.Job
import com.example.jobtown.data.model.JobApplication
import com.example.jobtown.data.model.ProfileEntry
import com.example.jobtown.data.model.User
import com.example.jobtown.data.repository.UserRepository
import com.example.jobtown.ui.profile.ProfileOptions
import com.example.jobtown.ui.theme.*
import com.example.jobtown.utils.isJobListingExpired
import kotlinx.coroutines.launch

@Composable
fun ApplyJobScreen(
    navController: NavController,
    job: Job,
    currentUser: User?,
    existingApplication: JobApplication? = null,
    onApplySubmit: (JobApplication, (Boolean, String?) -> Unit) -> Unit,
    onViewCompanyDetails: (String) -> Unit = {},
    onViewExistingApplication: () -> Unit = {}
) {
    var isApplying by remember { mutableStateOf(false) }
    val alreadyApplied = existingApplication != null &&
            !existingApplication.status.equals("Cancelled", ignoreCase = true)
    val listingExpired = isJobListingExpired(job)

    if (!isApplying) {
        JobDetailsOverviewScreen(
            job = job,
            alreadyApplied = alreadyApplied,
            listingExpired = listingExpired,
            existingStatus = existingApplication?.status.orEmpty(),
            onBackToHome = { navController.popBackStack() },
            onStartApplication = { isApplying = true },
            onViewExistingApplication = onViewExistingApplication,
            onViewCompanyDetails = onViewCompanyDetails
        )
    } else {
        ApplicationFlowScreen(
            navController = navController,
            job = job,
            currentUser = currentUser,
            onApplySubmit = onApplySubmit,
            onCancelApplication = { isApplying = false }
        )
    }
}

// ==================== Job Details Overview Screen ====================

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun JobDetailsOverviewScreen(
    job: Job,
    alreadyApplied: Boolean,
    listingExpired: Boolean,
    existingStatus: String,
    onBackToHome: () -> Unit,
    onStartApplication: () -> Unit,
    onViewExistingApplication: () -> Unit,
    onViewCompanyDetails: (String) -> Unit
) {
    val displayTitle = job.title.ifBlank { "Untitled Position" }
    val displayCompany = job.companyName.ifBlank { "Company Name" }
    val displayLocation = job.location.ifBlank { "Location Undisclosed" }
    val displaySalary = job.salary.ifBlank { "Salary Not Specified" }
    val displayType = job.jobType.ifBlank { "Full-time" }
    val displayDescription = job.description.ifBlank { "No detailed description available for this role." }

    Scaffold(
        containerColor = BackgroundWhite,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Job Details",
                        fontWeight = FontWeight.Bold,
                        color = DeepGreenDark,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackToHome) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Home",
                            tint = DeepGreenDark
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SageGreenMain)
            )
        },
        bottomBar = {
            Surface(
                shadowElevation = 16.dp,
                color = Color.White
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    if (listingExpired) {
                        Text(
                            text = "This job listing has expired and is no longer accepting applications.",
                            fontSize = 12.sp,
                            color = Color(0xFFC62828),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )
                    } else if (alreadyApplied) {
                        Text(
                            text = "You already applied · ${existingStatus.ifBlank { "Pending" }}. Only one application is allowed per job.",
                            fontSize = 12.sp,
                            color = DeepGreenDark,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onBackToHome,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SageGreenDark.copy(alpha = 0.4f))
                        ) {
                            Text("Not Interested", fontSize = 14.sp, color = TextDark, fontWeight = FontWeight.Medium)
                        }

                        Button(
                            onClick = if (alreadyApplied) onViewExistingApplication else onStartApplication,
                            enabled = alreadyApplied || !listingExpired,
                            modifier = Modifier
                                .weight(1.2f)
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DeepGreenDark)
                        ) {
                            Text(
                                when {
                                    listingExpired && !alreadyApplied -> "Expired"
                                    alreadyApplied -> "View application"
                                    else -> "Apply Now"
                                },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            if (!alreadyApplied && !listingExpired) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = displayTitle,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = SageGreenLight.copy(alpha = 0.4f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SageGreenDark.copy(alpha = 0.15f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onViewCompanyDetails(job.employerId ?: displayCompany) }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = DeepGreenDark,
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.Business,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = displayCompany,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DeepGreenDark
                                    )
                                    Text(
                                        text = "View company profile & active roles",
                                        fontSize = 11.sp,
                                        color = SageGreenDark,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = DeepGreenDark,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        InfoBadge(icon = Icons.Filled.LocationOn, text = displayLocation)
                        InfoBadge(icon = Icons.Filled.Work, text = displayType)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = DeepGreenDark.copy(alpha = 0.06f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AttachMoney,
                                contentDescription = null,
                                tint = DeepGreenDark,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = displaySalary,
                                fontWeight = FontWeight.Bold,
                                color = DeepGreenDark,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Job Description",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = displayDescription,
                        fontSize = 13.sp,
                        color = TextDark.copy(alpha = 0.8f),
                        lineHeight = 20.sp
                    )

                    if (!job.requirements.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Requirements",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextDark
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        job.requirements.orEmpty().forEach { req ->
                            Row(
                                modifier = Modifier.padding(vertical = 4.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("•", fontSize = 14.sp, color = DeepGreenDark, fontWeight = FontWeight.Bold)
                                Text(
                                    text = req,
                                    fontSize = 13.sp,
                                    color = TextDark.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }

                    if (!job.skills.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Required Skills",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextDark
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            job.skills.orEmpty().forEach { skill ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = SageGreenLight
                                ) {
                                    Text(
                                        text = skill,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        fontSize = 11.sp,
                                        color = DeepGreenDark,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==================== Multi-Step Application Flow ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ApplicationFlowScreen(
    navController: NavController,
    job: Job,
    currentUser: User?,
    onApplySubmit: (JobApplication, (Boolean, String?) -> Unit) -> Unit,
    onCancelApplication: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Form States
    var phoneNumber by remember { mutableStateOf(currentUser?.phone ?: "") }
    var linkedInUrl by remember { mutableStateOf("") }

    // Salary Range States (Slider min/max values in thousands e.g., 2000 to 10000)
    var salaryMin by remember { mutableStateOf(3000f) }
    var salaryMax by remember { mutableStateOf(6000f) }

    // Start Date States
    var selectedStartDateOption by remember { mutableStateOf("Immediate") }
    var customStartDate by remember { mutableStateOf("") }

    // If the applicant already has a resume on file (uploaded from a previous
    // application or from their profile), pre-fill it here so they don't have
    // to re-upload the same document every time they apply. resumeUri holds a
    // real https:// URL in this case (as opposed to a local content:// URI
    // from the picker below), which the submit step uses to skip re-uploading.
    val savedProfileResumeUrl = currentUser?.resumeUrl.orEmpty()
    var resumeUri by remember { mutableStateOf(savedProfileResumeUrl) }
    var resumeName by remember {
        mutableStateOf(if (savedProfileResumeUrl.isNotBlank()) extractFileNameFromUrl(savedProfileResumeUrl) else "")
    }

    var coverLetterUri by remember { mutableStateOf("") }
    var coverLetterName by remember { mutableStateOf("") }

    var additionalNotes by remember { mutableStateOf("") }

    var errorMessage by remember { mutableStateOf("") }
    var successMessage by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var showValidationErrors by remember { mutableStateOf(false) }
    var currentStep by remember { mutableStateOf(0) }

    var educationEntries by remember { mutableStateOf<List<ProfileEntry>>(emptyList()) }
    var experienceEntries by remember { mutableStateOf<List<ProfileEntry>>(emptyList()) }
    var certificationEntries by remember { mutableStateOf<List<ProfileEntry>>(emptyList()) }
    var originalEducationEntries by remember { mutableStateOf<List<ProfileEntry>>(emptyList()) }
    var originalExperienceEntries by remember { mutableStateOf<List<ProfileEntry>>(emptyList()) }
    var originalCertificationEntries by remember { mutableStateOf<List<ProfileEntry>>(emptyList()) }
    var isLoadingQualifications by remember { mutableStateOf(true) }
    var qualificationsEdited by remember { mutableStateOf(false) }
    var isSavingQualifications by remember { mutableStateOf(false) }
    var isUploadingQualCertificate by remember { mutableStateOf(false) }

    LaunchedEffect(currentUser?.id) {
        isLoadingQualifications = true
        qualificationsEdited = false
        val userId = currentUser?.id.orEmpty()
        val fresh = if (userId.isNotBlank()) {
            UserRepository.fetchUserById(userId) ?: currentUser
        } else {
            currentUser
        }
        educationEntries = fresh?.educationEntries.orEmpty()
        experienceEntries = fresh?.experienceEntries.orEmpty()
        certificationEntries = fresh?.certificationEntries.orEmpty()
        originalEducationEntries = educationEntries
        originalExperienceEntries = experienceEntries
        originalCertificationEntries = certificationEntries
        isLoadingQualifications = false
    }

    val onAddQualificationEntry: (String, ProfileEntry) -> Unit = { category, entry ->
        qualificationsEdited = true
        when (category) {
            "Education" -> educationEntries = educationEntries + entry
            "Experience" -> experienceEntries = experienceEntries + entry
            "Certification" -> certificationEntries = certificationEntries + entry
        }
    }
    val onRemoveQualificationEntry: (String, ProfileEntry) -> Unit = { category, entry ->
        qualificationsEdited = true
        when (category) {
            "Education" -> educationEntries = educationEntries.filterNot { it.id == entry.id }
            "Experience" -> experienceEntries = experienceEntries.filterNot { it.id == entry.id }
            "Certification" -> certificationEntries = certificationEntries.filterNot { it.id == entry.id }
        }
    }
    val onUploadQualificationCertificate: (ByteArray, String, (String?) -> Unit) -> Unit = { bytes, ext, onDone ->
        val userId = currentUser?.id
        if (userId.isNullOrBlank()) {
            onDone(null)
        } else {
            coroutineScope.launch {
                isUploadingQualCertificate = true
                val url = UserRepository.uploadCertificate(userId, bytes, ext)
                isUploadingQualCertificate = false
                onDone(url)
            }
        }
    }

    // File Pickers
    val resumePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) {}

            resumeUri = it.toString()
            resumeName = getFileNameFromUri(context, it)
            errorMessage = ""
        }
    }

    val coverLetterPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) {}

            coverLetterUri = it.toString()
            coverLetterName = getFileNameFromUri(context, it)
            errorMessage = ""
        }
    }

    val animatedProgress by animateFloatAsState(
        targetValue = (currentStep + 1) / 4f,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "progress"
    )

    val displayTitle = job.title.ifBlank { "Untitled Position" }
    val displayCompany = job.companyName

    val isPhoneValid = phoneNumber.isNotBlank() && phoneNumber.length >= 7
    val isResumeValid = resumeUri.isNotBlank()
    // True while resumeUri still points at the applicant's already-hosted
    // profile resume rather than a freshly-picked local file, so we know to
    // skip re-uploading it and can show "saved from your profile" in the UI.
    val isSavedProfileResume = resumeUri.startsWith("http://") || resumeUri.startsWith("https://")

    Scaffold(
        containerColor = BackgroundWhite,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Job Application",
                            fontWeight = FontWeight.Bold,
                            color = DeepGreenDark,
                            fontSize = 18.sp
                        )
                        Surface(
                            shape = CircleShape,
                            color = SageGreenLight,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${currentStep + 1}/4",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepGreenDark
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentStep > 0) currentStep-- else onCancelApplication()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = DeepGreenDark
                        )
                    }
                },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        repeat(4) { index ->
                            Box(
                                modifier = Modifier
                                    .size(if (index == currentStep) 10.dp else 8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            index <= currentStep -> DeepGreenDark
                                            else -> TextDark.copy(alpha = 0.2f)
                                        }
                                    )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SageGreenMain)
            )
        }
    ) { paddingValues ->
        // imePadding() shrinks this whole column (form area + the fixed
        // Back/Next/Submit row below it) as the keyboard rises, instead of
        // the keyboard just covering whatever's focused with nothing moving.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Progress Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(SageGreenLight)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(SageGreenMain, DeepGreenDark)
                                )
                            )
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                when (currentStep) {
                    0 -> StepTitle(icon = Icons.Filled.Person, title = "Personal & Expectations", subtitle = "Set your contact info, salary range & start date")
                    1 -> StepTitle(icon = Icons.Filled.School, title = "Education & Experience", subtitle = "Add your education, work experience & certificates")
                    2 -> StepTitle(icon = Icons.Filled.Description, title = "Documents Upload", subtitle = "Attach your resume & optional cover letter")
                    3 -> StepTitle(icon = Icons.Filled.CheckCircle, title = "Review Application", subtitle = "Final check before sending your details")
                }

                when (currentStep) {
                    0 -> Step1PersonalInfo(
                        phoneNumber = phoneNumber,
                        onPhoneNumberChange = { phoneNumber = it },
                        linkedInUrl = linkedInUrl,
                        onLinkedInUrlChange = { linkedInUrl = it },
                        salaryMin = salaryMin,
                        salaryMax = salaryMax,
                        onSalaryRangeChange = { min, max ->
                            salaryMin = min
                            salaryMax = max
                        },
                        selectedStartDateOption = selectedStartDateOption,
                        onStartDateOptionChange = { selectedStartDateOption = it },
                        customStartDate = customStartDate,
                        onCustomStartDateChange = { customStartDate = it },
                        showValidationErrors = showValidationErrors,
                        isPhoneValid = isPhoneValid
                    )
                    1 -> StepQualifications(
                        educationEntries = educationEntries,
                        experienceEntries = experienceEntries,
                        certificationEntries = certificationEntries,
                        onAddEntry = onAddQualificationEntry,
                        onRemoveEntry = onRemoveQualificationEntry,
                        isUploadingCertificate = isUploadingQualCertificate,
                        onUploadCertificate = onUploadQualificationCertificate,
                        showValidationErrors = showValidationErrors,
                        isLoading = isLoadingQualifications
                    )
                    2 -> Step2Documents(
                        resumeFileName = resumeName,
                        isResumeValid = isResumeValid,
                        isSavedProfileResume = isSavedProfileResume,
                        onPickResume = { resumePickerLauncher.launch(arrayOf("application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document")) },
                        onRemoveResume = { resumeUri = ""; resumeName = "" },
                        coverLetterFileName = coverLetterName,
                        onPickCoverLetter = { coverLetterPickerLauncher.launch(arrayOf("application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document")) },
                        onRemoveCoverLetter = { coverLetterUri = ""; coverLetterName = "" },
                        additionalNotes = additionalNotes,
                        onAdditionalNotesChange = { additionalNotes = it },
                        showValidationErrors = showValidationErrors
                    )
                    3 -> Step3Review(
                        jobTitle = displayTitle,
                        companyName = displayCompany,
                        resumeFileName = resumeName.ifBlank { "No resume attached" },
                        coverLetterFileName = coverLetterName.ifBlank { "Not attached (Optional)" },
                        additionalNotes = additionalNotes,
                        phoneNumber = phoneNumber,
                        linkedInUrl = linkedInUrl,
                        salaryRangeText = "RM ${salaryMin.toInt()} - RM ${salaryMax.toInt()}",
                        startDateText = if (selectedStartDateOption == "Custom Date" && customStartDate.isNotBlank()) customStartDate else selectedStartDateOption,
                        educationSummary = formatProfileEntries(educationEntries),
                        experienceSummary = formatProfileEntries(experienceEntries),
                        certificatesSummary = formatProfileEntries(certificationEntries)
                    )
                }

                if (errorMessage.isNotEmpty() && showValidationErrors) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = errorMessage,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                if (successMessage.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SageGreenLight),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Filled.CheckCircle, contentDescription = null, tint = DeepGreenDark, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = successMessage, color = DeepGreenDark, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Fixed action row, outside the scrollable area, so it's always
            // reachable and rides up with imePadding() instead of scrolling
            // away or getting buried under the keyboard.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                    OutlinedButton(
                        onClick = {
                            if (currentStep > 0) currentStep-- else onCancelApplication()
                        },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (currentStep > 0) "Back" else "Overview", fontSize = 14.sp)
                    }

                    Button(
                        onClick = {
                            if (currentStep < 3) {
                                when (currentStep) {
                                    0 -> {
                                        showValidationErrors = true
                                        if (!isPhoneValid) {
                                            errorMessage = "Please enter a valid phone number."
                                        } else {
                                            errorMessage = ""
                                            showValidationErrors = false
                                            currentStep++
                                        }
                                    }
                                    1 -> {
                                        showValidationErrors = true
                                        if (educationEntries.isEmpty()) {
                                            errorMessage = "Please add at least one education entry to continue."
                                        } else {
                                            errorMessage = ""
                                            showValidationErrors = false
                                            val entriesChanged = qualificationsEdited ||
                                                educationEntries != originalEducationEntries ||
                                                experienceEntries != originalExperienceEntries ||
                                                certificationEntries != originalCertificationEntries
                                            if (entriesChanged && currentUser != null && currentUser.id.isNotBlank()) {
                                                isSavingQualifications = true
                                                coroutineScope.launch {
                                                    try {
                                                        UserRepository.updateUserInSupabase(
                                                            currentUser.copy(
                                                                educationEntries = educationEntries,
                                                                experienceEntries = experienceEntries,
                                                                certificationEntries = certificationEntries
                                                            )
                                                        )
                                                        originalEducationEntries = educationEntries
                                                        originalExperienceEntries = experienceEntries
                                                        originalCertificationEntries = certificationEntries
                                                        qualificationsEdited = false
                                                    } catch (e: Exception) {
                                                        Log.e("ApplyJobScreen", "Failed to save qualifications to profile", e)
                                                    } finally {
                                                        isSavingQualifications = false
                                                        currentStep++
                                                    }
                                                }
                                            } else {
                                                currentStep++
                                            }
                                        }
                                    }
                                    2 -> {
                                        showValidationErrors = true
                                        if (!isResumeValid) {
                                            errorMessage = "Please attach your resume document to proceed."
                                        } else {
                                            errorMessage = ""
                                            showValidationErrors = false
                                            currentStep++
                                        }
                                    }
                                }
                            } else {
                                if (!isResumeValid) {
                                    errorMessage = "Please attach your resume document."
                                    showValidationErrors = true
                                    return@Button
                                }
                                val applicant = currentUser
                                if (applicant == null || applicant.id.isBlank()) {
                                    errorMessage = "You must be signed in to apply."
                                    showValidationErrors = true
                                    return@Button
                                }

                                isSubmitting = true
                                coroutineScope.launch {
                                    try {
                                        // resumeUri is already a real, publicly-reachable URL
                                        // when it came from the applicant's saved profile resume
                                        // (isSavedProfileResume) -- nothing to upload, just reuse
                                        // it directly. Otherwise it's a local content:// URI from
                                        // the picker, which only exists on this device and means
                                        // nothing to anyone else (e.g. the employer), so it needs
                                        // to be uploaded to Supabase Storage first.
                                        val uploadedResumeUrl: String
                                        if (isSavedProfileResume) {
                                            uploadedResumeUrl = resumeUri
                                        } else {
                                            val resumeBytes = try {
                                                context.contentResolver.openInputStream(Uri.parse(resumeUri))?.use { it.readBytes() }
                                            } catch (e: Exception) {
                                                null
                                            }

                                            if (resumeBytes == null) {
                                                errorMessage = "Couldn't read the resume file. Please pick it again."
                                                showValidationErrors = true
                                                isSubmitting = false
                                                return@launch
                                            }

                                            val newlyUploadedUrl = UserRepository.uploadResume(applicant.id, resumeBytes)
                                            if (newlyUploadedUrl == null) {
                                                errorMessage = "Failed to upload resume. Please check your connection and try again."
                                                showValidationErrors = true
                                                isSubmitting = false
                                                return@launch
                                            }
                                            uploadedResumeUrl = newlyUploadedUrl

                                            // Save the newly uploaded resume onto the applicant's
                                            // profile so next time they apply it's already there
                                            // and doesn't need to be picked/uploaded again. This is
                                            // best-effort -- if it fails, the application itself
                                            // still goes through with the resume attached.
                                            try {
                                                UserRepository.updateUserInSupabase(applicant.copy(resumeUrl = uploadedResumeUrl))
                                            } catch (e: Exception) {
                                                Log.e("ApplyJobScreen", "Failed to save resume to profile", e)
                                            }
                                        }

                                        val finalStart = if (selectedStartDateOption == "Custom Date" && customStartDate.isNotBlank()) customStartDate else selectedStartDateOption
                                        val finalSalaryRange = "RM ${salaryMin.toInt()} - RM ${salaryMax.toInt()}"

                                        val application = JobApplication(
                                            id = "app_${System.currentTimeMillis()}",
                                            jobId = job.id,
                                            userId = applicant.id,
                                            jobTitle = displayTitle,
                                            companyName = displayCompany,
                                            employerId = job.employerId ?: job.postedByUserId ?: "",
                                            applicantName = applicant.name ?: "Unknown Applicant",
                                            applicantEmail = applicant.email ?: "",
                                            resumeUrl = uploadedResumeUrl,
                                            coverLetter = coverLetterUri.ifBlank { additionalNotes.trim() },
                                            status = "Pending",
                                            location = job.location,
                                            education = formatProfileEntries(educationEntries),
                                            experience = formatProfileEntries(experienceEntries),
                                            certificates = formatProfileEntries(certificationEntries)
                                        )
                                        // Wait for the actual backend result instead of
                                        // optimistically claiming success - a blocked
                                        // duplicate or a failed insert must surface to the
                                        // applicant, not silently pop them back to a stale
                                        // "submitted" state.
                                        onApplySubmit(application) { success, message ->
                                            if (success) {
                                                successMessage = "Application submitted successfully!"
                                                // Navigation onward (to the Applied tab) is
                                                // owned by the caller once the backend confirms.
                                            } else {
                                                errorMessage = message ?: "Failed to submit application. Please try again."
                                                showValidationErrors = true
                                                isSubmitting = false
                                            }
                                        }
                                    } catch (e: Exception) {
                                        errorMessage = "Failed to submit: ${e.message}"
                                        showValidationErrors = true
                                        isSubmitting = false
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (currentStep == 3) DeepGreenDark else SageGreenMain
                        ),
                        enabled = !isSubmitting && !isSavingQualifications
                    ) {
                        when {
                            isSubmitting && currentStep == 3 -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = Color.White,
                                    strokeWidth = 2.5.dp
                                )
                            }
                            isSavingQualifications && currentStep == 1 -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = DeepGreenDark,
                                    strokeWidth = 2.5.dp
                                )
                            }
                            currentStep == 3 -> {
                                Icon(imageVector = Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Submit", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            else -> {
                                Text("Next", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DeepGreenDark)
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = DeepGreenDark, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
    }
}

// ==================== Component Helpers ====================

private fun getFileNameFromUri(context: android.content.Context, uri: android.net.Uri): String {
    var name = ""
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    cursor?.use { c ->
        val nameIndex = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        if (c.moveToFirst() && nameIndex != -1) {
            name = c.getString(nameIndex)
        }
    }
    if (name.isBlank()) {
        name = uri.lastPathSegment ?: "Attached Document.pdf"
    }
    return name
}

/** Best-effort display name for an already-hosted resume URL from the applicant's profile. */
private fun extractFileNameFromUrl(url: String): String {
    val rawName = url.substringBefore("?").substringAfterLast("/")
    return rawName.ifBlank { "Resume.pdf" }
}

/**
 * Flattens the applicant's education/experience/certification entries into the
 * single-line-per-entry text that JobApplication.education/experience/certificates
 * store, so what the employer (and the applicant's own "Application status" screen)
 * sees isn't stuck on "Not specified" even when qualifications were filled in.
 */
private fun formatProfileEntries(entries: List<ProfileEntry>): String {
    return entries.joinToString("\n") { entry ->
        buildString {
            append(entry.title)
            if (entry.subtitle.isNotBlank()) append(" — ${entry.subtitle}")
            if (entry.period.isNotBlank()) append(" (${entry.period})")
        }
    }
}

@Composable
private fun InfoBadge(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = DeepGreenDark,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            fontSize = 13.sp,
            color = TextDark.copy(alpha = 0.8f),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun StepTitle(icon: ImageVector, title: String, subtitle: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = SageGreenMain.copy(alpha = 0.35f),
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = DeepGreenDark,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Column {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = TextDark.copy(alpha = 0.6f)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Step1PersonalInfo(
    phoneNumber: String,
    onPhoneNumberChange: (String) -> Unit,
    linkedInUrl: String,
    onLinkedInUrlChange: (String) -> Unit,
    salaryMin: Float,
    salaryMax: Float,
    onSalaryRangeChange: (Float, Float) -> Unit,
    selectedStartDateOption: String,
    onStartDateOptionChange: (String) -> Unit,
    customStartDate: String,
    onCustomStartDateChange: (String) -> Unit,
    showValidationErrors: Boolean,
    isPhoneValid: Boolean
) {
    val startDateOptions = listOf("Immediate", "Within 2 Weeks", "Within 1 Month", "Custom Date")

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
            value = phoneNumber,
            onValueChange = onPhoneNumberChange,
            label = { Text("Phone Number *") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            isError = showValidationErrors && !isPhoneValid,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        )

        OutlinedTextField(
            value = linkedInUrl,
            onValueChange = onLinkedInUrlChange,
            label = { Text("LinkedIn Profile / Portfolio URL (Optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        )

        // Salary Range Selection Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(1.dp, SageGreenDark.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Expected Monthly Salary Range",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DeepGreenDark
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SageGreenLight
                    ) {
                        Text(
                            text = "RM ${salaryMin.toInt()} - RM ${salaryMax.toInt()}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepGreenDark
                        )
                    }
                }

                // Simplified Single-slider proxy or dual visual slider representation
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Minimum Expected (RM ${salaryMin.toInt()})",
                    fontSize = 11.sp,
                    color = TextDark.copy(alpha = 0.6f)
                )
                Slider(
                    value = salaryMin,
                    onValueChange = { newVal ->
                        if (newVal <= salaryMax) {
                            onSalaryRangeChange(newVal, salaryMax)
                        }
                    },
                    valueRange = 1500f..15000f,
                    steps = 26,
                    colors = SliderDefaults.colors(
                        thumbColor = DeepGreenDark,
                        activeTrackColor = DeepGreenDark,
                        inactiveTrackColor = SageGreenLight
                    )
                )

                Text(
                    text = "Maximum Expected (RM ${salaryMax.toInt()})",
                    fontSize = 11.sp,
                    color = TextDark.copy(alpha = 0.6f)
                )
                Slider(
                    value = salaryMax,
                    onValueChange = { newVal ->
                        if (newVal >= salaryMin) {
                            onSalaryRangeChange(salaryMin, newVal)
                        }
                    },
                    valueRange = 1500f..15000f,
                    steps = 26,
                    colors = SliderDefaults.colors(
                        thumbColor = DeepGreenDark,
                        activeTrackColor = DeepGreenDark,
                        inactiveTrackColor = SageGreenLight
                    )
                )
            }
        }

        // Start Date Selection Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(1.dp, SageGreenDark.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Available Start Date",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DeepGreenDark
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    startDateOptions.forEach { option ->
                        val isSelected = selectedStartDateOption == option
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) DeepGreenDark else SageGreenLight.copy(alpha = 0.4f),
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.dp,
                                color = if (isSelected) DeepGreenDark else SageGreenDark.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.clickable { onStartDateOptionChange(option) }
                        ) {
                            Text(
                                text = option,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else DeepGreenDark
                            )
                        }
                    }
                }

                if (selectedStartDateOption == "Custom Date") {
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = customStartDate,
                        onValueChange = onCustomStartDateChange,
                        label = { Text("Specify Date (e.g., 15 Sept 2026)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }
    }
}

// ==================== Step: Education / Experience / Certifications ====================

@Composable
private fun StepQualifications(
    educationEntries: List<ProfileEntry>,
    experienceEntries: List<ProfileEntry>,
    certificationEntries: List<ProfileEntry>,
    onAddEntry: (String, ProfileEntry) -> Unit,
    onRemoveEntry: (String, ProfileEntry) -> Unit,
    isUploadingCertificate: Boolean,
    onUploadCertificate: (ByteArray, String, (String?) -> Unit) -> Unit,
    showValidationErrors: Boolean,
    isLoading: Boolean
) {
    var addDialogFor by remember { mutableStateOf<String?>(null) }
    val uriHandler = LocalUriHandler.current

    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text(
            text = "Loaded from your profile. Add or remove anything for this application — changes are saved back to your profile.",
            fontSize = 12.sp,
            color = TextDark.copy(alpha = 0.6f),
            lineHeight = 16.sp
        )

        if (isLoading) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(color = DeepGreenDark, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    Text("Loading your education and experience…", fontSize = 13.sp, color = TextDark.copy(alpha = 0.7f))
                }
            }
        } else {
            QualificationSection(
                icon = Icons.Filled.School,
                title = "Education",
                entries = educationEntries,
                addLabel = "Add education",
                emptyText = "Add your highest qualification so employers know your background.",
                onAddClick = { addDialogFor = "Education" },
                onRemove = { onRemoveEntry("Education", it) },
                isError = showValidationErrors && educationEntries.isEmpty(),
                errorText = "Please add at least one education entry."
            )

            QualificationSection(
                icon = Icons.Filled.WorkHistory,
                title = "Work Experience (optional)",
                entries = experienceEntries,
                addLabel = "Add experience",
                emptyText = "Add relevant work experience, if any.",
                onAddClick = { addDialogFor = "Experience" },
                onRemove = { onRemoveEntry("Experience", it) }
            )

            QualificationSection(
                icon = Icons.Filled.WorkspacePremium,
                title = "Certifications (optional)",
                entries = certificationEntries,
                addLabel = "Add certification",
                emptyText = "Add any certifications relevant to this role.",
                onAddClick = { addDialogFor = "Certification" },
                onRemove = { onRemoveEntry("Certification", it) },
                onViewFile = { url -> uriHandler.openUri(url) }
            )
        }
    }

    addDialogFor?.let { category ->
        AddQualificationDialog(
            category = category,
            isUploadingFile = isUploadingCertificate,
            onDismiss = { addDialogFor = null },
            onSave = { entry ->
                onAddEntry(category, entry)
                addDialogFor = null
            },
            onUploadCertificate = onUploadCertificate
        )
    }
}

@Composable
private fun QualificationSection(
    icon: ImageVector,
    title: String,
    entries: List<ProfileEntry>,
    addLabel: String,
    emptyText: String,
    onAddClick: () -> Unit,
    onRemove: (ProfileEntry) -> Unit,
    isError: Boolean = false,
    errorText: String = "",
    onViewFile: ((String) -> Unit)? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, tint = DeepGreenDark, modifier = Modifier.size(18.dp))
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextDark)
            if (entries.isNotEmpty()) {
                Surface(shape = CircleShape, color = SageGreenLight) {
                    Text(
                        text = "${entries.size}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepGreenDark,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }

        if (entries.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SageGreenLight.copy(alpha = 0.25f),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = if (isError) MaterialTheme.colorScheme.error.copy(alpha = 0.6f) else SageGreenDark.copy(alpha = 0.2f)
                ),
                modifier = Modifier.fillMaxWidth().clickable(onClick = onAddClick)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = emptyText, fontSize = 12.sp, color = TextDark.copy(alpha = 0.6f), lineHeight = 16.sp)
                        if (isError) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = errorText, fontSize = 11.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
                        }
                    }
                    Icon(Icons.Filled.Add, contentDescription = addLabel, tint = DeepGreenDark, modifier = Modifier.size(22.dp))
                }
            }
        } else {
            entries.forEach { entry ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.Top) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SageGreenLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(icon, contentDescription = null, tint = DeepGreenDark, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = entry.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextDark)
                            if (entry.subtitle.isNotBlank()) Text(text = entry.subtitle, fontSize = 12.sp, color = DeepGreenDark)
                            if (entry.period.isNotBlank()) Text(text = entry.period, fontSize = 11.sp, color = TextDark.copy(alpha = 0.5f))
                            if (entry.description.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = entry.description, fontSize = 12.sp, color = TextDark.copy(alpha = 0.75f), lineHeight = 16.sp)
                            }
                            if (entry.fileUrl.isNotBlank() && onViewFile != null) {
                                TextButton(onClick = { onViewFile(entry.fileUrl) }, contentPadding = PaddingValues(0.dp)) {
                                    Icon(Icons.Default.AttachFile, contentDescription = null, tint = DeepGreenDark, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("View file", color = DeepGreenDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                        IconButton(onClick = { onRemove(entry) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Remove", tint = TextDark.copy(alpha = 0.35f), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
            OutlinedButton(
                onClick = onAddClick,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = DeepGreenDark)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(addLabel, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddQualificationDialog(
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
    var subtitleError by remember { mutableStateOf<String?>(null) }
    var fileError by remember { mutableStateOf<String?>(null) }
    var employmentType by remember { mutableStateOf(ProfileOptions.EMPLOYMENT_TYPES.first()) }
    var educationLevel by remember { mutableStateOf(ProfileOptions.EDUCATION_LEVELS.getOrElse(2) { ProfileOptions.EDUCATION_LEVELS.first() }) }
    var issuer by remember { mutableStateOf(ProfileOptions.CERTIFICATE_ISSUERS.first()) }
    var customIssuer by remember { mutableStateOf("") }
    var startYear by remember { mutableStateOf(ProfileOptions.YEARS.getOrElse(1) { "" }) }
    var endYear by remember { mutableStateOf("Present") }
    var year by remember { mutableStateOf(ProfileOptions.YEARS.getOrElse(1) { "" }) }
    var fileUrl by remember { mutableStateOf("") }
    var fileName by remember { mutableStateOf("") }

    val headerIcon = when (category) {
        "Education" -> Icons.Filled.School
        "Experience" -> Icons.Filled.WorkHistory
        else -> Icons.Filled.WorkspacePremium
    }
    val headerSubtitle = when (category) {
        "Education" -> "School, college, or university details"
        "Experience" -> "Role, company, and the years you worked there"
        else -> "Certificate name, issuer, and an optional file"
    }

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

    fun trySave() {
        when (category) {
            "Experience" -> {
                if (title.trim().isBlank()) {
                    titleError = "Job title is required."
                    return
                }
                if (subtitle.trim().isBlank()) {
                    subtitleError = "Company is required."
                    return
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
                if (subtitle.trim().isBlank()) {
                    subtitleError = "Institution is required."
                    return
                }
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
                    titleError = "Certification name is required."
                    return
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

    Dialog(
        onDismissRequest = { if (!isUploadingFile) onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !isUploadingFile,
            dismissOnClickOutside = false
        )
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = BackgroundWhite,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("Add $category", fontWeight = FontWeight.Bold, color = DeepGreenDark, fontSize = 18.sp)
                            Text(headerSubtitle, fontSize = 12.sp, color = TextDark.copy(alpha = 0.55f))
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss, enabled = !isUploadingFile) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = DeepGreenDark)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = SageGreenMain)
                )
            },
            bottomBar = {
                Surface(color = Color.White, shadowElevation = 12.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            enabled = !isUploadingFile,
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Cancel", fontWeight = FontWeight.Medium)
                        }
                        Button(
                            onClick = { trySave() },
                            enabled = !isUploadingFile,
                            modifier = Modifier.weight(1.3f).height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DeepGreenDark)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = SageGreenLight.copy(alpha = 0.45f)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(headerIcon, contentDescription = null, tint = DeepGreenDark, modifier = Modifier.size(24.dp))
                        }
                        Column {
                            Text("New $category", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextDark)
                            Text(headerSubtitle, fontSize = 12.sp, color = TextDark.copy(alpha = 0.6f), lineHeight = 16.sp)
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        when (category) {
                            "Experience" -> {
                                OutlinedTextField(
                                    value = title,
                                    onValueChange = { title = it.take(100); titleError = null },
                                    label = { Text("Job title") },
                                    placeholder = { Text("e.g. Marketing Intern") },
                                    singleLine = true,
                                    isError = titleError != null,
                                    supportingText = { titleError?.let { Text(it, color = Color.Red, fontSize = 12.sp) } },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                OutlinedTextField(
                                    value = subtitle,
                                    onValueChange = { subtitle = it.take(100); subtitleError = null },
                                    label = { Text("Company") },
                                    placeholder = { Text("e.g. JobTown Sdn Bhd") },
                                    singleLine = true,
                                    isError = subtitleError != null,
                                    supportingText = { subtitleError?.let { Text(it, color = Color.Red, fontSize = 12.sp) } },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                QualDropdownField(label = "Employment type", value = employmentType, options = ProfileOptions.EMPLOYMENT_TYPES, onSelect = { employmentType = it })
                                QualDropdownField(label = "Start year", value = startYear, options = ProfileOptions.YEARS.filter { it != "Present" }, onSelect = { startYear = it })
                                QualDropdownField(label = "End year", value = endYear, options = ProfileOptions.YEARS, onSelect = { endYear = it })
                                OutlinedTextField(
                                    value = description,
                                    onValueChange = { description = it.take(300) },
                                    label = { Text("Description (optional)") },
                                    placeholder = { Text("What did you work on?") },
                                    modifier = Modifier.fillMaxWidth().height(110.dp),
                                    shape = RoundedCornerShape(14.dp)
                                )
                            }
                            "Education" -> {
                                QualDropdownField(
                                    label = "Qualification",
                                    value = educationLevel,
                                    options = ProfileOptions.EDUCATION_LEVELS,
                                    onSelect = { educationLevel = it }
                                )
                                OutlinedTextField(
                                    value = subtitle,
                                    onValueChange = { subtitle = it.take(100); subtitleError = null },
                                    label = { Text("Institution") },
                                    placeholder = { Text("e.g. Universiti Malaya") },
                                    singleLine = true,
                                    isError = subtitleError != null,
                                    supportingText = { subtitleError?.let { Text(it, color = Color.Red, fontSize = 12.sp) } },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                QualDropdownField(label = "Start year", value = startYear, options = ProfileOptions.YEARS.filter { it != "Present" }, onSelect = { startYear = it })
                                QualDropdownField(label = "End year", value = endYear, options = ProfileOptions.YEARS, onSelect = { endYear = it })
                                OutlinedTextField(
                                    value = description,
                                    onValueChange = { description = it.take(300) },
                                    label = { Text("Field of study (optional)") },
                                    placeholder = { Text("e.g. Computer Science") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp)
                                )
                            }
                            else -> {
                                OutlinedTextField(
                                    value = title,
                                    onValueChange = { title = it.take(100); titleError = null },
                                    label = { Text("Certification name") },
                                    placeholder = { Text("e.g. Google UX Design") },
                                    singleLine = true,
                                    isError = titleError != null,
                                    supportingText = { titleError?.let { Text(it, color = Color.Red, fontSize = 12.sp) } },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                QualDropdownField(
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
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                }
                                QualDropdownField(
                                    label = "Year",
                                    value = year,
                                    options = ProfileOptions.YEARS.filter { it != "Present" },
                                    onSelect = { year = it }
                                )
                                OutlinedButton(
                                    onClick = { certificatePicker.launch(arrayOf("application/pdf", "image/*")) },
                                    enabled = !isUploadingFile,
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    if (isUploadingFile) {
                                        CircularProgressIndicator(color = DeepGreenDark, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Uploading…")
                                    } else {
                                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(if (fileUrl.isBlank()) "Upload certificate file (optional)" else "Replace file")
                                    }
                                }
                                if (fileName.isNotBlank()) {
                                    Text(fileName, fontSize = 12.sp, color = DeepGreenDark)
                                }
                                fileError?.let { Text(it, color = Color.Red, fontSize = 12.sp) }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QualDropdownField(
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
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(option) }, onClick = { onSelect(option); expanded = false })
            }
        }
    }
}

@Composable
private fun Step2Documents(
    resumeFileName: String,
    isResumeValid: Boolean,
    isSavedProfileResume: Boolean,
    onPickResume: () -> Unit,
    onRemoveResume: () -> Unit,
    coverLetterFileName: String,
    onPickCoverLetter: () -> Unit,
    onRemoveCoverLetter: () -> Unit,
    additionalNotes: String,
    onAdditionalNotesChange: (String) -> Unit,
    showValidationErrors: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Resume Section (Required)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Resume Document *",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (showValidationErrors && !isResumeValid) MaterialTheme.colorScheme.error else DeepGreenDark
            )

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (showValidationErrors && !isResumeValid) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f) else SageGreenLight.copy(alpha = 0.3f),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = if (showValidationErrors && !isResumeValid) MaterialTheme.colorScheme.error else SageGreenDark.copy(alpha = 0.3f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPickResume() }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isSavedProfileResume) SageGreenDark else DeepGreenDark,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = when {
                                        isSavedProfileResume -> Icons.Filled.CheckCircle
                                        isResumeValid -> Icons.Filled.Description
                                        else -> Icons.Filled.UploadFile
                                    },
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = when {
                                        isSavedProfileResume -> "Resume on file"
                                        isResumeValid -> resumeFileName
                                        else -> "Upload Resume (PDF / Doc)"
                                    },
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (isSavedProfileResume) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = SageGreenDark.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "Saved",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SageGreenDark,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = when {
                                    isSavedProfileResume -> "From your profile · Tap to use a different file"
                                    isResumeValid -> "Tap to change document"
                                    else -> "Required for application submission"
                                },
                                fontSize = 11.sp,
                                color = SageGreenDark
                            )
                        }
                    }

                    if (isResumeValid) {
                        IconButton(onClick = onRemoveResume) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Remove file",
                                tint = TextDark.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = DeepGreenDark,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Cover Letter Section (Optional Attachment)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Cover Letter Attachment (Optional)",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = DeepGreenDark
            )

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = SageGreenLight.copy(alpha = 0.2f),
                border = androidx.compose.foundation.BorderStroke(width = 1.dp, color = SageGreenDark.copy(alpha = 0.2f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPickCoverLetter() }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = SageGreenDark,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (coverLetterFileName.isNotBlank()) Icons.Filled.NoteAlt else Icons.Filled.PostAdd,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (coverLetterFileName.isNotBlank()) coverLetterFileName else "Upload Cover Letter Document",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark,
                                maxLines = 1
                            )
                            Text(
                                text = if (coverLetterFileName.isNotBlank()) "Tap to change cover letter file" else "PDF or Doc format (Optional)",
                                fontSize = 11.sp,
                                color = SageGreenDark
                            )
                        }
                    }

                    if (coverLetterFileName.isNotBlank()) {
                        IconButton(onClick = onRemoveCoverLetter) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Remove file",
                                tint = TextDark.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = DeepGreenDark,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        OutlinedTextField(
            value = additionalNotes,
            onValueChange = onAdditionalNotesChange,
            label = { Text("Additional Notes / Remarks (Optional)") },
            minLines = 3,
            maxLines = 5,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        )
    }
}

@Composable
private fun Step3Review(
    jobTitle: String,
    companyName: String,
    resumeFileName: String,
    coverLetterFileName: String,
    additionalNotes: String,
    phoneNumber: String,
    linkedInUrl: String,
    salaryRangeText: String,
    startDateText: String,
    educationSummary: String,
    experienceSummary: String,
    certificatesSummary: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "Position: $jobTitle", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = DeepGreenDark)
            Text(text = "Company: $companyName", fontSize = 13.sp, color = TextDark.copy(alpha = 0.8f))
            HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)
            ReviewRow(label = "Phone Number", value = phoneNumber)
            ReviewRow(label = "LinkedIn / Portfolio", value = linkedInUrl.ifBlank { "Not provided" })
            ReviewRow(label = "Expected Salary Range", value = salaryRangeText)
            ReviewRow(label = "Available Start Date", value = startDateText)
            HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)
            ReviewRow(label = "Education", value = educationSummary.ifBlank { "Not specified" })
            ReviewRow(label = "Experience", value = experienceSummary.ifBlank { "Not specified" })
            ReviewRow(label = "Certificates", value = certificatesSummary.ifBlank { "None attached" })
            HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)
            ReviewRow(label = "Attached Resume", value = resumeFileName)
            ReviewRow(label = "Cover Letter Document", value = coverLetterFileName)
            if (additionalNotes.isNotBlank()) {
                ReviewRow(label = "Additional Notes", value = additionalNotes)
            }
        }
    }
}

@Composable
private fun ReviewRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SageGreenDark)
        Text(text = value, fontSize = 13.sp, color = TextDark, maxLines = 5)
    }
}