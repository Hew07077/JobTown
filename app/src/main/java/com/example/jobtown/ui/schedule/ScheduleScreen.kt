package com.example.jobtown.ui.schedule

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
import androidx.compose.material.icons.filled.*
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
import com.example.jobtown.Screen
import com.example.jobtown.data.model.InterviewSchedule
import com.example.jobtown.data.model.User
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
    onUpdateSchedule: (InterviewSchedule) -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    val safeSchedules = schedules ?: emptyList()
    var showCreateDialog by remember(prefill) { mutableStateOf(isEmployer && !prefill.isEmpty) }
    var selectedFilterTab by remember { mutableStateOf(0) }

    // Dialog States
    var rescheduleTarget by remember { mutableStateOf<InterviewSchedule?>(null) }
    var rejectTarget by remember { mutableStateOf<InterviewSchedule?>(null) }
    var editTarget by remember { mutableStateOf<InterviewSchedule?>(null) }

    val filteredSchedules = remember(safeSchedules, selectedFilterTab) {
        when (selectedFilterTab) {
            1 -> safeSchedules.filter { it.status.equals("Pending", ignoreCase = true) || it.status.equals("Scheduled", ignoreCase = true) }
            2 -> safeSchedules.filter { it.status.equals("Accepted", ignoreCase = true) }
            3 -> safeSchedules.filter { it.status.equals("Reschedule Requested", ignoreCase = true) }
            4 -> safeSchedules.filter { it.status.equals("Completed", ignoreCase = true) || it.status.equals("Cancelled", ignoreCase = true) }
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
                            Icon(imageVector = Icons.Default.Person, contentDescription = "Profile", tint = DeepGreenDark)
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
                    val tabs = listOf("All", "Pending", "Accepted", "Rescheduled", "Ended")
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
                isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = DeepGreenDark)
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
                            Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null, tint = DeepGreenDark, modifier = Modifier.size(36.dp))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (isEmployer) "No interviews found." else "No interview invites found in this category.",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextDark.copy(alpha = 0.6f)
                        )
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
                                onCardClick = { navController.navigate(Screen.ScheduleDetail.createRoute(schedule.id)) },
                                onUpdateStatus = onUpdateStatus,
                                onRespondInvite = onRespondInvite,
                                onRequestReschedule = { rescheduleTarget = schedule },
                                onRejectConfirm = { rejectTarget = schedule },
                                onEditSchedule = { editTarget = schedule }
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialog: Jobseeker Reschedule Request
    rescheduleTarget?.let { target ->
        RescheduleRequestDialog(
            onDismiss = { rescheduleTarget = null },
            onSubmit = { reason, preferredTime ->
                val updated = target.copy(
                    rescheduleReason = reason,
                    preferredTime = preferredTime,
                    status = "Reschedule Requested"
                )
                onUpdateSchedule(updated)
                rescheduleTarget = null
            }
        )
    }

    // Dialog: Jobseeker Reject Confirmation
    rejectTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { rejectTarget = null },
            title = { Text("Reject Interview Invitation", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to reject this interview invite? The status will be marked as cancelled.") },
            confirmButton = {
                Button(
                    onClick = {
                        onRespondInvite(target.id, "Cancelled")
                        rejectTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Confirm Reject", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { rejectTarget = null }) { Text("Cancel", color = TextDark) }
            }
        )
    }

    // Dialog: Employer Edit Schedule
    editTarget?.let { target ->
        EditScheduleDialog(
            schedule = target,
            onDismiss = { editTarget = null },
            onSubmit = { newDate, newTime, newLocation, newNotes ->
                val updated = target.copy(
                    date = newDate,
                    time = newTime,
                    locationOrLink = newLocation,
                    notes = newNotes,
                    status = "Pending" // Change status back to Pending after edit
                )
                onUpdateSchedule(updated)
                editTarget = null
            }
        )
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
    onCardClick: () -> Unit,
    onUpdateStatus: (scheduleId: String, status: String) -> Unit,
    onRespondInvite: (scheduleId: String, status: String) -> Unit,
    onRequestReschedule: () -> Unit,
    onRejectConfirm: () -> Unit,
    onEditSchedule: () -> Unit
) {
    val context = LocalContext.current
    val statusText = schedule.status.ifBlank { "Pending" }

    Card(
        onClick = onCardClick,
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
                            "reschedule requested" -> MaterialTheme.colorScheme.tertiaryContainer
                            else -> SageGreenLight
                        },
                        labelColor = when (statusText.lowercase()) {
                            "rejected", "cancelled" -> MaterialTheme.colorScheme.error
                            "reschedule requested" -> MaterialTheme.colorScheme.onTertiaryContainer
                            else -> DeepGreenDark
                        }
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (schedule.seekerName.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = SageGreenDark, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = schedule.seekerName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

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

            // Display Reschedule Info if present
            if (schedule.rescheduleReason.isNotBlank() || schedule.preferredTime.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = SageGreenLight.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        if (schedule.preferredTime.isNotBlank()) {
                            Text("Preferred Time: ${schedule.preferredTime}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = DeepGreenDark)
                        }
                        if (schedule.rescheduleReason.isNotBlank()) {
                            Text("Reason: ${schedule.rescheduleReason}", fontSize = 12.sp, color = TextDark)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            if (isEmployer) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onEditSchedule,
                        colors = ButtonDefaults.buttonColors(containerColor = DeepGreenDark)
                    ) {
                        Text("Edit Schedule", fontSize = 12.sp, color = Color.White)
                    }
                    if (statusText.equals("Pending", ignoreCase = true) || statusText.equals("Accepted", ignoreCase = true)) {
                        OutlinedButton(onClick = { onUpdateStatus(schedule.id, "Completed") }) {
                            Text("Complete", fontSize = 12.sp)
                        }
                    }
                    OutlinedButton(onClick = { onUpdateStatus(schedule.id, "Cancelled") }) {
                        Text("Cancel", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                    }
                }
            } else {
                if (statusText.equals("Pending", ignoreCase = true) || statusText.equals("Scheduled", ignoreCase = true) || statusText.equals("Accepted", ignoreCase = true)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (!statusText.equals("Accepted", ignoreCase = true)) {
                            Button(
                                onClick = { onRespondInvite(schedule.id, "Accepted") },
                                colors = ButtonDefaults.buttonColors(containerColor = DeepGreenDark),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Accept", fontSize = 11.sp, color = Color.White)
                            }
                        }
                        OutlinedButton(
                            onClick = onRequestReschedule,
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Text("Reschedule", fontSize = 11.sp, color = DeepGreenDark)
                        }
                        OutlinedButton(
                            onClick = onRejectConfirm,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Reject", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RescheduleRequestDialog(
    onDismiss: () -> Unit,
    onSubmit: (reason: String, preferredTime: String) -> Unit
) {
    var reason by remember { mutableStateOf("") }
    var preferredTime by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Request Reschedule", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = preferredTime,
                    onValueChange = { preferredTime = it },
                    label = { Text("Preferred Date & Time") },
                    placeholder = { Text("e.g. 2026-09-01 at 02:00 PM") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason for Reschedule") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (reason.isBlank() || preferredTime.isBlank()) return@Button
                    onSubmit(reason.trim(), preferredTime.trim())
                },
                colors = ButtonDefaults.buttonColors(containerColor = DeepGreenDark)
            ) {
                Text("Send Request", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextDark) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditScheduleDialog(
    schedule: InterviewSchedule,
    onDismiss: () -> Unit,
    onSubmit: (date: String, time: String, location: String, notes: String) -> Unit
) {
    var date by remember { mutableStateOf(schedule.date) }
    var time by remember { mutableStateOf(schedule.time) }
    var locationOrLink by remember { mutableStateOf(schedule.locationOrLink) }
    var notes by remember { mutableStateOf(schedule.notes) }

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
                ) { Text("OK", color = DeepGreenDark) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel", color = TextDark) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Interview Schedule", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (schedule.preferredTime.isNotBlank()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SageGreenLight),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("Jobseeker Preferred Time:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(schedule.preferredTime, fontSize = 12.sp)
                        }
                    }
                }

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
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(date.trim(), time.trim(), locationOrLink.trim(), notes.trim()) },
                colors = ButtonDefaults.buttonColors(containerColor = DeepGreenDark)
            ) {
                Text("Update & Resend (Pending)", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextDark) }
        }
    )
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
    var seekerName by remember { mutableStateOf(prefill.seekerName.orEmpty()) }
    var company by remember { mutableStateOf(prefill.company.orEmpty()) }
    var title by remember { mutableStateOf(prefill.title.orEmpty()) }

    val defaultDate = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
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
                ) { Text("OK", color = DeepGreenDark) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel", color = TextDark) }
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
                OutlinedTextField(
                    value = seekerName,
                    onValueChange = { seekerName = it },
                    label = { Text("Candidate Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

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
                    }

                    onSubmit(
                        InterviewSchedule(
                            userId = seekerIdStr.trim(),
                            seekerName = seekerName.trim(),
                            employerId = employerIdStr.ifBlank { currentUserId },
                            jobId = "",
                            title = title.trim(),
                            company = company.trim(),
                            date = date.trim(),
                            time = time.trim(),
                            locationOrLink = locationOrLink.trim(),
                            status = "Pending",
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
            TextButton(enabled = !isSaving, onClick = onDismiss) { Text("Cancel", color = TextDark) }
        }
    )
}