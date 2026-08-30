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
    ACTIVE, CLOSED
}

class AppliedViewModel(
    private val applicationRepository: ApplicationRepository
) : ViewModel() {

    private val _applicationsList = MutableStateFlow<List<JobApplication>>(emptyList())
    // Expose both StateFlow and a List property wrapper for legacy compatibility across screens
    val applicationsListState: StateFlow<List<JobApplication>> = _applicationsList.asStateFlow()
    val applicationsList: List<JobApplication>
        get() = _applicationsList.value

    private val _selectedTab = MutableStateFlow(ApplicationTab.ACTIVE)
    val selectedTab: StateFlow<ApplicationTab> = _selectedTab.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isTrackingLive = MutableStateFlow(false)
    val isTrackingLive: StateFlow<Boolean> = _isTrackingLive.asStateFlow()

    private val _recentlyUpdatedApplicationId = MutableStateFlow<String?>(null)
    val recentlyUpdatedApplicationId: StateFlow<String?> = _recentlyUpdatedApplicationId.asStateFlow()

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
            val results = applicationRepository.getApplicationsForUser(userId)
            _applicationsList.value = results
            _isLoading.value = false
        }
    }

    fun loadEmployerApplications(employerId: String, forceRefresh: Boolean = false) {
        if (employerId.isBlank()) return
        viewModelScope.launch {
            if (_applicationsList.value.isEmpty() || forceRefresh) {
                _isLoading.value = true
            }
            val results = applicationRepository.getApplicationsForEmployer(employerId, forceRefresh)
            _applicationsList.value = results
            _isLoading.value = false
        }
    }

    fun submitNewApplication(application: JobApplication, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val success = applicationRepository.submitApplication(application)
            if (success) {
                loadApplications(application.userId, forceRefresh = true)
            }
            _isLoading.value = false
            onComplete(success)
        }
    }

    fun getFilteredApplications(tab: ApplicationTab): List<JobApplication> {
        return _applicationsList.value.filter { app ->
            val closed = app.status.equals("rejected", ignoreCase = true) ||
                app.status.equals("expired", ignoreCase = true)
            when (tab) {
                ApplicationTab.ACTIVE -> !closed
                ApplicationTab.CLOSED -> closed
            }
        }
    }

    fun updateApplicationStatus(applicationId: String, newStatus: String, onResult: (Boolean) -> Unit = {}) {
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
                            currentList[index] = updatedApp
                        } else {
                            currentList.add(0, updatedApp)
                        }

                        _applicationsList.value = currentList
                        _recentlyUpdatedApplicationId.value = updatedApp.id
                    }
                    is ApplicationTrackingEvent.Removed -> {
                        val currentList = _applicationsList.value.filter { it.id != event.applicationId }
                        _applicationsList.value = currentList
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

    override fun onCleared() {
        super.onCleared()
        stopTracking()
    }
}