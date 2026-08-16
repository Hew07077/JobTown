package com.example.jobtown.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.example.jobtown.data.Job
import com.example.jobtown.data.UserProfile
import com.example.jobtown.data.repository.JobListingEvent
import com.example.jobtown.data.repository.JobRepository
import com.example.jobtown.data.repository.UserRepository
import com.example.jobtown.utils.JobMatchUtils
import kotlinx.coroutines.Job as CoroutineJob
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

enum class JobSortMode { BEST_MATCH, NEWEST }

class HomeViewModel(private val repository: JobRepository) : ViewModel() {

    var jobsList by mutableStateOf<List<Job>>(emptyList())
        private set

    var myPostedJobs by mutableStateOf<List<Job>>(emptyList())
        private set

    var selectedJob by mutableStateOf<Job?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var isPostingJob by mutableStateOf(false)
        private set

    var seekerProfile by mutableStateOf<UserProfile?>(null)
        private set

    var matchScores by mutableStateOf<Map<String, Int>>(emptyMap())
        private set

    var sortMode by mutableStateOf(JobSortMode.BEST_MATCH)
        private set

    var isLive by mutableStateOf(false)
        private set

    var newListingsAvailable by mutableStateOf(0)
        private set

    private var realtimeJob: CoroutineJob? = null
    private var currentUserId: String? = null
    private var pendingNewJobs: List<Job> = emptyList()

    init {
        loadJobs()
    }

    fun loadJobs(currentUserId: String? = null) {
        this.currentUserId = currentUserId
        viewModelScope.launch {
            isLoading = true
            try {
                val fetchedJobs = repository.getAllJobs()
                jobsList = fetchedJobs
                pendingNewJobs = emptyList()
                newListingsAvailable = 0

                if (!currentUserId.isNullOrBlank()) {
                    val userJobs = repository.getJobsByUserId(currentUserId)
                    myPostedJobs = if (userJobs.isNotEmpty()) {
                        userJobs
                    } else {
                        fetchedJobs.filter { job ->
                            val ownerId = job.employerId.orEmpty().ifBlank { job.postedByUserId.orEmpty() }
                            ownerId == currentUserId
                        }
                    }
                    refreshMatchScores(currentUserId, fetchedJobs)
                } else {
                    myPostedJobs = emptyList()
                    seekerProfile = null
                    matchScores = emptyMap()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    private fun refreshMatchScores(userId: String, jobs: List<Job>) {
        viewModelScope.launch {
            try {
                val profile = UserRepository.fetchUserProfile(userId)
                seekerProfile = profile
                matchScores = jobs.associate { it.id to JobMatchUtils.score(it, profile).score }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error computing job match scores", e)
            }
        }
    }

    fun updateSortMode(mode: JobSortMode) {
        sortMode = mode
    }

    fun applySort(jobs: List<Job>): List<Job> = when (sortMode) {
        JobSortMode.BEST_MATCH -> if (seekerProfile != null) {
            jobs.sortedByDescending { matchScores[it.id] ?: 0 }
        } else {
            jobs
        }
        JobSortMode.NEWEST -> jobs.sortedByDescending { it.createdAt.orEmpty() }
    }

    fun startRealtimeUpdates() {
        if (realtimeJob != null) return
        isLive = true
        realtimeJob = viewModelScope.launch {
            repository.observeJobs()
                .catch { e -> Log.e("HomeViewModel", "Realtime job feed error", e) }
                .collect { event ->
                    when (event) {
                        is JobListingEvent.Upserted -> handleUpsert(event.job)
                        is JobListingEvent.Removed -> handleRemoval(event.jobId)
                    }
                }
        }
    }

    fun stopRealtimeUpdates() {
        realtimeJob?.cancel()
        realtimeJob = null
        isLive = false
    }

    private fun handleUpsert(job: Job) {
        val alreadyKnown = jobsList.any { it.id == job.id } || pendingNewJobs.any { it.id == job.id }
        if (alreadyKnown) {
            jobsList = jobsList.map { if (it.id == job.id) job else it }
            pendingNewJobs = pendingNewJobs.map { if (it.id == job.id) job else it }
            matchScores = matchScores + (job.id to JobMatchUtils.score(job, seekerProfile).score)
        } else {
            pendingNewJobs = listOf(job) + pendingNewJobs
            newListingsAvailable = pendingNewJobs.size
        }

        val ownerId = job.employerId.orEmpty().ifBlank { job.postedByUserId.orEmpty() }
        if (currentUserId != null && ownerId == currentUserId && myPostedJobs.none { it.id == job.id }) {
            myPostedJobs = listOf(job) + myPostedJobs
        }
    }

    private fun handleRemoval(jobId: String) {
        jobsList = jobsList.filterNot { it.id == jobId }
        pendingNewJobs = pendingNewJobs.filterNot { it.id == jobId }
        newListingsAvailable = pendingNewJobs.size
        myPostedJobs = myPostedJobs.filterNot { it.id == jobId }
    }

    fun showPendingListings() {
        if (pendingNewJobs.isEmpty()) return
        jobsList = pendingNewJobs + jobsList
        currentUserId?.let { uid -> refreshMatchScores(uid, jobsList) }
        pendingNewJobs = emptyList()
        newListingsAvailable = 0
    }

    fun selectJob(job: Job) {
        selectedJob = job
    }

    fun addJob(newJob: Job) {
        jobsList = listOf(newJob) + jobsList.filter { it.id != newJob.id }
        myPostedJobs = listOf(newJob) + myPostedJobs.filter { it.id != newJob.id }

        viewModelScope.launch {
            try {
                repository.addJob(newJob)
            } catch (e: Exception) {
                e.printStackTrace()
                loadJobs(newJob.postedByUserId ?: newJob.employerId)
            }
        }
    }

    fun postJob(job: Job, onResult: (success: Boolean, message: String?) -> Unit) {
        if (isPostingJob) return
        viewModelScope.launch {
            isPostingJob = true
            val inserted = repository.insertJob(job)
            isPostingJob = false
            if (inserted != null) {
                jobsList = listOf(inserted) + jobsList
                onResult(true, null)
            } else {
                onResult(false, "Couldn't post the job. Please check your connection and try again.")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopRealtimeUpdates()
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