package com.example.jobtown.ui.job

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.jobtown.data.Job
import com.example.jobtown.ui.theme.DeepGreenDark
import com.example.jobtown.ui.theme.SageGreenMain

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobDetailScreen(
    navController: NavController,
    job: Job,
    onApplyClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Job Details", fontWeight = FontWeight.Bold, color = DeepGreenDark) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = DeepGreenDark)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SageGreenMain)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFFAFAFA))
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Text(text = job.title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = DeepGreenDark)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "${job.company} • ${job.location}", fontSize = 15.sp, color = Color.DarkGray)

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(shape = RoundedCornerShape(8.dp), color = SageGreenMain.copy(alpha = 0.4f)) {
                    Text(text = job.salary, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DeepGreenDark)
                }
                Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFE2E8F0)) {
                    Text(text = job.type, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.DarkGray)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(text = "Job Description", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DeepGreenDark)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = job.description, fontSize = 14.sp, color = Color.Gray, lineHeight = 20.sp)

            if (job.requirements.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(text = "Requirements", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DeepGreenDark)
                Spacer(modifier = Modifier.height(8.dp))
                job.requirements.forEach { req ->
                    Text(text = "• $req", fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 4.dp))
                }
            }

            if (job.skills.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(text = "Required Skills", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DeepGreenDark)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    job.skills.forEach { skill ->
                        Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFF1F5F9)) {
                            Text(text = skill, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 12.sp, color = DeepGreenDark)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            Button(
                onClick = onApplyClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SageGreenMain)
            ) {
                Text("Apply Now", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DeepGreenDark)
            }
        }
    }
}