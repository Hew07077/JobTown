package com.example.jobtown.ui.applied

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.jobtown.data.JobApplication
import com.example.jobtown.data.User
import com.example.jobtown.data.UserRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyAppliedScreen(
    navController: NavController,
    user: User?,
    applications: List<JobApplication>
) {
    val isEmployer = user?.role == UserRole.EMPLOYER

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = if (isEmployer) "Manage Job Applications" else "My Applications")
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (applications.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (isEmployer) "No applications submitted for your jobs yet." else "You haven't applied to any jobs yet.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(applications) { app ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = app.jobTitle, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = "Company: ${app.companyName}", fontSize = 14.sp)
                                if (isEmployer) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(text = "Applicant: ${app.applicantName} (${app.applicantEmail})", fontSize = 13.sp)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(text = "Status: ${app.status}", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}