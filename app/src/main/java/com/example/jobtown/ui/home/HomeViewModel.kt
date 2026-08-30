package com.example.jobtown.ui.home

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.jobtown.data.model.Job
import com.example.jobtown.data.model.UserProfile
import com.example.jobtown.data.repository.JobListingEvent
import com.example.jobtown.data.repository.JobRepository
import com.example.jobtown.data.repository.UserRepository
import com.example.jobtown.utils.JobMatchUtils
import com.example.jobtown.utils.JobMatchResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Job as CoroutineJob
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

enum class JobFilterTab { ALL, FULL_TIME, PART_TIME, CONTRACT, INTERNSHIP, FREELANCE, REMOTE, EXPIRED }
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

    var matchResults by mutableStateOf<Map<String, JobMatchResult>>(emptyMap())
        private set

    var sortMode by mutableStateOf(JobSortMode.BEST_MATCH)
        private set

    var filterTab by mutableStateOf(JobFilterTab.ALL)
        private set

    var isLive by mutableStateOf(false)
        private set

    var newListingsAvailable by mutableStateOf(0)
        private set

    var savedJobIds by mutableStateOf<Set<String>>(emptySet())
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
                    loadSavedJobs(currentUserId)
                } else {
                    myPostedJobs = emptyList()
                    seekerProfile = null
                    matchScores = emptyMap()
                    savedJobIds = emptySet()
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error loading jobs", e)
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
                val results = jobs.associate { it.id to JobMatchUtils.score(it, profile) }
                matchResults = results
                matchScores = results.mapValues { it.value.score }
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
            val result = JobMatchUtils.score(job, seekerProfile)
            matchResults = matchResults + (job.id to result)
            matchScores = matchScores + (job.id to result.score)
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

    fun loadSavedJobs(userId: String) {
        if (userId.isBlank()) return
        viewModelScope.launch {
            try {
                savedJobIds = repository.getSavedJobIds(userId)
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error loading saved jobs", e)
            }
        }
    }

    fun toggleSaveJob(userId: String, jobId: String) {
        if (userId.isBlank() || jobId.isBlank()) return
        val wasSaved = savedJobIds.contains(jobId)
        // Optimistic update so the bookmark icon responds instantly.
        savedJobIds = if (wasSaved) savedJobIds - jobId else savedJobIds + jobId

        viewModelScope.launch {
            try {
                val nowSaved = repository.toggleSavedJob(userId, jobId)
                savedJobIds = if (nowSaved) savedJobIds + jobId else savedJobIds - jobId
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error toggling saved job", e)
                // Revert the optimistic update on failure.
                savedJobIds = if (wasSaved) savedJobIds + jobId else savedJobIds - jobId
            }
        }
    }

    fun selectJob(job: Job) {
        selectedJob = job
    }

    /** Optimized method to post jobs directly to Supabase and update state safely. */
    fun postJob(job: Job, onResult: (success: Boolean, message: String?) -> Unit) {
        if (isPostingJob) return
        viewModelScope.launch {
            isPostingJob = true
            val result = repository.postJob(job, isNewJob = true)
            isPostingJob = false

            result.fold(
                onSuccess = { inserted ->
                    Log.d("HomeViewModel", "Job posted successfully with ID: ${inserted.id}")
                    // Instantly update local feeds
                    jobsList = listOf(inserted) + jobsList.filter { it.id != inserted.id }
                    myPostedJobs = listOf(inserted) + myPostedJobs.filter { it.id != inserted.id }

                    seekerProfile?.let { profile ->
                        val result = JobMatchUtils.score(inserted, profile)
                        matchResults = matchResults + (inserted.id to result)
                        matchScores = matchScores + (inserted.id to result.score)
                    }

                    onResult(true, null)
                },
                onFailure = { error ->
                    Log.e("HomeViewModel", "Failed to insert job into Supabase: ${error.message}", error)
                    onResult(false, error.localizedMessage ?: "Failed to save job to Supabase database.")
                }
            )
        }
    }

    /** Compatibility helper function for navigation callbacks. */
    fun addJob(newJob: Job) {
        postJob(newJob) { success, _ ->
            if (!success) {
                // If insertion fails, reload feed to remove unpersisted local optimistic state
                loadJobs(currentUserId)
            }
        }
    }

    fun updateFilterTab(tab: JobFilterTab) {
        filterTab = tab
    }

    fun getFilteredJobs(jobs: List<Job>): List<Job> {
        return when (filterTab) {
            JobFilterTab.ALL -> jobs.filter { !isJobExpired(it) }
            JobFilterTab.FULL_TIME -> jobs.filter { !isJobExpired(it) && it.type.orEmpty().contains("full", ignoreCase = true) }
            JobFilterTab.PART_TIME -> jobs.filter { !isJobExpired(it) && it.type.orEmpty().contains("part", ignoreCase = true) }
            JobFilterTab.CONTRACT -> jobs.filter { !isJobExpired(it) && it.type.orEmpty().contains("contract", ignoreCase = true) }
            JobFilterTab.INTERNSHIP -> jobs.filter { !isJobExpired(it) && it.type.orEmpty().contains("intern", ignoreCase = true) }
            JobFilterTab.FREELANCE -> jobs.filter { !isJobExpired(it) && it.type.orEmpty().contains("free", ignoreCase = true) }
            JobFilterTab.REMOTE -> jobs.filter { !isJobExpired(it) && (it.type.orEmpty().contains("remote", ignoreCase = true) || it.location.orEmpty().contains("remote", ignoreCase = true)) }
            JobFilterTab.EXPIRED -> jobs.filter { isJobExpired(it) }
        }
    }

    fun refreshCurrentFeed() {
        loadJobs(currentUserId)
    }

    fun autoPublishPendingListings() {
        showPendingListings()
    }

    fun updateJob(updatedJob: Job, onResult: ((success: Boolean, message: String?) -> Unit)? = null) {
        viewModelScope.launch {
            val result = repository.postJob(updatedJob, isNewJob = false)
            result.fold(
                onSuccess = { updated ->
                    jobsList = jobsList.map { if (it.id == updated.id) updated else it }
                    myPostedJobs = myPostedJobs.map { if (it.id == updated.id) updated else it }

                    if (selectedJob?.id == updated.id) {
                        selectedJob = updated
                    }

                    seekerProfile?.let { profile ->
                        val result = JobMatchUtils.score(updated, profile)
                        matchResults = matchResults + (updated.id to result)
                        matchScores = matchScores + (updated.id to result.score)
                    }

                    onResult?.invoke(true, null)
                },
                onFailure = { error ->
                    Log.e("HomeViewModel", "Error saving job updates to Supabase", error)
                    onResult?.invoke(false, error.localizedMessage ?: "Failed to update job in Supabase.")
                }
            )
        }
    }

    fun getActivePostedJobs(): List<Job> {
        return myPostedJobs.filter { !isJobExpired(it) }
    }

    fun getExpiredPostedJobs(): List<Job> {
        return myPostedJobs.filter { !isJobExpired(it) }
    }

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