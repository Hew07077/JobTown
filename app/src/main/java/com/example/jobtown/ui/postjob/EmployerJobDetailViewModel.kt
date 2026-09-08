package com.example.jobtown.ui.postjob

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jobtown.data.model.Job
import com.example.jobtown.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class JobDetailUiEvent {
    object JobUpdated : JobDetailUiEvent()
    object JobDeleted : JobDetailUiEvent()
    data class ShowError(val message: String) : JobDetailUiEvent()
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

    fun updateJob(updatedJob: Job) {
        viewModelScope.launch {
            _isLoading.value = true
            val success = UserRepository.saveJobToSupabase(updatedJob)
            _isLoading.value = false

            if (success) {
                _jobState.value = updatedJob
                _eventFlow.emit(JobDetailUiEvent.JobUpdated)
            } else {
                _eventFlow.emit(JobDetailUiEvent.ShowError("Failed to update job listing. Please try again."))
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
                _eventFlow.emit(JobDetailUiEvent.ShowError("Failed to delete job listing. Please try again."))
            }
        }
    }
}