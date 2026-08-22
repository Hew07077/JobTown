package com.example.jobtown.ui.postjob

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.jobtown.data.Job
import com.example.jobtown.data.User
import com.example.jobtown.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

@Composable
fun StandardJobCard(
    job: Job,
    expiryDaysText: String? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (job.isFeatured == true) SageGreenMain.copy(alpha = 0.25f) else Color.White
        ),
        border = BorderStroke(
            width = if (job.isFeatured == true) 1.dp else 0.5.dp,
            color = if (job.isFeatured == true) DeepGreenDark else Color(0xFFE0E0E0)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(6.dp)),
                color = SageGreenMain.copy(alpha = 0.3f),
                border = BorderStroke(0.5.dp, DeepGreenDark.copy(alpha = 0.2f))
            ) {
                if (!job.companyImageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = job.companyImageUrl,
                        contentDescription = "Company Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Business,
                            contentDescription = null,
                            tint = DeepGreenDark,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = job.title.ifBlank { "Job Title Placeholder" },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark,
                        modifier = Modifier.weight(1f)
                    )
                    if (job.isFeatured == true) {
                        Surface(
                            color = DeepGreenDark,
                            shape = RoundedCornerShape(3.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color.Yellow,
                                    modifier = Modifier.size(8.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "FEATURED",
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Business,
                        contentDescription = null,
                        tint = DeepGreenDark,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = job.companyName,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextDark.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = DeepGreenDark,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = job.location.ifBlank { "Location" },
                        fontSize = 10.sp,
                        color = TextDark.copy(alpha = 0.6f)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Surface(
                    color = SageGreenMain.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(3.dp)
                ) {
                    Text(
                        text = job.jobType,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DeepGreenDark,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = DeepGreenDark.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(3.dp)
                    ) {
                        Text(
                            text = job.salary.ifBlank { "Negotiable" },
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepGreenDark,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }

                    if (!expiryDaysText.isNullOrBlank()) {
                        Surface(
                            color = Color.Gray.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(3.dp)
                        ) {
                            Text(
                                text = "Expires: $expiryDaysText",
                                fontSize = 9.sp,
                                color = TextDark.copy(alpha = 0.7f),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                if (job.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = job.description,
                        fontSize = 10.sp,
                        color = TextDark.copy(alpha = 0.7f),
                        maxLines = 2
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostJobScreen(
    navController: NavController? = null,
    currentUser: User? = null,
    onJobPosted: (Job, onComplete: (Boolean, String?) -> Unit) -> Unit = { _, _ -> },
    onBackClick: () -> Unit = { navController?.popBackStack() }
) {
    val employerAvatar = remember(currentUser?.avatarUrl) {
        currentUser?.avatarUrl?.trim()?.takeIf { it.isNotBlank() }
    }

    var title by remember { mutableStateOf("") }
    var company by remember {
        mutableStateOf(currentUser?.companyName?.ifBlank { currentUser.name } ?: currentUser?.name ?: "")
    }

    LaunchedEffect(currentUser?.companyName, currentUser?.name) {
        if (company.isBlank()) {
            company = currentUser?.companyName?.ifBlank { currentUser.name } ?: currentUser?.name ?: ""
        }
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

    var selectedExpiryMillis by remember { mutableStateOf<Long?>(null) }
    var showDatePickerDialog by remember { mutableStateOf(false) }

    val displayExpiryDate = remember(selectedExpiryMillis) {
        selectedExpiryMillis?.let {
            SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(it))
        } ?: "Select Date"
    }

    val displayIsoExpiryDate = remember(selectedExpiryMillis) {
        selectedExpiryMillis?.let {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            sdf.format(Date(it))
        }
    }

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

    if (showDatePickerDialog) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedExpiryMillis ?: Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 30)
            }.timeInMillis
        )

        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedExpiryMillis = datePickerState.selectedDateMillis
                    showDatePickerDialog = false
                }) {
                    Text("OK", color = DeepGreenDark, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    selectedDayContainerColor = DeepGreenDark,
                    todayDateBorderColor = DeepGreenDark,
                    todayContentColor = DeepGreenDark
                )
            )
        }
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
                StandardJobCard(
                    job = Job(
                        title = title,
                        company = company,
                        companyImageUrl = employerAvatar,
                        location = location,
                        salary = formattedSalary,
                        type = type,
                        description = description,
                        isFeatured = isFeatured
                    ),
                    expiryDaysText = displayExpiryDate,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
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

                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = displayExpiryDate,
                        onValueChange = {},
                        readOnly = true,
                        enabled = !isSubmitting,
                        label = { Text("Expiry Date", fontSize = 10.sp) },
                        trailingIcon = {
                            IconButton(onClick = { if (!isSubmitting) showDatePickerDialog = true }) {
                                Icon(
                                    Icons.Default.DateRange,
                                    contentDescription = "Select Date",
                                    tint = DeepGreenDark,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(6.dp)
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable(enabled = !isSubmitting) { showDatePickerDialog = true }
                    )
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

                        val newJob = Job(
                            id = UUID.randomUUID().toString(),
                            title = title.trim(),
                            company = company.trim(),
                            companyImageUrl = employerAvatar,
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
                            expiredAt = displayIsoExpiryDate
                        )

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