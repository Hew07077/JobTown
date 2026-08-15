package com.example.jobtown.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.jobtown.data.Job
import com.example.jobtown.data.repository.JobRepository
import kotlinx.coroutines.launch

class HomeViewModel(private val repository: JobRepository) : ViewModel() {

    var jobsList by mutableStateOf<List<Job>>(emptyList())
        private set

    // List specifically tracking "Your Posted Jobs" for employers
    var myPostedJobs by mutableStateOf<List<Job>>(emptyList())
        private set

    var selectedJob by mutableStateOf<Job?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    init {
        loadJobs()
    }

    /**
     * Loads all jobs from Supabase repository.
     * Optionally filters user-posted jobs if currentUserId is provided.
     */
    fun loadJobs(currentUserId: String? = null) {
        viewModelScope.launch {
            isLoading = true
            try {
                val fetchedJobs = repository.getAllJobs()
                jobsList = fetchedJobs

                // Load employer's posted jobs if logged in
                if (!currentUserId.isNullOrBlank()) {
                    val userJobs = repository.getJobsByUserId(currentUserId)
                    // If remote filter returns empty, fallback to local filtering from fetchedJobs
                    myPostedJobs = if (userJobs.isNotEmpty()) {
                        userJobs
                    } else {
                        fetchedJobs.filter { job ->
                            val ownerId = job.employerId.orEmpty().ifBlank { job.postedByUserId.orEmpty() }
                            ownerId == currentUserId
                        }
                    }
                } else {
                    myPostedJobs = emptyList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    fun selectJob(job: Job) {
        selectedJob = job
    }

    /**
     * Optimistically prepends the newly posted job to both `jobsList` and `myPostedJobs`
     * so it renders immediately on HomeScreen, then persists it in Supabase repository.
     */
    fun addJob(newJob: Job) {
        // 1. Optimistic UI update: Instantly update state for seamless UX
        jobsList = listOf(newJob) + jobsList.filter { it.id != newJob.id }
        myPostedJobs = listOf(newJob) + myPostedJobs.filter { it.id != newJob.id }

        // 2. Persist asynchronously in backend database
        viewModelScope.launch {
            try {
                repository.addJob(newJob)
            } catch (e: Exception) {
                e.printStackTrace()
                // If backend save fails, reload to reconcile state
                loadJobs(newJob.postedByUserId ?: newJob.employerId)
            }
        }
    }
}

class HomeViewModelFactory(private val repository: JobRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}