package com.example.jobtown.ui.schedule

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jobtown.data.InterviewSchedule
import com.example.jobtown.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleDetailScreen(
    schedule: InterviewSchedule?,
    isEmployer: Boolean,
    onBackClick: () -> Unit,
    onUpdateStatus: (scheduleId: String, status: String) -> Unit,
    onRespondInvite: (scheduleId: String, status: String) -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        containerColor = BackgroundWhite,
        topBar = {
            TopAppBar(
                title = { Text("Interview Details", fontWeight = FontWeight.Bold, color = TextDark) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextDark)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SageGreenMain)
            )
        }
    ) { paddingValues ->
        if (schedule == null) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("Schedule details not found.", color = TextDark)
            }
            return@Scaffold
        }

        val statusText = schedule.status.ifBlank { "Pending" }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Card
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
                            fontSize = 20.sp,
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

                    Spacer(modifier = Modifier.height(12.dp))

                    if (schedule.seekerName.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = SageGreenDark, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = schedule.seekerName, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Business, contentDescription = null, tint = SageGreenDark, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = schedule.company, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = TextDark)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = SageGreenDark, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "${schedule.date} at ${schedule.time}", fontSize = 15.sp, color = TextDark)
                    }
                }
            }

            // Meeting Link / Location Card
            if (schedule.locationOrLink.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Location / Meeting Link", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
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
                            colors = ButtonDefaults.buttonColors(containerColor = DeepGreenDark),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Link, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open Link / Location", color = Color.White)
                        }
                    }
                }
            }

            // Additional Notes
            if (schedule.notes.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Notes, contentDescription = null, tint = SageGreenDark)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Notes & Instructions", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = schedule.notes, fontSize = 14.sp, color = TextDark.copy(alpha = 0.8f))
                    }
                }
            }

            // Interactive Action Buttons
            if (isEmployer) {
                if (statusText.equals("Pending", ignoreCase = true) || statusText.equals("Scheduled", ignoreCase = true) || statusText.equals("Accepted", ignoreCase = true) || statusText.equals("Reschedule Requested", ignoreCase = true)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { onUpdateStatus(schedule.id, "Completed") },
                            colors = ButtonDefaults.buttonColors(containerColor = DeepGreenDark),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Mark Completed")
                        }
                        OutlinedButton(
                            onClick = { onUpdateStatus(schedule.id, "Cancelled") },
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
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Accept Invite", color = Color.White)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onRespondInvite(schedule.id, "Reschedule Requested") },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Reschedule", color = DeepGreenDark)
                            }
                            OutlinedButton(
                                onClick = { onRespondInvite(schedule.id, "Rejected") },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Reject", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                } else if (statusText.equals("Accepted", ignoreCase = true)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onRespondInvite(schedule.id, "Reschedule Requested") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Reschedule", color = DeepGreenDark)
                        }
                        OutlinedButton(
                            onClick = { onRespondInvite(schedule.id, "Rejected") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Reject", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}