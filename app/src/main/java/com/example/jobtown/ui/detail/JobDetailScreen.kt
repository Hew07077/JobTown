package com.example.jobtown.ui.job

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.jobtown.data.Job
import com.example.jobtown.ui.theme.DarkTextPurple
import com.example.jobtown.ui.theme.DeepGreenDark

@Composable
fun JobDetailScreen(
    navController: NavController,
    jobId: String? = null,
    job: Job? = null
) {
    val displayJob = job ?: Job(
        id = jobId ?: "1",
        title = "Senior Android Developer",
        company = "TechCorp Solutions",
        location = "Kuala Lumpur, Malaysia",
        salary = "RM 8,000 - RM 12,000 / mo",
        description = "We are seeking an experienced Android Developer to join our core mobile engineering team. You will lead Kotlin development using Jetpack Compose and Modern Android Architecture.",
        employerId = "emp1"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Surface
            Surface(
                color = Color(0xFFA1C695),
                shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = DarkTextPurple)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Work, contentDescription = null, tint = DeepGreenDark)
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(displayJob.title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = DarkTextPurple)
                            Text(displayJob.company, fontSize = 14.sp, color = DeepGreenDark, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Details Body
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(displayJob.location, fontSize = 14.sp, color = Color.Gray)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(displayJob.salary, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))

                Spacer(modifier = Modifier.height(24.dp))

                Text("Job Description", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DarkTextPurple)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = displayJob.description,
                    fontSize = 14.sp,
                    color = Color.DarkGray,
                    lineHeight = 22.sp
                )
            }
        }

        // Apply Bottom Action Bar
        Surface(
            color = Color.White,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(modifier = Modifier.padding(20.dp)) {
                Button(
                    onClick = { /* Handle application submit */ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DeepGreenDark)
                ) {
                    Text("Apply Now", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}