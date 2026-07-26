package com.example.jobtown

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.jobtown.data.Job
import com.example.jobtown.data.JobApplication
import com.example.jobtown.data.User
import com.example.jobtown.ui.components.JobTownBottomNavigationBar
import com.example.jobtown.ui.theme.JobTownTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            JobTownTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                var currentUser by remember { mutableStateOf<User?>(null) }
                var jobs by remember { mutableStateOf<List<Job>>(emptyList()) }
                var applications by remember { mutableStateOf<List<JobApplication>>(emptyList()) }

                // Show bottom navigation bar only on main tabs, excluding Profile
                val showBottomBar = currentRoute in listOf(
                    Screen.Home.route,
                    Screen.Applied.route,
                    Screen.Schedule.route,
                    Screen.Chat.route
                )

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (showBottomBar) {
                            JobTownBottomNavigationBar(
                                navController = navController,
                                currentUser = currentUser
                            )
                        }
                    }
                ) { innerPadding ->
                    NavGraph(
                        navController = navController,
                        currentUser = currentUser,
                        jobs = jobs,
                        applications = applications,
                        onSearch = { query ->
                            // Handle search if needed
                        },
                        onJobPosted = { job ->
                            jobs = jobs + job
                        },
                        onUpdateStatus = { appId, status ->
                            applications = applications.map {
                                if (it.id == appId) it.copy(status = status) else it
                            }
                        },
                        onLoginSuccess = { user ->
                            currentUser = user
                        },
                        onUpdateUser = { user ->
                            currentUser = user
                        },
                        onLogout = {
                            currentUser = null
                        }
                    )
                }
            }
        }
    }
}