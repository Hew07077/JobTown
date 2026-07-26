package com.example.jobtown.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.jobtown.Screen
import com.example.jobtown.data.User
import com.example.jobtown.data.UserRole

open class BottomNavItem(val route: String, val title: String, val icon: ImageVector) {
    object Home : BottomNavItem(Screen.Home.route, "Home", Icons.Default.Home)
    object Applied : BottomNavItem(Screen.Applied.route, "Applied", Icons.Default.AssignmentTurnedIn)
    object Schedule : BottomNavItem(Screen.Schedule.route, "Schedule", Icons.Default.Event)
    object Chat : BottomNavItem(Screen.Chat.route, "Messages", Icons.Default.Chat)
}

@Composable
fun JobTownBottomNavigationBar(
    navController: NavController,
    currentUser: User?
) {
    val isEmployer = currentUser?.role == UserRole.EMPLOYER

    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem(
            route = Screen.Applied.route,
            title = if (isEmployer) "Manage Jobs" else "Applied",
            icon = Icons.Default.AssignmentTurnedIn
        ),
        BottomNavItem.Schedule,
        BottomNavItem.Chat
    )

    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry.value?.destination?.route

    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.title) },
                label = { Text(item.title) },
                selected = currentRoute == item.route,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    }
}