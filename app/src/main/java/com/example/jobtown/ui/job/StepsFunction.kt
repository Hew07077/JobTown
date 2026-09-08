package com.example.jobtown.ui.job

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.WorkHistory
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jobtown.data.model.ProfileEntry
import com.example.jobtown.ui.applied.formatCertificatesForDisplay
import com.example.jobtown.ui.theme.DeepGreenDark
import com.example.jobtown.ui.theme.SageGreenDark
import com.example.jobtown.ui.theme.SageGreenLight
import com.example.jobtown.ui.theme.TextDark
import kotlin.collections.forEach

@OptIn(ExperimentalLayoutApi::class)
@Composable
 fun Step1PersonalInfo(
    phoneNumber: String,
    onPhoneNumberChange: (String) -> Unit,
    linkedInUrl: String,
    onLinkedInUrlChange: (String) -> Unit,
    salaryMin: Float,
    salaryMax: Float,
    onSalaryRangeChange: (Float, Float) -> Unit,
    selectedStartDateOption: String,
    onStartDateOptionChange: (String) -> Unit,
    customStartDate: String,
    onCustomStartDateChange: (String) -> Unit,
    showValidationErrors: Boolean,
    isPhoneValid: Boolean
) {
    val startDateOptions = listOf("Immediate", "Within 2 Weeks", "Within 1 Month", "Custom Date")

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
            value = phoneNumber,
            onValueChange = onPhoneNumberChange,
            label = { Text("Phone Number *") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            isError = showValidationErrors && !isPhoneValid,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        )

        OutlinedTextField(
            value = linkedInUrl,
            onValueChange = onLinkedInUrlChange,
            label = { Text("LinkedIn Profile / Portfolio URL (Optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        )

        // Salary Range Selection Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, SageGreenDark.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Expected Monthly Salary Range",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DeepGreenDark
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SageGreenLight
                    ) {
                        Text(
                            text = "RM ${salaryMin.toInt()} - RM ${salaryMax.toInt()}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepGreenDark
                        )
                    }
                }

                // Simplified Single-slider proxy or dual visual slider representation
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Minimum Expected (RM ${salaryMin.toInt()})",
                    fontSize = 11.sp,
                    color = TextDark.copy(alpha = 0.6f)
                )
                Slider(
                    value = salaryMin,
                    onValueChange = { newVal ->
                        if (newVal <= salaryMax) {
                            onSalaryRangeChange(newVal, salaryMax)
                        }
                    },
                    valueRange = 1500f..15000f,
                    steps = 26,
                    colors = SliderDefaults.colors(
                        thumbColor = DeepGreenDark,
                        activeTrackColor = DeepGreenDark,
                        inactiveTrackColor = SageGreenLight
                    )
                )

                Text(
                    text = "Maximum Expected (RM ${salaryMax.toInt()})",
                    fontSize = 11.sp,
                    color = TextDark.copy(alpha = 0.6f)
                )
                Slider(
                    value = salaryMax,
                    onValueChange = { newVal ->
                        if (newVal >= salaryMin) {
                            onSalaryRangeChange(salaryMin, newVal)
                        }
                    },
                    valueRange = 1500f..15000f,
                    steps = 26,
                    colors = SliderDefaults.colors(
                        thumbColor = DeepGreenDark,
                        activeTrackColor = DeepGreenDark,
                        inactiveTrackColor = SageGreenLight
                    )
                )
            }
        }

        // Start Date Selection Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, SageGreenDark.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Available Start Date",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DeepGreenDark
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    startDateOptions.forEach { option ->
                        val isSelected = selectedStartDateOption == option
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) DeepGreenDark else SageGreenLight.copy(alpha = 0.4f),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isSelected) DeepGreenDark else SageGreenDark.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.clickable { onStartDateOptionChange(option) }
                        ) {
                            Text(
                                text = option,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else DeepGreenDark
                            )
                        }
                    }
                }

                if (selectedStartDateOption == "Custom Date") {
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = customStartDate,
                        onValueChange = onCustomStartDateChange,
                        label = { Text("Specify Date (e.g., 15 Sept 2026)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }
    }
}

// ==================== Step: Education / Experience / Certifications ====================

@Composable
fun StepQualifications(
    educationEntries: List<ProfileEntry>,
    experienceEntries: List<ProfileEntry>,
    certificationEntries: List<ProfileEntry>,
    onAddEntry: (String, ProfileEntry) -> Unit,
    onRemoveEntry: (String, ProfileEntry) -> Unit,
    isUploadingCertificate: Boolean,
    onUploadCertificate: (ByteArray, String, (String?) -> Unit) -> Unit,
    showValidationErrors: Boolean,
    isLoading: Boolean
) {
    var addDialogFor by remember { mutableStateOf<String?>(null) }
    val uriHandler = LocalUriHandler.current

    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text(
            text = "Loaded from your profile. Add or remove anything for this application — changes are saved back to your profile.",
            fontSize = 12.sp,
            color = TextDark.copy(alpha = 0.6f),
            lineHeight = 16.sp
        )

        if (isLoading) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(color = DeepGreenDark, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    Text("Loading your education and experience…", fontSize = 13.sp, color = TextDark.copy(alpha = 0.7f))
                }
            }
        } else {
            QualificationSection(
                icon = Icons.Filled.School,
                title = "Education",
                entries = educationEntries,
                addLabel = "Add education",
                emptyText = "Add your highest qualification so employers know your background.",
                onAddClick = { addDialogFor = "Education" },
                onRemove = { onRemoveEntry("Education", it) },
                isError = showValidationErrors && educationEntries.isEmpty(),
                errorText = "Please add at least one education entry."
            )

            QualificationSection(
                icon = Icons.Filled.WorkHistory,
                title = "Work Experience (optional)",
                entries = experienceEntries,
                addLabel = "Add experience",
                emptyText = "Add relevant work experience, if any.",
                onAddClick = { addDialogFor = "Experience" },
                onRemove = { onRemoveEntry("Experience", it) }
            )

            QualificationSection(
                icon = Icons.Filled.WorkspacePremium,
                title = "Certifications (optional)",
                entries = certificationEntries,
                addLabel = "Add certification",
                emptyText = "Add any certifications relevant to this role.",
                onAddClick = { addDialogFor = "Certification" },
                onRemove = { onRemoveEntry("Certification", it) },
                onViewFile = { url -> uriHandler.openUri(url) }
            )
        }
    }

    addDialogFor?.let { category ->
        AddQualificationDialog(
            category = category,
            isUploadingFile = isUploadingCertificate,
            onDismiss = { addDialogFor = null },
            onSave = { entry ->
                onAddEntry(category, entry)
                addDialogFor = null
            },
            onUploadCertificate = onUploadCertificate
        )
    }
}

@Composable
private fun QualificationSection(
    icon: ImageVector,
    title: String,
    entries: List<ProfileEntry>,
    addLabel: String,
    emptyText: String,
    onAddClick: () -> Unit,
    onRemove: (ProfileEntry) -> Unit,
    isError: Boolean = false,
    errorText: String = "",
    onViewFile: ((String) -> Unit)? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, tint = DeepGreenDark, modifier = Modifier.size(18.dp))
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextDark)
            if (entries.isNotEmpty()) {
                Surface(shape = CircleShape, color = SageGreenLight) {
                    Text(
                        text = "${entries.size}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepGreenDark,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }

        if (entries.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SageGreenLight.copy(alpha = 0.25f),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isError) MaterialTheme.colorScheme.error.copy(alpha = 0.6f) else SageGreenDark.copy(alpha = 0.2f)
                ),
                modifier = Modifier.fillMaxWidth().clickable(onClick = onAddClick)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = emptyText, fontSize = 12.sp, color = TextDark.copy(alpha = 0.6f), lineHeight = 16.sp)
                        if (isError) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = errorText, fontSize = 11.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
                        }
                    }
                    Icon(Icons.Filled.Add, contentDescription = addLabel, tint = DeepGreenDark, modifier = Modifier.size(22.dp))
                }
            }
        } else {
            entries.forEach { entry ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.Top) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SageGreenLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(icon, contentDescription = null, tint = DeepGreenDark, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = entry.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextDark)
                            if (entry.subtitle.isNotBlank()) Text(text = entry.subtitle, fontSize = 12.sp, color = DeepGreenDark)
                            if (entry.period.isNotBlank()) Text(text = entry.period, fontSize = 11.sp, color = TextDark.copy(alpha = 0.5f))
                            if (entry.description.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = entry.description, fontSize = 12.sp, color = TextDark.copy(alpha = 0.75f), lineHeight = 16.sp)
                            }
                            if (entry.fileUrl.isNotBlank() && onViewFile != null) {
                                TextButton(onClick = { onViewFile(entry.fileUrl) }, contentPadding = PaddingValues(0.dp)) {
                                    Icon(Icons.Default.AttachFile, contentDescription = null, tint = DeepGreenDark, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("View file", color = DeepGreenDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                        IconButton(onClick = { onRemove(entry) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Remove", tint = TextDark.copy(alpha = 0.35f), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
            OutlinedButton(
                onClick = onAddClick,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = DeepGreenDark)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(addLabel, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
 fun Step2Documents(
    resumeFileName: String,
    isResumeValid: Boolean,
    isSavedProfileResume: Boolean,
    onPickResume: () -> Unit,
    onRemoveResume: () -> Unit,
    coverLetterFileName: String,
    onPickCoverLetter: () -> Unit,
    onRemoveCoverLetter: () -> Unit,
    additionalNotes: String,
    onAdditionalNotesChange: (String) -> Unit,
    showValidationErrors: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Resume Section (Required)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Resume Document *",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (showValidationErrors && !isResumeValid) MaterialTheme.colorScheme.error else DeepGreenDark
            )

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (showValidationErrors && !isResumeValid) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f) else SageGreenLight.copy(alpha = 0.3f),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (showValidationErrors && !isResumeValid) MaterialTheme.colorScheme.error else SageGreenDark.copy(alpha = 0.3f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPickResume() }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isSavedProfileResume) SageGreenDark else DeepGreenDark,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = when {
                                        isSavedProfileResume -> Icons.Filled.CheckCircle
                                        isResumeValid -> Icons.Filled.Description
                                        else -> Icons.Filled.UploadFile
                                    },
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = when {
                                        isSavedProfileResume -> "Resume on file"
                                        isResumeValid -> resumeFileName
                                        else -> "Upload Resume (PDF / Doc)"
                                    },
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (isSavedProfileResume) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = SageGreenDark.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "Saved",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SageGreenDark,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = when {
                                    isSavedProfileResume -> "From your profile · Tap to use a different file"
                                    isResumeValid -> "Tap to change document"
                                    else -> "Required for application submission"
                                },
                                fontSize = 11.sp,
                                color = SageGreenDark
                            )
                        }
                    }

                    if (isResumeValid) {
                        IconButton(onClick = onRemoveResume) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Remove file",
                                tint = TextDark.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = DeepGreenDark,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Cover Letter Section (Optional Attachment)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Cover Letter Attachment (Optional)",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = DeepGreenDark
            )

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = SageGreenLight.copy(alpha = 0.2f),
                border = BorderStroke(width = 1.dp, color = SageGreenDark.copy(alpha = 0.2f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPickCoverLetter() }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = SageGreenDark,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (coverLetterFileName.isNotBlank()) Icons.Filled.NoteAlt else Icons.Filled.PostAdd,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (coverLetterFileName.isNotBlank()) coverLetterFileName else "Upload Cover Letter Document",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark,
                                maxLines = 1
                            )
                            Text(
                                text = if (coverLetterFileName.isNotBlank()) "Tap to change cover letter file" else "PDF or Doc format (Optional)",
                                fontSize = 11.sp,
                                color = SageGreenDark
                            )
                        }
                    }

                    if (coverLetterFileName.isNotBlank()) {
                        IconButton(onClick = onRemoveCoverLetter) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Remove file",
                                tint = TextDark.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = DeepGreenDark,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        OutlinedTextField(
            value = additionalNotes,
            onValueChange = onAdditionalNotesChange,
            label = { Text("Additional Notes / Remarks (Optional)") },
            minLines = 3,
            maxLines = 5,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        )
    }
}

@Composable
fun Step3Review(
    jobTitle: String,
    companyName: String,
    resumeFileName: String,
    coverLetterFileName: String,
    additionalNotes: String,
    phoneNumber: String,
    linkedInUrl: String,
    salaryRangeText: String,
    startDateText: String,
    educationSummary: String,
    experienceSummary: String,
    certificatesSummary: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "Position: $jobTitle", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = DeepGreenDark)
            Text(text = "Company: $companyName", fontSize = 13.sp, color = TextDark.copy(alpha = 0.8f))
            HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)
            ReviewRow(label = "Phone Number", value = phoneNumber)
            ReviewRow(label = "LinkedIn / Portfolio", value = linkedInUrl.ifBlank { "Not provided" })
            ReviewRow(label = "Expected Salary Range", value = salaryRangeText)
            ReviewRow(label = "Available Start Date", value = startDateText)
            HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)
            ReviewRow(label = "Education", value = educationSummary.ifBlank { "Not specified" })
            ReviewRow(label = "Experience", value = experienceSummary.ifBlank { "Not specified" })
            ReviewRow(label = "Certificates", value = formatCertificatesForDisplay(certificatesSummary).first.ifBlank { "None attached" })
            HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)
            ReviewRow(label = "Attached Resume", value = resumeFileName)
            ReviewRow(label = "Cover Letter Document", value = coverLetterFileName)
            if (additionalNotes.isNotBlank()) {
                ReviewRow(label = "Additional Notes", value = additionalNotes)
            }
        }
    }
}

@Composable
private fun ReviewRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SageGreenDark)
        Text(text = value, fontSize = 13.sp, color = TextDark, maxLines = 5)
    }
}