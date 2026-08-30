package com.example.jobtown.ui.job

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachMoney
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
import com.example.jobtown.data.model.Job
import com.example.jobtown.ui.theme.*

@Composable
fun JobDetailScreen(
    navController: NavController,
    jobId: String? = null,
    job: Job? = null,
    isApplied: Boolean = false,
    onApplyClick: () -> Unit = {}
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
            .background(BackgroundWhite)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Hero Banner
            Surface(
                color = SageGreenMain,
                shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(24.dp)
                ) {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = DeepGreenDark
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.size(56.dp),
                            shadowElevation = 2.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Work,
                                    contentDescription = null,
                                    tint = DeepGreenDark,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = displayJob.title,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepGreenDark
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = displayJob.company,
                                fontSize = 14.sp,
                                color = TextDark.copy(alpha = 0.8f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Job Metadata & Details Body
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                // Location Chip
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Surface(
                        color = SageGreenLight,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = DeepGreenDark,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = displayJob.location,
                                fontSize = 13.sp,
                                color = DeepGreenDark,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Salary Card Display
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    tonalElevation = 1.dp,
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(SageGreenLight, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AttachMoney,
                                contentDescription = null,
                                tint = DeepGreenDark
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "Offered Salary",
                                fontSize = 12.sp,
                                color = TextDark.copy(alpha = 0.6f)
                            )
                            Text(
                                text = displayJob.salary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepGreenDark
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Job Description
                Text(
                    text = "Job Description",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepGreenDark
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = displayJob.description,
                    fontSize = 14.sp,
                    color = TextDark.copy(alpha = 0.8f),
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Apply Bottom Action Bar
        Surface(
            color = Color.White,
            shadowElevation = 12.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(20.dp)
            ) {
                Button(
                    onClick = onApplyClick,
                    enabled = !isApplied,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DeepGreenDark,
                        disabledContainerColor = SageGreenLight
                    )
                ) {
                    Text(
                        text = if (isApplied) "Application Submitted" else "Apply Now",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isApplied) DeepGreenDark else Color.White
                    )
                }
            }
        }
    }
}