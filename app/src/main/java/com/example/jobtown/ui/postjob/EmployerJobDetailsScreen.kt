package com.example.jobtown.ui.postjob

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.jobtown.data.Job
import com.example.jobtown.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EmployerJobDetailScreen(
    job: Job,
    navController: NavController? = null,
    onUpdateJob: (Job) -> Unit = {},
    onBackClick: () -> Unit = { navController?.popBackStack() },
    onViewCompanyDetails: (String) -> Unit = {}
) {
    var isEditing by remember { mutableStateOf(false) }

    // Edit Form States matching Job model properties
    var title by remember { mutableStateOf(job.title) }
    var company by remember { mutableStateOf(job.companyName.ifBlank { "Company Name" }) }
    var location by remember { mutableStateOf(job.location) }

    var minSalary by remember { mutableStateOf("") }
    var maxSalary by remember { mutableStateOf("") }
    var minSalaryExpanded by remember { mutableStateOf(false) }
    var maxSalaryExpanded by remember { mutableStateOf(false) }

    val minSalaryOptions = remember { (1000..20000 step 1000).map { "%,d".format(it) } }
    val maxSalaryOptions = remember { (2000..30000 step 1000).map { "%,d".format(it) } + listOf("30,000+") }

    var type by remember { mutableStateOf(job.jobType.ifBlank { "Full-time" }) }
    var jobTypeExpanded by remember { mutableStateOf(false) }
    val jobTypeOptions = listOf("Full-time", "Part-time", "Contract", "Internship", "Freelance")

    var description by remember { mutableStateOf(job.description) }

    // Format list items separated by comma
    var requirements by remember {
        mutableStateOf(job.requirements?.filter { it.isNotBlank() }?.joinToString(", ") ?: "")
    }
    var skills by remember {
        mutableStateOf(job.skills?.filter { it.isNotBlank() }?.joinToString(", ") ?: "")
    }

    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    fun parseSalaryValue(valueStr: String): Int {
        return valueStr.replace(",", "").replace("+", "").replace("$", "").trim().toIntOrNull() ?: 0
    }

    val formattedSalary = remember(minSalary, maxSalary, job.salary) {
        if (minSalary.isNotBlank() && maxSalary.isNotBlank()) "$$minSalary - $$maxSalary / month"
        else job.salary
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditing) "Edit Job Listing" else "Job Details",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = DeepGreenDark
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
                actions = {
                    IconButton(onClick = { isEditing = !isEditing }) {
                        Icon(
                            imageVector = if (isEditing) Icons.Default.Save else Icons.Default.Edit,
                            contentDescription = "Edit Toggle",
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isEditing) {
                // --- EDIT MODE ---
                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    label = { Text("Job Title *") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = company, onValueChange = { company = it },
                        label = { Text("Company *") }, singleLine = true,
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = location, onValueChange = { location = it },
                        label = { Text("Location *") }, singleLine = true,
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)
                    )
                }

                // SALARY DROPDOWNS
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ExposedDropdownMenuBox(
                        expanded = minSalaryExpanded,
                        onExpandedChange = { minSalaryExpanded = !minSalaryExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = if (minSalary.isNotEmpty()) "$$minSalary" else "", onValueChange = {},
                            label = { Text("Min Salary") }, readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = minSalaryExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(), shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(expanded = minSalaryExpanded, onDismissRequest = { minSalaryExpanded = false }) {
                            minSalaryOptions.forEach { opt ->
                                DropdownMenuItem(text = { Text("$$opt", fontSize = 13.sp) }, onClick = { minSalary = opt; minSalaryExpanded = false })
                            }
                        }
                    }

                    ExposedDropdownMenuBox(
                        expanded = maxSalaryExpanded,
                        onExpandedChange = { maxSalaryExpanded = !maxSalaryExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = if (maxSalary.isNotEmpty()) "$$maxSalary" else "", onValueChange = {},
                            label = { Text("Max Salary") }, readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = maxSalaryExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(), shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(expanded = maxSalaryExpanded, onDismissRequest = { maxSalaryExpanded = false }) {
                            maxSalaryOptions.forEach { opt ->
                                DropdownMenuItem(text = { Text("$$opt", fontSize = 13.sp) }, onClick = { maxSalary = opt; maxSalaryExpanded = false })
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
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Job Type *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = jobTypeExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = jobTypeExpanded,
                        onDismissRequest = { jobTypeExpanded = false }
                    ) {
                        jobTypeOptions.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt, fontSize = 13.sp) },
                                onClick = {
                                    type = opt
                                    jobTypeExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = description, onValueChange = { description = it },
                    label = { Text("Description *") }, minLines = 3, maxLines = 5,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = requirements, onValueChange = { requirements = it },
                    label = { Text("Requirements (separate with comma)") }, minLines = 2, maxLines = 3,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = skills, onValueChange = { skills = it },
                    label = { Text("Skills Required (separate with comma)") }, minLines = 2, maxLines = 3,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
                )

                if (showError) {
                    Text(errorMessage, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }

                Button(
                    onClick = {
                        val minVal = parseSalaryValue(minSalary)
                        val maxVal = parseSalaryValue(maxSalary)

                        if (title.isBlank() || company.isBlank() || location.isBlank() || description.isBlank()) {
                            errorMessage = "Please fill in all required fields."
                            showError = true
                        } else if (minSalary.isNotBlank() && maxSalary.isNotBlank() && maxSalary != "30,000+" && minVal > maxVal) {
                            errorMessage = "Min salary cannot be greater than Max salary."
                            showError = true
                        } else {
                            showError = false
                            val updatedJob = job.copy(
                                title = title.trim(),
                                location = location.trim(),
                                salary = formattedSalary,
                                salaryRange = formattedSalary,
                                type = type,
                                description = description.trim(),
                                requirements = requirements.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                                skills = skills.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                            )
                            onUpdateJob(updatedJob)
                            isEditing = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DeepGreenDark)
                ) {
                    Text("Save Changes", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            } else {
                // --- READ-ONLY DISPLAY MODE ---
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = job.title.ifBlank { "Untitled Position" },
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Non-clickable Company Profile Surface
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = SageGreenLight.copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SageGreenDark.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = SageGreenLight,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (!job.companyImageUrl.isNullOrBlank()) {
                                            AsyncImage(
                                                model = job.companyImageUrl,
                                                contentDescription = "Company Logo",
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(CircleShape),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Filled.Business,
                                                contentDescription = null,
                                                tint = DeepGreenDark,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                                Column {
                                    Text(
                                        text = job.companyName.ifBlank { "Company Name" },
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DeepGreenDark
                                    )
                                    Text(
                                        text = job.location.ifBlank { "Location Undisclosed" },
                                        fontSize = 11.sp,
                                        color = SageGreenDark,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            EmployerInfoBadge(icon = Icons.Filled.LocationOn, text = job.location.ifBlank { "Location Undisclosed" })
                            EmployerInfoBadge(icon = Icons.Filled.Work, text = job.jobType.ifBlank { "Full-time" })
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
                                    text = job.salary.ifBlank { "Salary Not Specified" },
                                    fontWeight = FontWeight.Bold,
                                    color = DeepGreenDark,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                }

                // Job Description, Requirements & Skills Card
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
                            text = job.description.ifBlank { "No detailed description available for this role." },
                            fontSize = 13.sp,
                            color = TextDark.copy(alpha = 0.8f),
                            lineHeight = 20.sp
                        )

                        // Requirements formatted cleanly as bullet points
                        val filteredRequirements = job.requirements?.flatMap { it.split(",") }?.map { it.trim() }?.filter { it.isNotBlank() }
                        if (!filteredRequirements.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Requirements",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextDark
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            filteredRequirements.forEach { req ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = "• ",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DeepGreenDark
                                    )
                                    Text(
                                        text = req,
                                        fontSize = 13.sp,
                                        color = TextDark.copy(alpha = 0.8f),
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }

                        // Skills formatted cleanly as bullet points
                        val filteredSkills = job.skills?.flatMap { it.split(",") }?.map { it.trim() }?.filter { it.isNotBlank() }
                        if (!filteredSkills.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Required Skills",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextDark
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            filteredSkills.forEach { skill ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = "• ",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DeepGreenDark
                                    )
                                    Text(
                                        text = skill,
                                        fontSize = 13.sp,
                                        color = TextDark.copy(alpha = 0.8f),
                                        lineHeight = 18.sp
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

@Composable
private fun EmployerInfoBadge(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
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