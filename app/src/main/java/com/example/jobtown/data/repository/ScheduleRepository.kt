package com.example.jobtown.data.repository

import android.util.Log
import com.example.jobtown.data.InterviewSchedule
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
                .update(schedule) {
                    filter { eq("id", schedule.id) }
                }
            true
        } catch (e: Exception) {
            Log.e("ScheduleRepository", "Error updating schedule: ${e.localizedMessage}", e)
            false
        }
    }
}