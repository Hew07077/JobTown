package com.example.jobtown.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.jobtown.Screen
import com.example.jobtown.ui.theme.DeepGreenDark
import com.example.jobtown.ui.theme.SageGreenMain

sealed class BottomNavItem(val route: String, val title: String, val icon: ImageVector) {
    object Home : BottomNavItem(Screen.Home.route, "Home", Icons.Default.Home)
    object Chat : BottomNavItem(Screen.Chat.route, "Chat", Icons.Default.Chat)
    object Applied : BottomNavItem(Screen.Applied.route, "Applied", Icons.Default.Work)
    object Schedule : BottomNavItem(Screen.Schedule.route, "Schedule", Icons.Default.CalendarToday)
}

@Composable
fun BottomNavigationBar(navController: NavController) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Chat,
        BottomNavItem.Applied,
        BottomNavItem.Schedule
    )

    NavigationBar(
        containerColor = androidx.compose.ui.graphics.Color.White,
        contentColor = DeepGreenDark
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.title) },
                label = { Text(item.title) },
                selected = currentRoute == item.route,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            popUpTo(Screen.Home.route) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = DeepGreenDark,
                    selectedTextColor = DeepGreenDark,
                    indicatorColor = SageGreenMain,
                    unselectedIconColor = androidx.compose.ui.graphics.Color.Gray,
                    unselectedTextColor = androidx.compose.ui.graphics.Color.Gray
                )
            )
        }
    }
}