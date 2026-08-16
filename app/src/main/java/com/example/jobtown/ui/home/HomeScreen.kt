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
    // Re-fetch data on screen load to keep UI synced, and keep the real-time
    // job-listings subscription alive only while this screen is visible.
    LaunchedEffect(Unit) {
        onRefresh()
    }
    DisposableEffect(Unit) {
        onStartRealtime()
        onDispose { onStopRealtime() }
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }

    val isEmployer = currentUser?.role == UserRole.EMPLOYER

    // Scope jobs for employers with full null safety using .orEmpty()
    val roleScopedJobs = remember(jobsList, currentUser?.id, currentUser?.name, currentUser?.companyName, isEmployer) {
        if (isEmployer) {
            val currentUserId = currentUser?.id.orEmpty()
            val userName = currentUser?.name.orEmpty()
            val companyName = currentUser?.companyName.orEmpty()
            val userCompanyName = companyName.ifBlank { userName }

            jobsList.filter { job ->
                val employerId = job.employerId.orEmpty()
                val postedByUserId = job.postedByUserId.orEmpty()
                val ownerId = employerId.ifBlank { postedByUserId }
                val jobCompany = job.company.orEmpty()

                when {
                    currentUserId.isNotBlank() && ownerId.isNotBlank() -> ownerId == currentUserId
                    userCompanyName.isNotBlank() -> jobCompany.equals(userCompanyName, ignoreCase = true)
                    else -> true
                }
            }
        } else {
            jobsList
        }
    }

    val categories = listOf("All", "Full-Time", "Part-Time", "Remote")

    // Filter Logic with safe null checks
    val filteredJobs = remember(roleScopedJobs, searchQuery, selectedFilter) {
        roleScopedJobs.filter { job ->
            val jobType = job.type.orEmpty()
            val matchesFilter = when (selectedFilter.lowercase()) {
                "all" -> true
                else -> jobType.replace("-", " ")
                    .contains(selectedFilter.replace("-", " "), ignoreCase = true)
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

    // Best job matching: when the seeker has a profile, "Recommended Jobs" is sorted so the
    // strongest matches surface first; a toggle lets them switch to a strict newest-first view.
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
                                    text = "Welcome back \uD83D\uDC4B",
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
                                text = currentUser?.name.orEmpty().ifBlank { "Job Finder" },
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

            // --- FILTER CHIPS ---
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    val isSelected = selectedFilter.equals(category, ignoreCase = true)
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
                            selectedContainerColor = DeepGreenDark,
                            selectedLabelColor = Color.White,
                            containerColor = Color.White,
                            labelColor = TextDark
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = SageGreenDark.copy(alpha = 0.3f),
                            selectedBorderColor = DeepGreenDark
                        ),
                        modifier = Modifier.semantics {
                            contentDescription = "Filter by $category" + if (isSelected) ", selected" else ""
                        }
                    )
                }
            }

            // --- SORT TOGGLE (job seekers only, once a profile exists to match against) ---
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
                    text = if (isEmployer) "Your Posted Jobs" else "Recommended Jobs",
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
                            Text("\uD83D\uDD0D", fontSize = 36.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isEmployer) "No Jobs Posted Yet" else "No Jobs Found",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isEmployer) {
                                    "Jobs you post will show up here."
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
                        // Updated job listings: new postings arriving in real time are staged
                        // here instead of jumping the list around, so the seeker chooses when
                        // to bring them into view.
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
                                    val jobId = job.id.orEmpty()
                                    if (jobId.isNotEmpty()) jobId else (job.title.orEmpty() + job.company.orEmpty())
                                }
                            ) { job ->
                                JobCard(
                                    job = job,
                                    onClick = {
                                        if (!isEmployer) {
                                            onJobClick(job)
                                        }
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
