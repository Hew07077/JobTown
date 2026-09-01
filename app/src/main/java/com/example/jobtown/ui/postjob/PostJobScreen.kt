package com.example.jobtown.ui.postjob

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.jobtown.data.model.Job
import com.example.jobtown.data.model.User
import com.example.jobtown.ui.theme.BackgroundWhite
import com.example.jobtown.ui.theme.DeepGreenDark
import com.example.jobtown.ui.theme.SageGreenDark
import com.example.jobtown.ui.theme.SageGreenLight
import com.example.jobtown.ui.theme.SageGreenMain
import com.example.jobtown.ui.theme.TextDark
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (job.isFeatured == true) SageGreenLight else Color.White
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (job.isFeatured == true) SageGreenDark.copy(alpha = 0.35f) else Color(0xFFE6EDE4)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    color = SageGreenMain.copy(alpha = 0.3f),
                    border = BorderStroke(1.dp, DeepGreenDark.copy(alpha = 0.15f))
                ) {
                    if (!job.companyImageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = job.companyImageUrl,
                            contentDescription = "Company avatar",
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
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = job.title.ifBlank { "Job title" },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (job.isFeatured == true) {
                            Surface(
                                color = DeepGreenDark,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFE082), modifier = Modifier.size(12.dp))
                                    Text(text = "Featured", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Business,
                            contentDescription = null,
                            tint = SageGreenDark,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = job.companyName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextDark.copy(alpha = 0.75f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), thickness = 1.dp, color = Color(0xFFE6EDE4))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PreviewChip(icon = Icons.Default.LocationOn, text = job.location.ifBlank { "Location" })
                PreviewChip(text = job.jobType)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = DeepGreenDark.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = job.salary.ifBlank { "Negotiable" },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepGreenDark,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                if (!expiryDaysText.isNullOrBlank() && !expiryDaysText.startsWith("Select", ignoreCase = true)) {
                    Surface(
                        color = Color.Gray.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Expires $expiryDaysText",
                            fontSize = 12.sp,
                            color = TextDark.copy(alpha = 0.7f),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            if (job.description.isNotBlank()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), thickness = 1.dp, color = Color(0xFFE6EDE4))
                Text(text = job.description, fontSize = 13.sp, color = TextDark.copy(alpha = 0.72f), lineHeight = 20.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun PreviewChip(
    text: String,
    icon: ImageVector? = null
) {
    Surface(
        color = SageGreenMain.copy(alpha = 0.45f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) {
                Icon(imageVector = icon, contentDescription = null, tint = DeepGreenDark, modifier = Modifier.size(13.dp))
            }
            Text(text = text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = DeepGreenDark)
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

    val savedAddresses = remember(currentUser?.location) {
        com.example.jobtown.utils.LocationOptions.parseAddresses(currentUser?.location.orEmpty())
            .map { it.display() }
            .filter { it.isNotBlank() }
    }

    val fields = rememberJobFormFields(
        company = currentUser?.companyName?.ifBlank { currentUser.name } ?: currentUser?.name ?: "",
        location = savedAddresses.firstOrNull().orEmpty(),
        useCustomLocation = savedAddresses.isEmpty()
    )

    LaunchedEffect(currentUser?.companyName, currentUser?.name) {
        if (fields.company.isBlank()) {
            fields.company = currentUser?.companyName?.ifBlank { currentUser.name } ?: currentUser?.name ?: ""
        }
    }

    var selectedExpiryMillis by remember { mutableStateOf<Long?>(null) }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var showPreview by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    val displayExpiryDate = remember(selectedExpiryMillis) {
        selectedExpiryMillis?.let {
            SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(it))
        } ?: "Select date"
    }

    val displayIsoExpiryDate = remember(selectedExpiryMillis) {
        selectedExpiryMillis?.let {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            sdf.format(Date(it))
        }
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
//
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Post Job", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = DeepGreenDark)
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick, enabled = !isSubmitting) {
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
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            JobListingForm(
                fields = fields,
                submitLabel = "Post job",
                enabled = !isSubmitting,
                isSubmitting = isSubmitting,
                requireSalary = true,
                savedAddresses = savedAddresses,
                showPreview = showPreview,
                onTogglePreview = { showPreview = !showPreview },
                previewContent = {
                    StandardJobCard(
                        job = Job(
                            title = fields.title,
                            company = fields.company,
                            companyImageUrl = employerAvatar,
                            location = fields.location,
                            salary = fields.formattedSalary(),
                            type = fields.type,
                            description = fields.description,
                            isFeatured = fields.isFeatured,
                            isOkuFriendly = fields.isOkuFriendly
                        ),
                        expiryDaysText = displayExpiryDate
                    )
                },
                expiryDateText = displayExpiryDate,
                onExpiryDateClick = { showDatePickerDialog = true },
                showFeaturedToggle = true,
                onSubmit = {
                    isSubmitting = true
                    val userId = currentUser?.id?.trim()?.ifEmpty { null }
                    val newJob = Job(
                        id = UUID.randomUUID().toString(),
                        title = fields.title.trim(),
                        company = fields.company.trim(),
                        companyImageUrl = employerAvatar,
                        location = fields.location.trim(),
                        salary = fields.formattedSalary(),
                        salaryRange = fields.formattedSalary(),
                        type = fields.type,
                        description = fields.description.trim(),
                        requirements = fields.requirementsList(),
                        skills = fields.skillsList(),
                        isFeatured = fields.isFeatured,
                        isOkuFriendly = fields.isOkuFriendly,
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
                            fields.errorMessage = message ?: "Failed to post job to Supabase."
                        }
                    }
                }
            )
        }
    }
}
