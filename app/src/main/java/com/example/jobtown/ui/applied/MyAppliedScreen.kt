package com.example.jobtown.ui.applied

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.jobtown.data.model.JobApplication
import com.example.jobtown.data.model.User
import com.example.jobtown.data.repository.UserRepository
import com.example.jobtown.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyAppliedScreen(
    viewModel: AppliedViewModel,
    navController: NavController,
    user: User?,
    isLoading: Boolean,
    onApplicationClick: (String) -> Unit = {},
    chatLoadingApplicationId: String? = null,
    isTrackingLive: Boolean = false,
    recentlyUpdatedApplicationId: String? = null,
    onStartTracking: (String) -> Unit = {},
    onStopTracking: () -> Unit = {},
    onConsumeRecentUpdate: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onChatWithCompany: (JobApplication) -> Unit
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val applications = viewModel.getFilteredApplications(selectedTab)

    val activeCount = viewModel.getFilteredApplications(ApplicationTab.ACTIVE).size
    val closedCount = viewModel.getFilteredApplications(ApplicationTab.CLOSED).size

    LaunchedEffect(user?.id) {
        user?.id?.let { userId ->
            viewModel.loadApplications(userId)
            if (!isTrackingLive) {
                onStartTracking(userId)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "My Applications",
                        fontWeight = FontWeight.Bold,
                        color = DeepGreenDark,
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
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = DeepGreenDark
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SageGreenMain)
            )
        },
        containerColor = BackgroundWhite
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = SageGreenMain.copy(alpha = 0.15f),
                contentColor = DeepGreenDark,
                modifier = Modifier.fillMaxWidth()
            ) {
                ApplicationTab.values().forEach { tab ->
                    val count = when (tab) {
                        ApplicationTab.ACTIVE -> activeCount
                        ApplicationTab.CLOSED -> closedCount
                    }
                    val tabLabel = when (tab) {
                        ApplicationTab.ACTIVE -> "Active"
                        ApplicationTab.CLOSED -> "Closed"
                    }

                    Tab(
                        selected = selectedTab == tab,
                        onClick = { viewModel.switchTab(tab) },
                        text = {
                            Text(
                                text = "$tabLabel ($count)",
                                fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        },
                        selectedContentColor = DeepGreenDark,
                        unselectedContentColor = TextDark.copy(alpha = 0.6f)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when {
                    isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = DeepGreenDark
                        )
                    }
                    applications.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Business,
                                    contentDescription = null,
                                    tint = SageGreenDark,
                                    modifier = Modifier.size(54.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No ${selectedTab.name.lowercase()} jobs found",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = TextDark
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = when (selectedTab) {
                                        ApplicationTab.ACTIVE -> "Explore available listings and send your applications to track them here."
                                        ApplicationTab.CLOSED -> "Applications that have closed, expired, or been rejected will appear here."
                                    },
                                    fontSize = 13.sp,
                                    color = TextDark.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(applications, key = { it.id.ifBlank { it.jobTitle + it.companyName } }) { application ->
                                val isRecentlyUpdated = recentlyUpdatedApplicationId == application.id

                                ApplicationCard(
                                    application = application,
                                    isSavedTab = false,
                                    isChatLoading = chatLoadingApplicationId == application.id,
                                    isHighlighted = isRecentlyUpdated,
                                    onCardClick = { onApplicationClick(application.id) },
                                    onChatWithCompany = { onChatWithCompany(application) },
                                    onApplyClick = {
                                        viewModel.updateApplicationStatus(application.id, "applied") { success ->
                                            if (success) {
                                                // Handle success if needed
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ApplicationCard(
    application: JobApplication,
    isSavedTab: Boolean,
    isChatLoading: Boolean = false,
    isHighlighted: Boolean = false,
    onCardClick: () -> Unit = {},
    onChatWithCompany: () -> Unit,
    onApplyClick: () -> Unit
) {
    var employerAvatarUrl by remember { mutableStateOf<String?>(null) }
    var jobCompanyImageUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(application.employerId, application.jobId) {
        val employerId = application.employerId
        if (!employerId.isNullOrBlank()) {
            val employerUser = UserRepository.fetchUserById(employerId)
            employerAvatarUrl = employerUser?.avatarUrl
        }

        val jobId = application.jobId
        if (!jobId.isNullOrBlank()) {
            val jobs = UserRepository.fetchAllJobs()
            val matchedJob = jobs.find { it.id == jobId }
            jobCompanyImageUrl = matchedJob?.companyImageUrl
        }
    }

    val activePhotoUrl = jobCompanyImageUrl?.takeIf { it.isNotBlank() }
        ?: employerAvatarUrl?.takeIf { it.isNotBlank() }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlighted) SageGreenMain.copy(alpha = 0.2f) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = SageGreenMain.copy(alpha = 0.3f),
                        modifier = Modifier.size(48.dp)
                    ) {
                        if (!activePhotoUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = activePhotoUrl,
                                contentDescription = "Employer Photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        } else {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Business,
                                    contentDescription = null,
                                    tint = DeepGreenDark,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = application.jobTitle,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextDark
                        )
                        Text(
                            text = application.companyName,
                            fontSize = 13.sp,
                            color = TextDark.copy(alpha = 0.6f)
                        )
                    }
                }

                if (!isSavedTab) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SageGreenMain.copy(alpha = 0.4f)
                    ) {
                        Text(
                            text = application.status.ifBlank { "Pending" },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = DeepGreenDark,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = TextDark.copy(alpha = 0.5f),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = application.location.ifBlank { "Remote" },
                    fontSize = 12.sp,
                    color = TextDark.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isSavedTab) {
                Button(
                    onClick = onApplyClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DeepGreenDark)
                ) {
                    Text(
                        text = "Apply Now",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            } else {
                Button(
                    onClick = onChatWithCompany,
                    enabled = !isChatLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DeepGreenDark,
                        disabledContainerColor = DeepGreenDark.copy(alpha = 0.6f)
                    )
                ) {
                    if (isChatLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Opening chat...",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Chat with Employer",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}