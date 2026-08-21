package com.example.jobtown.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.AsyncImage
import com.example.jobtown.Screen
import com.example.jobtown.data.Job
import com.example.jobtown.data.User
import com.example.jobtown.data.UserRole
import com.example.jobtown.ui.theme.*

data class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val badgeCount: Int = 0
)

@Composable
fun JobTownBottomNavigationBar(
    navController: NavController,
    currentUser: User?,
    unreadChatCount: Int = 0
) {
    val isEmployer = currentUser?.role == UserRole.EMPLOYER

    val items = listOf(
        BottomNavItem(
            route = Screen.Home.route,
            title = "Home",
            icon = Icons.Default.Home
        ),
        BottomNavItem(
            route = Screen.Applied.route,
            title = if (isEmployer) "Manage Jobs" else "Applied",
            icon = Icons.Default.AssignmentTurnedIn
        ),
        BottomNavItem(
            route = Screen.Schedule.route,
            title = "Schedule",
            icon = Icons.Default.Event
        ),
        BottomNavItem(
            route = Screen.Chat.route,
            title = "Messages",
            icon = Icons.Default.Chat,
            badgeCount = unreadChatCount
        )
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Surface(
        color = SageGreenMain,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        NavigationBar(
            containerColor = Color.Transparent,
            tonalElevation = 0.dp,
            modifier = Modifier.height(72.dp)
        ) {
            items.forEach { item ->
                val isSelected = currentRoute == item.route

                val iconScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.15f else 1.0f,
                    animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                    label = "iconScale"
                )

                val selectedBgColor by animateColorAsState(
                    targetValue = if (isSelected) SageGreenLight else Color.Transparent,
                    animationSpec = tween(durationMillis = 200),
                    label = "bgColor"
                )

                NavigationBarItem(
                    icon = {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .scale(iconScale)
                                .clip(CircleShape)
                        ) {
                            if (item.badgeCount > 0) {
                                BadgedBox(
                                    badge = {
                                        Badge(
                                            containerColor = Color(0xFFD32F2F),
                                            contentColor = Color.White
                                        ) {
                                            Text(
                                                text = if (item.badgeCount > 99) "99+" else item.badgeCount.toString(),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.title,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    },
                    label = {
                        Text(
                            text = item.title,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    selected = isSelected,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = DeepGreenDark,
                        selectedTextColor = DeepGreenDark,
                        indicatorColor = selectedBgColor,
                        unselectedIconColor = DeepGreenDark.copy(alpha = 0.5f),
                        unselectedTextColor = DeepGreenDark.copy(alpha = 0.5f)
                    ),
                    onClick = {
                        if (currentRoute != item.route) {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun JobCard(
    job: Job,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (job.isFeatured == true) SageGreenMain.copy(alpha = 0.25f) else Color.White
        ),
        border = BorderStroke(
            width = if (job.isFeatured == true) 1.dp else 0.5.dp,
            color = if (job.isFeatured == true) DeepGreenDark else Color(0xFFE0E0E0)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Company Avatar Container (Loads the logo image from job.companyImageUrl)
            Surface(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(6.dp)),
                color = SageGreenMain.copy(alpha = 0.3f),
                border = BorderStroke(0.5.dp, DeepGreenDark.copy(alpha = 0.2f))
            ) {
                if (!job.companyImageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = job.companyImageUrl,
                        contentDescription = "Company Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Business,
                            contentDescription = null,
                            tint = DeepGreenDark,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Job Details Column
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = job.title.ifBlank { "Job Title Placeholder" },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark,
                        modifier = Modifier.weight(1f)
                    )
                    if (job.isFeatured == true) {
                        Surface(
                            color = DeepGreenDark,
                            shape = RoundedCornerShape(3.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color.Yellow,
                                    modifier = Modifier.size(8.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "FEATURED",
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Company & Location Row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Business,
                        contentDescription = null,
                        tint = DeepGreenDark,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = job.companyName,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextDark.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = DeepGreenDark,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = job.location.ifBlank { "Location" },
                        fontSize = 10.sp,
                        color = TextDark.copy(alpha = 0.6f)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Job Type Tag
                Surface(
                    color = SageGreenMain.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(3.dp)
                ) {
                    Text(
                        text = job.jobType,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DeepGreenDark,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Salary Tag
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = DeepGreenDark.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(3.dp)
                    ) {
                        Text(
                            text = job.salary.ifBlank { "Negotiable" },
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepGreenDark,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }

                if (job.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = job.description,
                        fontSize = 10.sp,
                        color = TextDark.copy(alpha = 0.7f),
                        maxLines = 2
                    )
                }
            }
        }
    }
}