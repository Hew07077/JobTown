package com.example.jobtown.ui.applied

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jobtown.data.model.JobApplication
import com.example.jobtown.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobseekerApplicationDetailScreen(
    applicationId: String,
    viewModel: AppliedViewModel,
    onBackClick: () -> Unit,
    onChatWithEmployerClick: (JobApplication) -> Unit
) {
    val context = LocalContext.current

    // FIX: same issue as ApplicationDetailScreen — collect the StateFlow so this
    // screen recomposes when the applications list loads or a status changes.
    val applicationsList by viewModel.applicationsListState.collectAsStateWithLifecycle()
    val application = remember(applicationId, applicationsList) {
        applicationsList.find { it.id == applicationId }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Application Status",
                        fontWeight = FontWeight.Bold,
                        color = DeepGreenDark,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
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
        if (application == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Application details not found",
                    color = TextDark,
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Main Info Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = application.jobTitle,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepGreenDark
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = application.companyName,
                                    fontSize = 15.sp,
                                    color = TextDark.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            // Dynamic Status Badge
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = getStatusBackgroundColor(application.status)
                            ) {
                                Text(
                                    text = application.status.ifBlank { "Pending" },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = getStatusTextColor(application.status),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = TextDark.copy(alpha = 0.5f),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = application.location,
                                fontSize = 13.sp,
                                color = TextDark.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = TextDark.copy(alpha = 0.5f),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Applied: ${application.appliedDate}",
                                fontSize = 13.sp,
                                color = TextDark.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Applicant Summary
                Text(
                    text = "Submitted Profile",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = DeepGreenDark
                )
                Spacer(modifier = Modifier.height(10.dp))

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        JobseekerDetailRow(
                            icon = Icons.Default.Person,
                            label = "Full Name",
                            value = application.applicantName
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 10.dp),
                            color = BackgroundWhite
                        )
                        JobseekerDetailRow(
                            icon = Icons.Default.Email,
                            label = "Email Address",
                            value = application.applicantEmail
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Cover Letter
                if (application.coverLetter.isNotBlank()) {
                    Text(
                        text = "Cover Letter",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = DeepGreenDark
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = application.coverLetter,
                            modifier = Modifier.padding(16.dp),
                            fontSize = 14.sp,
                            color = TextDark.copy(alpha = 0.8f),
                            lineHeight = 22.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Resume Attachment View/Download Button
                if (application.resumeUrl.isNotBlank()) {
                    Text(
                        text = "Submitted Resume",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = DeepGreenDark
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(application.resumeUrl))
                            context.startActivity(intent)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DeepGreenDark)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "View / Download Resume",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Primary Direct Action: Chat with Employer
                Button(
                    onClick = { onChatWithEmployerClick(application) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DeepGreenDark)
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Chat with Employer",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun JobseekerDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = CircleShape,
            color = SageGreenMain.copy(alpha = 0.3f),
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = DeepGreenDark,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = label, fontSize = 11.sp, color = TextDark.copy(alpha = 0.5f))
            Text(
                text = value.ifBlank { "Not provided" },
                fontSize = 14.sp,
                color = TextDark,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun getStatusBackgroundColor(status: String): Color {
    return when (status.lowercase()) {
        "shortlisted" -> SageGreenMain
        "interview" -> SageGreenDark
        "rejected" -> Color(0xFFFFEBEE)
        "accepted" -> Color(0xFFE8F5E9)
        else -> SageGreenMain.copy(alpha = 0.4f)
    }
}

@Composable
private fun getStatusTextColor(status: String): Color {
    return when (status.lowercase()) {
        "rejected" -> Color(0xFFC62828)
        "accepted" -> Color(0xFF2E7D32)
        else -> DeepGreenDark
    }
}