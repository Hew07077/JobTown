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
import com.example.jobtown.ui.chat.ChatListScreen
import com.example.jobtown.ui.home.HomeScreen
import com.example.jobtown.ui.job.JobDetailScreen
import com.example.jobtown.ui.job.PostJobScreen
import com.example.jobtown.ui.profile.ProfileScreen
import com.example.jobtown.ui.schedule.ScheduleScreen
import com.example.jobtown.ui.startup.StartupScreen

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
        startDestination = Screen.Startup.route
    ) {
        composable(Screen.Startup.route) {
            StartupScreen(navController = navController)
        }
        composable(Screen.Login.route) {
            LoginScreen(
                navController = navController,
                onLoginSuccess = onLoginSuccess
            )
        }
        composable(Screen.SignUp.route) {
            SignUpScreen(
                navController = navController,
                onSignUpSuccess = { user ->
                    onLoginSuccess(user)
                }
            )
        }
        composable(Screen.CompleteProfile.route) {
            CompleteProfileScreen(
                navController = navController,
                currentUser = currentUser,
                onUpdateUser = onUpdateUser
            )
        }
        composable(Screen.Home.route) {
            HomeScreen(
                navController = navController,
                jobs = jobs,
                currentUser = currentUser,
                onSearch = onSearch
            )
        }
        composable(Screen.PostJob.route) {
            PostJobScreen(
                navController = navController,
                currentUser = currentUser,
                onJobPosted = onJobPosted
            )
        }
        composable(Screen.Chat.route) {
            ChatListScreen(
                navController = navController,
                userRole = currentUser?.role ?: "seeker",
                userId = currentUser?.id ?: "",
                applications = applications,
                onUpdateStatus = onUpdateStatus
            )
        }
        composable(Screen.Applied.route) {
            MyAppliedScreen(
                navController = navController,
                applications = applications,
                allJobs = jobs,
                userRole = currentUser?.role ?: "seeker",
                userId = currentUser?.id ?: "",
                onUpdateApplicationStatus = onUpdateStatus
            )
        }
        composable(Screen.Schedule.route) {
            ScheduleScreen(navController = navController)




        }
        composable(Screen.Profile.route) {
            ProfileScreen(
                navController = navController,
                currentUser = currentUser,
                onUpdateUser = onUpdateUser,
                onLogout = onLogout
            )
        }
        composable("job_detail/{jobId}") { backStackEntry ->
            val jobId = backStackEntry.arguments?.getString("jobId")
            val job = jobs.find { it.id == jobId }
            if (job != null) {
                JobDetailScreen(
                    navController = navController,
                    job = job,
                    onApplyClick = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
