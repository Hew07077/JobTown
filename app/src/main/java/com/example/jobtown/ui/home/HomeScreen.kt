package com.example.jobtown.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.jobtown.data.Job
import com.example.jobtown.data.User
import com.example.jobtown.data.UserRole
import com.example.jobtown.ui.components.JobCard
import com.example.jobtown.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    navController: NavController,
    currentUser: User?,
    jobsList: List<Job>,
    isLoading: Boolean,
    onJobClick: (Job) -> Unit,
    onPostJobClick: () -> Unit,
    onProfileClick: () -> Unit,
    onRefresh: () -> Unit = {},
    matchScores: Map<String, Int> = emptyMap(),
    sortMode: JobSortMode = JobSortMode.NEWEST,
    onSortModeChange: (JobSortMode) -> Unit = {},
    hasMatchProfile: Boolean = false,
    isLive: Boolean = false,
    newListingsAvailable: Int = 0,
    onShowNewListings: () -> Unit = {},
    onStartRealtime: () -> Unit = {},
    onStopRealtime: () -> Unit = {}
) {
    // Re-fetch data on screen load and manage real-time updates lifecycle
    LaunchedEffect(currentUser?.id) {
        onRefresh()
    }

    DisposableEffect(Unit) {
        onStartRealtime()
        onDispose { onStopRealtime() }
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }

    val isEmployer = currentUser?.role == UserRole.EMPLOYER

    // Greeting name helper
    val greetingName = (if (isEmployer) currentUser?.companyName else currentUser?.name)
        .orEmpty()
        .ifBlank { "Job Finder" }

    // Helper to check if a job is expired based on explicit status or `expiredAt` ISO timestamp
    fun isJobExpired(job: Job): Boolean {
        if (job.status?.equals("expired", ignoreCase = true) == true) return true
        val expiredAtStr = job.expiredAt ?: return false
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            val expiryDate = sdf.parse(expiredAtStr)
            expiryDate != null && Date().after(expiryDate)
        } catch (e: Exception) {
            false
        }
    }

    // STRICT ROLE SCOPING:
    // Employers see only their own posted jobs. Job Seekers see all listings.
    val roleScopedJobs = remember(jobsList, currentUser?.id, currentUser?.companyName, currentUser?.name, isEmployer) {
        if (isEmployer) {
            val currentUserId = currentUser?.id.orEmpty()
            val userCompanyName = currentUser?.companyName.orEmpty().ifBlank { currentUser?.name.orEmpty() }

            jobsList.filter { job ->
                val employerId = job.employerId.orEmpty()
                val postedByUserId = job.postedByUserId.orEmpty()
                val ownerId = employerId.ifBlank { postedByUserId }
                val jobCompany = job.company.orEmpty()

                when {
                    currentUserId.isNotBlank() && ownerId.isNotBlank() -> ownerId == currentUserId
                    userCompanyName.isNotBlank() && jobCompany.isNotBlank() -> jobCompany.equals(userCompanyName, ignoreCase = true)
                    else -> false
                }
            }
        } else {
            jobsList
        }
    }

    // Comprehensive category and job type filters including EXPIRED tab
    val categories = listOf("All", "Full-Time", "Part-Time", "Contract", "Internship", "Freelance", "Remote", "Expired")

    // Search and Category/Expired Filter Logic
    val filteredJobs = remember(roleScopedJobs, searchQuery, selectedFilter) {
        roleScopedJobs.filter { job ->
            val jobType = job.type.orEmpty()
            val jobExpired = isJobExpired(job)

            val matchesFilter = when (selectedFilter.lowercase()) {
                "all" -> !jobExpired // Default ALL hides expired items to keep feed clean
                "expired" -> jobExpired
                "remote" -> !jobExpired && (jobType.contains("remote", ignoreCase = true) || job.location.orEmpty().contains("remote", ignoreCase = true))
                else -> !jobExpired && jobType.replace("-", " ").contains(selectedFilter.replace("-", " "), ignoreCase = true)
            }

            val query = searchQuery.trim().lowercase()
            val title = job.title.orEmpty().lowercase()
            val company = job.company.orEmpty().lowercase()
            val location = job.location.orEmpty().lowercase()

            val matchesSearch = query.isEmpty() ||
                    title.contains(query) ||
                    company.contains(query) ||
                    location.contains(query)

            matchesFilter && matchesSearch
        }
    }

    // Sort order logic
    val displayedJobs = remember(filteredJobs, sortMode, matchScores) {
        if (!isEmployer && sortMode == JobSortMode.BEST_MATCH && matchScores.isNotEmpty()) {
            filteredJobs.sortedByDescending { matchScores[it.id] ?: 0 }
        } else {
            filteredJobs.sortedByDescending { it.createdAt.orEmpty() }
        }
    }

    Scaffold(
        containerColor = BackgroundWhite,
        floatingActionButton = {
            if (isEmployer) {
                ExtendedFloatingActionButton(
                    onClick = onPostJobClick,
                    containerColor = DeepGreenDark,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null
                        )
                    },
                    text = {
                        Text(
                            text = "Add Job",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    },
                    modifier = Modifier.semantics { contentDescription = "Post a new job" }
                )
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(BackgroundWhite)
        ) {
            // --- HEADER SECTION ---
            Surface(
                color = SageGreenMain,
                shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Welcome back 👋",
                                    fontSize = 13.sp,
                                    color = DeepGreenDark.copy(alpha = 0.8f)
                                )
                                if (isLive) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.semantics {
                                            contentDescription = "Live updates connected"
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FiberManualRecord,
                                            contentDescription = null,
                                            tint = Color(0xFF1B7A3D),
                                            modifier = Modifier.size(8.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "Live",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1B7A3D)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = greetingName,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                        }

                        IconButton(
                            onClick = onProfileClick,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(SageGreenLight)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Open your profile",
                                tint = DeepGreenDark
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search jobs, companies...", fontSize = 14.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = SageGreenDark)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = SageGreenDark)
                                }
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = DeepGreenDark,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Search jobs, companies, or locations" }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- FILTER CHIPS BAR (Job Types + Expired) ---
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    val isSelected = selectedFilter.equals(category, ignoreCase = true)
                    val isExpiredTab = category.equals("Expired", ignoreCase = true)

                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = category },
                        label = {
                            Text(
                                text = category,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (isExpiredTab) MaterialTheme.colorScheme.error else DeepGreenDark,
                            selectedLabelColor = Color.White,
                            containerColor = Color.White,
                            labelColor = if (isExpiredTab) MaterialTheme.colorScheme.error else TextDark
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isExpiredTab) MaterialTheme.colorScheme.error.copy(alpha = 0.4f) else SageGreenDark.copy(alpha = 0.3f),
                            selectedBorderColor = if (isExpiredTab) MaterialTheme.colorScheme.error else DeepGreenDark
                        ),
                        modifier = Modifier.semantics {
                            contentDescription = "Filter by $category" + if (isSelected) ", selected" else ""
                        }
                    )
                }
            }

            // --- SORT TOGGLE (Job Seekers only) ---
            if (!isEmployer && hasMatchProfile) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        JobSortMode.BEST_MATCH to "Best Match",
                        JobSortMode.NEWEST to "Newest"
                    ).forEach { (mode, label) ->
                        val isSelected = sortMode == mode
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSortModeChange(mode) },
                            label = { Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                            shape = RoundedCornerShape(20.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SageGreenDark,
                                selectedLabelColor = Color.White,
                                containerColor = SageGreenLight,
                                labelColor = DeepGreenDark
                            ),
                            modifier = Modifier.semantics {
                                contentDescription = "Sort by $label" + if (isSelected) ", selected" else ""
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- SECTION HEADER ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when {
                        selectedFilter.equals("Expired", ignoreCase = true) -> "Expired Jobs"
                        isEmployer -> "Your Posted Jobs"
                        else -> "Recommended Jobs"
                    },
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                Text(
                    text = "${displayedJobs.size} ${if (isEmployer) "Posted" else "Available"}",
                    fontSize = 13.sp,
                    color = TextDark.copy(alpha = 0.5f),
                    modifier = Modifier.semantics {
                        liveRegion = LiveRegionMode.Polite
                        contentDescription = "${displayedJobs.size} jobs ${if (isEmployer) "posted" else "available"}"
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // --- JOBS LIST / EMPTY STATE / LOADING ---
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .semantics { contentDescription = "Loading jobs" },
                        color = DeepGreenDark
                    )
                } else if (displayedJobs.isEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🔍", fontSize = 36.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (selectedFilter.equals("Expired", ignoreCase = true)) "No Expired Jobs Found"
                                else if (isEmployer) "No Jobs Posted Yet"
                                else "No Jobs Found",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isEmployer) {
                                    "Tap '+ Add Job' to post your company's first listing."
                                } else {
                                    "Try clearing your search query or switching filters."
                                },
                                fontSize = 13.sp,
                                color = TextDark.copy(alpha = 0.6f)
                            )
                        }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Realtime banner (Seekers)
                        if (!isEmployer && newListingsAvailable > 0) {
                            Surface(
                                onClick = onShowNewListings,
                                shape = RoundedCornerShape(14.dp),
                                color = DeepGreenDark,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 10.dp)
                                    .semantics {
                                        contentDescription = "$newListingsAvailable new job listing" +
                                                (if (newListingsAvailable > 1) "s" else "") + " available. Double tap to show."
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowUp,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "$newListingsAvailable new job" + (if (newListingsAvailable > 1) "s" else "") + " posted",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            items(
                                items = displayedJobs,
                                key = { job ->
                                    val id = job.id.orEmpty()
                                    if (id.isNotBlank()) id else "${job.title}_${job.company}_${job.createdAt}"
                                }
                            ) { job ->
                                JobCard(
                                    job = job,
                                    onClick = {
                                        onJobClick(job)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}