package com.example.jobtown.ui.navigation

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.jobtown.Screen
import com.example.jobtown.data.User
import com.example.jobtown.data.UserRole
import com.example.jobtown.data.repository.ApplicationRepository
import com.example.jobtown.data.repository.JobRepository
import com.example.jobtown.data.repository.MessageRepository
import com.example.jobtown.ui.applied.AppliedViewModel
import com.example.jobtown.ui.applied.AppliedViewModelFactory
import com.example.jobtown.ui.applied.MyAppliedScreen
import com.example.jobtown.ui.auth.CompleteProfileScreen
import com.example.jobtown.ui.auth.LoginScreen
import com.example.jobtown.ui.auth.SignUpScreen
import com.example.jobtown.ui.auth.StartupScreen
import com.example.jobtown.ui.chat.ChatDetailScreen
import com.example.jobtown.ui.chat.ChatListScreen
import com.example.jobtown.ui.chat.ChatViewModel
import com.example.jobtown.ui.chat.ChatViewModelFactory
import com.example.jobtown.ui.components.JobTownBottomNavigationBar
import com.example.jobtown.ui.home.HomeScreen
import com.example.jobtown.ui.home.HomeViewModel
import com.example.jobtown.ui.home.HomeViewModelFactory
import com.example.jobtown.ui.job.ApplyJobScreen
import com.example.jobtown.ui.profile.ProfileScreen
import com.example.jobtown.ui.schedule.ScheduleScreen
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.launch

@Composable
fun AppNavGraph(
    supabaseClient: SupabaseClient,
    navController: NavHostController = rememberNavController(),
    currentUser: User? = null,
    startDestination: String = "startup",
    onLogout: () -> Unit = {}
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val coroutineScope = rememberCoroutineScope()

    // Single source of truth for "who is logged in right now" inside this graph.
    // Seeded from the `currentUser` parameter (e.g. a restored session) but
    // updated in-place whenever login / signup / profile edits succeed, so
    // every screen below always sees the freshest user without requiring
    // the caller (MainActivity) to manage this state itself.
    var loggedInUser by remember { mutableStateOf(currentUser) }

    // Repositories
    val jobRepository = remember(supabaseClient) { JobRepository(supabaseClient) }
    val applicationRepository = remember(supabaseClient) { ApplicationRepository(supabaseClient) }
    val messageRepository = remember(supabaseClient) { MessageRepository(supabaseClient) }

    // ViewModels
    val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModelFactory(jobRepository))
    val appliedViewModel: AppliedViewModel = viewModel(factory = AppliedViewModelFactory(applicationRepository))
    val chatViewModel: ChatViewModel = viewModel(factory = ChatViewModelFactory(messageRepository))

    LaunchedEffect(loggedInUser?.id) {
        loggedInUser?.id?.let { userId ->
            appliedViewModel.loadApplications(userId)
            chatViewModel.loadUserChatRooms(userId)
        }
    }

    val bottomBarRoutes = listOf(
        Screen.Home.route,
        Screen.Applied.route,
        Screen.Schedule.route,
        Screen.Chat.route,
        "chat_list"
    )

    Scaffold(
        bottomBar = {
            if (currentRoute in bottomBarRoutes) {
                JobTownBottomNavigationBar(
                    navController = navController,
                    currentUser = loggedInUser,
                    unreadChatCount = 0
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            // --- STARTUP / SPLASH SCREEN ---
            composable("startup") {
                StartupScreen(
                    onNavigateToLogin = {
                        navController.navigate("login") {
                            popUpTo("startup") { inclusive = true }
                        }
                    }
                )
            }

            // --- LOGIN ---
            composable("login") {
                LoginScreen(
                    onLoginSuccess = { user ->
                        // Persist the authenticated user so every other screen can read it.
                        loggedInUser = user
                        navController.navigate(Screen.Home.route) {
                            popUpTo("login") { inclusive = true }
                        }
                    },
                    onSignUpClick = { navController.navigate("signup") }
                )
            }

            // --- SIGNUP ---
            composable("signup") {
                SignUpScreen(
                    onNextClick = { name, email, password, role ->
                        // Build a draft user from the signup form and carry it into
                        // CompleteProfileScreen instead of discarding it. The single
                        // "name" field on SignUpScreen means different things
                        // depending on role: a person's full name for job seekers,
                        // or the company's name for employers -- route it into the
                        // matching User field accordingly.
                        loggedInUser = User(
                            name = if (role == UserRole.EMPLOYER) "" else name,
                            companyName = if (role == UserRole.EMPLOYER) name else "",
                            email = email,
                            password = password,
                            role = role
                        )
                        navController.navigate("complete_profile") {
                            popUpTo("signup") { inclusive = true }
                        }
                    },
                    onLoginClick = { navController.popBackStack() }
                )
            }

            // --- COMPLETE PROFILE (post-signup) ---
            composable("complete_profile") {
                CompleteProfileScreen(
                    user = loggedInUser,
                    onComplete = { completedUser ->
                        loggedInUser = completedUser
                        navController.navigate(Screen.Home.route) {
                            popUpTo("complete_profile") { inclusive = true }
                        }
                    }
                )
            }

            // --- HOME ---
            composable(Screen.Home.route) {
                HomeScreen(
                    navController = navController,
                    currentUser = loggedInUser,
                    jobsList = homeViewModel.jobsList,
                    isLoading = homeViewModel.isLoading,
                    onJobClick = { selectedJob ->
                        homeViewModel.selectJob(selectedJob)
                        navController.navigate("apply_job")
                    },
                    onPostJobClick = { },
                    onProfileClick = { navController.navigate("profile") },
                    onRefresh = { homeViewModel.loadJobs() }
                )
            }

            // --- APPLY JOB ---
            composable("apply_job") {
                val selectedJob = homeViewModel.selectedJob
                if (selectedJob != null) {
                    ApplyJobScreen(
                        navController = navController,
                        job = selectedJob,
                        currentUser = loggedInUser,
                        onApplySubmit = { application ->
                            appliedViewModel.submitNewApplication(application) {
                                navController.navigate(Screen.Applied.route) {
                                    popUpTo(Screen.Home.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                } else {
                    navController.popBackStack()
                }
            }

            // --- PROFILE ---
            composable("profile") {
                ProfileScreen(
                    navController = navController,
                    currentUser = loggedInUser,
                    onProfileUpdated = { updatedUser ->
                        loggedInUser = updatedUser
                    },
                    onLogout = {
                        loggedInUser = null
                        onLogout()
                        navController.navigate("startup") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            // --- APPLIED ---
            composable(Screen.Applied.route) {
                LaunchedEffect(Unit) {
                    loggedInUser?.id?.let { userId ->
                        appliedViewModel.loadApplications(userId, forceRefresh = true)
                    }
                }

                MyAppliedScreen(
                    navController = navController,
                    user = loggedInUser,
                    applications = appliedViewModel.applicationsList,
                    isLoading = appliedViewModel.isLoading,
                    onRefresh = {
                        loggedInUser?.id?.let { userId ->
                            appliedViewModel.loadApplications(userId, forceRefresh = true)
                        }
                    },
                    onChatWithCompany = { application ->
                        val userId = loggedInUser?.id ?: return@MyAppliedScreen
                        val userName = loggedInUser?.name?.ifBlank { loggedInUser?.email ?: "" } ?: ""

                        coroutineScope.launch {
                            val roomId = messageRepository.getOrCreateChatRoom(
                                seekerId = userId,
                                seekerName = userName,
                                employerId = application.id.ifBlank { "employer_default" },
                                companyName = application.companyName
                            )

                            if (roomId.isNotBlank()) {
                                chatViewModel.loadUserChatRooms(userId)

                                val encodedCompany = Uri.encode(application.companyName.ifBlank { "Company" })
                                val encodedTitle = Uri.encode(application.jobTitle.ifBlank { "Position" })

                                navController.navigate(Screen.Chat.route) {
                                    popUpTo(Screen.Home.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }

                                navController.navigate("chat_detail/$roomId/$encodedCompany/$encodedTitle/none")
                            } else {
                                Log.e("AppNavGraph", "Could not create or retrieve chat room.")
                            }
                        }
                    }
                )
            }

            // --- SCHEDULE ---
            composable(Screen.Schedule.route) {
                ScheduleScreen(
                    navController = navController,
                    user = loggedInUser,
                    schedules = emptyList()
                )
            }

            // --- NESTED CHAT GRAPH ---
            navigation(
                startDestination = "chat_list",
                route = Screen.Chat.route
            ) {
                // Chat List screen
                composable("chat_list") {
                    LaunchedEffect(Unit) {
                        loggedInUser?.id?.let { userId ->
                            chatViewModel.loadUserChatRooms(userId)
                        }
                    }

                    ChatListScreen(
                        currentUser = loggedInUser,
                        chatRooms = chatViewModel.chatRooms.value,
                        isLoading = chatViewModel.isLoadingRooms,
                        onChatRoomClick = { chatRoomId, companyName ->
                            val encodedCompany = Uri.encode(companyName)
                            navController.navigate("chat_detail/$chatRoomId/$encodedCompany/Position/none")
                        }
                    )
                }

                // Chat Detail screen
                composable(
                    route = "chat_detail/{chatRoomId}/{company}/{title}/{initialQuestion}",
                    arguments = listOf(
                        navArgument("chatRoomId") { type = NavType.StringType },
                        navArgument("company") { type = NavType.StringType; defaultValue = "Company" },
                        navArgument("title") { type = NavType.StringType; defaultValue = "Position" },
                        navArgument("initialQuestion") { type = NavType.StringType; defaultValue = "" }
                    )
                ) { backStackEntry ->
                    val roomId = backStackEntry.arguments?.getString("chatRoomId") ?: ""
                    val company = backStackEntry.arguments?.getString("company") ?: "Company"
                    val title = backStackEntry.arguments?.getString("title") ?: "Position"
                    val initialQuestion = backStackEntry.arguments?.getString("initialQuestion") ?: ""

                    ChatDetailScreen(
                        navController = navController,
                        roomId = roomId,
                        companyName = company,
                        chatTitle = title,
                        initialQuestion = if (initialQuestion == "none") "" else initialQuestion,
                        currentUserId = loggedInUser?.id ?: "1",
                        chatViewModel = chatViewModel,
                        onNavigateToSchedule = {
                            navController.navigate(Screen.Schedule.route)
                        }
                    )
                }
            }
        }
    }
}
