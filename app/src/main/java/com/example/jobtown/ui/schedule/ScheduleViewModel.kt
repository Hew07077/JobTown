package com.example.jobtown.ui.schedule

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jobtown.data.model.InterviewSchedule
import com.example.jobtown.data.repository.ScheduleRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.util.UUID

class ScheduleViewModel(
    private val scheduleRepository: ScheduleRepository
) : ViewModel() {

    var schedulesList by mutableStateOf<List<InterviewSchedule>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var isSaving by mutableStateOf(false)
        private set

    var schedulePrefill by mutableStateOf(SchedulePrefill())
        private set

    private var realtimeJob: Job? = null

    fun loadSchedules(userId: String, isEmployer: Boolean) {
        if (userId.isBlank()) return

        realtimeJob?.cancel()

        viewModelScope.launch {
            isLoading = true
            schedulesList = scheduleRepository.getSchedulesForUser(userId, isEmployer)
            isLoading = false
        }

        // Keep the list live so an employer's new/updated invite (or a seeker's
        // accept/reject) shows up immediately for the other side.
        realtimeJob = viewModelScope.launch {
            scheduleRepository.observeSchedulesForUser(userId, isEmployer)
                .catch { /* Initial load above already covers the non-realtime case. */ }
                .collect { updated -> schedulesList = updated }
        }
    }

    fun setPrefill(prefill: SchedulePrefill) {
        schedulePrefill = prefill
    }

    fun clearPrefill() {
        schedulePrefill = SchedulePrefill()
    }

    fun createSchedule(
        newSchedule: InterviewSchedule,
        currentUserId: String,
        isEmployer: Boolean,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            isSaving = true
            val scheduleWithId = newSchedule.copy(id = UUID.randomUUID().toString())

            // Push to Supabase backend database
            val success = scheduleRepository.createSchedule(scheduleWithId)

            if (success) {
                schedulesList = scheduleRepository.getSchedulesForUser(currentUserId, isEmployer)
                clearPrefill()
                onResult(true, "Interview successfully scheduled and sent to candidate!")
            } else {
                onResult(false, "Failed to sync schedule with server. Please check your connection.")
            }
            isSaving = false
        }
    }

    fun updateScheduleStatus(
        scheduleId: String,
        status: String,
        currentUserId: String,
        isEmployer: Boolean,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val success = scheduleRepository.updateScheduleStatus(scheduleId, status)
            if (success) {
                schedulesList = scheduleRepository.getSchedulesForUser(currentUserId, isEmployer)
                onResult(true)
            } else {
                onResult(false)
            }
        }
    }

    fun updateSchedule(
        updatedSchedule: InterviewSchedule,
        currentUserId: String,
        isEmployer: Boolean,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            isSaving = true
            val success = scheduleRepository.updateSchedule(updatedSchedule)
            if (success) {
                schedulesList = scheduleRepository.getSchedulesForUser(currentUserId, isEmployer)
                onResult(true)
            } else {
                onResult(false)
            }
            isSaving = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        realtimeJob?.cancel()
    }
}