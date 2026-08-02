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

    var selectedJob by mutableStateOf<Job?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    init {
        loadJobs()
    }

    fun loadJobs() {
        viewModelScope.launch {
            isLoading = true
            jobsList = repository.getAllJobs()
            isLoading = false
        }
    }

    fun selectJob(job: Job) {
        selectedJob = job
    }
}

class HomeViewModelFactory(private val repository: JobRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}