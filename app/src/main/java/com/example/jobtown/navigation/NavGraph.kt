package com.example.jobtown.navigation

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.jobtown.Screen
import com.example.jobtown.data.model.ChatRoom
import com.example.jobtown.data.model.User
import com.example.jobtown.data.model.UserRole
import com.example.jobtown.data.model.NotificationType
import com.example.jobtown.data.repository.ApplicationRepository
import com.example.jobtown.data.repository.JobRepository
import com.example.jobtown.data.repository.MessageRepository
import com.example.jobtown.data.repository.NotificationRepository
import com.example.jobtown.data.repository.ScheduleRepository
import com.example.jobtown.ui.applied.AppliedViewModel
import com.example.jobtown.ui.applied.MyAppliedScreen
import com.example.jobtown.ui.applied.ApplicationDetailScreen
import com.example.jobtown.ui.applied.JobseekerApplicationDetailScreen
import com.example.jobtown.ui.auth.CompleteProfileScreen
import com.example.jobtown.ui.auth.LoginScreen
import com.example.jobtown.ui.auth.ResetPasswordScreen
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
import com.example.jobtown.ui.home.SavedJobsScreen
import com.example.jobtown.ui.job.ApplyJobScreen
import com.example.jobtown.ui.postjob.EmployerJobDetailScreen
import com.example.jobtown.ui.postjob.PostJobScreen
import com.example.jobtown.ui.profile.NotificationsScreen
import com.example.jobtown.ui.profile.ProfileScreen
import com.example.jobtown.ui.schedule.ScheduleScreen
import com.example.jobtown.ui.schedule.ScheduleDetailScreen
import com.example.jobtown.ui.schedule.ScheduleViewModel
import com.example.jobtown.ui.schedule.SchedulePrefill
import com.example.jobtown.ui.schedule.InterviewEditorScreen
import com.example.jobtown.ui.schedule.toSchedulePrefill
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.launch

@Composable
fun AppNavGraph(
    supabaseClient: SupabaseClient,
    navController: NavHostController = rememberNavController(),
    currentUser: User? = null,
    startDestination: String = "startup",
    onLogout: () -> Unit = {},
    pendingDeepLinkUri: Uri? = null,
    onDeepLinkHandled: () -> Unit = {}
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val coroutineScope = rememberCoroutineScope()

    var loggedInUser by remember { mutableStateOf(currentUser) }
    var signupDraft by remember { mutableStateOf(SignUpFields()) }
    var chatCreationInProgressId by remember { mutableStateOf<String?>(null) }

    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    var showInterviewEditor by remember { mutableStateOf(false) }

    LaunchedEffect(pendingDeepLinkUri) {
        val uri = pendingDeepLinkUri
        if (uri != null && uri.host == "reset-password") {
            navController.navigate("reset_password") {
                launchSingleTop = true
            }
            onDeepLinkHandled()
        }
    }

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

    val chatRoomsList by chatViewModel.chatRooms.collectAsStateWithLifecycle()
    val isChatLoading by chatViewModel.isLoadingRooms.collectAsStateWithLifecycle()

    val totalUnreadChatCount = remember(chatRoomsList) {
        chatRoomsList.sumOf { it.unreadCount }
    }

    fun startOrOpenChat(
        counterpartId: String,
        counterpartName: String,
        companyName: String,
        jobTitle: String,
        progressKey: String
    ) {
        val user = loggedInUser
        if (user == null) {
            snackbarMessage = "You need to be logged in to start a chat."
            return
        }
        if (counterpartId.isBlank()) {
            snackbarMessage = "Couldn't find who to message for this listing."
            return
        }
        if (chatCreationInProgressId != null) return
        chatCreationInProgressId = progressKey

        val isEmployerViewer = user.role == UserRole.EMPLOYER
        val seekerId = if (isEmployerViewer) counterpartId else user.id
        val seekerName = if (isEmployerViewer) {
            counterpartName.ifBlank { "Applicant" }
        } else {
            user.name.ifBlank { user.email }
        }
        val employerId = if (isEmployerViewer) user.id else counterpartId.ifBlank { "employer_default" }

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
                chatViewModel.loadUserChatRooms(user.id)

                navController.navigate(Screen.Chat.route) {
                    popUpTo(Screen.Home.route) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
                navController.navigate(
                    Screen.ChatDetail.createRoute(
                        chatRoomId = roomId,
                        company = if (isEmployerViewer) seekerName else companyName.ifBlank { "Company Name" },
                        title = jobTitle.ifBlank { "Position" }
                    )
                )
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
                    onNextClick = { navController.navigate("complete_profile") },
                    onLoginClick = {
                        signupDraft = SignUpFields()
                        navController.popBackStack()
                    }
                )
            }

            composable("reset_password") {
                ResetPasswordScreen(
                    onPasswordUpdated = {
                        snackbarMessage = "Password updated. Please log in with your new password."
                        navController.navigate("login") {
                            popUpTo("reset_password") { inclusive = true }
                        }
                    },
                    onCancel = {
                        if (!navController.popBackStack()) {
                            navController.navigate("login")
                        }
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
                    onPostJobClick = { navController.navigate("post_job") },
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
                    onStopRealtime = { homeViewModel.stopRealtimeUpdates() },
                    savedJobIds = homeViewModel.savedJobIds,
                    onToggleSaveJob = { job ->
                        val jobId = job.id.orEmpty()
                        val userId = loggedInUser?.id.orEmpty()
                        if (jobId.isNotBlank() && userId.isNotBlank()) {
                            homeViewModel.toggleSaveJob(userId, jobId)
                        }
                    },
                    onSavedJobsClick = { navController.navigate("saved_jobs") }
                )
            }

            composable("saved_jobs") {
                SavedJobsScreen(
                    navController = navController,
                    allJobs = homeViewModel.jobsList,
                    savedJobIds = homeViewModel.savedJobIds,
                    onJobClick = { job ->
                        homeViewModel.selectJob(job)
                        navController.navigate("apply_job")
                    },
                    onToggleSaveJob = { jobId ->
                        val userId = loggedInUser?.id.orEmpty()
                        if (userId.isNotBlank()) {
                            homeViewModel.toggleSaveJob(userId, jobId)
                        }
                    }
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
                            application.toSchedulePrefill(
                                employerId = loggedInUser?.id.orEmpty(),
                                fallbackCompany = loggedInUser?.companyName.orEmpty()
                            )
                        )
                        showInterviewEditor = true
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
                if (loggedInUser?.role != UserRole.EMPLOYER) {
                    LaunchedEffect(Unit) { navController.popBackStack() }
                } else {
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
                        currentUser = loggedInUser,
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
                        existingApplication = loggedInUser?.id?.let { uid ->
                            appliedViewModel.findApplicationForJob(uid, selectedJob.id)
                        },
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
                        },
                        onViewExistingApplication = {
                            val existing = loggedInUser?.id?.let { uid ->
                                appliedViewModel.findApplicationForJob(uid, selectedJob.id)
                            }
                            if (existing != null) {
                                navController.navigate(Screen.ApplicationDetail.createRoute(existing.id))
                            }
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
                        coroutineScope.launch {
                            try {
                                supabaseClient.auth.signOut()
                            } catch (_: Exception) {
                            }
                        }
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
                    user = loggedInUser,
                    viewModel = appliedViewModel,
                    isLoading = isLoading,
                    onApplicationClick = { applicationId ->
                        navController.navigate(Screen.ApplicationDetail.createRoute(applicationId))
                    },
                    chatLoadingApplicationId = chatCreationInProgressId,
                    isTrackingLive = isTrackingLive,
                    recentlyUpdatedApplicationId = recentlyUpdatedId,
                    onStartTracking = { appliedViewModel.startTracking(it) },
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

                if (loggedInUser?.role == UserRole.EMPLOYER) {
                    ApplicationDetailScreen(
                        applicationId = applicationId,
                        viewModel = appliedViewModel,
                        onBackClick = { navController.popBackStack() },
                        onChatClick = { applicantId: String, applicantName: String ->
                            val application = appliedViewModel.applicationsList.find { it.id == applicationId }
                            startOrOpenChat(
                                counterpartId = applicantId,
                                counterpartName = applicantName,
                                companyName = application?.companyName ?: loggedInUser?.companyName.orEmpty(),
                                jobTitle = application?.jobTitle.orEmpty(),
                                progressKey = applicationId
                            )
                        },
                        onScheduleClick = { appId, applicantId, applicantName, jobTitle, companyName ->
                            val application = appliedViewModel.applicationsList.find { it.id == appId || it.id == applicationId }
                            scheduleViewModel.setPrefill(
                                SchedulePrefill(
                                    seekerId = applicantId,
                                    seekerName = applicantName,
                                    employerId = loggedInUser?.id.orEmpty(),
                                    company = companyName.ifBlank { loggedInUser?.companyName.orEmpty() },
                                    title = jobTitle,
                                    jobId = application?.jobId.orEmpty()
                                )
                            )
                            showInterviewEditor = true
                        },
                        onStatusChange = { targetAppId, newStatus ->
                            appliedViewModel.updateApplicationStatus(targetAppId, newStatus) { success ->
                                snackbarMessage = if (success) {
                                    if (!newStatus.equals("Viewed", ignoreCase = true)) {
                                        val app = appliedViewModel.applicationsList.find { it.id == targetAppId }
                                        if (app != null) {
                                            coroutineScope.launch {
                                                val company = loggedInUser?.companyName
                                                    ?.ifBlank { loggedInUser?.name }
                                                    .orEmpty()
                                                    .ifBlank { "A company" }
                                                NotificationRepository.notifyUser(
                                                    userId = app.userId,
                                                    title = "Application update",
                                                    body = "$company updated your application for ${app.jobTitle} to $newStatus.",
                                                    type = NotificationType.APPLICATION_STATUS,
                                                    relatedId = app.id
                                                )
                                            }
                                        }
                                    }
                                    "Application status updated to $newStatus"
                                } else {
                                    "Failed to update application status."
                                }
                            }
                        },
                        viewerCompanyName = loggedInUser?.companyName?.ifBlank { loggedInUser?.name }.orEmpty()
                    )
                } else {
                    JobseekerApplicationDetailScreen(
                        applicationId = applicationId,
                        viewModel = appliedViewModel,
                        jobLocation = appliedViewModel.applicationsList.find { it.id == applicationId }?.let { app ->
                            app.location.ifBlank {
                                homeViewModel.jobsList.find { it.id == app.jobId }?.location.orEmpty()
                            }
                        },
                        onBackClick = { navController.popBackStack() },
                        onChatWithEmployerClick = { application ->
                            startOrOpenChat(
                                counterpartId = application.employerId.ifBlank { "employer_default" },
                                counterpartName = application.companyName,
                                companyName = application.companyName,
                                jobTitle = application.jobTitle,
                                progressKey = application.id
                            )
                        },
                        onCancelApplication = { application ->
                            appliedViewModel.cancelApplication(application.id) { success ->
                                snackbarMessage = if (success) {
                                    "Application cancelled"
                                } else {
                                    "Failed to cancel application."
                                }
                                if (success) navController.popBackStack()
                            }
                        }
                    )
                }
            }

            composable(Screen.Schedule.route) {
                val currentUserId = loggedInUser?.id.orEmpty()
                val isEmployer = loggedInUser?.role == UserRole.EMPLOYER

                LaunchedEffect(currentUserId) {
                    if (currentUserId.isNotBlank()) {
                        scheduleViewModel.loadSchedules(currentUserId, isEmployer)
                        if (isEmployer) {
                            appliedViewModel.loadEmployerApplications(currentUserId)
                        }
                    }
                }

                ScheduleScreen(
                    navController = navController,
                    user = loggedInUser,
                    schedules = scheduleViewModel.schedulesList,
                    isLoading = scheduleViewModel.isLoading,
                    isEmployer = isEmployer,
                    isSaving = scheduleViewModel.isSaving,
                    prefill = scheduleViewModel.schedulePrefill,
                    applicants = appliedViewModel.applicationsList,
                    onCreateSchedule = { newSchedule ->
                        scheduleViewModel.createSchedule(newSchedule, currentUserId, isEmployer) { success, message ->
                            snackbarMessage = message
                            if (success) {
                                coroutineScope.launch {
                                    val company = loggedInUser?.companyName
                                        ?.ifBlank { loggedInUser?.name }
                                        .orEmpty()
                                        .ifBlank { "A company" }
                                    NotificationRepository.notifyUser(
                                        userId = newSchedule.userId,
                                        title = "Interview scheduled",
                                        body = "$company scheduled an interview for ${newSchedule.title.ifBlank { "a role" }} on ${newSchedule.date} at ${newSchedule.time}.",
                                        type = NotificationType.INTERVIEW_SCHEDULED,
                                        relatedId = newSchedule.jobId
                                    )
                                }
                            }
                        }
                    },
                    onUpdateStatus = { scheduleId, status ->
                        scheduleViewModel.updateScheduleStatus(scheduleId, status, currentUserId, isEmployer) { success ->
                            snackbarMessage = if (success) "Schedule status updated to $status." else "Failed to update schedule status."
                        }
                    },
                    onRespondInvite = { scheduleId, status ->
                        scheduleViewModel.updateScheduleStatus(scheduleId, status, currentUserId, isEmployer) { success ->
                            snackbarMessage = if (success) "Interview invitation $status successfully." else "Failed to update interview invitation response."
                        }
                    },
                    onUpdateSchedule = { updated ->
                        scheduleViewModel.updateSchedule(updated, currentUserId, isEmployer) { success ->
                            if (success && updated.status.equals("Pending", ignoreCase = true)) {
                                coroutineScope.launch {
                                    val company = loggedInUser?.companyName
                                        ?.ifBlank { loggedInUser?.name }
                                        .orEmpty()
                                        .ifBlank { "A company" }
                                    NotificationRepository.notifyUser(
                                        userId = updated.userId,
                                        title = "Interview updated",
                                        body = "$company updated your interview for ${updated.title.ifBlank { "a role" }} on ${updated.date} at ${updated.time}.",
                                        type = NotificationType.INTERVIEW_SCHEDULED,
                                        relatedId = updated.jobId
                                    )
                                }
                                snackbarMessage = "Interview updated and resent to the candidate."
                            } else {
                                snackbarMessage = if (success) "Schedule updated." else "Failed to update schedule."
                            }
                        }
                    },
                    onDeleteSchedule = { scheduleId ->
                        scheduleViewModel.deleteSchedule(scheduleId, currentUserId, isEmployer) { success ->
                            snackbarMessage = if (success) "Interview removed." else "Couldn't delete this interview. Try again."
                        }
                    },
                    onClearPrefill = { scheduleViewModel.clearPrefill() },
                    onProfileClick = { navController.navigate("profile") }
                )
            }

            // FIX: this destination was referenced by ScheduleScreen
            // (navController.navigate(Screen.ScheduleDetail.createRoute(...)))
            // but was never registered here, so tapping a schedule card
            // crashed with "Navigation destination that matches request ...
            // cannot be found in the navigation graph". Added below.
            composable(
                route = Screen.ScheduleDetail.route,
                arguments = listOf(navArgument("scheduleId") { type = NavType.StringType })
            ) { backStackEntry ->
                val scheduleId = backStackEntry.arguments?.getString("scheduleId") ?: ""
                val currentUserId = loggedInUser?.id.orEmpty()
                val isEmployer = loggedInUser?.role == UserRole.EMPLOYER
                val schedule = scheduleViewModel.schedulesList.find { it.id == scheduleId }

                ScheduleDetailScreen(
                    schedule = schedule,
                    isEmployer = isEmployer,
                    isSaving = scheduleViewModel.isSaving,
                    currentUserId = currentUserId,
                    defaultCompany = loggedInUser?.companyName?.ifBlank { loggedInUser?.name }.orEmpty(),
                    applicants = appliedViewModel.applicationsList,
                    onBackClick = { navController.popBackStack() },
                    onUpdateStatus = { targetScheduleId, status ->
                        scheduleViewModel.updateScheduleStatus(targetScheduleId, status, currentUserId, isEmployer) { success ->
                            snackbarMessage = if (success) "Schedule status updated to $status." else "Failed to update schedule status."
                        }
                    },
                    onRespondInvite = { targetScheduleId, status ->
                        scheduleViewModel.updateScheduleStatus(targetScheduleId, status, currentUserId, isEmployer) { success ->
                            snackbarMessage = if (success) "Interview invitation $status successfully." else "Failed to update interview invitation response."
                        }
                    },
                    onUpdateSchedule = { updated ->
                        scheduleViewModel.updateSchedule(updated, currentUserId, isEmployer) { success ->
                            if (success && updated.status.equals("Pending", ignoreCase = true)) {
                                coroutineScope.launch {
                                    val company = loggedInUser?.companyName
                                        ?.ifBlank { loggedInUser?.name }
                                        .orEmpty()
                                        .ifBlank { "A company" }
                                    NotificationRepository.notifyUser(
                                        userId = updated.userId,
                                        title = "Interview updated",
                                        body = "$company updated your interview for ${updated.title.ifBlank { "a role" }} on ${updated.date} at ${updated.time}.",
                                        type = NotificationType.INTERVIEW_SCHEDULED,
                                        relatedId = updated.jobId
                                    )
                                }
                                snackbarMessage = "Interview updated and resent to the candidate."
                            } else {
                                snackbarMessage = if (success) {
                                    if (updated.status.equals("Reschedule Requested", ignoreCase = true)) {
                                        "Reschedule request sent."
                                    } else {
                                        "Schedule updated."
                                    }
                                } else {
                                    "Failed to update schedule."
                                }
                            }
                        }
                    },
                    onDeleteSchedule = { targetScheduleId ->
                        scheduleViewModel.deleteSchedule(targetScheduleId, currentUserId, isEmployer) { success ->
                            snackbarMessage = if (success) "Interview removed." else "Couldn't delete this interview. Try again."
                            if (success) navController.popBackStack()
                        }
                    }
                )
            }

            composable(Screen.Chat.route) {
                val currentUserId = loggedInUser?.id.orEmpty()
                LaunchedEffect(currentUserId) {
                    if (currentUserId.isNotBlank()) {
                        chatViewModel.loadUserChatRooms(currentUserId)
                    }
                }

                ChatListScreen(
                    currentUser = loggedInUser,
                    chatRooms = chatRoomsList,
                    isLoading = isChatLoading,
                    onChatRoomClick = { roomId, titleName, position ->
                        navController.navigate(
                            Screen.ChatDetail.createRoute(
                                chatRoomId = roomId,
                                company = titleName,
                                title = position
                            )
                        )
                    },
                    onProfileClick = { navController.navigate("profile") }
                )
            }

            composable(
                route = Screen.ChatDetail.route,
                arguments = listOf(
                    navArgument("chatRoomId") { type = NavType.StringType },
                    navArgument("company") { type = NavType.StringType; defaultValue = "" },
                    navArgument("title") { type = NavType.StringType; defaultValue = "" },
                    navArgument("initialQuestion") { type = NavType.StringType; defaultValue = "" }
                )
            ) { backStackEntry ->
                val chatId = backStackEntry.arguments?.getString("chatRoomId") ?: ""
                val company = Uri.decode(backStackEntry.arguments?.getString("company").orEmpty())
                val title = Uri.decode(backStackEntry.arguments?.getString("title").orEmpty())
                val initialQuestion = Uri.decode(backStackEntry.arguments?.getString("initialQuestion").orEmpty())

                ChatDetailScreen(
                    navController = navController,
                    roomId = chatId,
                    companyName = company,
                    chatTitle = title,
                    initialQuestion = initialQuestion,
                    currentUserId = loggedInUser?.id.orEmpty(),
                    chatViewModel = chatViewModel,
                    onNavigateToSchedule = { navController.navigate(Screen.Schedule.route) }
                )
            }

            composable(Screen.Notifications.route) {
                NotificationsScreen(
                    userId = loggedInUser?.id.orEmpty(),
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }

    if (showInterviewEditor) {
        val employer = loggedInUser
        InterviewEditorScreen(
            isEdit = false,
            isSaving = scheduleViewModel.isSaving,
            currentUserId = employer?.id.orEmpty(),
            defaultCompany = employer?.companyName?.ifBlank { employer.name }.orEmpty(),
            applicants = appliedViewModel.applicationsList,
            prefill = scheduleViewModel.schedulePrefill,
            onDismiss = {
                if (!scheduleViewModel.isSaving) {
                    showInterviewEditor = false
                    scheduleViewModel.clearPrefill()
                }
            },
            onSave = { newSchedule ->
                val currentUserId = employer?.id.orEmpty()
                val isEmployerUser = employer?.role == UserRole.EMPLOYER
                scheduleViewModel.createSchedule(newSchedule, currentUserId, isEmployerUser) { success, message ->
                    snackbarMessage = message
                    if (success) {
                        showInterviewEditor = false
                        coroutineScope.launch {
                            val company = employer?.companyName
                                ?.ifBlank { employer.name }
                                .orEmpty()
                                .ifBlank { "A company" }
                            NotificationRepository.notifyUser(
                                userId = newSchedule.userId,
                                title = "Interview scheduled",
                                body = "$company scheduled an interview for ${newSchedule.title.ifBlank { "a role" }} on ${newSchedule.date} at ${newSchedule.time}.",
                                type = NotificationType.INTERVIEW_SCHEDULED,
                                relatedId = newSchedule.jobId
                            )
                        }
                    }
                }
            }
        )
    }
}