package com.example.jobtown.ui.job

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.jobtown.data.Job
import com.example.jobtown.data.JobApplication
import com.example.jobtown.data.User
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
    // Stage state: false = viewing Job Details, true = performing multi-step application
    var isApplying by remember { mutableStateOf(false) }

    if (!isApplying) {
        // Overview Screen before committing to apply
        JobDetailsOverviewScreen(
            job = job,
            onBackToHome = { navController.popBackStack() },
            onStartApplication = { isApplying = true },
            onViewCompanyDetails = onViewCompanyDetails
        )
    } else {
        // Multi-step application wizard
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
                shadowElevation = 8.dp,
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
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Not Interested", fontSize = 14.sp, color = TextDark)
                    }

                    Button(
                        onClick = onStartApplication,
                        modifier = Modifier
                            .weight(1.2f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
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
            // Main Overview Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
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

                    Spacer(modifier = Modifier.height(12.dp))

                    // Interactive Company Profile Card
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SageGreenLight.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SageGreenDark.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onViewCompanyDetails(job.employerId ?: displayCompany) }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = DeepGreenDark,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.Business,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
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
                                        text = "Tap to view company profile & jobs",
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

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = DeepGreenDark.copy(alpha = 0.08f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
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

            // Job Description & Requirements Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
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

                    // Null-safe handling for Requirements
                    if (!job.requirements.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Requirements",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextDark
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        job.requirements.orEmpty().forEach { req ->
                            Row(
                                modifier = Modifier.padding(vertical = 2.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
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

                    // Null-safe handling for Skills
                    if (!job.skills.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Required Skills",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextDark
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            job.skills.orEmpty().forEach { skill ->
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = SageGreenLight
                                ) {
                                    Text(
                                        text = skill,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
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

    // Form state
    var coverLetter by remember { mutableStateOf("") }
    var resumeUrl by remember { mutableStateOf("") }
    var additionalNotes by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var linkedInUrl by remember { mutableStateOf("") }
    var expectedSalary by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf("") }

    // UI state
    var errorMessage by remember { mutableStateOf("") }
    var successMessage by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var showValidationErrors by remember { mutableStateOf(false) }
    var currentStep by remember { mutableStateOf(0) }

    // Animation states
    val animatedProgress by animateFloatAsState(
        targetValue = (currentStep + 1) / 3f,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "progress"
    )

    // Job details
    val displayTitle = job.title.ifBlank { "Untitled Position" }
    val displayCompany = job.companyName

    // Validation
    val isResumeValid = resumeUrl.isNotBlank() &&
            (resumeUrl.startsWith("http://") || resumeUrl.startsWith("https://"))
    val isPhoneValid = phoneNumber.isBlank() || phoneNumber.length >= 10
    val isSalaryValid = expectedSalary.isBlank() || expectedSalary.toIntOrNull() != null

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
                        if (currentStep > 0) {
                            currentStep--
                        } else {
                            onCancelApplication()
                        }
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
                    Spacer(modifier = Modifier.width(4.dp))
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

                // Step Title
                when (currentStep) {
                    0 -> StepTitle(
                        icon = Icons.Filled.Person,
                        title = "Personal Information",
                        subtitle = "Tell us about yourself"
                    )
                    1 -> StepTitle(
                        icon = Icons.Filled.Description,
                        title = "Resume & Cover Letter",
                        subtitle = "Share your qualifications"
                    )
                    2 -> StepTitle(
                        icon = Icons.Filled.CheckCircle,
                        title = "Review & Submit",
                        subtitle = "Double-check your application"
                    )
                }

                // Step Content
                when (currentStep) {
                    0 -> Step1PersonalInfo(
                        phoneNumber = phoneNumber,
                        onPhoneNumberChange = { newValue -> phoneNumber = newValue },
                        linkedInUrl = linkedInUrl,
                        onLinkedInUrlChange = { newValue -> linkedInUrl = newValue },
                        expectedSalary = expectedSalary,
                        onExpectedSalaryChange = { newValue -> expectedSalary = newValue },
                        startDate = startDate,
                        onStartDateChange = { newValue -> startDate = newValue },
                        showValidationErrors = showValidationErrors,
                        isPhoneValid = isPhoneValid,
                        isSalaryValid = isSalaryValid,
                        errorMessage = errorMessage
                    )
                    1 -> Step2Resume(
                        resumeUrl = resumeUrl,
                        onResumeUrlChange = {
                            resumeUrl = it
                            errorMessage = ""
                        },
                        coverLetter = coverLetter,
                        onCoverLetterChange = { newValue -> coverLetter = newValue },
                        additionalNotes = additionalNotes,
                        onAdditionalNotesChange = { newValue -> additionalNotes = newValue },
                        showValidationErrors = showValidationErrors,
                        isResumeValid = isResumeValid,
                        errorMessage = errorMessage
                    )
                    2 -> Step3Review(
                        jobTitle = displayTitle,
                        companyName = displayCompany,
                        coverLetter = coverLetter,
                        resumeUrl = resumeUrl,
                        additionalNotes = additionalNotes,
                        phoneNumber = phoneNumber,
                        linkedInUrl = linkedInUrl,
                        expectedSalary = expectedSalary,
                        startDate = startDate
                    )
                }

                // Error Message
                if (errorMessage.isNotEmpty() && showValidationErrors) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(22.dp)
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

                Spacer(modifier = Modifier.weight(1f))

                // Navigation Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (currentStep > 0) currentStep-- else onCancelApplication()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (currentStep > 0) "Back" else "Overview", fontSize = 15.sp)
                    }

                    Button(
                        onClick = {
                            if (currentStep < 2) {
                                when (currentStep) {
                                    0 -> {
                                        showValidationErrors = true
                                        if (phoneNumber.isNotEmpty() && !isPhoneValid) {
                                            errorMessage = "Please enter a valid phone number"
                                        } else if (expectedSalary.isNotEmpty() && !isSalaryValid) {
                                            errorMessage = "Please enter a valid number for expected salary"
                                        } else {
                                            errorMessage = ""
                                            currentStep++
                                        }
                                    }
                                    1 -> {
                                        showValidationErrors = true
                                        if (!isResumeValid) {
                                            errorMessage = "Please provide a valid resume URL (must start with http:// or https://)"
                                        } else {
                                            errorMessage = ""
                                            currentStep++
                                        }
                                    }
                                }
                            } else {
                                if (!isResumeValid) {
                                    errorMessage = "Please provide a valid resume URL"
                                    showValidationErrors = true
                                    return@Button
                                }

                                isSubmitting = true
                                try {
                                    val application = JobApplication(
                                        id = "app_${System.currentTimeMillis()}",
                                        jobId = job.id,
                                        userId = currentUser?.id ?: "",
                                        jobTitle = displayTitle,
                                        companyName = displayCompany,
                                        employerId = job.employerId ?: job.postedByUserId ?: "",
                                        applicantName = currentUser?.name ?: "Unknown Applicant",
                                        applicantEmail = currentUser?.email ?: "",
                                        resumeUrl = resumeUrl.trim(),
                                        coverLetter = coverLetter.trim(),
                                        status = "Pending"
                                    )
                                    onApplySubmit(application)
                                    successMessage = "Application submitted successfully!"
                                    coroutineScope.launch {
                                        delay(1200)
                                        navController.popBackStack()
                                    }
                                } catch (e: Exception) {
                                    errorMessage = "Failed to submit: ${e.message}"
                                    showValidationErrors = true
                                    isSubmitting = false
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
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
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Submit Application",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            else -> {
                                Text(
                                    text = "Continue",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Success Overlay
            if (successMessage.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .clickable(enabled = false) { },
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                Color(0xFF4CAF50).copy(alpha = 0.2f),
                                                Color(0xFF4CAF50).copy(alpha = 0.05f)
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = "Application Submitted!",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = successMessage,
                                fontSize = 14.sp,
                                color = TextDark.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth(0.6f)
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = DeepGreenDark,
                                trackColor = SageGreenLight
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==================== Step Title Component ====================

@Composable
private fun StepTitle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = SageGreenLight,
            modifier = Modifier.size(40.dp)
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
                fontSize = 13.sp,
                color = TextDark.copy(alpha = 0.6f)
            )
        }
    }
}

// ==================== Info Badge ====================

@Composable
private fun InfoBadge(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = SageGreenDark,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            fontSize = 13.sp,
            color = TextDark.copy(alpha = 0.7f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ==================== Step 1: Personal Info ====================

@Composable
private fun Step1PersonalInfo(
    phoneNumber: String,
    onPhoneNumberChange: (String) -> Unit,
    linkedInUrl: String,
    onLinkedInUrlChange: (String) -> Unit,
    expectedSalary: String,
    onExpectedSalaryChange: (String) -> Unit,
    startDate: String,
    onStartDateChange: (String) -> Unit,
    showValidationErrors: Boolean,
    isPhoneValid: Boolean,
    isSalaryValid: Boolean,
    errorMessage: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Contact Information",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextDark
                )

                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { input ->
                        onPhoneNumberChange(input.filter { c -> c.isDigit() || c == '+' || c == '-' })
                    },
                    label = { Text("Phone Number") },
                    placeholder = { Text("+1 234 567 8900") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Phone,
                            contentDescription = null,
                            tint = SageGreenDark
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    isError = showValidationErrors && !isPhoneValid && phoneNumber.isNotEmpty(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SageGreenDark,
                        unfocusedBorderColor = SageGreenMain.copy(alpha = 0.6f),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )

                OutlinedTextField(
                    value = linkedInUrl,
                    onValueChange = onLinkedInUrlChange,
                    label = { Text("LinkedIn Profile (Optional)") },
                    placeholder = { Text("https://linkedin.com/in/your-profile") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            tint = SageGreenDark
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SageGreenDark,
                        unfocusedBorderColor = SageGreenMain.copy(alpha = 0.6f),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Preferences",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextDark
                )

                OutlinedTextField(
                    value = expectedSalary,
                    onValueChange = { input ->
                        onExpectedSalaryChange(input.filter { it.isDigit() })
                    },
                    label = { Text("Expected Annual Salary (Optional)") },
                    placeholder = { Text("e.g. 75000") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.AttachMoney,
                            contentDescription = null,
                            tint = SageGreenDark
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = showValidationErrors && !isSalaryValid && expectedSalary.isNotEmpty(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SageGreenDark,
                        unfocusedBorderColor = SageGreenMain.copy(alpha = 0.6f),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )

                OutlinedTextField(
                    value = startDate,
                    onValueChange = onStartDateChange,
                    label = { Text("Earliest Start Date (Optional)") },
                    placeholder = { Text("e.g. Immediate, 2 weeks notice, MM/YYYY") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.DateRange,
                            contentDescription = null,
                            tint = SageGreenDark
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SageGreenDark,
                        unfocusedBorderColor = SageGreenMain.copy(alpha = 0.6f),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )
            }
        }
    }
}

// ==================== Step 2: Resume & Cover Letter ====================

@Composable
private fun Step2Resume(
    resumeUrl: String,
    onResumeUrlChange: (String) -> Unit,
    coverLetter: String,
    onCoverLetterChange: (String) -> Unit,
    additionalNotes: String,
    onAdditionalNotesChange: (String) -> Unit,
    showValidationErrors: Boolean,
    isResumeValid: Boolean,
    errorMessage: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Resume Link *",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextDark
                )

                OutlinedTextField(
                    value = resumeUrl,
                    onValueChange = onResumeUrlChange,
                    label = { Text("Resume URL") },
                    placeholder = { Text("https://drive.google.com/your-resume") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Link,
                            contentDescription = null,
                            tint = SageGreenDark
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    isError = showValidationErrors && !isResumeValid,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SageGreenDark,
                        unfocusedBorderColor = SageGreenMain.copy(alpha = 0.6f),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )

                Text(
                    text = "Provide a link to your resume hosted on Google Drive, Dropbox, or a portfolio site.",
                    fontSize = 12.sp,
                    color = TextDark.copy(alpha = 0.5f)
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Cover Letter & Additional Info",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextDark
                )

                OutlinedTextField(
                    value = coverLetter,
                    onValueChange = onCoverLetterChange,
                    label = { Text("Cover Letter (Optional)") },
                    placeholder = { Text("Explain why you're a great fit for this position...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 6,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SageGreenDark,
                        unfocusedBorderColor = SageGreenMain.copy(alpha = 0.6f),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )

                OutlinedTextField(
                    value = additionalNotes,
                    onValueChange = onAdditionalNotesChange,
                    label = { Text("Additional Notes (Optional)") },
                    placeholder = { Text("Any extra details or portfolio links...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 80.dp),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SageGreenDark,
                        unfocusedBorderColor = SageGreenMain.copy(alpha = 0.6f),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )
            }
        }
    }
}

// ==================== Step 3: Review ====================

@Composable
private fun Step3Review(
    jobTitle: String,
    companyName: String,
    coverLetter: String,
    resumeUrl: String,
    additionalNotes: String,
    phoneNumber: String,
    linkedInUrl: String,
    expectedSalary: String,
    startDate: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Application Summary",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = DeepGreenDark
            )

            HorizontalDivider(color = SageGreenLight)

            ReviewItem(label = "Applying For", value = "$jobTitle at $companyName")
            ReviewItem(label = "Resume Link", value = resumeUrl.ifBlank { "Not provided" })

            if (phoneNumber.isNotBlank()) {
                ReviewItem(label = "Phone Number", value = phoneNumber)
            }
            if (linkedInUrl.isNotBlank()) {
                ReviewItem(label = "LinkedIn Profile", value = linkedInUrl)
            }
            if (expectedSalary.isNotBlank()) {
                ReviewItem(label = "Expected Salary", value = "$$expectedSalary")
            }
            if (startDate.isNotBlank()) {
                ReviewItem(label = "Earliest Start Date", value = startDate)
            }
            if (coverLetter.isNotBlank()) {
                ReviewItem(label = "Cover Letter", value = coverLetter)
            }
            if (additionalNotes.isNotBlank()) {
                ReviewItem(label = "Additional Notes", value = additionalNotes)
            }
        }
    }
}

@Composable
private fun ReviewItem(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = SageGreenDark
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = TextDark,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis
        )
    }
}