package com.example.jobtown.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.jobtown.data.model.JobApplication
import com.example.jobtown.data.model.User
import com.example.jobtown.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class SchedulePrefill(
    val seekerId: String? = "",
    val seekerName: String? = "",
    val employerId: String? = "",
    val jobId: String? = "",
    val company: String? = "",
    val title: String? = ""
) {
    val isEmpty: Boolean get() = seekerId.isNullOrBlank() && title.isNullOrBlank()
}

fun JobApplication.toSchedulePrefill(employerId: String, fallbackCompany: String): SchedulePrefill =
    SchedulePrefill(
        seekerId = userId,
        seekerName = applicantName,
        employerId = employerId,
        company = companyName.ifBlank { fallbackCompany },
        title = jobTitle,
        jobId = jobId
    )

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
    onDeleteSchedule: (scheduleId: String) -> Unit = {},
    onClearPrefill: () -> Unit = {},
    applicants: List<JobApplication> = emptyList(),
    onProfileClick: () -> Unit = {}
) {
    val safeSchedules = schedules ?: emptyList()
    var showCreateDialog by remember(prefill) { mutableStateOf(isEmployer && !prefill.isEmpty) }
    var selectedFilterTab by remember { mutableStateOf(0) }

    var rescheduleTarget by remember { mutableStateOf<InterviewSchedule?>(null) }
    var rejectTarget by remember { mutableStateOf<InterviewSchedule?>(null) }
    var cancelTarget by remember { mutableStateOf<InterviewSchedule?>(null) }
    var editTarget by remember { mutableStateOf<InterviewSchedule?>(null) }
    var deleteTarget by remember { mutableStateOf<InterviewSchedule?>(null) }

    val filteredSchedules = remember(safeSchedules, selectedFilterTab) {
        when (selectedFilterTab) {
            1 -> safeSchedules.filter { it.status.equals("Pending", ignoreCase = true) || it.status.equals("Scheduled", ignoreCase = true) }
            2 -> safeSchedules.filter { it.status.equals("Accepted", ignoreCase = true) }
            3 -> safeSchedules.filter { it.status.equals("Reschedule Requested", ignoreCase = true) }
            4 -> safeSchedules.filter {
                it.status.equals("Completed", ignoreCase = true) ||
                    it.status.equals("Cancelled", ignoreCase = true) ||
                    it.status.equals("Rejected", ignoreCase = true)
            }
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
                                onCancelConfirm = { cancelTarget = schedule },
                                onEditSchedule = { editTarget = schedule },
                                onDeleteSchedule = { deleteTarget = schedule }
                            )
                        }
                    }
                }
            }
        }
    }

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

    rejectTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { rejectTarget = null },
            title = { Text("Reject Interview Invitation", fontWeight = FontWeight.Bold) },
            text = { Text("Reject this interview invite? The employer will see the status as Rejected. This is different from cancelling an already accepted interview.") },
            confirmButton = {
                Button(
                    onClick = {
                        onRespondInvite(target.id, "Rejected")
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

    cancelTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { cancelTarget = null },
            title = { Text("Cancel Interview", fontWeight = FontWeight.Bold) },
            text = { Text("Cancel this scheduled interview? The status will be marked as Cancelled, not Rejected.") },
            confirmButton = {
                Button(
                    onClick = {
                        onRespondInvite(target.id, "Cancelled")
                        cancelTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Confirm Cancel", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { cancelTarget = null }) { Text("Keep interview", color = TextDark) }
            }
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete interview", fontWeight = FontWeight.Bold) },
            text = { Text("Remove this cancelled interview from your list? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteSchedule(target.id)
                        deleteTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Keep", color = TextDark) }
            }
        )
    }

    editTarget?.let { target ->
        InterviewEditorScreen(
            isEdit = true,
            isSaving = isSaving,
            currentUserId = user?.id.orEmpty(),
            defaultCompany = user?.companyName?.ifBlank { user.name }.orEmpty(),
            applicants = applicants,
            prefill = prefill,
            existing = target,
            onDismiss = { if (!isSaving) editTarget = null },
            onSave = { updated ->
                onUpdateSchedule(updated)
                editTarget = null
            }
        )
    }

    if (showCreateDialog) {
        InterviewEditorScreen(
            isEdit = false,
            isSaving = isSaving,
            currentUserId = user?.id.orEmpty(),
            defaultCompany = user?.companyName?.ifBlank { user.name }.orEmpty(),
            applicants = applicants,
            prefill = prefill,
            onDismiss = {
                if (!isSaving) {
                    showCreateDialog = false
                    onClearPrefill()
                }
            },
            onSave = { schedule ->
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
    onCancelConfirm: () -> Unit,
    onEditSchedule: () -> Unit,
    onDeleteSchedule: () -> Unit
) {
    val context = LocalContext.current
    val statusText = schedule.status.ifBlank { "Pending" }
    val cancelled = schedule.isCancelledInterview()
    val meetingKind = detectMeetingKind(schedule.locationOrLink)

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
                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = SageGreenDark, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(formatDisplayDate(schedule.date), fontSize = 13.sp, color = TextDark.copy(alpha = 0.75f))
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Schedule, contentDescription = null, tint = SageGreenDark, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(schedule.time.ifBlank { "Time not set" }, fontSize = 13.sp, color = TextDark.copy(alpha = 0.7f))
            }

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

            if (!cancelled && schedule.locationOrLink.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { openInterviewDestination(context, schedule.locationOrLink) },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepGreenDark),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = if (meetingKind == MeetingKind.ONLINE) Icons.Default.Videocam else Icons.Default.Map,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (meetingKind == MeetingKind.ONLINE) "Join Google Meet" else "Open in Google Maps",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (cancelled) {
                OutlinedButton(
                    onClick = onDeleteSchedule,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Delete from list", fontSize = 12.sp)
                }
            } else if (isEmployer) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onEditSchedule,
                        colors = ButtonDefaults.buttonColors(containerColor = DeepGreenDark)
                    ) {
                        Text("Edit", fontSize = 12.sp, color = Color.White)
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
                if (statusText.equals("Pending", ignoreCase = true) || statusText.equals("Scheduled", ignoreCase = true)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = { onRespondInvite(schedule.id, "Accepted") },
                            colors = ButtonDefaults.buttonColors(containerColor = DeepGreenDark),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Accept", fontSize = 11.sp, color = Color.White)
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
                } else if (statusText.equals("Accepted", ignoreCase = true) ||
                    statusText.equals("Reschedule Requested", ignoreCase = true)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = onRequestReschedule,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Reschedule", fontSize = 11.sp, color = DeepGreenDark)
                        }
                        OutlinedButton(
                            onClick = onCancelConfirm,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RescheduleRequestDialog(
    onDismiss: () -> Unit,
    onSubmit: (reason: String, preferredTime: String) -> Unit
) {
    var reason by remember { mutableStateOf("") }
    val defaultDate = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    var date by remember { mutableStateOf(defaultDate) }
    var time by remember { mutableStateOf("10:00 AM") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    val timePickerState = rememberTimePickerState(initialHour = 10, initialMinute = 0, is24Hour = false)

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

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val calendar = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                            set(Calendar.MINUTE, timePickerState.minute)
                        }
                        time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(calendar.time)
                        showTimePicker = false
                    }
                ) { Text("OK", color = DeepGreenDark) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel", color = TextDark) }
            },
            text = { TimePicker(state = timePickerState) }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Request Reschedule", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Pick the date and time you prefer. The employer will see this and can update the interview.",
                    fontSize = 13.sp,
                    color = TextDark.copy(alpha = 0.7f)
                )
                OutlinedTextField(
                    value = date,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Preferred date") },
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = "Pick date", tint = DeepGreenDark)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = time,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Preferred time") },
                    trailingIcon = {
                        IconButton(onClick = { showTimePicker = true }) {
                            Icon(Icons.Default.Schedule, contentDescription = "Pick time", tint = DeepGreenDark)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason for reschedule") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (reason.isBlank()) return@Button
                    onSubmit(reason.trim(), "$date at $time")
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
