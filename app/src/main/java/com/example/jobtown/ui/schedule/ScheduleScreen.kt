package com.example.jobtown.ui.schedule

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.jobtown.data.InterviewSchedule
import com.example.jobtown.data.User
import com.example.jobtown.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SchedulePrefill(
    val seekerId: String? = "",
    val seekerName: String? = "",
    val employerId: String? = "",
    val company: String? = "",
    val title: String? = ""
) {
    val isEmpty: Boolean get() = seekerId.isNullOrBlank() && company.isNullOrBlank() && title.isNullOrBlank()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    navController: NavController,
    user: User?,
    schedules: List<InterviewSchedule>?,
    isLoading: Boolean = false,
    isEmployer: Boolean = false,
    isSaving: Boolean = false,
    prefill: SchedulePrefill = SchedulePrefill(),
    onCreateSchedule: (InterviewSchedule) -> Unit = {},
    onUpdateStatus: (scheduleId: String, status: String) -> Unit = { _, _ -> },
    onRespondInvite: (scheduleId: String, status: String) -> Unit = { _, _ -> },
    onProfileClick: () -> Unit = {}
) {
    val safeSchedules = schedules ?: emptyList()
    var showCreateDialog by remember(prefill) { mutableStateOf(isEmployer && !prefill.isEmpty) }
    var selectedFilterTab by remember { mutableStateOf(0) }

    val filteredSchedules = remember(safeSchedules, selectedFilterTab) {
        when (selectedFilterTab) {
            1 -> safeSchedules.filter { it.status.equals("Scheduled", ignoreCase = true) }
            2 -> safeSchedules.filter { it.status.equals("Accepted", ignoreCase = true) }
            3 -> safeSchedules.filter { it.status.equals("Completed", ignoreCase = true) }
            else -> safeSchedules
        }
    }

    Scaffold(
        containerColor = BackgroundWhite,
        topBar = {
            Column(modifier = Modifier.background(SageGreenMain)) {
                TopAppBar(
                    title = {
                        Text(
                            text = if (isEmployer) "Manage Interviews" else "My Interviews",
                            fontWeight = FontWeight.Bold,
                            color = TextDark,
                            fontSize = 18.sp
                        )
                    },
                    actions = {
                        IconButton(
                            onClick = onProfileClick,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(SageGreenLight)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profile",
                                tint = DeepGreenDark
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )

                ScrollableTabRow(
                    selectedTabIndex = selectedFilterTab,
                    containerColor = Color.Transparent,
                    edgePadding = 16.dp,
                    indicator = {},
                    divider = {}
                ) {
                    val tabs = listOf("All", "Pending", "Accepted", "Completed")
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedFilterTab == index,
                            onClick = { selectedFilterTab = index },
                            modifier = Modifier
                                .padding(horizontal = 4.dp, vertical = 8.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (selectedFilterTab == index) DeepGreenDark else SageGreenLight)
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = title,
                                color = if (selectedFilterTab == index) Color.White else DeepGreenDark,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        },
        floatingActionButton = {
            if (isEmployer) {
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = DeepGreenDark,
                    contentColor = Color.White
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Schedule Interview")
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = DeepGreenDark
                    )
                }
                filteredSchedules.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(SageGreenLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = DeepGreenDark,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (isEmployer) "No interviews found." else "No interview invites found in this category.",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextDark.copy(alpha = 0.6f)
                        )
                        if (isEmployer) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { showCreateDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = DeepGreenDark)
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Schedule New Interview", color = Color.White, fontSize = 13.sp)
                            }
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 72.dp)
                    ) {
                        items(filteredSchedules, key = { it.id.ifBlank { it.title + it.date + it.time } }) { schedule ->
                            ScheduleCard(
                                schedule = schedule,
                                isEmployer = isEmployer,
                                onUpdateStatus = onUpdateStatus,
                                onRespondInvite = onRespondInvite
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateScheduleDialog(
            currentUserId = user?.id.orEmpty(),
            prefill = prefill,
            isSaving = isSaving,
            onDismiss = { if (!isSaving) showCreateDialog = false },
            onSubmit = { schedule ->
                onCreateSchedule(schedule)
                showCreateDialog = false
            }
        )
    }
}

@Composable
private fun ScheduleCard(
    schedule: InterviewSchedule,
    isEmployer: Boolean,
    onUpdateStatus: (scheduleId: String, status: String) -> Unit,
    onRespondInvite: (scheduleId: String, status: String) -> Unit
) {
    val context = LocalContext.current
    val statusText = schedule.status.ifBlank { "Scheduled" }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = schedule.title.ifBlank { "Interview Session" },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepGreenDark,
                    modifier = Modifier.weight(1f)
                )
                AssistChip(
                    onClick = {},
                    label = { Text(statusText, fontSize = 12.sp) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = when (statusText.lowercase()) {
                            "accepted", "completed" -> SageGreenLight
                            "rejected", "cancelled" -> MaterialTheme.colorScheme.errorContainer
                            else -> SageGreenLight
                        },
                        labelColor = when (statusText.lowercase()) {
                            "rejected", "cancelled" -> MaterialTheme.colorScheme.error
                            else -> DeepGreenDark
                        }
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Business, contentDescription = null, tint = SageGreenDark, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(schedule.company, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextDark)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Schedule, contentDescription = null, tint = SageGreenDark, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("${schedule.date} at ${schedule.time}", fontSize = 13.sp, color = TextDark.copy(alpha = 0.7f))
            }

            if (schedule.locationOrLink.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        val target = schedule.locationOrLink.trim()
                        val formattedUrl = if (target.startsWith("http://") || target.startsWith("https://")) {
                            target
                        } else {
                            "https://$target"
                        }
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(formattedUrl))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Unable to open link", Toast.LENGTH_SHORT).show()
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(Icons.Default.Link, contentDescription = null, tint = DeepGreenDark, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(schedule.locationOrLink, fontSize = 12.sp, color = DeepGreenDark, maxLines = 1)
                }
            }

            if (schedule.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(schedule.notes, fontSize = 13.sp, color = TextDark.copy(alpha = 0.6f))
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isEmployer) {
                if (statusText.equals("Scheduled", ignoreCase = true) || statusText.equals("Accepted", ignoreCase = true)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onUpdateStatus(schedule.id, "Completed") }) {
                            Text("Mark Completed", fontSize = 12.sp)
                        }
                        OutlinedButton(onClick = { onUpdateStatus(schedule.id, "Cancelled") }) {
                            Text("Cancel", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            } else {
                if (statusText.equals("Scheduled", ignoreCase = true)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onRespondInvite(schedule.id, "Accepted") },
                            colors = ButtonDefaults.buttonColors(containerColor = DeepGreenDark)
                        ) {
                            Text("Accept Invite", fontSize = 12.sp, color = Color.White)
                        }
                        OutlinedButton(
                            onClick = { onRespondInvite(schedule.id, "Rejected") }
                        ) {
                            Text("Reject", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateScheduleDialog(
    currentUserId: String,
    prefill: SchedulePrefill,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (InterviewSchedule) -> Unit
) {
    val context = LocalContext.current
    var company by remember { mutableStateOf(prefill.company.orEmpty()) }
    var title by remember { mutableStateOf(prefill.title.orEmpty()) }

    val defaultDate = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }
    var date by remember { mutableStateOf(defaultDate) }
    var time by remember { mutableStateOf("10:00 AM") }
    var locationOrLink by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(millis))
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK", color = DeepGreenDark)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = TextDark)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text("Schedule an Interview", fontWeight = FontWeight.Bold, color = TextDark) },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!prefill.seekerName.isNullOrBlank()) {
                    OutlinedTextField(
                        value = prefill.seekerName,
                        onValueChange = {},
                        label = { Text("Candidate") },
                        singleLine = true,
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Job Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = company,
                    onValueChange = { company = it },
                    label = { Text("Company Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Date (YYYY-MM-DD)") },
                    singleLine = true,
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = "Pick Date", tint = DeepGreenDark)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = time,
                    onValueChange = { time = it },
                    label = { Text("Time (e.g. 10:00 AM)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = locationOrLink,
                    onValueChange = { locationOrLink = it },
                    label = { Text("Location or Meeting Link") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = !isSaving,
                onClick = {
                    val seekerIdStr = prefill.seekerId.orEmpty()
                    val employerIdStr = prefill.employerId.orEmpty()

                    when {
                        seekerIdStr.isBlank() -> {
                            Toast.makeText(context, "Candidate profile is missing.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        title.isBlank() -> {
                            Toast.makeText(context, "Job title is required.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        company.isBlank() -> {
                            Toast.makeText(context, "Company name is required.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        date.isBlank() -> {
                            Toast.makeText(context, "Date is required.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        time.isBlank() -> {
                            Toast.makeText(context, "Time is required.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                    }

                    onSubmit(
                        InterviewSchedule(
                            userId = seekerIdStr.trim(),
                            employerId = employerIdStr.ifBlank { currentUserId },
                            jobId = "",
                            title = title.trim(),
                            company = company.trim(),
                            date = date.trim(),
                            time = time.trim(),
                            locationOrLink = locationOrLink.trim(),
                            status = "Scheduled",
                            notes = notes.trim()
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = DeepGreenDark, contentColor = Color.White)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Schedule & Send")
                }
            }
        },
        dismissButton = {
            TextButton(
                enabled = !isSaving,
                onClick = onDismiss
            ) {
                Text("Cancel", color = TextDark)
            }
        }
    )
}