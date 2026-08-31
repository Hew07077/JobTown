package com.example.jobtown.ui.applied

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.jobtown.data.model.JobApplication
import com.example.jobtown.data.model.User
import com.example.jobtown.data.repository.UserRepository
import com.example.jobtown.ui.theme.BackgroundWhite
import com.example.jobtown.ui.theme.DeepGreenDark
import com.example.jobtown.ui.theme.SageGreenDark
import com.example.jobtown.ui.theme.SageGreenLight
import com.example.jobtown.ui.theme.SageGreenMain
import com.example.jobtown.ui.theme.TextDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyAppliedScreen(
    viewModel: AppliedViewModel,
    user: User?,
    isLoading: Boolean,
    onApplicationClick: (String) -> Unit = {},
    chatLoadingApplicationId: String? = null,
    isTrackingLive: Boolean = false,
    recentlyUpdatedApplicationId: String? = null,
    onStartTracking: (String) -> Unit = {},
    onProfileClick: () -> Unit = {},
    onChatWithCompany: (JobApplication) -> Unit
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val applicationsList by viewModel.applicationsListState.collectAsState()
    val applications = remember(applicationsList, selectedTab) {
        viewModel.getFilteredApplications(selectedTab)
    }

    val activeCount = remember(applicationsList) {
        viewModel.getFilteredApplications(ApplicationTab.ACTIVE).size
    }
    val closedCount = remember(applicationsList) {
        viewModel.getFilteredApplications(ApplicationTab.CLOSED).size
    }

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
                containerColor = Color.White,
                contentColor = DeepGreenDark,
                divider = {
                    HorizontalDivider(thickness = 1.dp, color = AppliedDividerColor)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                ApplicationTab.entries.forEach { tab ->
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
                                fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        },
                        selectedContentColor = DeepGreenDark,
                        unselectedContentColor = TextDark.copy(alpha = 0.55f)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when {
                    isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = DeepGreenDark
                        )
                    }
                    applications.isEmpty() -> {
                        EmptyApplicationsState(selectedTab = selectedTab)
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(
                                applications,
                                key = { it.id.ifBlank { it.jobTitle + it.companyName } }
                            ) { application ->
                                ApplicationCard(
                                    application = application,
                                    isSavedTab = false,
                                    isChatLoading = chatLoadingApplicationId == application.id,
                                    isHighlighted = recentlyUpdatedApplicationId == application.id,
                                    onCardClick = { onApplicationClick(application.id) },
                                    onChatWithCompany = { onChatWithCompany(application) },
                                    onApplyClick = {
                                        viewModel.updateApplicationStatus(application.id, "applied")
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
private fun EmptyApplicationsState(selectedTab: ApplicationTab) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = SageGreenLight,
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = Icons.Default.Business,
                    contentDescription = null,
                    tint = SageGreenDark,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = when (selectedTab) {
                ApplicationTab.ACTIVE -> "No active applications"
                ApplicationTab.CLOSED -> "No closed applications"
            },
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            color = TextDark
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = when (selectedTab) {
                ApplicationTab.ACTIVE -> "Jobs you apply for will show up here so you can track their status."
                ApplicationTab.CLOSED -> "Rejected or expired applications will appear in this list."
            },
            fontSize = 14.sp,
            color = TextDark.copy(alpha = 0.6f),
            lineHeight = 20.sp,
            textAlign = TextAlign.Center
        )
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
    val statusText = application.status.ifBlank { "Pending" }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlighted) SageGreenLight else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = SageGreenLight,
                    modifier = Modifier.size(52.dp)
                ) {
                    if (!activePhotoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = activePhotoUrl,
                            contentDescription = "Company photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = Icons.Default.Business,
                                contentDescription = null,
                                tint = DeepGreenDark,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = application.jobTitle.ifBlank { "Untitled role" },
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextDark,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 21.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = application.companyName.ifBlank { "Company" },
                        fontSize = 13.sp,
                        color = TextDark.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (!isSavedTab) {
                    ApplicationStatusBadge(statusText)
                }
            }

            AppliedDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MetaItem(
                    icon = Icons.Default.LocationOn,
                    text = application.location.ifBlank { "Remote" }
                )
                MetaItem(
                    icon = Icons.Default.CalendarToday,
                    text = application.appliedDate
                )
            }

            AppliedDivider()

            if (isSavedTab) {
                Button(
                    onClick = onApplyClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DeepGreenDark)
                ) {
                    Text(
                        text = "Apply now",
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
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
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
                            text = "Chat with employer",
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

@Composable
private fun MetaItem(
    icon: ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = SageGreenDark,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = text,
            fontSize = 13.sp,
            color = TextDark.copy(alpha = 0.6f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
