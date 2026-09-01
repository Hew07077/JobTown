package com.example.jobtown.ui.schedule

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jobtown.data.model.InterviewSchedule
import com.example.jobtown.data.model.JobApplication
import com.example.jobtown.ui.theme.BackgroundWhite
import com.example.jobtown.ui.theme.DeepGreenDark
import com.example.jobtown.ui.theme.SageGreenDark
import com.example.jobtown.ui.theme.SageGreenLight
import com.example.jobtown.ui.theme.SageGreenMain
import com.example.jobtown.ui.theme.TextDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleDetailScreen(
    schedule: InterviewSchedule?,
    isEmployer: Boolean,
    isSaving: Boolean = false,
    currentUserId: String = "",
    defaultCompany: String = "",
    applicants: List<JobApplication> = emptyList(),
    onBackClick: () -> Unit,
    onUpdateStatus: (scheduleId: String, status: String) -> Unit,
    onRespondInvite: (scheduleId: String, status: String) -> Unit,
    onUpdateSchedule: (InterviewSchedule) -> Unit = {},
    onDeleteSchedule: (scheduleId: String) -> Unit = {}
) {
    val context = LocalContext.current
    var showRescheduleDialog by remember { mutableStateOf(false) }
    var showEditor by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showRejectConfirm by remember { mutableStateOf(false) }
    var showCancelConfirm by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = BackgroundWhite,
        topBar = {
            TopAppBar(
                title = { Text("Interview details", fontWeight = FontWeight.Bold, color = TextDark) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextDark)
                    }
                },
                actions = {
                    if (isEmployer && schedule != null && !schedule.isCancelledInterview()) {
                        IconButton(onClick = { showEditor = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit interview", tint = DeepGreenDark)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SageGreenMain)
            )
        }
    ) { paddingValues ->
        if (schedule == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Schedule details not found.", color = TextDark)
            }
            return@Scaffold
        }

        val statusText = schedule.status.ifBlank { "Pending" }
        val cancelled = schedule.isCancelledInterview()
        val meetingKind = detectMeetingKind(schedule.locationOrLink)
        val meetingValue = meetingDisplayValue(schedule.locationOrLink)
        val timeParts = parseTimeRange(schedule.time)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = DeepGreenDark)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    AssistChip(
                        onClick = {},
                        label = { Text(statusText, fontWeight = FontWeight.SemiBold) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = Color.White.copy(alpha = 0.18f),
                            labelColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = schedule.title.ifBlank { "Interview session" },
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = schedule.company.ifBlank { "Company" },
                        fontSize = 15.sp,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    if (schedule.seekerName.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "with ${schedule.seekerName}",
                            fontSize = 14.sp,
                            color = SageGreenMain
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(SageGreenLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = DeepGreenDark)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Date", fontSize = 12.sp, color = TextDark.copy(alpha = 0.55f))
                        Text(
                            text = formatShortDate(schedule.date),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextDark
                        )
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(SageGreenLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = DeepGreenDark)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Time", fontSize = 12.sp, color = TextDark.copy(alpha = 0.55f))
                        Text(
                            text = timeParts.first,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextDark
                        )
                        if (timeParts.second.isNotBlank()) {
                            Text(
                                text = "to ${timeParts.second}",
                                fontSize = 13.sp,
                                color = TextDark.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            if (!cancelled) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (meetingKind == MeetingKind.ONLINE) SageGreenLight else Color.White
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (meetingKind == MeetingKind.ONLINE) DeepGreenDark else SageGreenMain),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (meetingKind == MeetingKind.ONLINE) Icons.Default.Videocam else Icons.Default.Map,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (meetingKind == MeetingKind.ONLINE) "Online meeting" else "In-person meeting",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = DeepGreenDark
                                )
                                Text(
                                    text = meetingValue.ifBlank {
                                        if (meetingKind == MeetingKind.ONLINE) "Google Meet" else "Location not set"
                                    },
                                    fontSize = 13.sp,
                                    color = TextDark.copy(alpha = 0.7f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { openInterviewDestination(context, schedule.locationOrLink) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DeepGreenDark)
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
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (schedule.seekerName.isNotBlank()) {
                        DetailRow(icon = Icons.Default.Person, label = "Candidate", value = schedule.seekerName)
                    }
                    DetailRow(icon = Icons.Default.Business, label = "Company", value = schedule.company.ifBlank { "—" })
                    DetailRow(icon = Icons.Default.CalendarMonth, label = "Full date", value = formatDisplayDate(schedule.date))
                }
            }

            if (schedule.preferredTime.isNotBlank() || schedule.rescheduleReason.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = SageGreenLight.copy(alpha = 0.7f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Reschedule request", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = DeepGreenDark)
                        if (schedule.preferredTime.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Preferred time: ${schedule.preferredTime}", fontSize = 14.sp, color = TextDark)
                        }
                        if (schedule.rescheduleReason.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Reason: ${schedule.rescheduleReason}", fontSize = 14.sp, color = TextDark)
                        }
                    }
                }
            }

            if (schedule.notes.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Notes, contentDescription = null, tint = SageGreenDark)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Notes & instructions", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = schedule.notes, fontSize = 14.sp, color = TextDark.copy(alpha = 0.8f))
                    }
                }
            }

            if (cancelled) {
                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete from my list", fontWeight = FontWeight.SemiBold)
                }
            } else if (isEmployer) {
                if (
                    statusText.equals("Pending", ignoreCase = true) ||
                    statusText.equals("Scheduled", ignoreCase = true) ||
                    statusText.equals("Accepted", ignoreCase = true) ||
                    statusText.equals("Reschedule Requested", ignoreCase = true)
                ) {
                    Button(
                        onClick = { showEditor = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DeepGreenDark)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Edit & resend invite", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onUpdateStatus(schedule.id, "Completed") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Mark completed")
                        }
                        OutlinedButton(
                            onClick = { showCancelConfirm = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            } else {
                if (statusText.equals("Pending", ignoreCase = true) || statusText.equals("Scheduled", ignoreCase = true)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onRespondInvite(schedule.id, "Accepted") },
                            colors = ButtonDefaults.buttonColors(containerColor = DeepGreenDark),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Accept invite", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showRescheduleDialog = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Reschedule", color = DeepGreenDark)
                            }
                            OutlinedButton(
                                onClick = { showRejectConfirm = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Reject", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                } else if (
                    statusText.equals("Accepted", ignoreCase = true) ||
                    statusText.equals("Reschedule Requested", ignoreCase = true)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showRescheduleDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Reschedule", color = DeepGreenDark)
                        }
                        OutlinedButton(
                            onClick = { showCancelConfirm = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    if (showRescheduleDialog && schedule != null) {
        RescheduleRequestDialog(
            onDismiss = { showRescheduleDialog = false },
            onSubmit = { reason, preferredTime ->
                onUpdateSchedule(
                    schedule.copy(
                        rescheduleReason = reason,
                        preferredTime = preferredTime,
                        status = "Reschedule Requested"
                    )
                )
                showRescheduleDialog = false
            }
        )
    }

    if (showEditor && schedule != null) {
        InterviewEditorScreen(
            isEdit = true,
            isSaving = isSaving,
            currentUserId = currentUserId,
            defaultCompany = defaultCompany,
            applicants = applicants,
            prefill = SchedulePrefill(
                seekerId = schedule.userId,
                seekerName = schedule.seekerName,
                employerId = schedule.employerId,
                jobId = schedule.jobId,
                company = schedule.company,
                title = schedule.title
            ),
            existing = schedule,
            onDismiss = { if (!isSaving) showEditor = false },
            onSave = { updated ->
                onUpdateSchedule(updated)
                showEditor = false
            }
        )
    }

    if (showDeleteConfirm && schedule != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete interview", fontWeight = FontWeight.Bold) },
            text = { Text("Remove this cancelled interview from your list? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteSchedule(schedule.id)
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Keep", color = TextDark) }
            }
        )
    }

    if (showRejectConfirm && schedule != null) {
        AlertDialog(
            onDismissRequest = { showRejectConfirm = false },
            title = { Text("Reject interview", fontWeight = FontWeight.Bold) },
            text = { Text("Reject this interview invite? The employer will see the status as Rejected.") },
            confirmButton = {
                Button(
                    onClick = {
                        onRespondInvite(schedule.id, "Rejected")
                        showRejectConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Confirm reject", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showRejectConfirm = false }) { Text("Back", color = TextDark) }
            }
        )
    }

    if (showCancelConfirm && schedule != null) {
        AlertDialog(
            onDismissRequest = { showCancelConfirm = false },
            title = { Text("Cancel interview", fontWeight = FontWeight.Bold) },
            text = { Text("Cancel this scheduled interview? It will be marked as Cancelled.") },
            confirmButton = {
                Button(
                    onClick = {
                        onRespondInvite(schedule.id, "Cancelled")
                        showCancelConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Confirm cancel", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showCancelConfirm = false }) { Text("Keep interview", color = TextDark) }
            }
        )
    }
}

@Composable
private fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = SageGreenDark, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(label, fontSize = 11.sp, color = TextDark.copy(alpha = 0.5f))
            Text(value, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextDark)
        }
    }
}
