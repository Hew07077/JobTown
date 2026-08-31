package com.example.jobtown.ui.postjob

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.jobtown.data.model.Job
import com.example.jobtown.data.repository.UserRepository
import com.example.jobtown.ui.theme.BackgroundWhite
import com.example.jobtown.ui.theme.DeepGreenDark
import com.example.jobtown.ui.theme.SageGreenDark
import com.example.jobtown.ui.theme.SageGreenLight
import com.example.jobtown.ui.theme.SageGreenMain
import com.example.jobtown.ui.theme.TextDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployerJobDetailScreen(
    job: Job,
    navController: NavController? = null,
    avatarUrl: String? = null,
    onUpdateJob: (Job) -> Unit = {},
    onBackClick: () -> Unit = { navController?.popBackStack() }
) {
    var isEditing by remember { mutableStateOf(false) }
    var fetchedAvatarUrl by remember { mutableStateOf<String?>(null) }

    val (initialMinSalary, initialMaxSalary) = remember(job.salary) { parseSalaryBounds(job.salary) }

    val fields = rememberJobFormFields(
        title = job.title,
        company = job.companyName.ifBlank { "Company Name" },
        location = job.location,
        minSalary = initialMinSalary,
        maxSalary = initialMaxSalary,
        type = job.jobType.ifBlank { "Full-time" },
        description = job.description,
        requirements = job.requirements?.filter { it.isNotBlank() }?.joinToString(", ").orEmpty(),
        skills = job.skills?.filter { it.isNotBlank() }?.joinToString(", ").orEmpty()
    )

    LaunchedEffect(job.employerId) {
        if (!job.employerId.isNullOrBlank()) {
            val employerUser = UserRepository.fetchUserById(job.employerId)
            fetchedAvatarUrl = employerUser?.avatarUrl
        }
    }

    val displayAvatarUrl = remember(avatarUrl, job.companyImageUrl, fetchedAvatarUrl, fields.company) {
        avatarUrl?.takeIf { it.isNotBlank() }
            ?: job.companyImageUrl?.takeIf { it.isNotBlank() }
            ?: fetchedAvatarUrl?.takeIf { it.isNotBlank() }
            ?: run {
                val safeCompany = fields.company.ifBlank { "Company" }.trim()
                "https://ui-avatars.com/api/?name=${java.net.URLEncoder.encode(safeCompany, "UTF-8")}&background=E8F5E9&color=1B5E20&bold=true"
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = if (isEditing) "Edit Job Listing" else "Job Details", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = DeepGreenDark)
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = DeepGreenDark
                        )
                    }
                },
                actions = {
                    // FIX: previously `{ isEditing = !isEditing }` — tapping this
                    // while editing showed a Save icon but just discarded the
                    // draft. Now it actually saves; it only exits edit mode if
                    // validation passes.
                    IconButton(onClick = { if (isEditing) trySave() else isEditing = true }) {
                        Icon(
                            imageVector = if (isEditing) Icons.Default.Close else Icons.Default.Edit,
                            contentDescription = if (isEditing) "Cancel editing" else "Edit listing",
                            tint = DeepGreenDark
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SageGreenMain)
            )
        },
        containerColor = BackgroundWhite
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            if (isEditing) {
                JobListingForm(
                    fields = fields,
                    submitLabel = "Save changes",
                    companyPhotoUrl = displayAvatarUrl,
                    requireSalary = false,
                    onSubmit = {
                        val salary = fields.formattedSalary(blankFallback = job.salary)
                        onUpdateJob(
                            job.copy(
                                title = fields.title.trim(),
                                company = fields.company.trim(),
                                location = fields.location.trim(),
                                salary = salary,
                                salaryRange = salary,
                                type = fields.type,
                                description = fields.description.trim(),
                                requirements = fields.requirementsList(),
                                skills = fields.skillsList()
                            )
                        )
                        isEditing = false
                    }
                )
            } else {
                JobDetailsReadView(
                    job = job,
                    displayAvatarUrl = displayAvatarUrl
                )
            }
        }
    }
}

@Composable
private fun JobDetailsReadView(
    job: Job,
    displayAvatarUrl: String
) {
    val filteredRequirements = job.requirements
        ?.flatMap { it.split(",") }
        ?.map { it.trim() }
        ?.filter { it.isNotBlank() }
        .orEmpty()
    val filteredSkills = job.skills
        ?.flatMap { it.split(",") }
        ?.map { it.trim() }
        ?.filter { it.isNotBlank() }
        .orEmpty()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 22.dp, vertical = 24.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = SageGreenLight,
                    modifier = Modifier.size(56.dp),
                    border = BorderStroke(1.dp, SageGreenDark.copy(alpha = 0.25f))
                ) {
                    AsyncImage(
                        model = displayAvatarUrl,
                        contentDescription = "Company photo",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = job.companyName.ifBlank { "Company Name" }, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = SageGreenDark)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = job.title.ifBlank { "Untitled Position" }, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextDark, lineHeight = 28.sp)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 18.dp), thickness = 1.dp, color = Color(0xFFE6EDE4))
            JobDetailMetaRow(icon = Icons.Filled.LocationOn, label = "Location", value = job.location.ifBlank { "Location undisclosed" })

            HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), thickness = 1.dp, color = Color(0xFFE6EDE4))
            JobDetailMetaRow(icon = Icons.Filled.Work, label = "Job type", value = job.jobType.ifBlank { "Full-time" })

            HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), thickness = 1.dp, color = Color(0xFFE6EDE4))
            JobDetailMetaRow(icon = Icons.Filled.AttachMoney, label = "Salary", value = job.salary.ifBlank { "Salary not specified" })

            HorizontalDivider(modifier = Modifier.padding(vertical = 18.dp), thickness = 1.dp, color = Color(0xFFE6EDE4))
            Text(text = "Job description", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = DeepGreenDark)

            Spacer(modifier = Modifier.height(10.dp))
            Text(text = job.description.ifBlank { "No detailed description available for this role." }, fontSize = 14.sp, color = TextDark.copy(alpha = 0.78f), lineHeight = 22.sp)

            if (filteredRequirements.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 18.dp), thickness = 1.dp, color = Color(0xFFE6EDE4))
                Text(
                    text = "Requirements",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DeepGreenDark
                )
                Spacer(modifier = Modifier.height(10.dp))
                filteredRequirements.forEach { req ->
                    RequirementRow(text = req)
                }
            }

            if (filteredSkills.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 18.dp), thickness = 1.dp, color = Color(0xFFE6EDE4))
                Text(
                    text = "Required skills",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DeepGreenDark
                )
                Spacer(modifier = Modifier.height(12.dp))
                SkillChips(skills = filteredSkills)
            }
        }
    }
}

@Composable
private fun JobDetailMetaRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = SageGreenLight,
            modifier = Modifier.size(38.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = SageGreenDark,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = TextDark.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
        }
    }
}

@Composable
private fun RequirementRow(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(text = "•", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SageGreenDark)
        Text(text = text, fontSize = 14.sp, color = TextDark.copy(alpha = 0.78f), lineHeight = 21.sp)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SkillChips(skills: List<String>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        skills.forEach { skill ->
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = SageGreenLight
            ) {
                Text(text = skill, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = DeepGreenDark, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
            }
        }
    }
}
