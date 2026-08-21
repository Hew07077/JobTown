package com.example.jobtown.ui.schedule

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
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.jobtown.data.InterviewSchedule
import com.example.jobtown.data.User
import com.example.jobtown.ui.theme.*

/** Prefill values carried over from a chat thread when an employer taps "Manage Schedule". */
data class SchedulePrefill(
    val seekerId: String = "",
    val seekerName: String = "",
    val employerId: String = "",
    val company: String = "",
    val title: String = ""
) {
    val isEmpty: Boolean get() = seekerId.isBlank() && company.isBlank() && title.isBlank()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    navController: NavController,
    user: User?,
    schedules: List<InterviewSchedule>,
    isLoading: Boolean = false,
    isEmployer: Boolean = false,
    isSaving: Boolean = false,
    prefill: SchedulePrefill = SchedulePrefill(),
    onCreateSchedule: (InterviewSchedule) -> Unit = {},
    onUpdateStatus: (scheduleId: String, status: String) -> Unit = { _, _ -> },
    onProfileClick: () -> Unit = {}
) {
    var showCreateDialog by remember(prefill) { mutableStateOf(isEmployer && !prefill.isEmpty) }

    Scaffold(
        containerColor = BackgroundWhite,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Interview Schedules",
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SageGreenMain)
            )
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
                schedules.isEmpty() -> {
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
                            text = "No scheduled interviews found.",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextDark.copy(alpha = 0.6f)
                        )
                        if (isEmployer) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap + to schedule one with a candidate.",
                                fontSize = 13.sp,
                                color = TextDark.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 72.dp)
                    ) {
                        items(schedules, key = { it.id.ifBlank { it.title + it.date + it.time } }) { schedule ->
                            ScheduleCard(
                                schedule = schedule,
                                isEmployer = isEmployer,
                                onUpdateStatus = onUpdateStatus
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
            onDismiss = { showCreateDialog = false },
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
    onUpdateStatus: (scheduleId: String, status: String) -> Unit
) {
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
                    text = schedule.title.ifBlank { "Interview" },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepGreenDark,
                    modifier = Modifier.weight(1f)
                )
                AssistChip(
                    onClick = {},
                    label = { Text(schedule.status.ifBlank { "Scheduled" }, fontSize = 12.sp) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = SageGreenLight,
                        labelColor = DeepGreenDark
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
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Link, contentDescription = null, tint = SageGreenDark, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(schedule.locationOrLink, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = DeepGreenDark)
                }
            }

            if (schedule.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(schedule.notes, fontSize = 13.sp, color = TextDark.copy(alpha = 0.6f))
            }

            if (isEmployer && schedule.status.equals("Scheduled", ignoreCase = true)) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onUpdateStatus(schedule.id, "Completed") }) {
                        Text("Mark Completed", fontSize = 12.sp)
                    }
                    OutlinedButton(onClick = { onUpdateStatus(schedule.id, "Cancelled") }) {
                        Text("Cancel", fontSize = 12.sp)
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
    var seekerId by remember { mutableStateOf(prefill.seekerId) }
    var seekerName by remember { mutableStateOf(prefill.seekerName) }
    var company by remember { mutableStateOf(prefill.company) }
    var title by remember { mutableStateOf(prefill.title) }
    var date by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var locationOrLink by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Schedule an Interview", fontWeight = FontWeight.Bold, color = TextDark) },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = seekerName,
                    onValueChange = { seekerName = it },
                    label = { Text("Candidate name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = seekerId,
                    onValueChange = { seekerId = it },
                    label = { Text("Candidate user ID") },
                    singleLine = true,
                    enabled = prefill.seekerId.isBlank(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Job title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = company,
                    onValueChange = { company = it },
                    label = { Text("Company") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Date (e.g. 2026-08-20)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = time,
                    onValueChange = { time = it },
                    label = { Text("Time (e.g. 3:00 PM)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = locationOrLink,
                    onValueChange = { locationOrLink = it },
                    label = { Text("Location or call link") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (showError) {
                    Text(
                        text = "Candidate ID, title, company, date and time are required.",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isSaving,
                onClick = {
                    if (seekerId.isBlank() || title.isBlank() || company.isBlank() || date.isBlank() || time.isBlank()) {
                        showError = true
                        return@Button
                    }
                    onSubmit(
                        InterviewSchedule(
                            userId = seekerId.trim(),
                            employerId = prefill.employerId.ifBlank { currentUserId },
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
                    Text("Schedule")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextDark) }
        }
    )
}