package com.example.jobtown.ui.job

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.WorkHistory
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.jobtown.data.model.ProfileEntry
import com.example.jobtown.ui.profile.ProfileOptions
import com.example.jobtown.ui.theme.BackgroundWhite
import com.example.jobtown.ui.theme.DeepGreenDark
import com.example.jobtown.ui.theme.SageGreenLight
import com.example.jobtown.ui.theme.SageGreenMain
import com.example.jobtown.ui.theme.TextDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddQualificationDialog(
    category: String,
    isUploadingFile: Boolean,
    onDismiss: () -> Unit,
    onSave: (ProfileEntry) -> Unit,
    onUploadCertificate: (ByteArray, String, (String?) -> Unit) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var subtitle by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var titleError by remember { mutableStateOf<String?>(null) }
    var subtitleError by remember { mutableStateOf<String?>(null) }
    var fileError by remember { mutableStateOf<String?>(null) }
    var yearRangeError by remember { mutableStateOf<String?>(null) }
    var employmentType by remember { mutableStateOf(ProfileOptions.EMPLOYMENT_TYPES.first()) }
    var educationLevel by remember { mutableStateOf(ProfileOptions.EDUCATION_LEVELS.getOrElse(2) { ProfileOptions.EDUCATION_LEVELS.first() }) }
    var issuer by remember { mutableStateOf(ProfileOptions.CERTIFICATE_ISSUERS.first()) }
    var customIssuer by remember { mutableStateOf("") }
    var startYear by remember { mutableStateOf(ProfileOptions.YEARS.getOrElse(1) { "" }) }
    var endYear by remember { mutableStateOf("Present") }
    var year by remember { mutableStateOf(ProfileOptions.YEARS.getOrElse(1) { "" }) }
    var fileUrl by remember { mutableStateOf("") }
    var fileName by remember { mutableStateOf("") }

    val headerIcon = when (category) {
        "Education" -> Icons.Filled.School
        "Experience" -> Icons.Filled.WorkHistory
        else -> Icons.Filled.WorkspacePremium
    }
    val headerSubtitle = when (category) {
        "Education" -> "School, college, or university details"
        "Experience" -> "Role, company, and the years you worked there"
        else -> "Certificate name, issuer, and an optional file"
    }

    val certificatePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val mimeType = context.contentResolver.getType(uri).orEmpty()
        val extension = when {
            mimeType.contains("pdf") -> "pdf"
            mimeType.contains("png") -> "png"
            mimeType.contains("webp") -> "webp"
            mimeType.contains("jpeg") || mimeType.contains("jpg") -> "jpg"
            else -> null
        }
        if (extension == null) {
            fileError = "Please upload a PDF or image file."
            return@rememberLauncherForActivityResult
        }
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        if (bytes == null) {
            fileError = "Couldn't read the selected file."
            return@rememberLauncherForActivityResult
        }
        fileError = null
        onUploadCertificate(bytes, extension) { url ->
            if (url != null) {
                fileUrl = url
                fileName = uri.lastPathSegment?.substringAfterLast("/") ?: "certificate.$extension"
            } else {
                fileError = "Failed to upload certificate. Please try again."
            }
        }
    }

    // "Present" means still ongoing, so it's always >= any start year. Otherwise
    // compare the numeric years and reject an end year earlier than the start year.
    fun isYearRangeValid(start: String, end: String): Boolean {
        if (end == "Present") return true
        val startNum = start.toIntOrNull() ?: return true
        val endNum = end.toIntOrNull() ?: return true
        return endNum >= startNum
    }

    fun trySave() {
        when (category) {
            "Experience" -> {
                if (title.trim().isBlank()) {
                    titleError = "Job title is required."
                    return
                }
                if (subtitle.trim().isBlank()) {
                    subtitleError = "Company is required."
                    return
                }
                if (!isYearRangeValid(startYear, endYear)) {
                    yearRangeError = "End year cannot be earlier than start year."
                    return
                }
                yearRangeError = null
                onSave(
                    ProfileEntry(
                        title = title.trim(),
                        subtitle = subtitle.trim(),
                        period = "$startYear - $endYear · $employmentType",
                        description = description.trim()
                    )
                )
            }
            "Education" -> {
                if (subtitle.trim().isBlank()) {
                    subtitleError = "Institution is required."
                    return
                }
                if (!isYearRangeValid(startYear, endYear)) {
                    yearRangeError = "End year cannot be earlier than start year."
                    return
                }
                yearRangeError = null
                onSave(
                    ProfileEntry(
                        title = educationLevel,
                        subtitle = subtitle.trim(),
                        period = "$startYear - $endYear",
                        description = description.trim()
                    )
                )
            }
            else -> {
                if (title.trim().isBlank()) {
                    titleError = "Certification name is required."
                    return
                }
                val issuerName = if (issuer == "Other") customIssuer.trim().ifBlank { "Other" } else issuer
                onSave(
                    ProfileEntry(
                        title = title.trim(),
                        subtitle = issuerName,
                        period = year,
                        fileUrl = fileUrl
                    )
                )
            }
        }
    }

    Dialog(
        onDismissRequest = { if (!isUploadingFile) onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !isUploadingFile,
            dismissOnClickOutside = false
        )
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = BackgroundWhite,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("Add $category", fontWeight = FontWeight.Bold, color = DeepGreenDark, fontSize = 18.sp)
                            Text(headerSubtitle, fontSize = 12.sp, color = TextDark.copy(alpha = 0.55f))
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss, enabled = !isUploadingFile) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = DeepGreenDark)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = SageGreenMain)
                )
            },
            bottomBar = {
                Surface(color = Color.White, shadowElevation = 12.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            enabled = !isUploadingFile,
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Cancel", fontWeight = FontWeight.Medium)
                        }
                        Button(
                            onClick = { trySave() },
                            enabled = !isUploadingFile,
                            modifier = Modifier.weight(1.3f).height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DeepGreenDark)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = SageGreenLight.copy(alpha = 0.45f)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(headerIcon, contentDescription = null, tint = DeepGreenDark, modifier = Modifier.size(24.dp))
                        }
                        Column {
                            Text("New $category", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextDark)
                            Text(headerSubtitle, fontSize = 12.sp, color = TextDark.copy(alpha = 0.6f), lineHeight = 16.sp)
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        when (category) {
                            "Experience" -> {
                                OutlinedTextField(
                                    value = title,
                                    onValueChange = { title = it.take(100); titleError = null },
                                    label = { Text("Job title") },
                                    placeholder = { Text("e.g. Marketing Intern") },
                                    singleLine = true,
                                    isError = titleError != null,
                                    supportingText = { titleError?.let { Text(it, color = Color.Red, fontSize = 12.sp) } },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                OutlinedTextField(
                                    value = subtitle,
                                    onValueChange = { subtitle = it.take(100); subtitleError = null },
                                    label = { Text("Company") },
                                    placeholder = { Text("e.g. JobTown Sdn Bhd") },
                                    singleLine = true,
                                    isError = subtitleError != null,
                                    supportingText = { subtitleError?.let { Text(it, color = Color.Red, fontSize = 12.sp) } },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                QualDropdownField(label = "Employment type", value = employmentType, options = ProfileOptions.EMPLOYMENT_TYPES, onSelect = { employmentType = it })
                                QualDropdownField(label = "Start year", value = startYear, options = ProfileOptions.YEARS.filter { it != "Present" }, onSelect = { startYear = it; yearRangeError = null })
                                QualDropdownField(label = "End year", value = endYear, options = ProfileOptions.YEARS, onSelect = { endYear = it; yearRangeError = null }, errorText = yearRangeError)
                                OutlinedTextField(
                                    value = description,
                                    onValueChange = { description = it.take(300) },
                                    label = { Text("Description (optional)") },
                                    placeholder = { Text("What did you work on?") },
                                    modifier = Modifier.fillMaxWidth().height(110.dp),
                                    shape = RoundedCornerShape(14.dp)
                                )
                            }
                            "Education" -> {
                                QualDropdownField(
                                    label = "Qualification",
                                    value = educationLevel,
                                    options = ProfileOptions.EDUCATION_LEVELS,
                                    onSelect = { educationLevel = it }
                                )
                                OutlinedTextField(
                                    value = subtitle,
                                    onValueChange = { subtitle = it.take(100); subtitleError = null },
                                    label = { Text("Institution") },
                                    placeholder = { Text("e.g. Universiti Malaya") },
                                    singleLine = true,
                                    isError = subtitleError != null,
                                    supportingText = { subtitleError?.let { Text(it, color = Color.Red, fontSize = 12.sp) } },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                QualDropdownField(label = "Start year", value = startYear, options = ProfileOptions.YEARS.filter { it != "Present" }, onSelect = { startYear = it; yearRangeError = null })
                                QualDropdownField(label = "End year", value = endYear, options = ProfileOptions.YEARS, onSelect = { endYear = it; yearRangeError = null }, errorText = yearRangeError)
                                OutlinedTextField(
                                    value = description,
                                    onValueChange = { description = it.take(300) },
                                    label = { Text("Field of study (optional)") },
                                    placeholder = { Text("e.g. Computer Science") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp)
                                )
                            }
                            else -> {
                                OutlinedTextField(
                                    value = title,
                                    onValueChange = { title = it.take(100); titleError = null },
                                    label = { Text("Certification name") },
                                    placeholder = { Text("e.g. Google UX Design") },
                                    singleLine = true,
                                    isError = titleError != null,
                                    supportingText = { titleError?.let { Text(it, color = Color.Red, fontSize = 12.sp) } },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                QualDropdownField(
                                    label = "Issued by",
                                    value = issuer,
                                    options = ProfileOptions.CERTIFICATE_ISSUERS,
                                    onSelect = { issuer = it }
                                )
                                if (issuer == "Other") {
                                    OutlinedTextField(
                                        value = customIssuer,
                                        onValueChange = { customIssuer = it.take(100) },
                                        label = { Text("Issuer name") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                }
                                QualDropdownField(
                                    label = "Year",
                                    value = year,
                                    options = ProfileOptions.YEARS.filter { it != "Present" },
                                    onSelect = { year = it }
                                )
                                OutlinedButton(
                                    onClick = { certificatePicker.launch(arrayOf("application/pdf", "image/*")) },
                                    enabled = !isUploadingFile,
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    if (isUploadingFile) {
                                        CircularProgressIndicator(color = DeepGreenDark, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Uploading…")
                                    } else {
                                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(if (fileUrl.isBlank()) "Upload certificate file (optional)" else "Replace file")
                                    }
                                }
                                if (fileName.isNotBlank()) {
                                    Text(fileName, fontSize = 12.sp, color = DeepGreenDark)
                                }
                                fileError?.let { Text(it, color = Color.Red, fontSize = 12.sp) }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QualDropdownField(
    label: String,
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    errorText: String? = null
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            isError = errorText != null,
            supportingText = errorText?.let { { Text(it, color = Color.Red, fontSize = 12.sp) } },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(option) }, onClick = { onSelect(option); expanded = false })
            }
        }
    }
}
