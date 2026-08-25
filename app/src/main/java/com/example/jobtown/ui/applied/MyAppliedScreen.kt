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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.jobtown.data.JobApplication
import com.example.jobtown.data.User
import com.example.jobtown.ui.theme.BackgroundWhite
import com.example.jobtown.ui.theme.DeepGreenDark
import com.example.jobtown.ui.theme.SageGreenDark
import com.example.jobtown.ui.theme.SageGreenLight
import com.example.jobtown.ui.theme.SageGreenMain
import com.example.jobtown.ui.theme.TextDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyAppliedScreen(
    navController: NavController,
    user: User?,
    applications: List<JobApplication>,
    isLoading: Boolean,
    onRefresh: () -> Unit,
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
    LaunchedEffect(user?.id) {
        user?.id?.let { userId ->
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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
                                text = "No Job Applications Found",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = TextDark
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Explore available listings and send your applications to track them here.",
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
                                isChatLoading = chatLoadingApplicationId == application.id,
                                isHighlighted = isRecentlyUpdated,
                                onCardClick = { onApplicationClick(application.id) },
                                onChatWithCompany = { onChatWithCompany(application) }
                            )
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
    isChatLoading: Boolean = false,
    isHighlighted: Boolean = false,
    onCardClick: () -> Unit = {},
    onChatWithCompany: () -> Unit
) {
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
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Business,
                                contentDescription = null,
                                tint = DeepGreenDark,
                                modifier = Modifier.size(20.dp)
                            )
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