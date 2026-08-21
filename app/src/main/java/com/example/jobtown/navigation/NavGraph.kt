package com.example.jobtown.navigation

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.jobtown.Screen // Ensure this points to your Screen sealed class file
import com.example.jobtown.data.User
import com.example.jobtown.data.UserRole
import com.example.jobtown.data.repository.ApplicationRepository
import com.example.jobtown.data.repository.JobRepository
import com.example.jobtown.data.repository.MessageRepository
import com.example.jobtown.ui.applied.AppliedViewModel
import com.example.jobtown.ui.applied.AppliedViewModelFactory
import com.example.jobtown.ui.applied.MyAppliedScreen
import com.example.jobtown.ui.applied.ApplicationDetailScreen
import com.example.jobtown.ui.auth.CompleteProfileScreen
import com.example.jobtown.ui.auth.LoginScreen
import com.example.jobtown.ui.auth.SignUpFields
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
import com.example.jobtown.ui.postjob.PostJobScreen
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

    var loggedInUser by remember { mutableStateOf(currentUser) }
    var signupDraft by remember { mutableStateOf(SignUpFields()) }
    var chatCreationInProgressId by remember { mutableStateOf<String?>(null) }

    var chatErrorMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(chatErrorMessage) {
        chatErrorMessage?.let {
            snackbarHostState.showSnackbar(message = it, duration = SnackbarDuration.Long)
            chatErrorMessage = null
        }
    }

    val jobRepository = remember(supabaseClient) { JobRepository(supabaseClient) }
    val applicationRepository = remember(supabaseClient) { ApplicationRepository(supabaseClient) }
    val messageRepository = remember(supabaseClient) { MessageRepository(supabaseClient) }

    val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModelFactory(jobRepository))
    val appliedViewModel: AppliedViewModel = viewModel(factory = AppliedViewModelFactory(applicationRepository))
    val chatViewModel: ChatViewModel = viewModel(factory = ChatViewModelFactory(messageRepository))

    LaunchedEffect(loggedInUser?.id) {
        loggedInUser?.id?.let { userId ->
            homeViewModel.loadJobs(userId)
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                        loggedInUser = user
                        homeViewModel.loadJobs(user.id)
                        navController.navigate(Screen.Home.route) {
                            popUpTo("login") { inclusive = true }
                        }
                    },
                    onSignUpClick = { navController.navigate("signup") }
                )
            }

            composable("signup") {
                SignUpScreen(
                    draft = signupDraft,
                    onDraftChange = { signupDraft = it },
                    onNextClick = {
                        navController.navigate("complete_profile")
                    },
                    onLoginClick = {
                        signupDraft = SignUpFields()
                        navController.popBackStack()
                    }
                )
            }

            composable("complete_profile") {
                val draftUser = User(
                    name = if (signupDraft.role == UserRole.EMPLOYER) "" else signupDraft.name,
                    companyName = if (signupDraft.role == UserRole.EMPLOYER) signupDraft.name else "",
                    email = signupDraft.email,
                    password = signupDraft.password,
                    role = signupDraft.role
                )

                CompleteProfileScreen(
                    user = draftUser,
                    draft = signupDraft,
                    onDraftChange = { signupDraft = it },
                    onBack = { navController.popBackStack() },
                    onComplete = { completedUser ->
                        loggedInUser = completedUser
                        signupDraft = SignUpFields()
                        homeViewModel.loadJobs(completedUser.id)
                        navController.navigate(Screen.Home.route) {
                            popUpTo("signup") { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Home.route) {
                HomeScreen(
                    navController = navController,
                    currentUser = loggedInUser,
                    jobsList = homeViewModel.jobsList,
                    isLoading = homeViewModel.isLoading,
                    onJobClick = { selectedJob ->
                        homeViewModel.selectJob(selectedJob)
                        if (loggedInUser?.role == UserRole.EMPLOYER) {
                            navController.navigate("employer_job_detail/${selectedJob.id}")
                        } else {
                            navController.navigate("apply_job")
                        }
                    },
                    onPostJobClick = {
                        navController.navigate("post_job")
                    },
                    onProfileClick = { navController.navigate("profile") },
                    onRefresh = { homeViewModel.loadJobs(loggedInUser?.id) },
                    matchScores = homeViewModel.matchScores,
                    sortMode = homeViewModel.sortMode,
                    onSortModeChange = { homeViewModel.updateSortMode(it) },
                    hasMatchProfile = homeViewModel.seekerProfile != null,
                    isLive = homeViewModel.isLive,
                    newListingsAvailable = homeViewModel.newListingsAvailable,
                    onShowNewListings = { homeViewModel.showPendingListings() },
                    onStartRealtime = { homeViewModel.startRealtimeUpdates() },
                    onStopRealtime = { homeViewModel.stopRealtimeUpdates() }
                )
            }

            composable("post_job") {
                PostJobScreen(
                    navController = navController,
                    currentUser = loggedInUser,
                    onJobPosted = { createdJob, onComplete ->
                        homeViewModel.postJob(createdJob) { success, message ->
                            onComplete(success, message)
                            if (success) {
                                homeViewModel.loadJobs(loggedInUser?.id)
                            }
                        }
                    }
                )
            }

            composable(
                route = "employer_job_detail/{jobId}",
                arguments = listOf(navArgument("jobId") { type = NavType.StringType })
            ) { backStackEntry ->
                val jobId = backStackEntry.arguments?.getString("jobId") ?: ""
                val currentJob = homeViewModel.jobsList.find { it.id == jobId } ?: homeViewModel.selectedJob

                if (currentJob != null) {
                    PostJobScreen(
                        navController = navController,
                        currentUser = loggedInUser,
                        onJobPosted = { _, _ -> navController.popBackStack() }
                    )
                } else {
                    navController.popBackStack()
                }
            }

            composable("apply_job") {
                val selectedJob = homeViewModel.selectedJob
                val isEmployer = loggedInUser?.role == UserRole.EMPLOYER
                if (selectedJob != null && !isEmployer) {
                    ApplyJobScreen(
                        navController = navController,
                        job = selectedJob,
                        currentUser = loggedInUser,
                        onApplySubmit = { application ->
                            appliedViewModel.submitNewApplication(application) {
                                navController.navigate(Screen.Applied.route) {
                                    popUpTo(Screen.Home.route) {
                                        saveState = true
                                    }
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

            composable("profile") {
                ProfileScreen(
                    navController = navController,
                    currentUser = loggedInUser,
                    onProfileUpdated = { updatedUser -> loggedInUser = updatedUser },
                    onLogout = {
                        loggedInUser = null
                        onLogout()
                        navController.navigate("startup") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

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
                    onApplicationClick = { applicationId ->
                        navController.navigate(Screen.ApplicationDetail.createRoute(applicationId))
                    },
                    chatLoadingApplicationId = chatCreationInProgressId,
                    isTrackingLive = appliedViewModel.isTrackingLive,
                    recentlyUpdatedApplicationId = appliedViewModel.recentlyUpdatedApplicationId,
                    onStartTracking = { userId: String -> appliedViewModel.startTracking(userId) },
                    onStopTracking = { appliedViewModel.stopTracking() },
                    onConsumeRecentUpdate = { appliedViewModel.consumeRecentUpdate() },
                    onChatWithCompany = { application ->
                        val userId = loggedInUser?.id
                        if (userId.isNullOrBlank()) {
                            chatErrorMessage = "You need to be logged in to start a chat."
                            return@MyAppliedScreen
                        }

                        if (chatCreationInProgressId != null) return@MyAppliedScreen

                        val userName = loggedInUser?.name?.ifBlank { loggedInUser?.email ?: "" } ?: ""
                        chatCreationInProgressId = application.id

                        coroutineScope.launch {
                            var caughtErrorText: String? = null

                            val roomId = try {
                                messageRepository.getOrCreateChatRoom(
                                    seekerId = userId,
                                    seekerName = userName,
                                    employerId = application.employerId.ifBlank { "employer_default" },
                                    companyName = application.companyName,
                                    jobTitle = application.jobTitle.ifBlank { "Position" }
                                )
                            } catch (e: Exception) {
                                Log.e("AppNavGraph", "Error creating chat room", e)
                                caughtErrorText = e.message ?: e.toString()
                                ""
                            }

                            chatCreationInProgressId = null

                            if (roomId.isNotBlank()) {
                                chatViewModel.loadUserChatRooms(userId)

                                navController.navigate(Screen.Chat.route) {
                                    popUpTo(Screen.Home.route) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }

                                val chatDetailRoute = Screen.ChatDetail.createRoute(
                                    chatRoomId = roomId,
                                    company = application.companyName,
                                    title = application.jobTitle,
                                    initialQuestion = ""
                                )
                                navController.navigate(chatDetailRoute)
                            } else {
                                chatErrorMessage = if (caughtErrorText != null) {
                                    "Chat error: $caughtErrorText"
                                } else {
                                    "Couldn't start the chat. Please check your connection and try again."
                                }
                            }
                        }
                    }
                )
            }

            composable(
                route = Screen.ApplicationDetail.route,
                arguments = listOf(
                    navArgument("applicationId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val applicationId = backStackEntry.arguments?.getString("applicationId") ?: ""
                ApplicationDetailScreen(
                    applicationId = applicationId,
                    viewModel = appliedViewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(Screen.Schedule.route) {
                ScheduleScreen(
                    navController = navController,
                    user = loggedInUser,
                    schedules = emptyList()
                )
            }

            navigation(
                startDestination = "chat_list",
                route = Screen.Chat.route
            ) {
                composable("chat_list") {
                    LaunchedEffect(Unit) {
                        loggedInUser?.id?.let { userId ->
                            chatViewModel.loadUserChatRooms(userId)
                        }
                    }

                    val chatRooms by chatViewModel.chatRooms.collectAsStateWithLifecycle()
                    val isLoadingRooms by chatViewModel.isLoadingRooms.collectAsStateWithLifecycle()

                    ChatListScreen(
                        currentUser = loggedInUser,
                        chatRooms = chatRooms,
                        isLoading = isLoadingRooms,
                        onChatRoomClick = { chatRoomId: String, companyName: String, position: String ->
                            val chatDetailRoute = Screen.ChatDetail.createRoute(
                                chatRoomId = chatRoomId,
                                company = companyName,
                                title = position,
                                initialQuestion = ""
                            )
                            navController.navigate(chatDetailRoute)
                        }
                    )
                }

                composable(
                    route = Screen.ChatDetail.route,
                    arguments = listOf(
                        navArgument("chatRoomId") { type = NavType.StringType },
                        navArgument("company") { type = NavType.StringType; defaultValue = "Company Name" },
                        navArgument("title") { type = NavType.StringType; defaultValue = "Position" },
                        navArgument("initialQuestion") { type = NavType.StringType; defaultValue = "" }
                    )
                ) { backStackEntry ->
                    val roomId = backStackEntry.arguments?.getString("chatRoomId") ?: ""
                    val company = backStackEntry.arguments?.getString("company") ?: "Company Name"
                    val title = backStackEntry.arguments?.getString("title") ?: "Position"
                    val initialQuestion = backStackEntry.arguments?.getString("initialQuestion") ?: ""

                    ChatDetailScreen(
                        navController = navController,
                        roomId = roomId,
                        companyName = company,
                        chatTitle = title,
                        initialQuestion = initialQuestion,
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