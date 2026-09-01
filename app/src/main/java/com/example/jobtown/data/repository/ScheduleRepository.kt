package com.example.jobtown.data.repository

import android.util.Log
import com.example.jobtown.data.model.InterviewSchedule
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ScheduleRepository(private val supabaseClient: SupabaseClient) {

    suspend fun getSchedulesForUser(userId: String, isEmployer: Boolean): List<InterviewSchedule> = withContext(Dispatchers.IO) {
        try {
            val column = if (isEmployer) "employer_id" else "user_id"
            supabaseClient.from("interview_schedules")
                .select {
                    filter { eq(column, userId) }
                }
                .decodeList<InterviewSchedule>()
        } catch (e: Exception) {
            Log.e("ScheduleRepository", "Error fetching schedules: ${e.localizedMessage}")
            emptyList()
        }
    }

    fun observeSchedulesForUser(userId: String, isEmployer: Boolean): Flow<List<InterviewSchedule>> = callbackFlow {
        if (userId.isBlank()) {
            close()
            return@callbackFlow
        }

        suspend fun refresh() {
            trySend(getSchedulesForUser(userId, isEmployer))
        }

        refresh()

        val realtimePlugin = supabaseClient.realtime
        val channel = realtimePlugin.channel("interview_schedules_$userId")

        val job = launch {
            channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "interview_schedules"
            }.collect {
                refresh()
            }
        }

        launch {
            try {
                channel.subscribe()
            } catch (e: Exception) {
                Log.e("ScheduleRepository", "Realtime channel subscription error: ${e.localizedMessage}")
            }
        }

        awaitClose {
            job.cancel()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    channel.unsubscribe()
                    realtimePlugin.removeChannel(channel)
                } catch (e: Exception) {
                    // Cleanup complete
                }
            }
        }
    }

    suspend fun createSchedule(schedule: InterviewSchedule): Boolean = withContext(Dispatchers.IO) {
        try {
            supabaseClient.from("interview_schedules").insert(schedule)
            true
        } catch (e: Exception) {
            Log.e("ScheduleRepository", "Error creating schedule: ${e.localizedMessage}", e)
            false
        }
    }

    suspend fun updateScheduleStatus(scheduleId: String, status: String): Boolean = withContext(Dispatchers.IO) {
        try {
            supabaseClient.from("interview_schedules")
                .update({
                    set("status", status)
                }) {
                    filter { eq("id", scheduleId) }
                }
            true
        } catch (e: Exception) {
            Log.e("ScheduleRepository", "Error updating status: ${e.localizedMessage}", e)
            false
        }
    }

    suspend fun updateSchedule(schedule: InterviewSchedule): Boolean = withContext(Dispatchers.IO) {
        try {
            supabaseClient.from("interview_schedules")
                .update({
                    set("date", schedule.date)
                    set("time", schedule.time)
                    set("location_or_link", schedule.locationOrLink)
                    set("notes", schedule.notes)
                    set("status", schedule.status)
                    set("reschedule_reason", schedule.rescheduleReason)
                    set("preferred_time", schedule.preferredTime)
                    set("title", schedule.title)
                    set("company", schedule.company)
                    if (schedule.jobId.isNotBlank()) set("job_id", schedule.jobId)
                    if (schedule.userId.isNotBlank()) set("user_id", schedule.userId)
                    if (schedule.employerId.isNotBlank()) set("employer_id", schedule.employerId)
                }) {
                    filter { eq("id", schedule.id) }
                }
            true
        } catch (e: Exception) {
            Log.e("ScheduleRepository", "Error updating schedule: ${e.localizedMessage}", e)
            false
        }
    }

    suspend fun deleteSchedule(scheduleId: String): Boolean = withContext(Dispatchers.IO) {
        if (scheduleId.isBlank()) return@withContext false
        try {
            supabaseClient.from("interview_schedules").delete {
                filter { eq("id", scheduleId) }
            }
            true
        } catch (e: Exception) {
            Log.e("ScheduleRepository", "Error deleting schedule: ${e.localizedMessage}", e)
            false
        }
    }
}