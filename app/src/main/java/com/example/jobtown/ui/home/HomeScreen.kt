package com.example.jobtown.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    onRefresh: () -> Unit = {}
) {
    // Re-fetch data on screen load to keep UI synced
    LaunchedEffect(Unit) {
        onRefresh()
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }

    // Employers only ever manage their own postings here -- they don't apply
    // to jobs, so they shouldn't see (or be able to tap into) the wider
    // marketplace of everyone else's listings.
    val isEmployer = currentUser?.role == UserRole.EMPLOYER

    val roleScopedJobs = remember(jobsList, currentUser?.id, isEmployer) {
        if (isEmployer) {
            jobsList.filter { job ->
                val ownerId = job.employerId?.ifBlank { null } ?: job.postedByUserId
                !ownerId.isNullOrBlank() && ownerId == currentUser?.id
            }
        } else {
            jobsList
        }
    }

    val categories = listOf("All", "Full-Time", "Part-Time", "Remote")

    // Filter Logic with flexible matching
    val filteredJobs = remember(roleScopedJobs, searchQuery, selectedFilter) {
        roleScopedJobs.filter { job ->
            val matchesFilter = when (selectedFilter.lowercase()) {
                "all" -> true
                else -> job.type.replace("-", " ")
                    .contains(selectedFilter.replace("-", " "), ignoreCase = true)
            }

            val query = searchQuery.trim().lowercase()
            val matchesSearch = query.isEmpty() ||
                    job.title.lowercase().contains(query) ||
                    job.company.lowercase().contains(query) ||
                    job.location.lowercase().contains(query)

            matchesFilter && matchesSearch
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
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
                        Text(
                            text = "Welcome back 👋",
                            fontSize = 13.sp,
                            color = DeepGreenDark.copy(alpha = 0.8f)
                        )
                        Text(
                            text = currentUser?.name?.ifBlank { "Job Finder" } ?: "Job Finder",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                    }

                    IconButton(
                        onClick = onProfileClick,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(SageGreenLight)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile",
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
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear Search", tint = SageGreenDark)
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
                    modifier = Modifier.fillMaxWidth()
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
                    )
                )
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
                text = "${filteredJobs.size} ${if (isEmployer) "Posted" else "Available"}",
                fontSize = 13.sp,
                color = TextDark.copy(alpha = 0.5f)
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
                    modifier = Modifier.align(Alignment.Center),
                    color = DeepGreenDark
                )
            } else if (filteredJobs.isEmpty()) {
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
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(
                        items = filteredJobs,
                        key = { job -> job.id.ifEmpty { job.title + job.company } }
                    ) { job ->
                        JobCard(
                            job = job,
                            onClick = {
                                // Employers manage/view their own postings here -- they
                                // don't have an "apply" action, so tapping a card is a
                                // no-op for them instead of launching ApplyJobScreen.
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