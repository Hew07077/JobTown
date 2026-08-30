package com.example.jobtown.ui.job

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.jobtown.data.model.Job
import com.example.jobtown.data.model.JobApplication
import com.example.jobtown.data.model.User
import com.example.jobtown.data.repository.UserRepository
import com.example.jobtown.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ApplyJobScreen(
    navController: NavController,
    job: Job,
    currentUser: User?,
    onApplySubmit: (JobApplication) -> Unit,
    onViewCompanyDetails: (String) -> Unit = {}
) {
    var isApplying by remember { mutableStateOf(false) }

    if (!isApplying) {
        JobDetailsOverviewScreen(
            job = job,
            onBackToHome = { navController.popBackStack() },
            onStartApplication = { isApplying = true },
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
    onBackToHome: () -> Unit,
    onStartApplication: () -> Unit,
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
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
                        onClick = onStartApplication,
                        modifier = Modifier
                            .weight(1.2f)
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DeepGreenDark)
                    ) {
                        Text("Apply Now", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
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
    onApplySubmit: (JobApplication) -> Unit,
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

    var resumeUri by remember { mutableStateOf("") }
    var resumeName by remember { mutableStateOf("") }

    var coverLetterUri by remember { mutableStateOf("") }
    var coverLetterName by remember { mutableStateOf("") }

    var additionalNotes by remember { mutableStateOf("") }

    var errorMessage by remember { mutableStateOf("") }
    var successMessage by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var showValidationErrors by remember { mutableStateOf(false) }
    var currentStep by remember { mutableStateOf(0) }

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
        targetValue = (currentStep + 1) / 3f,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "progress"
    )

    val displayTitle = job.title.ifBlank { "Untitled Position" }
    val displayCompany = job.companyName

    val isPhoneValid = phoneNumber.isNotBlank() && phoneNumber.length >= 7
    val isResumeValid = resumeUri.isNotBlank()

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
                                    text = "${currentStep + 1}/3",
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
                        repeat(3) { index ->
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
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
                    1 -> StepTitle(icon = Icons.Filled.Description, title = "Documents Upload", subtitle = "Attach your resume & optional cover letter")
                    2 -> StepTitle(icon = Icons.Filled.CheckCircle, title = "Review Application", subtitle = "Final check before sending your details")
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
                    1 -> Step2Documents(
                        resumeFileName = resumeName,
                        isResumeValid = isResumeValid,
                        onPickResume = { resumePickerLauncher.launch(arrayOf("application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document")) },
                        onRemoveResume = { resumeUri = ""; resumeName = "" },
                        coverLetterFileName = coverLetterName,
                        onPickCoverLetter = { coverLetterPickerLauncher.launch(arrayOf("application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document")) },
                        onRemoveCoverLetter = { coverLetterUri = ""; coverLetterName = "" },
                        additionalNotes = additionalNotes,
                        onAdditionalNotesChange = { additionalNotes = it },
                        showValidationErrors = showValidationErrors
                    )
                    2 -> Step3Review(
                        jobTitle = displayTitle,
                        companyName = displayCompany,
                        resumeFileName = resumeName.ifBlank { "No resume attached" },
                        coverLetterFileName = coverLetterName.ifBlank { "Not attached (Optional)" },
                        additionalNotes = additionalNotes,
                        phoneNumber = phoneNumber,
                        linkedInUrl = linkedInUrl,
                        salaryRangeText = "RM ${salaryMin.toInt()} - RM ${salaryMax.toInt()}",
                        startDateText = if (selectedStartDateOption == "Custom Date" && customStartDate.isNotBlank()) customStartDate else selectedStartDateOption
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

                Spacer(modifier = Modifier.weight(1f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                            if (currentStep < 2) {
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
                                        // Actually upload the resume file's bytes to Supabase
                                        // Storage -- resumeUri up to this point is just a local
                                        // content:// URI, which only exists on this device and
                                        // means nothing to anyone else (e.g. the employer). We
                                        // need the real, publicly-reachable URL back before we
                                        // can save the application.
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

                                        val uploadedResumeUrl = UserRepository.uploadResume(applicant.id, resumeBytes)
                                        if (uploadedResumeUrl == null) {
                                            errorMessage = "Failed to upload resume. Please check your connection and try again."
                                            showValidationErrors = true
                                            isSubmitting = false
                                            return@launch
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
                                            status = "Pending"
                                        )
                                        onApplySubmit(application)
                                        successMessage = "Application submitted successfully!"
                                        delay(1200)
                                        navController.popBackStack()
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
                            containerColor = if (currentStep == 2) DeepGreenDark else SageGreenMain
                        ),
                        enabled = !isSubmitting
                    ) {
                        when {
                            isSubmitting && currentStep == 2 -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = Color.White,
                                    strokeWidth = 2.5.dp
                                )
                            }
                            currentStep == 2 -> {
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

@Composable
private fun Step2Documents(
    resumeFileName: String,
    isResumeValid: Boolean,
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
                            color = DeepGreenDark,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isResumeValid) Icons.Filled.Description else Icons.Filled.UploadFile,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isResumeValid) resumeFileName else "Upload Resume (PDF / Doc)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark,
                                maxLines = 1
                            )
                            Text(
                                text = if (isResumeValid) "Tap to change document" else "Required for application submission",
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
    startDateText: String
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
        Text(text = value, fontSize = 13.sp, color = TextDark, maxLines = 3)
    }
}