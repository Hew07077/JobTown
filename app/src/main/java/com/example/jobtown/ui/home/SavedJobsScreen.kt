package com.example.jobtown.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.jobtown.data.model.Job
import com.example.jobtown.ui.components.JobCard
import com.example.jobtown.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedJobsScreen(
    navController: NavController,
    allJobs: List<Job>,
    savedJobIds: Set<String>,
    onJobClick: (Job) -> Unit,
    onToggleSaveJob: (String) -> Unit
) {
    val savedJobs = allJobs.filter { savedJobIds.contains(it.id.orEmpty()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Saved Jobs", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = DeepGreenDark)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SageGreenMain,
                    titleContentColor = DeepGreenDark
                )
            )
        },
        containerColor = BackgroundWhite
    ) { padding ->
        if (savedJobs.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.BookmarkBorder,
                    contentDescription = null,
                    tint = SageGreenDark,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "No saved jobs yet",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Tap the bookmark icon on a job to save it for later.",
                    fontSize = 13.sp,
                    color = TextDark.copy(alpha = 0.6f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items = savedJobs, key = { it.id.orEmpty() }) { job ->
                    JobCard(
                        job = job,
                        onClick = { onJobClick(job) },
                        isSaved = true,
                        onToggleSave = {
                            val id = job.id.orEmpty()
                            if (id.isNotBlank()) onToggleSaveJob(id)
                        }
                    )
                }
            }
        }
    }
}
