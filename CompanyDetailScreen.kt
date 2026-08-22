package com.example.jobtown.ui.employer

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.jobtown.data.Job
import com.example.jobtown.data.User
import com.example.jobtown.data.repository.UserRepository
import com.example.jobtown.ui.theme.*

data class CompanyProfile(
    val id: String = "",
    val name: String = "",
    val logoUrl: String = "",
    val tagline: String = "",
    val industry: String = "",
    val location: String = "",
    val companySize: String = "",
    val websiteUrl: String = "",
    val description: String = "",
    val perks: List<String> = emptyList(),
    // True only when this profile was actually loaded from an employer
    // account in Supabase -- never fabricated.
    val isVerified: Boolean = false
)

// Builds a real CompanyProfile from an employer's "users" row.
private fun User.toCompanyProfile(fallbackId: String): CompanyProfile = CompanyProfile(
    id = id.ifBlank { fallbackId },
    name = companyName.ifBlank { name }.ifBlank { fallbackId },
    logoUrl = avatarUrl,
    tagline = tagline,
    industry = industry,
    location = location,
    companySize = companySize,
    websiteUrl = websiteUrl,
    description = bio,
    perks = perks,
    isVerified = true
)

// Fallback when there's no employer account to load (e.g. legacy jobs
// posted without an employer_id) -- built from whatever real job data
// is already on hand, never invented.
private fun companyProfileFromJobs(companyIdOrName: String, jobs: List<Job>): CompanyProfile {
    val match = jobs.firstOrNull {
        it.employerId == companyIdOrName || it.companyName.equals(companyIdOrName, ignoreCase = true)
    }
    return CompanyProfile(
        id = companyIdOrName,
        name = match?.companyName?.ifBlank { companyIdOrName } ?: companyIdOrName,
        logoUrl = match?.companyImageUrl.orEmpty(),
        location = match?.location.orEmpty(),
        isVerified = false
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CompanyDetailScreen(
    navController: NavController,
    companyIdOrName: String,
    openJobs: List<Job> = emptyList(),
    onJobClick: (Job) -> Unit = {}
) {
    val context = LocalContext.current
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    // Real company data, loaded from the employer's account in Supabase.
    // Renamed from 'company' to 'companyProfileState' to prevent conflicts.
    var companyProfileState by remember(companyIdOrName) { mutableStateOf<CompanyProfile?>(null) }

    LaunchedEffect(companyIdOrName) {
        companyProfileState = null
        // If your repository uses fetchUserById instead, change getUserById back to fetchUserById
        val employer = UserRepository.fetchUserById(companyIdOrName)
        companyProfileState = employer?.toCompanyProfile(companyIdOrName)
            ?: companyProfileFromJobs(companyIdOrName, openJobs)
    }

    val loadedCompany = companyProfileState

    // Filter active jobs posted by this company
    val companyJobs = remember(openJobs, loadedCompany) {
        if (loadedCompany == null) {
            emptyList()
        } else {
            openJobs.filter {
                it.employerId == loadedCompany.id ||
                        it.companyName.equals(loadedCompany.name, ignoreCase = true)
            }
        }
    }

    if (loadedCompany == null) {
        Scaffold(
            containerColor = BackgroundWhite,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Company Profile",
                            fontWeight = FontWeight.Bold,
                            color = DeepGreenDark,
                            fontSize = 18.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = DeepGreenDark
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = SageGreenMain)
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = DeepGreenDark)
            }
        }
        return
    }

    val company = loadedCompany

    Scaffold(
        containerColor = BackgroundWhite,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Company Profile",
                        fontWeight = FontWeight.Bold,
                        color = DeepGreenDark,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = DeepGreenDark
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SageGreenMain)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Header Banner & Profile Info
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(SageGreenMain, SageGreenLight)
                            )
                        )
                )
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .offset(y = (-40).dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = DeepGreenDark,
                            border = androidx.compose.foundation.BorderStroke(4.dp, Color.White),
                            modifier = Modifier.size(80.dp),
                            shadowElevation = 4.dp
                        ) {
                            if (company.logoUrl.isNotBlank()) {
                                AsyncImage(
                                    model = company.logoUrl,
                                    contentDescription = "Company Logo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.Business,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                            }
                        }

                        if (company.websiteUrl.isNotBlank()) {
                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(company.websiteUrl))
                                    context.startActivity(intent)
                                },
                                shape = RoundedCornerShape(20.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, DeepGreenDark)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Language,
                                    contentDescription = null,
                                    tint = DeepGreenDark,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Website", fontSize = 12.sp, color = DeepGreenDark, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = company.name,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        if (company.isVerified) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Filled.Verified,
                                contentDescription = "Verified Company",
                                tint = DeepGreenDark,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    if (company.tagline.isNotBlank()) {
                        Text(
                            text = company.tagline,
                            fontSize = 13.sp,
                            color = TextDark.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (company.industry.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Work, contentDescription = null, tint = SageGreenDark, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(company.industry, fontSize = 12.sp, color = TextDark.copy(alpha = 0.8f))
                            }
                        }
                        if (company.location.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.LocationOn, contentDescription = null, tint = SageGreenDark, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(company.location, fontSize = 12.sp, color = TextDark.copy(alpha = 0.8f))
                            }
                        }
                    }
                }
            }

            // Tab Navigation Header
            item {
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color.White,
                    contentColor = DeepGreenDark,
                    modifier = Modifier
                        .offset(y = (-20).dp)
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = { Text("About", fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = {
                            Text(
                                text = "Open Roles (${companyJobs.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Tab 1: About Tab Content
            if (selectedTabIndex == 0) {
                item {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Quick Stats Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                CompanyStatItem(
                                    title = "Company Size",
                                    value = company.companySize.ifBlank { "Not specified" },
                                    icon = Icons.Filled.People
                                )
                                CompanyStatItem(
                                    title = "Open Roles",
                                    value = companyJobs.size.toString(),
                                    icon = Icons.Filled.WorkOutline
                                )
                            }
                        }

                        // Description Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "About Company",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = company.description.ifBlank { "This company hasn't added a description yet." },
                                    fontSize = 13.sp,
                                    color = TextDark.copy(alpha = 0.8f),
                                    lineHeight = 20.sp
                                )
                            }
                        }

                        // Perks & Benefits Card
                        if (company.perks.isNotEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Perks & Benefits",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextDark
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        company.perks.forEach { perk ->
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = SageGreenLight
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Check,
                                                        contentDescription = null,
                                                        tint = DeepGreenDark,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = perk,
                                                        fontSize = 12.sp,
                                                        color = DeepGreenDark,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Tab 2: Open Roles Content
            if (selectedTabIndex == 1) {
                if (companyJobs.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.WorkOutline,
                                    contentDescription = null,
                                    tint = SageGreenDark,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No Active Openings",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "This company currently has no open job listings.",
                                    fontSize = 13.sp,
                                    color = TextDark.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                } else {
                    items(companyJobs) { job ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                            CompanyJobCard(job = job, onClick = { onJobClick(job) })
                        }
                    }
                }
            }
        }
    }
}

// ==================== Helper Components ====================

@Composable
private fun CompanyStatItem(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
            shape = CircleShape,
            color = SageGreenLight,
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(imageVector = icon, contentDescription = null, tint = DeepGreenDark, modifier = Modifier.size(18.dp))
            }
        }
        Column {
            Text(text = title, fontSize = 11.sp, color = TextDark.copy(alpha = 0.6f))
            Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextDark)
        }
    }
}

@Composable
private fun CompanyJobCard(job: Job, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = job.title.ifBlank { "Untitled Position" },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = SageGreenDark,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = job.location.ifBlank { "Remote" },
                    fontSize = 12.sp,
                    color = TextDark.copy(alpha = 0.7f)
                )
                Text(
                    text = "•",
                    fontSize = 12.sp,
                    color = TextDark.copy(alpha = 0.4f)
                )
                Text(
                    text = job.jobType.ifBlank { "Full-Time" },
                    fontSize = 12.sp,
                    color = TextDark.copy(alpha = 0.7f)
                )
            }

            if (job.salary.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = DeepGreenDark.copy(alpha = 0.08f)
                ) {
                    Text(
                        text = job.salary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepGreenDark
                    )
                }
            }
        }
    }
}