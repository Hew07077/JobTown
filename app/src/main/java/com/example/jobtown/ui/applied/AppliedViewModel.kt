package com.example.jobtown.ui.applied

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.jobtown.data.JobApplication
import com.example.jobtown.data.repository.ApplicationRepository
import kotlinx.coroutines.launch

class AppliedViewModel(
    private val applicationRepository: ApplicationRepository
) : ViewModel() {

    var applicationsList by mutableStateOf<List<JobApplication>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    fun loadApplications(userId: String, forceRefresh: Boolean = false) {
        if (userId.isBlank()) return
        if (!forceRefresh && applicationsList.isNotEmpty()) return

        viewModelScope.launch {
            isLoading = true
            try {
                val result = applicationRepository.getApplicationsForUser(userId)
                applicationsList = result
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    fun submitNewApplication(application: JobApplication, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            isLoading = true
            try {
                val success = applicationRepository.applyForJob(application)
                if (success) {
                    // Prepend new application locally
                    applicationsList = listOf(application) + applicationsList
                }
                onComplete(success)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false)
            } finally {
                isLoading = false
            }
        }
    }
}

class AppliedViewModelFactory(
    private val repository: ApplicationRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AppliedViewModel::class.java)) {
            return AppliedViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}