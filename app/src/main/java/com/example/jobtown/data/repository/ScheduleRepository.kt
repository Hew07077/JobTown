package com.example.jobtown.data.repository

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
            emptyList()
        }
    }

    /**
     * Live updates for a user's interview schedules.
     */
    fun observeSchedulesForUser(userId: String, isEmployer: Boolean): Flow<List<InterviewSchedule>> = callbackFlow {
        if (userId.isBlank()) {
            close()
            return@callbackFlow
        }

        suspend fun refresh() {
            trySend(getSchedulesForUser(userId, isEmployer))
        }

        // 1. Emit initial state immediately
        refresh()

        // 2. Create realtime channel using the realtime plugin module
        val realtimePlugin = supabaseClient.realtime
        val channel = realtimePlugin.channel("interview_schedules_$userId")

        // 3. Listen for changes in public.interview_schedules
        val job = launch {
            channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "interview_schedules"
            }.collect {
                refresh()
            }
        }

        // 4. Connect to channel stream
        launch {
            try {
                channel.subscribe()
            } catch (e: Exception) {
                // Subscription failure fallback (initial list still emitted)
            }
        }

        // 5. Tear down and cleanup on cancellation
        awaitClose {
            job.cancel()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    channel.unsubscribe()
                    realtimePlugin.removeChannel(channel)
                } catch (e: Exception) {
                    // Ignore teardown errors
                }
            }
        }
    }

    suspend fun createSchedule(schedule: InterviewSchedule): Boolean = withContext(Dispatchers.IO) {
        try {
            supabaseClient.from("interview_schedules").insert(schedule)
            true
        } catch (e: Exception) {
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
            false
        }
    }
}