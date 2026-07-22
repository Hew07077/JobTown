package com.example.jobtown

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.jobtown.data.Job
import com.example.jobtown.data.JobApplication
import com.example.jobtown.data.User
import com.example.jobtown.ui.components.BottomNavigationBar
import com.example.jobtown.ui.theme.JobTownTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            JobTownTheme {
                val navController = rememberNavController()

                var currentUser by remember { mutableStateOf<User?>(null) }
                var jobs by remember {
                    mutableStateOf(
                        listOf(
                            Job(
                                id = "1",
                                title = "Software Engineer",
                                company = "TechCorp",
                                location = "Kuala Lumpur",
                                salary = "RM 5,000 - RM 7,000",
                                salaryRange = "RM 5,000 - RM 7,000",
                                type = "Full-time",
                                description = "Looking for a skilled Android developer experienced in Jetpack Compose and Kotlin architecture.",
                                requirements = listOf("3+ years experience", "Proficient in Jetpack Compose"),
                                skills = listOf("Kotlin", "Compose", "Android")
                            )
                        )
                    )
                }
                var applications by remember { mutableStateOf(listOf<JobApplication>()) }

                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val showBottomBar = currentUser != null &&
                        currentRoute != Screen.Startup.route &&
                        currentRoute != Screen.Login.route &&
                        currentRoute != Screen.SignUp.route &&
                        currentRoute != Screen.CompleteProfile.route

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (showBottomBar) {
                            BottomNavigationBar(navController = navController)
                        }
                    }
                ) { innerPadding ->
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        NavGraph(
                            navController = navController,
                            currentUser = currentUser,
                            jobs = jobs,
                            applications = applications,
                            onSearch = { query -> },
                            onJobPosted = { newJob ->
                                jobs = jobs + newJob
                            },
                            onUpdateStatus = { appId, newStatus ->
                                applications = applications.map {
                                    if (it.id == appId) it.copy(status = newStatus) else it
                                }
                            },
                            onLoginSuccess = { user ->
                                currentUser = user
                            },
                            onUpdateUser = { updatedUser ->
                                currentUser = updatedUser
                            },
                            onLogout = {
                                currentUser = null
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}