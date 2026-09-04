package com.example.jobtown.ui.schedule

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.jobtown.data.model.InterviewSchedule
import com.example.jobtown.data.model.JobApplication
import com.example.jobtown.ui.theme.BackgroundWhite
import com.example.jobtown.ui.theme.DeepGreenDark
import com.example.jobtown.ui.theme.SageGreenLight
import com.example.jobtown.ui.theme.SageGreenMain
import com.example.jobtown.ui.theme.TextDark
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InterviewEditorScreen(
    isEdit: Boolean,
    isSaving: Boolean,
    currentUserId: String,
    defaultCompany: String,
    applicants: List<JobApplication>,
    prefill: SchedulePrefill,
    existing: InterviewSchedule? = null,
    onDismiss: () -> Unit,
    onSave: (InterviewSchedule) -> Unit
) {
    val context = LocalContext.current
    val uniqueApplicants = remember(applicants) {
        applicants.distinctBy { it.userId }.filter { it.userId.isNotBlank() }
    }

    var selectedApplicantId by remember {
        mutableStateOf(existing?.userId?.ifBlank { prefill.seekerId.orEmpty() } ?: prefill.seekerId.orEmpty())
    }
    var seekerName by remember {
        mutableStateOf(
            existing?.seekerName?.ifBlank { prefill.seekerName.orEmpty() }
                ?: prefill.seekerName.orEmpty().ifBlank {
                    uniqueApplicants.find { it.userId == selectedApplicantId }?.applicantName.orEmpty()
                }
        )
    }
    var title by remember {
        mutableStateOf(existing?.title?.ifBlank { prefill.title.orEmpty() } ?: prefill.title.orEmpty())
    }
    val company = defaultCompany.ifBlank { existing?.company.orEmpty().ifBlank { prefill.company.orEmpty() } }

    val initialRange = remember(existing?.time) { parseTimeRange(existing?.time.orEmpty()) }

    // Parse candidate preferred time string if present ("yyyy-MM-dd at hh:mm a")
    val preferredParsed = remember(existing?.preferredTime) {
        val pref = existing?.preferredTime.orEmpty().trim()
        if (pref.contains(" at ")) {
            val parts = pref.split(" at ")
            Pair(parts.getOrNull(0)?.trim(), parts.getOrNull(1)?.trim())
        } else {
            Pair(null, null)
        }
    }

    var date by remember {
        mutableStateOf(
            preferredParsed.first
                ?: existing?.date?.ifBlank { null }
                ?: SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        )
    }

    var startTime by remember {
        mutableStateOf(preferredParsed.second ?: initialRange.first)
    }

    // Auto-set End Time to Preferred Start Time + 1 Hour (or fall back to initial range)
    var endTime by remember {
        mutableStateOf(
            preferredParsed.second?.let { addOneHour(it) }
                ?: initialRange.second.ifBlank { "11:00 AM" }
        )
    }

    var meetingKind by remember {
        mutableStateOf(detectMeetingKind(existing?.locationOrLink.orEmpty()))
    }
    var meetingValue by remember {
        mutableStateOf(meetingDisplayValue(existing?.locationOrLink.orEmpty()))
    }
    var notes by remember { mutableStateOf(existing?.notes.orEmpty()) }
    var applicantMenuExpanded by remember { mutableStateOf(false) }

    var showDatePicker by remember { mutableStateOf(false) }
    var timePickerTarget by remember { mutableStateOf<String?>(null) }
    val datePickerState = rememberDatePickerState()

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(millis))
                        }
                        showDatePicker = false
                    }
                ) { Text("OK", color = DeepGreenDark, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel", color = TextDark) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    timePickerTarget?.let { target ->
        val initial = parseClock(if (target == "start") startTime else endTime)
        val timePickerState = rememberTimePickerState(
            initialHour = initial.first,
            initialMinute = initial.second,
            is24Hour = false
        )
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { timePickerTarget = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        val formatted = formatClock(timePickerState.hour, timePickerState.minute)
                        if (target == "start") startTime = formatted else endTime = formatted
                        timePickerTarget = null
                    }
                ) { Text("OK", color = DeepGreenDark, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { timePickerTarget = null }) { Text("Cancel", color = TextDark) }
            },
            title = { Text(if (target == "start") "Start time" else "End time") },
            text = { TimePicker(state = timePickerState) }
        )
    }

    Dialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !isSaving,
            dismissOnClickOutside = false
        )
    ) {
        Scaffold(
            containerColor = BackgroundWhite,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = if (isEdit) "Edit interview" else "Schedule interview",
                            fontWeight = FontWeight.Bold,
                            color = DeepGreenDark
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { if (!isSaving) onDismiss() }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = DeepGreenDark)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = SageGreenMain)
                )
            },
            bottomBar = {
                Surface(shadowElevation = 12.dp, color = Color.White) {
                    Button(
                        enabled = !isSaving,
                        onClick = {
                            val seekerIdStr = selectedApplicantId.ifBlank { existing?.userId.orEmpty().ifBlank { prefill.seekerId.orEmpty() } }
                            if (seekerIdStr.isBlank()) {
                                Toast.makeText(context, "Select a candidate from your applications.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (title.isBlank()) {
                                Toast.makeText(context, "Job title is required.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            // --- TIME VALIDATION ---
                            val startClock = parseClock(startTime)
                            val endClock = parseClock(endTime)
                            val startMinutes = startClock.first * 60 + startClock.second
                            val endMinutes = endClock.first * 60 + endClock.second

                            if (startMinutes >= endMinutes) {
                                Toast.makeText(context, "Start time must be earlier than end time.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            if (meetingKind == MeetingKind.PHYSICAL && meetingValue.isBlank()) {
                                Toast.makeText(context, "Enter the meeting address.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            val selectedJobId = uniqueApplicants.find { it.userId == seekerIdStr }?.jobId
                                ?: existing?.jobId.orEmpty().ifBlank { prefill.jobId.orEmpty() }

                            val base = existing ?: InterviewSchedule()
                            onSave(
                                base.copy(
                                    userId = seekerIdStr.trim(),
                                    seekerName = seekerName.trim().ifBlank {
                                        uniqueApplicants.find { it.userId == seekerIdStr }?.applicantName.orEmpty()
                                    },
                                    employerId = existing?.employerId?.ifBlank { currentUserId }
                                        ?: prefill.employerId.orEmpty().ifBlank { currentUserId },
                                    jobId = selectedJobId,
                                    title = title.trim(),
                                    company = company.trim(),
                                    date = date.trim(),
                                    time = formatTimeRange(startTime, endTime),
                                    locationOrLink = encodeMeeting(meetingKind, meetingValue),
                                    status = "Pending",
                                    notes = notes.trim(),
                                    rescheduleReason = if (isEdit) "" else base.rescheduleReason,
                                    preferredTime = if (isEdit) "" else base.preferredTime
                                )
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DeepGreenDark)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text(
                                text = if (isEdit) "Update & resend" else "Schedule & send",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (existing?.preferredTime?.isNotBlank() == true) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SageGreenLight),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Candidate preferred time", fontWeight = FontWeight.Bold, color = DeepGreenDark, fontSize = 13.sp)
                            Text(existing.preferredTime, fontSize = 14.sp, color = TextDark)
                            if (existing.rescheduleReason.isNotBlank()) {
                                Text("Reason: ${existing.rescheduleReason}", fontSize = 13.sp, color = TextDark.copy(alpha = 0.75f))
                            }
                        }
                    }
                }

                Text("Candidate", fontWeight = FontWeight.SemiBold, color = DeepGreenDark, fontSize = 13.sp)
                if (uniqueApplicants.isNotEmpty() && !isEdit) {
                    ExposedDropdownMenuBox(
                        expanded = applicantMenuExpanded,
                        onExpandedChange = { applicantMenuExpanded = !applicantMenuExpanded }
                    ) {
                        OutlinedTextField(
                            value = uniqueApplicants.find { it.userId == selectedApplicantId }?.let {
                                "${it.applicantName} · ${it.jobTitle}"
                            } ?: seekerName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select applicant") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = applicantMenuExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(expanded = applicantMenuExpanded, onDismissRequest = { applicantMenuExpanded = false }) {
                            uniqueApplicants.forEach { applicant ->
                                DropdownMenuItem(
                                    text = { Text("${applicant.applicantName} · ${applicant.jobTitle}") },
                                    onClick = {
                                        selectedApplicantId = applicant.userId
                                        seekerName = applicant.applicantName
                                        if (title.isBlank()) title = applicant.jobTitle
                                        applicantMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = seekerName.ifBlank { existing?.seekerName.orEmpty() },
                        onValueChange = { if (!isEdit) seekerName = it },
                        readOnly = isEdit,
                        label = { Text("Candidate") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Job title") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                if (company.isNotBlank()) {
                    Text("Company · $company", fontSize = 13.sp, color = TextDark.copy(alpha = 0.65f))
                }

                Text("Date", fontWeight = FontWeight.SemiBold, color = DeepGreenDark, fontSize = 13.sp)
                Card(
                    onClick = { showDatePicker = true },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, SageGreenMain),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = null, tint = DeepGreenDark)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Interview date", fontSize = 12.sp, color = TextDark.copy(alpha = 0.55f))
                            Text(formatShortDate(date), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextDark)
                        }
                    }
                }

                Text("Time range", fontWeight = FontWeight.SemiBold, color = DeepGreenDark, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    TimeRangeCard(
                        label = "Starts",
                        value = startTime,
                        modifier = Modifier.weight(1f),
                        onClick = { timePickerTarget = "start" }
                    )
                    TimeRangeCard(
                        label = "Ends",
                        value = endTime,
                        modifier = Modifier.weight(1f),
                        onClick = { timePickerTarget = "end" }
                    )
                }

                Text("Meeting type", fontWeight = FontWeight.SemiBold, color = DeepGreenDark, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    FilterChip(
                        selected = meetingKind == MeetingKind.ONLINE,
                        onClick = { meetingKind = MeetingKind.ONLINE },
                        label = { Text("Online · Google Meet") },
                        leadingIcon = { Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SageGreenMain,
                            selectedLabelColor = DeepGreenDark
                        )
                    )
                    FilterChip(
                        selected = meetingKind == MeetingKind.PHYSICAL,
                        onClick = { meetingKind = MeetingKind.PHYSICAL },
                        label = { Text("In person") },
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SageGreenMain,
                            selectedLabelColor = DeepGreenDark
                        )
                    )
                }

                if (meetingKind == MeetingKind.ONLINE) {
                    OutlinedTextField(
                        value = meetingValue,
                        onValueChange = { meetingValue = it },
                        label = { Text("Google Meet link (optional)") },
                        placeholder = { Text("https://meet.google.com/...") },
                        leadingIcon = { Icon(Icons.Default.Link, contentDescription = null, tint = DeepGreenDark) },
                        supportingText = { Text("Leave empty to open a new Google Meet room") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                } else {
                    OutlinedTextField(
                        value = meetingValue,
                        onValueChange = { meetingValue = it },
                        label = { Text("Address") },
                        placeholder = { Text("Office address, city") },
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = DeepGreenDark) },
                        supportingText = { Text("Candidates can open this in Google Maps") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes for the candidate (optional)") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun TimeRangeCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, SageGreenMain)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Schedule, contentDescription = null, tint = DeepGreenDark, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(label, fontSize = 12.sp, color = TextDark.copy(alpha = 0.55f))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextDark)
        }
    }
}

private fun parseClock(value: String): Pair<Int, Int> {
    return try {
        val parsed = SimpleDateFormat("hh:mm a", Locale.US).parse(value.trim())
        val calendar = Calendar.getInstance().apply { if (parsed != null) time = parsed }
        calendar.get(Calendar.HOUR_OF_DAY) to calendar.get(Calendar.MINUTE)
    } catch (_: Exception) {
        10 to 0
    }
}

private fun formatClock(hour: Int, minute: Int): String {
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
    }
    return SimpleDateFormat("hh:mm a", Locale.US).format(calendar.time).uppercase(Locale.US)
}

private fun addOneHour(timeStr: String): String {
    return try {
        val parsed = SimpleDateFormat("hh:mm a", Locale.US).parse(timeStr.trim())
        val calendar = Calendar.getInstance().apply {
            if (parsed != null) time = parsed
            add(Calendar.HOUR_OF_DAY, 1)
        }
        SimpleDateFormat("hh:mm a", Locale.US).format(calendar.time).uppercase(Locale.US)
    } catch (_: Exception) {
        "11:00 AM"
    }
}