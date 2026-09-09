package com.example.jobtown.ui.postjob

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jobtown.data.model.Job
import com.example.jobtown.data.repository.JobRepository
import com.example.jobtown.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class JobPostViewModel(
    private val jobRepository: JobRepository
) : ViewModel() {

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    fun postJob(fields: JobFormFields, userId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isSubmitting.value = true

            val newLocation = fields.location.trim()

            // 1. Save new location to Supabase user profile/repository
            if (userId.isNotBlank() && newLocation.isNotBlank()) {
                UserRepository.addSavedAddressToEmployer(userId, newLocation)
            }

            // 2. Prepare job model and save to Supabase
            val newJob = Job(
                title = fields.title,
                company = fields.company,
                location = newLocation,
                salary = "${fields.minSalary} - ${fields.maxSalary}",
                type = fields.type,
                description = fields.description,
                requirements = fields.requirements.split(",").map { it.trim() }.filter { it.isNotBlank() },
                skills = fields.skills.split(",").map { it.trim() }.filter { it.isNotBlank() },
                isOkuFriendly = fields.isOkuFriendly,
                employerId = userId,
                postedByUserId = userId
            )

            val result = jobRepository.postJob(newJob, isNewJob = true)
            _isSubmitting.value = false

            if (result.isSuccess) {
                onSuccess()
            } else {
                fields.errorMessage = result.exceptionOrNull()?.message ?: "Failed to post job."
            }
        }
    }
}