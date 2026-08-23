package com.example.jobtown.data.repository

import com.example.jobtown.data.InterviewSchedule
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
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