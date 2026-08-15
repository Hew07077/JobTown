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
    navController: NavController? = null,
    currentUser: User? = null,
    onJobPosted: (Job) -> Unit = {},
    onBackClick: () -> Unit = { navController?.popBackStack() }
) {
    var title by remember { mutableStateOf("") }
    var company by remember {
        mutableStateOf(
            currentUser?.companyName?.ifBlank { currentUser.name }
                ?: currentUser?.name
                ?: ""
        )
    }
    var location by remember { mutableStateOf("") }

    // Min and Max Salary States
    var minSalary by remember { mutableStateOf("") }
    var maxSalary by remember { mutableStateOf("") }
    var minSalaryExpanded by remember { mutableStateOf(false) }
    var maxSalaryExpanded by remember { mutableStateOf(false) }

    // Dropdown options in increments of $1,000
    val minSalaryOptions = remember { (1000..20000 step 1000).map { "%,d".format(it) } }
    val maxSalaryOptions = remember { (2000..30000 step 1000).map { "%,d".format(it) } + listOf("30,000+") }

    // Job Type State & Dropdown
    var type by remember { mutableStateOf("Full-time") }
    var jobTypeExpanded by remember { mutableStateOf(false) }
    val jobTypeOptions = listOf("Full-time", "Part-time", "Contract", "Internship", "Freelance")

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
                    IconButton(onClick = onBackClick) {
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

            // SALARY RANGE: MIN & MAX DROPDOWNS
            Text(
                text = "Salary Range ($ / month)",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextDark
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Min Salary Dropdown
                ExposedDropdownMenuBox(
                    expanded = minSalaryExpanded,
                    onExpandedChange = { minSalaryExpanded = !minSalaryExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = minSalary,
                        onValueChange = { minSalary = it },
                        label = { Text("Min ($)") },
                        placeholder = { Text("e.g. 2,000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = minSalaryExpanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier.menuAnchor(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = minSalaryExpanded,
                        onDismissRequest = { minSalaryExpanded = false }
                    ) {
                        minSalaryOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(text = "$$option") },
                                onClick = {
                                    minSalary = option
                                    minSalaryExpanded = false
                                }
                            )
                        }
                    }
                }

                // Max Salary Dropdown
                ExposedDropdownMenuBox(
                    expanded = maxSalaryExpanded,
                    onExpandedChange = { maxSalaryExpanded = !maxSalaryExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = maxSalary,
                        onValueChange = { maxSalary = it },
                        label = { Text("Max ($)") },
                        placeholder = { Text("e.g. 5,000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = maxSalaryExpanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier.menuAnchor(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = maxSalaryExpanded,
                        onDismissRequest = { maxSalaryExpanded = false }
                    ) {
                        maxSalaryOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(text = "$$option") },
                                onClick = {
                                    maxSalary = option
                                    maxSalaryExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // JOB TYPE DROPDOWN
            ExposedDropdownMenuBox(
                expanded = jobTypeExpanded,
                onExpandedChange = { jobTypeExpanded = !jobTypeExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = type,
                    onValueChange = { type = it },
                    label = { Text("Job Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = jobTypeExpanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                ExposedDropdownMenu(
                    expanded = jobTypeExpanded,
                    onDismissRequest = { jobTypeExpanded = false }
                ) {
                    jobTypeOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(text = option) },
                            onClick = {
                                type = option
                                jobTypeExpanded = false
                            }
                        )
                    }
                }
            }

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
                        // Crucial: Use current user ID for ownership check
                        val userId = currentUser?.id.orEmpty()

                        val formattedSalary = when {
                            minSalary.isNotBlank() && maxSalary.isNotBlank() -> "$$minSalary - $$maxSalary / month"
                            minSalary.isNotBlank() -> "From $$minSalary / month"
                            maxSalary.isNotBlank() -> "Up to $$maxSalary / month"
                            else -> "Negotiable"
                        }

                        val newJob = Job(
                            id = "job_${System.currentTimeMillis()}",
                            title = title,
                            company = company,
                            location = location,
                            salary = formattedSalary,
                            salaryRange = formattedSalary,
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