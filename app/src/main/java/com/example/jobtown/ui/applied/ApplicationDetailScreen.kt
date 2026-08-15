package com.example.jobtown.ui.applied

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jobtown.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplicationDetailScreen(
    applicationId: String,
    viewModel: AppliedViewModel,
    onBackClick: () -> Unit
) {
    val application = remember(applicationId, viewModel.applicationsList) {
        viewModel.applicationsList.find { it.id == applicationId }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Application Detail",
                        fontWeight = FontWeight.Bold,
                        color = TextDark
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
                Text(text = "Application not found", color = TextDark)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                // Header section
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = application.jobTitle,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepGreenDark
                        )
                        Text(
                            text = application.companyName,
                            fontSize = 16.sp,
                            color = TextDark.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Medium
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SageGreenMain.copy(alpha = 0.4f)
                        ) {
                            Text(
                                text = application.status.ifBlank { "Pending" },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepGreenDark,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Applicant Details
                Text(
                    text = "Applicant Information",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = DeepGreenDark
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                DetailItem(icon = Icons.Default.Person, label = "Name", value = application.applicantName)
                DetailItem(icon = Icons.Default.Email, label = "Email", value = application.applicantEmail)
                DetailItem(icon = Icons.Default.CalendarToday, label = "Applied On", value = application.appliedDate)

                Spacer(modifier = Modifier.height(24.dp))

                // Cover Letter
                if (application.coverLetter.isNotBlank()) {
                    Text(
                        text = "Cover Letter",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = DeepGreenDark
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(12.dp),
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
                }
            }
        }
    }
}

@Composable
fun DetailItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = DeepGreenDark,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = label, fontSize = 12.sp, color = TextDark.copy(alpha = 0.5f))
            Text(
                text = value.ifBlank { "Not provided" },
                fontSize = 15.sp,
                color = TextDark,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
