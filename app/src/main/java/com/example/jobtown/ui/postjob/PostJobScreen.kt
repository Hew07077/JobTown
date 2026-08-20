package com.example.jobtown.ui.postjob

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.jobtown.data.Job
import com.example.jobtown.data.User
import com.example.jobtown.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostJobScreen(
    navController: NavController? = null,
    currentUser: User? = null,
    onJobPosted: (Job, onComplete: (Boolean, String?) -> Unit) -> Unit = { _, _ -> },
    onBackClick: () -> Unit = { navController?.popBackStack() }
) {
    var title by remember { mutableStateOf("") }
    var company by remember {
        mutableStateOf(currentUser?.companyName?.ifBlank { currentUser.name } ?: currentUser?.name ?: "")
    }
    var location by remember { mutableStateOf("") }

    var minSalary by remember { mutableStateOf("") }
    var maxSalary by remember { mutableStateOf("") }
    var minSalaryExpanded by remember { mutableStateOf(false) }
    var maxSalaryExpanded by remember { mutableStateOf(false) }

    val minSalaryOptions = remember { (1000..20000 step 1000).map { "%,d".format(it) } }
    val maxSalaryOptions = remember { (2000..30000 step 1000).map { "%,d".format(it) } + listOf("30,000+") }

    var type by remember { mutableStateOf("Full-time") }
    var jobTypeExpanded by remember { mutableStateOf(false) }
    val jobTypeOptions = listOf("Full-time", "Part-time", "Contract", "Internship", "Freelance")

    var expiryDays by remember { mutableStateOf("30 Days") }
    var expiryExpanded by remember { mutableStateOf(false) }
    val expiryOptions = listOf("7 Days", "14 Days", "30 Days", "60 Days", "Never")

    var description by remember { mutableStateOf("") }
    var requirements by remember { mutableStateOf("") }
    var skills by remember { mutableStateOf("") }

    var isFeatured by remember { mutableStateOf(false) }
    var showPreview by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    fun parseSalaryValue(valueStr: String): Int {
        return valueStr.replace(",", "").replace("+", "").replace("$", "").trim().toIntOrNull() ?: 0
    }

    val formattedSalary = remember(minSalary, maxSalary) {
        if (minSalary.isNotBlank() && maxSalary.isNotBlank()) "$$minSalary - $$maxSalary / month"
        else if (minSalary.isNotBlank()) "From $$minSalary / month"
        else if (maxSalary.isNotBlank()) "Up to $$maxSalary / month"
        else "Negotiable"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Post Job", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextDark) },
                navigationIcon = {
                    IconButton(onClick = onBackClick, enabled = !isSubmitting, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = DeepGreenDark)
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
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Job Listing Details", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextDark)
                TextButton(
                    onClick = { showPreview = !showPreview },
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = if (showPreview) "Hide Preview" else "Show Card Preview",
                        fontSize = 11.sp,
                        color = DeepGreenDark,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (showPreview) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isFeatured) SageGreenMain.copy(alpha = 0.25f) else Color.White
                    ),
                    border = BorderStroke(
                        width = if (isFeatured) 1.5.dp else 1.dp,
                        color = if (isFeatured) DeepGreenDark else Color(0xFFE0E0E0)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = title.ifBlank { "Job Title Placeholder" },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark,
                                modifier = Modifier.weight(1f)
                            )
                            if (isFeatured) {
                                Surface(
                                    color = DeepGreenDark,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Star,
                                            contentDescription = null,
                                            tint = Color.Yellow,
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = "FEATURED",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Business,
                                contentDescription = null,
                                tint = DeepGreenDark,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = company.ifBlank { "Company Name" },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextDark.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = DeepGreenDark,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = location.ifBlank { "Location" },
                                fontSize = 11.sp,
                                color = TextDark.copy(alpha = 0.6f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = SageGreenMain.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = type,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DeepGreenDark,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            Surface(
                                color = DeepGreenDark.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = formattedSalary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepGreenDark,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            Surface(
                                color = Color.Gray.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "Expires: $expiryDays",
                                    fontSize = 10.sp,
                                    color = TextDark.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        if (description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = description,
                                fontSize = 11.sp,
                                color = TextDark.copy(alpha = 0.7f),
                                maxLines = 2
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title *", fontSize = 10.sp) },
                singleLine = true,
                enabled = !isSubmitting,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(6.dp)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = company,
                    onValueChange = { company = it },
                    label = { Text("Company *", fontSize = 10.sp) },
                    singleLine = true,
                    enabled = !isSubmitting,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(6.dp)
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location *", fontSize = 10.sp) },
                    singleLine = true,
                    enabled = !isSubmitting,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(6.dp)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ExposedDropdownMenuBox(
                    expanded = minSalaryExpanded && !isSubmitting,
                    onExpandedChange = { if (!isSubmitting) minSalaryExpanded = !minSalaryExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = if (minSalary.isNotEmpty()) "$$minSalary" else "",
                        onValueChange = {},
                        label = { Text("Min Salary *", fontSize = 10.sp) },
                        readOnly = true,
                        enabled = !isSubmitting,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = minSalaryExpanded) },
                        modifier = Modifier.menuAnchor(),
                        shape = RoundedCornerShape(6.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = minSalaryExpanded,
                        onDismissRequest = { minSalaryExpanded = false }
                    ) {
                        minSalaryOptions.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text("$$opt", fontSize = 11.sp) },
                                onClick = {
                                    minSalary = opt
                                    minSalaryExpanded = false
                                }
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = maxSalaryExpanded && !isSubmitting,
                    onExpandedChange = { if (!isSubmitting) maxSalaryExpanded = !maxSalaryExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = if (maxSalary.isNotEmpty()) "$$maxSalary" else "",
                        onValueChange = {},
                        label = { Text("Max Salary *", fontSize = 10.sp) },
                        readOnly = true,
                        enabled = !isSubmitting,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = maxSalaryExpanded) },
                        modifier = Modifier.menuAnchor(),
                        shape = RoundedCornerShape(6.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = maxSalaryExpanded,
                        onDismissRequest = { maxSalaryExpanded = false }
                    ) {
                        maxSalaryOptions.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text("$$opt", fontSize = 11.sp) },
                                onClick = {
                                    maxSalary = opt
                                    maxSalaryExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ExposedDropdownMenuBox(
                    expanded = jobTypeExpanded && !isSubmitting,
                    onExpandedChange = { if (!isSubmitting) jobTypeExpanded = !jobTypeExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = type,
                        onValueChange = {},
                        readOnly = true,
                        enabled = !isSubmitting,
                        label = { Text("Job Type *", fontSize = 10.sp) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = jobTypeExpanded) },
                        modifier = Modifier.menuAnchor(),
                        shape = RoundedCornerShape(6.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = jobTypeExpanded,
                        onDismissRequest = { jobTypeExpanded = false }
                    ) {
                        jobTypeOptions.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt, fontSize = 11.sp) },
                                onClick = {
                                    type = opt
                                    jobTypeExpanded = false
                                }
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = expiryExpanded && !isSubmitting,
                    onExpandedChange = { if (!isSubmitting) expiryExpanded = !expiryExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = expiryDays,
                        onValueChange = {},
                        readOnly = true,
                        enabled = !isSubmitting,
                        label = { Text("Expires In", fontSize = 10.sp) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expiryExpanded) },
                        modifier = Modifier.menuAnchor(),
                        shape = RoundedCornerShape(6.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expiryExpanded,
                        onDismissRequest = { expiryExpanded = false }
                    ) {
                        expiryOptions.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt, fontSize = 11.sp) },
                                onClick = {
                                    expiryDays = opt
                                    expiryExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description *", fontSize = 10.sp) },
                enabled = !isSubmitting,
                minLines = 2,
                maxLines = 2,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(6.dp)
            )

            OutlinedTextField(
                value = requirements,
                onValueChange = { requirements = it },
                label = { Text("Requirements (comma separated)", fontSize = 10.sp) },
                enabled = !isSubmitting,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(6.dp)
            )

            OutlinedTextField(
                value = skills,
                onValueChange = { skills = it },
                label = { Text("Skills Required (comma separated)", fontSize = 10.sp) },
                enabled = !isSubmitting,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(6.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Mark as Featured Job", fontSize = 11.sp, color = TextDark, fontWeight = FontWeight.Medium)
                Switch(
                    checked = isFeatured,
                    onCheckedChange = { isFeatured = it },
                    enabled = !isSubmitting,
                    modifier = Modifier.scale(0.7f)
                )
            }

            if (showError) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Button(
                onClick = {
                    val minVal = parseSalaryValue(minSalary)
                    val maxVal = parseSalaryValue(maxSalary)

                    if (title.isBlank() || company.isBlank() || location.isBlank() || description.isBlank()) {
                        errorMessage = "Please fill in all required fields marked with *"
                        showError = true
                    } else if (minSalary.isBlank() || maxSalary.isBlank()) {
                        errorMessage = "Please select both Min and Max salary range."
                        showError = true
                    } else if (maxSalary != "30,000+" && minVal > maxVal) {
                        errorMessage = "Minimum salary cannot be greater than Maximum salary."
                        showError = true
                    } else {
                        showError = false
                        isSubmitting = true
                        val userId = currentUser?.id?.trim()?.ifEmpty { null }

                        val days = when (expiryDays) {
                            "7 Days" -> 7L
                            "14 Days" -> 14L
                            "30 Days" -> 30L
                            "60 Days" -> 60L
                            else -> null
                        }

                        val expiredAtString = days?.let {
                            val calendar = Calendar.getInstance()
                            calendar.add(Calendar.DAY_OF_YEAR, it.toInt())
                            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(calendar.time)
                        }

                        // Added UUID.randomUUID().toString() so Supabase has a primary key
                        val newJob = Job(
                            id = UUID.randomUUID().toString(),
                            title = title.trim(),
                            company = company.trim(),
                            location = location.trim(),
                            salary = formattedSalary,
                            salaryRange = formattedSalary,
                            type = type,
                            description = description.trim(),
                            requirements = requirements.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                            skills = skills.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                            isFeatured = isFeatured,
                            employerId = userId,
                            postedByUserId = userId,
                            status = "active",
                            expiredAt = expiredAtString
                        )

                        // Trigger Supabase call via ViewModel and handle result callback
                        onJobPosted(newJob) { success, message ->
                            isSubmitting = false
                            if (success) {
                                navController?.popBackStack()
                            } else {
                                errorMessage = message ?: "Failed to post job to Supabase."
                                showError = true
                            }
                        }
                    }
                },
                enabled = !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DeepGreenDark)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                } else {
                    Text("Post Job", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}