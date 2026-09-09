package com.example.jobtown.ui.postjob

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.jobtown.ui.components.LocationPicker
import com.example.jobtown.ui.theme.DeepGreenDark
import com.example.jobtown.ui.theme.SageGreenDark
import com.example.jobtown.ui.theme.SageGreenLight
import com.example.jobtown.ui.theme.TextDark
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

private val FormJobTypeOptions: List<String> = listOf("Full-time", "Part-time", "Contract", "Internship", "Temporary")

private fun formatRm(value: Float): String {
    if (value >= 50000f) {
        return "RM 50,000+"
    }
    return "RM " + NumberFormat.getNumberInstance(Locale.US).format(value.roundToInt())
}

private fun formatSalaryPreviewText(minSalaryStr: String, maxSalaryStr: String): String {
    val minVal = minSalaryStr.toFloatOrNull() ?: 1500f
    val maxVal = maxSalaryStr.toFloatOrNull() ?: 10000f

    val formattedMin = formatRm(minVal)
    val formattedMax = formatRm(maxVal)

    return "$formattedMin - $formattedMax / month"
}

@Composable
private fun RequiredLabel(text: String, required: Boolean = false) {
    if (!required) {
        Text(text)
        return
    }
    Text(
        buildAnnotatedString {
            append(text)
            append(" ")
            withStyle(SpanStyle(color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)) {
                append("*")
            }
        }
    )
}

@Composable
fun JobPreviewCard(
    fields: JobFormFields,
    companyPhotoUrl: String? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = SageGreenLight,
                    modifier = Modifier.size(48.dp),
                    border = BorderStroke(1.dp, SageGreenDark.copy(alpha = 0.3f))
                ) {
                    if (!companyPhotoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = companyPhotoUrl,
                            contentDescription = "Company Photo",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = fields.company.take(2).uppercase().ifBlank { "JOB" },
                                fontWeight = FontWeight.Bold,
                                color = DeepGreenDark,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = fields.title.ifBlank { "Job title" },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = fields.company.ifBlank { "Company Name" },
                        fontSize = 13.sp,
                        color = TextDark.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFEBF3EA)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = DeepGreenDark,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = fields.location.ifBlank { "Kuala Lumpur, Malaysia" },
                            fontSize = 12.sp,
                            color = DeepGreenDark,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFEBF3EA)
                ) {
                    Text(
                        text = fields.type.ifBlank { "Full-time" },
                        fontSize = 12.sp,
                        color = DeepGreenDark,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFF1F5F9)
            ) {
                Text(
                    text = formatSalaryPreviewText(fields.minSalary, fields.maxSalary),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun JobListingForm(
    fields: JobFormFields,
    submitLabel: String,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isSubmitting: Boolean = false,
    requireSalary: Boolean = false,
    savedAddresses: List<String> = emptyList(),
    companyPhotoUrl: String? = null,
    showPreview: Boolean = false,
    onTogglePreview: (() -> Unit)? = null,
    previewContent: (@Composable () -> Unit)? = null,
    expiryDateText: String? = null,
    onExpiryDateClick: (() -> Unit)? = null,
    isCompanyEditable: Boolean = true
) {
    var jobTypeExpanded by remember { mutableStateOf(false) }

    // Parse initial salary range directly from fields instead of hardcoded defaults
    var salaryRange by remember(fields.minSalary, fields.maxSalary) {
        val initialMin = fields.minSalary.toFloatOrNull()?.coerceIn(500f, 50000f) ?: 1500f
        val initialMax = fields.maxSalary.toFloatOrNull()?.coerceIn(initialMin, 50000f) ?: 10000f
        mutableStateOf(initialMin..initialMax)
    }

    var isAddingNewLocation by remember(savedAddresses, fields.location) {
        mutableStateOf(savedAddresses.isEmpty() || savedAddresses.none { it.equals(fields.location, ignoreCase = true) })
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (onTogglePreview != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Job listing", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    TextButton(onClick = onTogglePreview, enabled = enabled) {
                        Text(
                            text = if (showPreview) "Hide preview" else "Show preview",
                            fontSize = 13.sp,
                            color = DeepGreenDark,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                if (showPreview) {
                    if (previewContent != null) {
                        previewContent()
                    } else {
                        JobPreviewCard(fields = fields, companyPhotoUrl = companyPhotoUrl)
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 1.dp, color = Color(0xFFE6EDE4))
                }
            }

            if (!companyPhotoUrl.isNullOrBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = SageGreenLight,
                        modifier = Modifier.size(48.dp),
                        border = BorderStroke(1.dp, SageGreenDark.copy(alpha = 0.3f))
                    ) {
                        AsyncImage(
                            model = companyPhotoUrl,
                            contentDescription = "Company photo",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Column {
                        Text(text = fields.company.ifBlank { "Company Name" }, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DeepGreenDark)
                        Text(text = "Company photo is taken from the profile", fontSize = 12.sp, color = TextDark.copy(alpha = 0.55f), lineHeight = 16.sp)
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 1.dp, color = Color(0xFFE6EDE4))
            }

            Text(text = "Company", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = DeepGreenDark)
            OutlinedTextField(
                value = fields.company,
                onValueChange = { fields.company = it },
                label = { RequiredLabel("Company", required = true) },
                singleLine = true,
                enabled = enabled,
                readOnly = !isCompanyEditable,
                supportingText = if (!isCompanyEditable) {
                    { Text("Taken from your profile and can't be changed here", fontSize = 11.sp) }
                } else null,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Text(
                text = "Location",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = DeepGreenDark
            )

            if (savedAddresses.isNotEmpty()) {
                Text(
                    text = "Select from saved profile locations or add a new location:",
                    fontSize = 12.sp,
                    color = TextDark.copy(alpha = 0.6f)
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    savedAddresses.forEach { address ->
                        val isSelected = !isAddingNewLocation && fields.location.equals(address, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                isAddingNewLocation = false
                                fields.location = address
                            },
                            label = { Text(address, fontSize = 12.sp) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SageGreenLight,
                                selectedLabelColor = DeepGreenDark
                            )
                        )
                    }

                    FilterChip(
                        selected = isAddingNewLocation,
                        onClick = {
                            isAddingNewLocation = true
                            fields.location = ""
                        },
                        label = { Text("Add new location", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SageGreenLight,
                            selectedLabelColor = DeepGreenDark
                        )
                    )
                }
            }

            AnimatedVisibility(visible = isAddingNewLocation || savedAddresses.isEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Choose State & City",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextDark.copy(alpha = 0.7f)
                    )
                    LocationPicker(
                        locationString = fields.location,
                        onLocationStringChange = { fields.location = it },
                        allowMultipleBranches = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 1.dp, color = Color(0xFFE6EDE4))

            Text(text = "Position", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = DeepGreenDark)
            OutlinedTextField(
                value = fields.title,
                onValueChange = { fields.title = it },
                label = { RequiredLabel("Job title", required = true) },
                singleLine = true,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            ExposedDropdownMenuBox(
                expanded = jobTypeExpanded && enabled,
                onExpandedChange = { if (enabled) jobTypeExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = fields.type.ifBlank { "Full-time" },
                    onValueChange = {},
                    readOnly = true,
                    enabled = enabled,
                    label = { RequiredLabel("Job type", required = true) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = jobTypeExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = jobTypeExpanded,
                    onDismissRequest = { jobTypeExpanded = false }
                ) {
                    FormJobTypeOptions.forEach { opt: String ->
                        DropdownMenuItem(
                            text = { Text(opt, fontSize = 13.sp) },
                            onClick = {
                                fields.type = opt
                                jobTypeExpanded = false
                            }
                        )
                    }
                }
            }

            if (expiryDateText != null && onExpiryDateClick != null) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = expiryDateText,
                        onValueChange = {},
                        readOnly = true,
                        enabled = enabled,
                        label = { Text("Expiry date") },
                        trailingIcon = {
                            IconButton(onClick = { if (enabled) onExpiryDateClick() }) {
                                Icon(Icons.Default.DateRange, contentDescription = "Select date", tint = DeepGreenDark)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable(enabled = enabled, onClick = onExpiryDateClick)
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 1.dp, color = Color(0xFFE6EDE4))

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Salary Range", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = DeepGreenDark)
                    Text(
                        text = "${formatRm(salaryRange.start)} - ${formatRm(salaryRange.endInclusive)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepGreenDark
                    )
                }

                RangeSlider(
                    value = salaryRange,
                    onValueChange = { range ->
                        val step = 500f
                        val start = (range.start / step).roundToInt() * step
                        val end = (range.endInclusive / step).roundToInt() * step
                        salaryRange = start..end
                        fields.minSalary = start.roundToInt().toString()
                        fields.maxSalary = end.roundToInt().toString()
                    },
                    valueRange = 500f..50000f,
                    enabled = enabled,
                    colors = SliderDefaults.colors(
                        thumbColor = DeepGreenDark,
                        activeTrackColor = DeepGreenDark,
                        inactiveTrackColor = SageGreenLight
                    ),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 1.dp, color = Color(0xFFE6EDE4))

            Text(text = "Role details", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = DeepGreenDark)
            OutlinedTextField(
                value = fields.description,
                onValueChange = { fields.description = it },
                label = { RequiredLabel("Description", required = true) },
                enabled = enabled,
                minLines = 4,
                maxLines = 8,
                singleLine = false,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(
                value = fields.requirements,
                onValueChange = { fields.requirements = it },
                label = { Text("Requirements (separate with comma)") },
                enabled = enabled,
                minLines = 2,
                maxLines = 4,
                singleLine = false,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(
                value = fields.skills,
                onValueChange = { fields.skills = it },
                label = { Text("Skills required (separate with comma)") },
                enabled = enabled,
                minLines = 2,
                maxLines = 4,
                singleLine = false,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )


            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 1.dp, color = Color(0xFFE6EDE4))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = enabled) { fields.isOkuFriendly = !fields.isOkuFriendly },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(text = "OKU-friendly role", fontSize = 15.sp, color = TextDark, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = "This job welcomes applicants with disabilities (OKU)",
                        fontSize = 12.sp,
                        color = TextDark.copy(alpha = 0.55f)
                    )
                }
                Checkbox(
                    checked = fields.isOkuFriendly,
                    onCheckedChange = { fields.isOkuFriendly = it },
                    enabled = enabled,
                    colors = CheckboxDefaults.colors(checkedColor = DeepGreenDark)
                )
            }

            if (fields.errorMessage.isNotBlank()) {
                Text(text = fields.errorMessage, color = MaterialTheme.colorScheme.error, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }

            Button(
                onClick = {
                    val error = fields.validate(requireSalary)
                    if (error != null) {
                        fields.errorMessage = error
                    } else {
                        fields.errorMessage = ""
                        onSubmit()
                    }
                },
                enabled = enabled && !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DeepGreenDark)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                } else {
                    Text(submitLabel, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}