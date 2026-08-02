package com.example.jobtown.ui.postjob

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.jobtown.data.Job
import com.example.jobtown.data.User
import com.example.jobtown.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostJobScreen(
    navController: NavController,
    currentUser: User?,
    onJobPosted: (Job) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var company by remember { mutableStateOf(currentUser?.name ?: "") }
    var location by remember { mutableStateOf("") }
    var salary by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Full-time") }
    var description by remember { mutableStateOf("") }
    var requirements by remember { mutableStateOf("") }
    var skills by remember { mutableStateOf("") }

    var showError by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Post a New Job",
                        fontWeight = FontWeight.Bold,
                        color = TextDark,
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
        },
        containerColor = BackgroundWhite
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Job Title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = company,
                onValueChange = { company = it },
                label = { Text("Company Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Location (e.g. Remote, New York)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = salary,
                onValueChange = { salary = it },
                label = { Text("Salary Range (e.g. $80k - $100k)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = type,
                onValueChange = { type = it },
                label = { Text("Job Type (Full-time, Part-time, Contract)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Job Description") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                maxLines = 5,
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = requirements,
                onValueChange = { requirements = it },
                label = { Text("Requirements (comma separated)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp),
                maxLines = 3,
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = skills,
                onValueChange = { skills = it },
                label = { Text("Skills Required (comma separated)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp),
                maxLines = 3,
                shape = RoundedCornerShape(12.dp)
            )

            if (showError) {
                Text(
                    text = "Please fill in all required fields.",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (title.isBlank() || company.isBlank() || location.isBlank() || description.isBlank()) {
                        showError = true
                    } else {
                        val userId = currentUser?.id ?: ""

                        // ✅ Instantiated strictly with parameters matching Job.kt
                        val newJob = Job(
                            id = "job_${System.currentTimeMillis()}",
                            title = title,
                            company = company,
                            location = location,
                            salary = salary,
                            salaryRange = salary,
                            type = type,
                            description = description,
                            requirements = requirements.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                            skills = skills.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                            isFeatured = false,
                            employerId = userId,
                            postedByUserId = userId,
                            createdAt = System.currentTimeMillis().toString()
                        )
                        onJobPosted(newJob)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DeepGreenDark,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "Post Job",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}