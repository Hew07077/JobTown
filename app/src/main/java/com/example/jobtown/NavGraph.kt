package com.example.jobtown

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.jobtown.data.Job
import com.example.jobtown.data.JobApplication
import com.example.jobtown.data.User
import com.example.jobtown.ui.applied.MyAppliedScreen
import com.example.jobtown.ui.auth.CompleteProfileScreen
import com.example.jobtown.ui.auth.LoginScreen
import com.example.jobtown.ui.auth.SignUpScreen
import com.example.jobtown.ui.auth.StartupScreen
import com.example.jobtown.ui.chat.ChatDetailScreen
import com.example.jobtown.ui.chat.ChatListScreen
import com.example.jobtown.ui.home.HomeScreen
import com.example.jobtown.ui.job.JobDetailScreen
import com.example.jobtown.ui.job.PostJobScreen
import com.example.jobtown.ui.profile.ProfileScreen
import com.example.jobtown.ui.schedule.ScheduleScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    currentUser: User?,
    jobs: List<Job>,
    applications: List<JobApplication>,
    onSearch: (String) -> Unit,
    onJobPosted: (Job) -> Unit,
    onUpdateStatus: (String, String) -> Unit,
    onLoginSuccess: (User) -> Unit,
    onUpdateUser: (User) -> Unit,
    onLogout: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = "startup"
    ) {
        composable("startup") {
            StartupScreen(
                onNavigateToLogin = {
                    navController.navigate("login") {
                        popUpTo("startup") { inclusive = true }
                    }
                }
            )
        }
        composable("login") {
            LoginScreen(
                onLoginSuccess = { user ->
                    onLoginSuccess(user)
                    navController.navigate("home") {
                        popUpTo("startup") { inclusive = true }
                    }
                },
                onSignUpClick = {
                    navController.navigate("sign_up")
                }
            )
        }
        composable("sign_up") {
            SignUpScreen(
                onNextClick = { name, email, password, role ->
                    val newUser = User(
                        id = "user_${System.currentTimeMillis()}",
                        name = name,
                        email = email,
                        password = password,
                        role = role
                    )
                    onLoginSuccess(newUser)
                    navController.navigate("complete_profile")
                },
                onLoginClick = {
                    navController.navigate("login")
                }
            )
        }
        composable("complete_profile") {
            CompleteProfileScreen(
                user = currentUser,
                onComplete = { updatedUser ->
                    onUpdateUser(updatedUser)
                    navController.navigate("home") {
                        popUpTo("startup") { inclusive = true }
                    }
                }
            )
        }
        composable("home") {
            HomeScreen(
                navController = navController,
                currentUser = currentUser,
                jobsList = jobs,
                isLoading = false,
                onJobClick = { jobId ->
                    navController.navigate("job_detail/$jobId")
                },
                onPostJobClick = {
                    navController.navigate("post_job")
                },
                onProfileClick = {
                    navController.navigate("profile")
                }
            )
        }
        composable("post_job") {
            PostJobScreen(
                navController = navController,
                currentUser = currentUser,
                onJobPosted = { job ->
                    onJobPosted(job)
                    navController.popBackStack()
                }
            )
        }
        composable("chat") {
            ChatListScreen(
                currentUser = currentUser,
                chatRooms = emptyList(),
                isLoading = false,
                onChatRoomClick = { roomId, otherUserName ->
                    navController.navigate("chat_detail/$roomId/$otherUserName")
                }
            )
        }
        composable("chat_detail/{roomId}/{otherUserName}") { backStackEntry ->
            val roomId = backStackEntry.arguments?.getString("roomId") ?: ""
            val otherUserName = backStackEntry.arguments?.getString("otherUserName") ?: ""
            ChatDetailScreen(
                navController = navController,
                chatTitle = "Chat",
                companyName = otherUserName,
                currentUserId = currentUser?.id ?: ""
            )
        }
        composable("applied") {
            MyAppliedScreen(
                navController = navController,
                user = currentUser,
                applications = applications
            )
        }
        composable("schedule") {
            ScheduleScreen(
                navController = navController,
                user = currentUser,
                schedules = emptyList()
            )
        }
        composable("profile") {
            ProfileScreen(
                navController = navController,
                currentUser = currentUser,
                onLogout = {
                    onLogout()
                    navController.navigate("startup") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable("job_detail/{jobId}") { backStackEntry ->
            val jobId = backStackEntry.arguments?.getString("jobId")
            val job = jobs.find { it.id == jobId }
            if (job != null) {
                JobDetailScreen(
                    navController = navController,
                    job = job
                )
            }
        }
    }
}