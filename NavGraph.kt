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
import com.example.jobtown.Screen
import com.example.jobtown.data.InterviewSchedule
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
import com.example.jobtown.ui.employer.CompanyDetailScreen
import com.example.jobtown.ui.employer.ManageJobsScreen
import com.example.jobtown.ui.components.JobTownBottomNavigationBar
import com.example.jobtown.ui.home.HomeScreen
import com.example.jobtown.ui.home.HomeViewModel
import com.example.jobtown.ui.home.HomeViewModelFactory
import com.example.jobtown.ui.job.ApplyJobScreen
import com.example.jobtown.ui.postjob.EmployerJobDetailScreen
import com.example.jobtown.ui.postjob.PostJobScreen
import com.example.jobtown.ui.profile.ProfileScreen
import com.example.jobtown.ui.schedule.ScheduleScreen
import com.example.jobtown.ui.schedule.SchedulePrefill
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.launch
import java.util.UUID

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

    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    var scheduleList by remember { mutableStateOf<List<InterviewSchedule>>(emptyList()) }
    var isSavingSchedule by remember { mutableStateOf(false) }
    var schedulePrefill by remember { mutableStateOf(SchedulePrefill()) }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(message = it, duration = SnackbarDuration.Long)
            snackbarMessage = null
        }
    }

    val jobRepository = remember(supabaseClient) { JobRepository(supabaseClient) }
    val applicationRepository = remember(supabaseClient) { ApplicationRepository(supabaseClient) }
    val messageRepository = remember(supabaseClient) { MessageRepository(supabaseClient) }

    val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModelFactory(jobRepository))
    val appliedViewModel: AppliedViewModel = viewModel(factory = AppliedViewModelFactory(applicationRepository))
    val chatViewModel: ChatViewModel = viewModel(factory = ChatViewModelFactory(messageRepository))

    LaunchedEffect(loggedInUser?.id) {
        loggedInUser?.let { user ->
            homeViewModel.loadJobs(user.id)
            if (user.role == UserRole.EMPLOYER) {
                appliedViewModel.loadEmployerApplications(user.id)
            } else {
                appliedViewModel.loadApplications(user.id)
            }
            chatViewModel.loadUserChatRooms(user.id)
        }
    }

    val bottomBarRoutes = listOf(
        Screen.Home.route,
        Screen.Applied.route,
        Screen.Schedule.route,
        Screen.Chat.route,
        "chat_list",
        "manage_jobs"
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
                        if (loggedInUser?.role == UserRole.EMPLOYER) {
                            navController.navigate("manage_jobs")
                        } else {
                            navController.navigate("post_job")
                        }
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

            // MANAGE JOBS SCREEN (For Employers)
            composable("manage_jobs") {
                LaunchedEffect(loggedInUser?.id) {
                    loggedInUser?.id?.let { employerId ->
                        appliedViewModel.loadEmployerApplications(employerId, forceRefresh = true)
                    }
                }

                val employerJobs = homeViewModel.jobsList.filter { job ->
                    job.employerId == loggedInUser?.id || job.postedByUserId == loggedInUser?.id
                }

                ManageJobsScreen(
                    navController = navController,
                    employerJobs = employerJobs,
                    appliedViewModel = appliedViewModel,
                    onAddJobClick = { navController.navigate("post_job") },
                    onJobClick = { job -> navController.navigate("employer_job_detail/${job.id}") },
                    onApplicationClick = { applicationId ->
                        navController.navigate(Screen.ApplicationDetail.createRoute(applicationId))
                    },
                    onProfileClick = { navController.navigate("profile") }
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
                    EmployerJobDetailScreen(
                        job = currentJob,
                        navController = navController,
                        onUpdateJob = { updatedJob ->
                            homeViewModel.updateJob(updatedJob) { success, message ->
                                if (success) {
                                    navController.popBackStack()
                                } else {
                                    snackbarMessage = message ?: "Failed to update job. Please try again."
                                }
                            }
                        },
                        onBackClick = { navController.popBackStack() }
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
                            appliedViewModel.submitNewApplication(application) { success ->
                                if (success) {
                                    navController.navigate(Screen.Applied.route) {
                                        popUpTo(Screen.Home.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                } else {
                                    snackbarMessage = "Failed to submit application. Please try again."
                                }
                            }
                        },
                        onViewCompanyDetails = { companyId ->
                            val encodedCompanyId = Uri.encode(companyId)
                            navController.navigate("company_detail/$encodedCompanyId")
                        }
                    )
                } else {
                    navController.popBackStack()
                }
            }

            composable(
                route = "company_detail/{companyId}",
                arguments = listOf(navArgument("companyId") { type = NavType.StringType })
            ) { backStackEntry ->
                val companyId = backStackEntry.arguments?.getString("companyId") ?: ""

                CompanyDetailScreen(
                    navController = navController,
                    companyIdOrName = companyId,
                    openJobs = homeViewModel.jobsList,
                    onJobClick = { selectedJob ->
                        homeViewModel.selectJob(selectedJob)
                        if (loggedInUser?.role == UserRole.EMPLOYER) {
                            navController.navigate("employer_job_detail/${selectedJob.id}")
                        } else {
                            navController.navigate("apply_job")
                        }
                    }
                )
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
                    loggedInUser?.let { user ->
                        if (user.role == UserRole.EMPLOYER) {
                            navController.navigate("manage_jobs") {
                                popUpTo(Screen.Applied.route) { inclusive = true }
                            }
                        } else {
                            appliedViewModel.loadApplications(user.id, forceRefresh = true)
                        }
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
                    onProfileClick = { navController.navigate("profile") },
                    onChatWithCompany = { application ->
                        val userId = loggedInUser?.id
                        if (userId.isNullOrBlank()) {
                            snackbarMessage = "You need to be logged in to start a chat."
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

                                val encodedCompany = Uri.encode(application.companyName.ifBlank { "Company Name" })
                                val encodedTitle = Uri.encode(application.jobTitle.ifBlank { "Position" })

                                navController.navigate(Screen.Chat.route) {
                                    popUpTo(Screen.Home.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }

                                navController.navigate("chat_detail/$roomId/$encodedCompany/$encodedTitle/none")
                            } else {
                                snackbarMessage = if (caughtErrorText != null) {
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
                val currentUserId = loggedInUser?.id.orEmpty()
                val isEmployer = loggedInUser?.role == UserRole.EMPLOYER
                val visibleSchedules = scheduleList.filter { schedule ->
                    if (isEmployer) schedule.employerId == currentUserId else schedule.userId == currentUserId
                }

                ScheduleScreen(
                    navController = navController,
                    user = loggedInUser,
                    schedules = visibleSchedules,
                    isEmployer = isEmployer,
                    isSaving = isSavingSchedule,
                    prefill = schedulePrefill,
                    onCreateSchedule = { newSchedule ->
                        isSavingSchedule = true
                        scheduleList = listOf(newSchedule.copy(id = UUID.randomUUID().toString())) + scheduleList
                        schedulePrefill = SchedulePrefill()
                        isSavingSchedule = false
                    },
                    onUpdateStatus = { scheduleId, status ->
                        scheduleList = scheduleList.map {
                            if (it.id == scheduleId) it.copy(status = status) else it
                        }
                    },
                    onProfileClick = { navController.navigate("profile") }
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
                        onChatRoomClick = { chatRoomId, companyName, position ->
                            val encodedCompany = Uri.encode(companyName.ifBlank { "Company Name" })
                            val encodedPosition = Uri.encode(position.ifBlank { "Position" })
                            navController.navigate("chat_detail/$chatRoomId/$encodedCompany/$encodedPosition/none")
                        },
                        onProfileClick = { navController.navigate("profile") }
                    )
                }

                composable(
                    route = "chat_detail/{chatRoomId}/{company}/{title}/{initialQuestion}",
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