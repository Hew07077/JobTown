package com.example.jobtown.ui.postjob

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jobtown.data.model.Job
import com.example.jobtown.data.model.User
import com.example.jobtown.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface JobDetailUiEvent {
    data object JobUpdated : JobDetailUiEvent
    data object JobDeleted : JobDetailUiEvent
    data class ShowError(val message: String) : JobDetailUiEvent
}

class EmployerJobDetailViewModel : ViewModel() {

    private val _jobState = MutableStateFlow<Job?>(null)
    val jobState: StateFlow<Job?> = _jobState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _eventFlow = MutableSharedFlow<JobDetailUiEvent>()
    val eventFlow: SharedFlow<JobDetailUiEvent> = _eventFlow.asSharedFlow()

    fun setInitialJob(job: Job) {
        if (_jobState.value == null) {
            _jobState.value = job
        }
    }

    fun updateJob(updatedJob: Job, onUserUpdated: (User) -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true

            // Sync new location to employer profile using pipe | logic
            val employerId = updatedJob.employerId ?: updatedJob.postedByUserId.orEmpty()
            if (employerId.isNotBlank() && updatedJob.location.isNotBlank()) {
                val added = UserRepository.addSavedAddressToEmployer(employerId, updatedJob.location)
                if (added) {
                    val freshUser = UserRepository.fetchUserById(employerId)
                    if (freshUser != null) {
                        onUserUpdated(freshUser)
                    }
                }
            }

            val success = UserRepository.updateJob(updatedJob)
            _isLoading.value = false

            if (success) {
                _jobState.value = updatedJob
                _eventFlow.emit(JobDetailUiEvent.JobUpdated)
            } else {
                _eventFlow.emit(JobDetailUiEvent.ShowError("Failed to update job listing. Please check your network or try again."))
            }
        }
    }

    fun deleteJob(jobId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val success = UserRepository.deleteJob(jobId)
            _isLoading.value = false

            if (success) {
                _eventFlow.emit(JobDetailUiEvent.JobDeleted)
            } else {
                _eventFlow.emit(JobDetailUiEvent.ShowError("Failed to delete job listing. Please check your network or try again."))
            }
        }
    }
}