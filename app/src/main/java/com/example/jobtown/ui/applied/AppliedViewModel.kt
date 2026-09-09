package com.example.jobtown.ui.applied

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jobtown.data.model.JobApplication
import com.example.jobtown.data.repository.ApplicationRepository
import com.example.jobtown.data.repository.ApplicationTrackingEvent
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ApplicationTab {
    PENDING, VIEWED, SCHEDULED, CONSIDERED, OFFERED, REJECTED, CANCELLED
}

class AppliedViewModel(
    private val applicationRepository: ApplicationRepository
) : ViewModel() {

    private val _applicationsList = MutableStateFlow<List<JobApplication>>(emptyList())
    val applicationsListState: StateFlow<List<JobApplication>> = _applicationsList.asStateFlow()
    val applicationsList: List<JobApplication>
        get() = _applicationsList.value

    private val _selectedTab = MutableStateFlow(ApplicationTab.PENDING)
    val selectedTab: StateFlow<ApplicationTab> = _selectedTab.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isTrackingLive = MutableStateFlow(false)
    val isTrackingLive: StateFlow<Boolean> = _isTrackingLive.asStateFlow()

    private val _recentlyUpdatedApplicationId = MutableStateFlow<String?>(null)
    val recentlyUpdatedApplicationId: StateFlow<String?> = _recentlyUpdatedApplicationId.asStateFlow()

    private val _hasUnseenUpdate = MutableStateFlow(false)
    val hasUnseenUpdate: StateFlow<Boolean> = _hasUnseenUpdate.asStateFlow()

    private var trackingJob: Job? = null

    fun switchTab(tab: ApplicationTab) {
        _selectedTab.value = tab
    }

    fun loadApplications(userId: String, forceRefresh: Boolean = false) {
        if (userId.isBlank()) return
        viewModelScope.launch {
            if (_applicationsList.value.isEmpty() || forceRefresh) {
                _isLoading.value = true
            }
            _applicationsList.value = applicationRepository.getApplicationsForUser(userId)
            _isLoading.value = false
        }
    }

    fun loadEmployerApplications(employerId: String, forceRefresh: Boolean = false) {
        if (employerId.isBlank()) return
        viewModelScope.launch {
            if (_applicationsList.value.isEmpty() || forceRefresh) {
                _isLoading.value = true
            }
            _applicationsList.value = applicationRepository.getApplicationsForEmployer(employerId, forceRefresh)
            _isLoading.value = false
        }
    }

    fun submitNewApplication(application: JobApplication, onComplete: (Boolean, String?) -> Unit) {
        val duplicate = _applicationsList.value.any { existing ->
            existing.userId == application.userId &&
                    existing.jobId == application.jobId &&
                    !existing.status.equals("Cancelled", ignoreCase = true)
        }
        if (duplicate) {
            onComplete(false, "You've already applied for this job.")
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            val success = applicationRepository.submitApplication(application)
            if (success) {
                loadApplications(application.userId, forceRefresh = true)
            }
            _isLoading.value = false
            onComplete(success, if (success) null else "Failed to submit application. Please try again.")
        }
    }

    /**
     * Filters out applications deleted by the local user role.
     * Uses status checks for per-side soft-deletes.
     */
    fun getFilteredApplications(tab: ApplicationTab, isEmployer: Boolean = false): List<JobApplication> {
        return _applicationsList.value.filter { app ->
            val isDeletedForUser = if (isEmployer) {
                app.status.equals("DeletedByEmployer", ignoreCase = true) || app.status.equals("Deleted", ignoreCase = true)
            } else {
                app.status.equals("DeletedBySeeker", ignoreCase = true) || app.status.equals("Deleted", ignoreCase = true)
            }
            !isDeletedForUser && app.applicationTab() == tab
        }
    }

    fun findApplicationForJob(userId: String, jobId: String): JobApplication? {
        return _applicationsList.value.firstOrNull { app ->
            app.userId == userId &&
                    app.jobId == jobId &&
                    !app.status.equals("Cancelled", ignoreCase = true)
        }
    }

    fun cancelApplication(applicationId: String, onResult: (Boolean) -> Unit = {}) {
        updateApplicationStatus(applicationId, "Cancelled", onResult)
    }

    fun deleteApplicationForRole(
        applicationId: String,
        isEmployer: Boolean,
        onResult: (Boolean) -> Unit = {}
    ) {
        val app = _applicationsList.value.find { it.id == applicationId } ?: return

        val newStatus = if (isEmployer) {
            // When employer deletes, change status to "Rejected" so it shows as Rejected for the jobseeker
            "Rejected"
        } else {
            // When jobseeker deletes, check if employer already rejected/deleted
            val otherSideAlreadyDeleted = app.status.equals("DeletedByEmployer", ignoreCase = true) ||
                    app.status.equals("Rejected", ignoreCase = true)
            if (otherSideAlreadyDeleted) "Deleted" else "DeletedBySeeker"
        }

        updateApplicationStatus(applicationId, newStatus, onResult)
    }

    fun updateApplicationStatus(
        applicationId: String,
        newStatus: String,
        onResult: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            val success = applicationRepository.updateApplicationStatus(applicationId, newStatus)
            if (success) {
                _applicationsList.value = _applicationsList.value.map { app ->
                    if (app.id == applicationId) app.copy(status = newStatus) else app
                }
            }
            onResult(success)
        }
    }

    fun startTracking(userId: String) {
        if (_isTrackingLive.value || userId.isBlank()) return
        _isTrackingLive.value = true

        trackingJob = viewModelScope.launch {
            applicationRepository.observeApplicationsForUser(userId).collect { event ->
                when (event) {
                    is ApplicationTrackingEvent.Upserted -> {
                        val updatedApp = event.application
                        val currentList = _applicationsList.value.toMutableList()
                        val index = currentList.indexOfFirst { it.id == updatedApp.id }
                        if (index != -1) {
                            val previousStatus = currentList[index].status
                            currentList[index] = updatedApp
                            if (!previousStatus.equals(updatedApp.status, ignoreCase = true)) {
                                _hasUnseenUpdate.value = true
                            }
                        } else {
                            currentList.add(0, updatedApp)
                        }
                        _applicationsList.value = currentList
                        _recentlyUpdatedApplicationId.value = updatedApp.id
                    }
                    is ApplicationTrackingEvent.Removed -> {
                        _applicationsList.value = _applicationsList.value.filter { it.id != event.applicationId }
                    }
                }
            }
        }
    }

    fun stopTracking() {
        trackingJob?.cancel()
        trackingJob = null
        _isTrackingLive.value = false
    }

    fun consumeRecentUpdate() {
        _recentlyUpdatedApplicationId.value = null
    }

    fun markUpdatesSeen() {
        _hasUnseenUpdate.value = false
    }

    override fun onCleared() {
        super.onCleared()
        stopTracking()
    }
}