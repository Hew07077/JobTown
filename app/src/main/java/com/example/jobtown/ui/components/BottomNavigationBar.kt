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
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.jobtown.Screen
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