package com.example.jobtown.ui.navigation

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
import com.example.jobtown.ui.auth.SignUpFields
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

    // Shared draft for the two-step signup flow (SignUpScreen -> CompleteProfileScreen).
    // Owned here (not by either screen) so navigating back and forth between the
    // two steps never loses what was typed. Nothing here touches Supabase --
    // only the final "Save & Continue" on CompleteProfileScreen does that.
    var signupDraft by remember { mutableStateOf(SignUpFields()) }

    // Tracks which application is currently spinning up a chat room, so the
    // "Chat with Employer" button can show a spinner and can't be double-tapped
    // into creating duplicate rooms. null == nothing in flight.
    var chatCreationInProgressId by remember { mutableStateOf<String?>(null) }

    // Surfaced via a Snackbar when chat-room creation fails (e.g. no network),
    // instead of silently logging and leaving the user stuck on a dead button.
    var chatErrorMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(chatErrorMessage) {
        chatErrorMessage?.let {
            snackbarHostState.showSnackbar(message = it, duration = SnackbarDuration.Long)
            chatErrorMessage = null
        }
    }

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

            // --- SIGNUP (step 1: personal / company details) ---
            composable("signup") {
                SignUpScreen(
                    draft = signupDraft,
                    onDraftChange = { signupDraft = it },
                    onNextClick = {
                        // Deliberately NOT popping "signup" off the back stack here --
                        // that's what used to make the Back button on the next screen
                        // impossible to wire up. All the typed data already lives in
                        // signupDraft, so nothing is lost either way.
                        navController.navigate("complete_profile")
                    },
                    onLoginClick = {
                        // Leaving the signup flow entirely -- clear the cached draft.
                        signupDraft = SignUpFields()
                        navController.popBackStack()
                    }
                )
            }

            // --- COMPLETE PROFILE (step 2: professional details, post-signup) ---
            composable("complete_profile") {
                // Built fresh from the shared draft every recomposition -- this is
                // what gets passed to Supabase Auth once the user hits Save.
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
                    onBack = {
                        // Just pop back to SignUpScreen -- the draft is untouched,
                        // so whatever was typed on this screen is still there if
                        // the user comes forward again.
                        navController.popBackStack()
                    },
                    onComplete = { completedUser ->
                        loggedInUser = completedUser
                        signupDraft = SignUpFields() // Flow finished -- clear the cache.
                        navController.navigate(Screen.Home.route) {
                            popUpTo("signup") { inclusive = true }
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
                // Belt-and-suspenders: employers have no apply action, so even if this
                // route were reached some other way (deep link, restored back stack)
                // it still won't let an employer submit an application.
                val isEmployer = loggedInUser?.role == UserRole.EMPLOYER
                if (selectedJob != null && !isEmployer) {
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
                    chatLoadingApplicationId = chatCreationInProgressId,
                    onChatWithCompany = { application ->
                        Log.d("AppNavGraph", "Chat with Employer tapped for application=${application.id}")

                        // Previously this was `val userId = loggedInUser?.id ?: return@MyAppliedScreen`,
                        // which silently did nothing if loggedInUser was null -- the button would
                        // appear to not respond at all. Now every dead end shows a Snackbar.
                        val userId = loggedInUser?.id
                        if (userId.isNullOrBlank()) {
                            Log.e("AppNavGraph", "Chat tapped but no logged-in user id available.")
                            chatErrorMessage = "You need to be logged in to start a chat."
                            return@MyAppliedScreen
                        }

                        // Ignore taps while a room is already being created (for this
                        // or any other application) to avoid firing duplicate inserts.
                        if (chatCreationInProgressId != null) {
                            Log.d("AppNavGraph", "Chat creation already in progress, ignoring tap.")
                            return@MyAppliedScreen
                        }

                        val userName = loggedInUser?.name?.ifBlank { loggedInUser?.email ?: "" } ?: ""
                        chatCreationInProgressId = application.id

                        coroutineScope.launch {
                            var caughtErrorText: String? = null

                            val roomId = try {
                                messageRepository.getOrCreateChatRoom(
                                    seekerId = userId,
                                    seekerName = userName,
                                    // Real employer user id, carried over from the Job when the
                                    // application was submitted -- NOT the application's own id.
                                    // Falls back to a shared placeholder only for older
                                    // applications that predate this field.
                                    employerId = application.employerId.ifBlank { "employer_default" },
                                    companyName = application.companyName
                                )
                            } catch (e: Exception) {
                                Log.e("AppNavGraph", "Error creating chat room", e)
                                // Surface the actual Postgrest/Supabase error text on-screen
                                // (see the Snackbar below) instead of requiring Logcat/adb to
                                // see what went wrong.
                                caughtErrorText = e.message ?: e.toString()
                                ""
                            }

                            Log.d("AppNavGraph", "getOrCreateChatRoom returned roomId='$roomId'")
                            chatCreationInProgressId = null

                            if (roomId.isNotBlank()) {
                                // Auto-create/refresh the entry in the Messages tab so the
                                // conversation is there the moment the user lands on it.
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
                                // roomId comes back blank both when getOrCreateChatRoom throws
                                // (see catch above -- usually a missing/misconfigured Supabase
                                // "chat_rooms" table or an RLS policy blocking the insert/select)
                                // and when seekerId was blank. Either way, surface it instead of
                                // leaving the user staring at a button that "did nothing".
                                Log.e("AppNavGraph", "Could not create or retrieve chat room.")
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