package com.example.jobtown.ui.job

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.jobtown.data.Job
import com.example.jobtown.data.JobApplication
import com.example.jobtown.data.User
import com.example.jobtown.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplyJobScreen(
    navController: NavController,
    job: Job,
    currentUser: User?,
    onApplySubmit: (JobApplication) -> Unit
) {
    var coverLetter by remember { mutableStateOf("") }
    var resumeUrl by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    val displayTitle = job.title.ifBlank { "Untitled Position" }
    val displayCompany = job.companyName
    val displayLocation = job.location.ifBlank { "Location Undisclosed" }
    val displaySalary = job.salary.ifBlank { "Salary Not Specified" }
    val displayType = job.jobType

    Scaffold(
        containerColor = BackgroundWhite,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Job Application",
                        fontWeight = FontWeight.Bold,
                        color = DeepGreenDark,
                        fontSize = 18.sp
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
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- JOB HEADER SUMMARY CARD ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = displayTitle,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepGreenDark,
                            modifier = Modifier.weight(1f)
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SageGreenLight
                        ) {
                            Text(
                                text = displayType,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                color = DeepGreenDark,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Business,
                            contentDescription = null,
                            tint = SageGreenDark,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = displayCompany,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = SageGreenDark
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = TextDark.copy(alpha = 0.5f),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = displayLocation,
                            fontSize = 13.sp,
                            color = TextDark.copy(alpha = 0.6f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = displaySalary,
                        fontWeight = FontWeight.Bold,
                        color = DeepGreenDark,
                        fontSize = 15.sp
                    )

                    // Job Description snippet if available
                    if (job.description.isNotBlank()) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = SageGreenLight
                        )
                        Text(
                            text = "About the Role",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = job.description,
                            fontSize = 13.sp,
                            color = TextDark.copy(alpha = 0.7f),
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // --- INPUT FIELDS SECTION ---
            Text(
                text = "Your Application Details",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = DeepGreenDark
            )

            OutlinedTextField(
                value = resumeUrl,
                onValueChange = {
                    resumeUrl = it
                    errorMessage = ""
                },
                label = { Text("Resume Link (PDF / Google Drive URL)") },
                placeholder = { Text("https://drive.google.com/your-resume") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null,
                        tint = SageGreenDark
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SageGreenDark,
                    unfocusedBorderColor = SageGreenMain.copy(alpha = 0.6f),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )

            OutlinedTextField(
                value = coverLetter,
                onValueChange = {
                    coverLetter = it
                    errorMessage = ""
                },
                label = { Text("Cover Letter / Pitch") },
                placeholder = { Text("Explain why you are a strong candidate for this position...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = SageGreenDark
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SageGreenDark,
                    unfocusedBorderColor = SageGreenMain.copy(alpha = 0.6f),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )

            if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (resumeUrl.isBlank()) {
                        errorMessage = "Please provide a valid resume URL."
                    } else {
                        val application = JobApplication(
                            id = "app_${System.currentTimeMillis()}",
                            jobId = job.id,
                            userId = currentUser?.id ?: "",
                            jobTitle = displayTitle,
                            companyName = displayCompany,
                            applicantName = currentUser?.name ?: "Unknown Applicant",
                            applicantEmail = currentUser?.email ?: "",
                            resumeUrl = resumeUrl.trim(),
                            coverLetter = coverLetter.trim(),
                            status = "Pending"
                        )
                        onApplySubmit(application)
                        navController.popBackStack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DeepGreenDark)
            ) {
                Text(
                    text = "Submit Application",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}