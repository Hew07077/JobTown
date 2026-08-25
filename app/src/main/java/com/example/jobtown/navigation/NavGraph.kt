package com.example.jobtown.navigation

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
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
import com.example.jobtown.data.User
import com.example.jobtown.data.UserRole
import com.example.jobtown.data.repository.ApplicationRepository
import com.example.jobtown.data.repository.JobRepository
import com.example.jobtown.data.repository.MessageRepository
import com.example.jobtown.data.repository.ScheduleRepository
import com.example.jobtown.ui.applied.AppliedViewModel
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
import com.example.jobtown.ui.schedule.ScheduleViewModel
import com.example.jobtown.ui.schedule.SchedulePrefill
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

    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(message = it, duration = SnackbarDuration.Long)
            snackbarMessage = null
        }
    }

    val jobRepository = remember(supabaseClient) { JobRepository(supabaseClient) }
    val applicationRepository = remember(supabaseClient) { ApplicationRepository(supabaseClient) }
    val messageRepository = remember(supabaseClient) { MessageRepository(supabaseClient) }
    val scheduleRepository = remember(supabaseClient) { ScheduleRepository(supabaseClient) }

    val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModelFactory(jobRepository))

    val appliedViewModel: AppliedViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AppliedViewModel(applicationRepository) as T
            }
        }
    )

    val chatViewModel: ChatViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ChatViewModel(messageRepository) as T
            }
        }
    )

    val scheduleViewModel: ScheduleViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ScheduleViewModel(scheduleRepository) as T
            }
        }
    )

    LaunchedEffect(loggedInUser?.id) {
        loggedInUser?.let { user ->
            homeViewModel.loadJobs(user.id)
            if (user.role == UserRole.EMPLOYER) {
                appliedViewModel.loadEmployerApplications(user.id)
            } else {
                appliedViewModel.loadApplications(user.id)
            }
            chatViewModel.loadUserChatRooms(user.id)
            scheduleViewModel.loadSchedules(user.id, user.role == UserRole.EMPLOYER)
        }
    }

    // Kept live at this level (rather than only inside the chat_list route) so the bottom nav
    // badge and any other chrome can reflect unread messages no matter which screen is open.
    val chatRoomsForBadge by chatViewModel.chatRooms.collectAsStateWithLifecycle()
    val totalUnreadChatCount = remember(chatRoomsForBadge) {
        chatRoomsForBadge.sumOf { it.unreadCount }
    }

    /**
     * Single entry point for starting (or reopening) a chat, usable by both seekers and
     * employers. Looks up/creates the room via [MessageRepository.getOrCreateChatRoom] and
     * then navigates into it. [progressKey] just needs to be unique per in-flight request
     * (an application id, a room id, etc.) so simultaneous taps on different rows don't
     * clobber each other while still guarding against double-taps on the same one.
     */
    fun startOrOpenChat(
        counterpartId: String,
        counterpartName: String,
        companyName: String,
        jobTitle: String,
        progressKey: String
    ) {
        val currentUser = loggedInUser
        if (currentUser == null) {
            snackbarMessage = "You need to be logged in to start a chat."
            return
        }
        if (counterpartId.isBlank()) {
            snackbarMessage = "Couldn't find who to message for this listing."
            return
        }
        if (chatCreationInProgressId != null) return
        chatCreationInProgressId = progressKey

        val isEmployerViewer = currentUser.role == UserRole.EMPLOYER
        val seekerId = if (isEmployerViewer) counterpartId else currentUser.id
        val seekerName = if (isEmployerViewer) {
            counterpartName.ifBlank { "Applicant" }
        } else {
            currentUser.name.ifBlank { currentUser.email }
        }
        val employerId = if (isEmployerViewer) currentUser.id else counterpartId.ifBlank { "employer_default" }

        coroutineScope.launch {
            var caughtErrorText: String? = null

            val roomId = try {
                messageRepository.getOrCreateChatRoom(
                    seekerId = seekerId,
                    seekerName = seekerName,
                    employerId = employerId,
                    companyName = companyName.ifBlank { "Company" },
                    jobTitle = jobTitle.ifBlank { "Position" }
                )
            } catch (e: Exception) {
                Log.e("AppNavGraph", "Error creating chat room", e)
                caughtErrorText = e.message ?: e.toString()
                ""
            }

            chatCreationInProgressId = null

            if (roomId.isNotBlank()) {
                chatViewModel.loadUserChatRooms(currentUser.id)

                // The header always shows "who you're talking to": the company name for a
                // seeker, the applicant's name for an employer.
                val displayName = if (isEmployerViewer) seekerName else companyName.ifBlank { "Company Name" }
                val encodedName = Uri.encode(displayName.ifBlank { "Chat" })
                val encodedTitle = Uri.encode(jobTitle.ifBlank { "Position" })

                navController.navigate(Screen.Chat.route) {
                    popUpTo(Screen.Home.route) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
                navController.navigate("chat_detail/$roomId/$encodedName/$encodedTitle/none")
            } else {
                snackbarMessage = if (caughtErrorText != null) {
                    "Chat error: $caughtErrorText"
                } else {
                    "Couldn't start the chat. Please check your connection and try again."
                }
            }
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
                    unreadChatCount = totalUnreadChatCount
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
                    matchResults = homeViewModel.matchResults,
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
                    onScheduleInterview = { application ->
                        scheduleViewModel.setPrefill(
                            SchedulePrefill(
                                seekerId = application.userId,
                                seekerName = application.applicantName,
                                employerId = loggedInUser?.id.orEmpty(),
                                company = application.companyName,
                                title = application.jobTitle
                            )
                        )
                        navController.navigate(Screen.Schedule.route)
                    },
                    onStartChat = { application ->
                        startOrOpenChat(
                            counterpartId = application.userId,
                            counterpartName = application.applicantName,
                            companyName = application.companyName,
                            jobTitle = application.jobTitle,
                            progressKey = application.id
                        )
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

                val isLoading by appliedViewModel.isLoading.collectAsStateWithLifecycle()
                val isTrackingLive by appliedViewModel.isTrackingLive.collectAsStateWithLifecycle()
                val recentlyUpdatedId by appliedViewModel.recentlyUpdatedApplicationId.collectAsStateWithLifecycle()

                MyAppliedScreen(
                    navController = navController,
                    user = loggedInUser,
                    applications = appliedViewModel.applicationsList,
                    isLoading = isLoading,
                    onRefresh = {
                        loggedInUser?.id?.let { userId ->
                            appliedViewModel.loadApplications(userId, forceRefresh = true)
                        }
                    },
                    onApplicationClick = { applicationId ->
                        navController.navigate(Screen.ApplicationDetail.createRoute(applicationId))
                    },
                    chatLoadingApplicationId = chatCreationInProgressId,
                    isTrackingLive = isTrackingLive,
                    recentlyUpdatedApplicationId = recentlyUpdatedId,
                    onStartTracking = { userId: String -> appliedViewModel.startTracking(userId) },
                    onStopTracking = { appliedViewModel.stopTracking() },
                    onConsumeRecentUpdate = { appliedViewModel.consumeRecentUpdate() },
                    onProfileClick = { navController.navigate("profile") },
                    onChatWithCompany = { application ->
                        startOrOpenChat(
                            counterpartId = application.employerId.ifBlank { "employer_default" },
                            counterpartName = application.companyName,
                            companyName = application.companyName,
                            jobTitle = application.jobTitle,
                            progressKey = application.id
                        )
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
                    onBackClick = { navController.popBackStack() },
                    onChatClick = { applicantId, applicantName ->
                        val application = appliedViewModel.applicationsList.find { it.id == applicationId }
                        startOrOpenChat(
                            counterpartId = applicantId,
                            counterpartName = applicantName,
                            companyName = application?.companyName ?: loggedInUser?.companyName.orEmpty(),
                            jobTitle = application?.jobTitle.orEmpty(),
                            progressKey = applicationId
                        )
                    },
                    onScheduleClick = { _, applicantId, applicantName, jobTitle, companyName ->
                        scheduleViewModel.setPrefill(
                            SchedulePrefill(
                                seekerId = applicantId,
                                seekerName = applicantName,
                                employerId = loggedInUser?.id.orEmpty(),
                                company = companyName,
                                title = jobTitle
                            )
                        )
                        navController.navigate(Screen.Schedule.route)
                    },
                    onStatusChange = { targetAppId, newStatus ->
                        appliedViewModel.updateApplicationStatus(targetAppId, newStatus) { success ->
                            if (success) {
                                snackbarMessage = "Application status updated to $newStatus"
                            } else {
                                snackbarMessage = "Failed to update application status."
                            }
                        }
                    }
                )
            }

            composable(Screen.Schedule.route) {
                val currentUserId = loggedInUser?.id.orEmpty()
                val isEmployer = loggedInUser?.role == UserRole.EMPLOYER

                // Automatically fetch or refresh remote schedules when opening schedule screen
                LaunchedEffect(currentUserId) {
                    if (currentUserId.isNotBlank()) {
                        scheduleViewModel.loadSchedules(currentUserId, isEmployer)
                    }
                }

                ScheduleScreen(
                    navController = navController,
                    user = loggedInUser,
                    schedules = scheduleViewModel.schedulesList,
                    isEmployer = isEmployer,
                    isSaving = scheduleViewModel.isSaving,
                    prefill = scheduleViewModel.schedulePrefill,
                    onCreateSchedule = { newSchedule ->
                        scheduleViewModel.createSchedule(newSchedule, currentUserId, isEmployer) { success, message ->
                            snackbarMessage = message
                            if (success) {
                                navController.popBackStack()
                            }
                        }
                    },
                    onUpdateStatus = { scheduleId, status ->
                        scheduleViewModel.updateScheduleStatus(scheduleId, status, currentUserId, isEmployer) { success ->
                            if (success) {
                                snackbarMessage = "Schedule status updated to $status."
                            } else {
                                snackbarMessage = "Failed to update schedule status."
                            }
                        }
                    },
                    onRespondInvite = { scheduleId, status ->
                        scheduleViewModel.updateScheduleStatus(scheduleId, status, currentUserId, isEmployer) { success ->
                            if (success) {
                                snackbarMessage = "Interview invitation $status successfully."
                            } else {
                                snackbarMessage = "Failed to update interview invitation response."
                            }
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