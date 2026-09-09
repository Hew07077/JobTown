package com.example.jobtown.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.jobtown.Screen
import com.example.jobtown.data.model.User
import com.example.jobtown.data.model.UserRole
import com.example.jobtown.ui.theme.*

data class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val badgeCount: Int = 0,
    val showDot: Boolean = false
)

@Composable
fun JobTownBottomNavigationBar(
    navController: NavController,
    currentUser: User?,
    unreadChatCount: Int = 0,
    hasScheduleAlert: Boolean = false,
    hasApplicationAlert: Boolean = false
) {
    val isEmployer = currentUser?.role == UserRole.EMPLOYER
    val applicationsTabRoute = if (isEmployer) Screen.ManageJobs.route else Screen.Applied.route

    val items = listOf(
        BottomNavItem(
            route = Screen.Home.route,
            title = "Home",
            icon = Icons.Default.Home
        ),
        BottomNavItem(
            route = applicationsTabRoute,
            title = if (isEmployer) "Manage" else "Applied",
            icon = Icons.Default.AssignmentTurnedIn,
            showDot = hasApplicationAlert
        ),
        BottomNavItem(
            route = Screen.Schedule.route,
            title = "Schedule",
            icon = Icons.Default.Event,
            showDot = hasScheduleAlert
        ),
        BottomNavItem(
            route = Screen.Chat.route,
            title = "Messages",
            icon = Icons.Default.Chat,
            badgeCount = unreadChatCount
        )
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

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
                // Check hierarchy to stay selected even if sub-routes or search params are active
                val isSelected = currentDestination?.hierarchy?.any {
                    it.route?.startsWith(item.route) == true
                } == true

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
                            } else if (item.showDot) {
                                // Plain red dot (no count) — signals there's something
                                // new to look at without implying an exact number.
                                BadgedBox(
                                    badge = {
                                        Badge(containerColor = Color(0xFFD32F2F))
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
                        if (!isSelected) {
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