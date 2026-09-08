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
    onViewExistingApplication: () -> Unit = {},
    onNotInterested: () -> Unit = {}
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
            onViewCompanyDetails = onViewCompanyDetails,
            onNotInterested = {
                onNotInterested()
                navController.popBackStack()
            }
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
    onViewCompanyDetails: (String) -> Unit,
    onNotInterested: () -> Unit = onBackToHome
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
                        if (!alreadyApplied) {
                            OutlinedButton(
                                onClick = onNotInterested,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, SageGreenDark.copy(alpha = 0.4f))
                            ) {
                                Text("Not Interested", fontSize = 14.sp, color = TextDark, fontWeight = FontWeight.Medium)
                            }
                        }

                        Button(
                            onClick = if (alreadyApplied) onViewExistingApplication else onStartApplication,
                            enabled = alreadyApplied || !listingExpired,
                            modifier = Modifier
                                .weight(if (alreadyApplied) 1f else 1.2f)
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
                        resumeUri = resumeUri,
                        coverLetterFileName = coverLetterName.ifBlank { "Not attached (Optional)" },
                        coverLetterUri = coverLetterUri,
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
            // Certification entries carry an uploaded file (PDF/image). Include the
            // URL so the applicant's "Application status" screen and the employer's
            // view can detect it and render it as a "Tap to open file" link, the
            // same way the resume already does.
            if (entry.fileUrl.isNotBlank()) append(" ${entry.fileUrl}")
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