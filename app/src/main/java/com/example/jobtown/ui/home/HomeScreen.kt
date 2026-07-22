package com.example.jobtown.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.jobtown.Screen
import com.example.jobtown.data.Job
import com.example.jobtown.data.User
import com.example.jobtown.ui.theme.DarkTextPurple
import com.example.jobtown.ui.theme.DeepGreenDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    jobs: List<Job>,
    currentUser: User?,
    onSearch: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val isEmployer = currentUser?.role == "company"

    Scaffold(
        // Floating '+' action button positioned above the bottom navigation bar
        floatingActionButton = {
            if (isEmployer) {
                FloatingActionButton(
                    onClick = { navController.navigate(Screen.PostJob.route) },
                    containerColor = Color(0xFFA1C695),
                    contentColor = DeepGreenDark,
                    shape = CircleShape,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Post New Job"
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFFAFAFA))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Header Section showing Username and Profile Icon
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFFA1C695),
                    shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "JobTown",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepGreenDark
                        )

                        // Displays Username & Profile Avatar
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                navController.navigate(Screen.Profile.route)
                            }
                        ) {
                            Text(
                                text = currentUser?.name ?: "Guest",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DarkTextPurple
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Surface(
                                modifier = Modifier.size(36.dp),
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.5f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Profile",
                                        tint = DeepGreenDark,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Search Bar Section
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            onSearch(it)
                        },
                        placeholder = {
                            Text(if (isEmployer) "Search your posted jobs..." else "Search jobs...")
                        },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Color.Gray)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color.White,
                            focusedContainerColor = Color.White
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = if (isEmployer) "My Job Postings (${jobs.size})" else "Available Jobs (${jobs.size})",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepGreenDark
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Job Items List
                if (jobs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isEmployer) "No job postings created yet." else "No jobs available.",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(jobs) { job ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { navController.navigate("job_detail/${job.id}") },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = job.title,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DeepGreenDark
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = job.company,
                                        fontSize = 14.sp,
                                        color = DarkTextPurple
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}