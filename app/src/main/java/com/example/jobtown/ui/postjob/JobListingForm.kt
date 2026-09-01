package com.example.jobtown.ui.postjob

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.jobtown.ui.theme.DeepGreenDark
import com.example.jobtown.ui.theme.SageGreenDark
import com.example.jobtown.ui.theme.SageGreenLight
import com.example.jobtown.ui.theme.TextDark


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

@OptIn(ExperimentalMaterial3Api::class)
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
    showFeaturedToggle: Boolean = false
) {
    var locationDropdownExpanded by remember { mutableStateOf(false) }
    var minSalaryExpanded by remember { mutableStateOf(false) }
    var maxSalaryExpanded by remember { mutableStateOf(false) }
    var jobTypeExpanded by remember { mutableStateOf(false) }

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
                        Text(text = if (showPreview) "Hide preview" else "Show preview", fontSize = 13.sp, color = DeepGreenDark, fontWeight = FontWeight.SemiBold)
                    }
                }
                if (showPreview && previewContent != null) {
                    previewContent()
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
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            if (savedAddresses.isNotEmpty() && !fields.useCustomLocation) {
                ExposedDropdownMenuBox(
                    expanded = locationDropdownExpanded && enabled,
                    onExpandedChange = { if (enabled) locationDropdownExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = fields.location,
                        onValueChange = {},
                        readOnly = true,
                        label = { RequiredLabel("Location", required = true) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = locationDropdownExpanded) },
                        enabled = enabled,
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = locationDropdownExpanded,
                        onDismissRequest = { locationDropdownExpanded = false }
                    ) {
                        savedAddresses.forEach { address ->
                            DropdownMenuItem(
                                text = { Text(address) },
                                onClick = {
                                    fields.location = address
                                    locationDropdownExpanded = false
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Enter a different address...") },
                            onClick = {
                                fields.location = ""
                                fields.useCustomLocation = true
                                locationDropdownExpanded = false
                            }
                        )
                    }
                }
            } else {
                OutlinedTextField(
                    value = fields.location,
                    onValueChange = { fields.location = it },
                    label = { RequiredLabel("Location", required = true) },
                    singleLine = true,
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                if (savedAddresses.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            fields.useCustomLocation = false
                            fields.location = savedAddresses.first()
                        },
                        enabled = enabled
                    ) {
                        Text(text = "Use a saved address", fontSize = 13.sp, color = DeepGreenDark, fontWeight = FontWeight.SemiBold)
                    }
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
                    value = fields.type,
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
                    JobTypeOptions.forEach { opt ->
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

            Text(text = "Salary", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = DeepGreenDark)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ExposedDropdownMenuBox(
                    expanded = minSalaryExpanded && enabled,
                    onExpandedChange = { if (enabled) minSalaryExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = if (fields.minSalary.isNotEmpty()) "$${fields.minSalary}" else "",
                        onValueChange = {},
                        label = { RequiredLabel("Min salary", required = requireSalary) },
                        readOnly = true,
                        enabled = enabled,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = minSalaryExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = minSalaryExpanded,
                        onDismissRequest = { minSalaryExpanded = false }
                    ) {
                        MinSalaryOptions.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text("$$opt", fontSize = 13.sp) },
                                onClick = {
                                    fields.minSalary = opt
                                    minSalaryExpanded = false
                                }
                            )
                        }
                    }
                }
                ExposedDropdownMenuBox(
                    expanded = maxSalaryExpanded && enabled,
                    onExpandedChange = { if (enabled) maxSalaryExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = if (fields.maxSalary.isNotEmpty()) "$${fields.maxSalary}" else "",
                        onValueChange = {},
                        label = { RequiredLabel("Max salary", required = requireSalary) },
                        readOnly = true,
                        enabled = enabled,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = maxSalaryExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = maxSalaryExpanded,
                        onDismissRequest = { maxSalaryExpanded = false }
                    ) {
                        MaxSalaryOptions.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text("$$opt", fontSize = 13.sp) },
                                onClick = {
                                    fields.maxSalary = opt
                                    maxSalaryExpanded = false
                                }
                            )
                        }
                    }
                }
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
//
            if (showFeaturedToggle) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 1.dp, color = Color(0xFFE6EDE4))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Featured listing", fontSize = 15.sp, color = TextDark, fontWeight = FontWeight.SemiBold)
                        Text(text = "Highlight this job on the feed", fontSize = 12.sp, color = TextDark.copy(alpha = 0.55f))
                    }
                    Switch(
                        checked = fields.isFeatured,
                        onCheckedChange = { fields.isFeatured = it },
                        enabled = enabled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = DeepGreenDark,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0xFFCBD5E1)
                        )
                    )
                }
            }

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
