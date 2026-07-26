package com.example.jobtown.data

import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object UserRepository {

    // --- USERS ---
    suspend fun saveUserToSupabase(user: User): Boolean = withContext(Dispatchers.IO) {
        try {
            // Upsert handles both new signups and profile updates safely without primary key collisions
            SupabaseClient.client.from("users").upsert(user)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun findUserByEmail(email: String): User? = withContext(Dispatchers.IO) {
        try {
            SupabaseClient.client.from("users")
                .select {
                    filter {
                        eq("email", email.trim())
                    }
                }
                .decodeSingleOrNull<User>()
        } catch (e: Exception) {
            // Catches serialization issues or missing rows safely without crashing the app
            e.printStackTrace()
            null
        }
    }

    suspend fun updateUserInSupabase(user: User): Boolean = withContext(Dispatchers.IO) {
        try {
            SupabaseClient.client.from("users").update(user) {
                filter { eq("id", user.id) }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // --- JOBS ---
    suspend fun fetchAllJobs(): List<Job> = withContext(Dispatchers.IO) {
        try {
            SupabaseClient.client.from("jobs")
                .select()
                .decodeList<Job>()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun saveJobToSupabase(job: Job): Boolean = withContext(Dispatchers.IO) {
        try {
            SupabaseClient.client.from("jobs").insert(job)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // --- APPLICATIONS ---
    suspend fun fetchApplicationsForUser(userId: String, isEmployer: Boolean): List<JobApplication> = withContext(Dispatchers.IO) {
        try {
            if (isEmployer) {
                SupabaseClient.client.from("applications")
                    .select()
                    .decodeList<JobApplication>()
            } else {
                SupabaseClient.client.from("applications")
                    .select { filter { eq("userId", userId) } }
                    .decodeList<JobApplication>()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun saveApplicationToSupabase(application: JobApplication): Boolean = withContext(Dispatchers.IO) {
        try {
            SupabaseClient.client.from("applications").insert(application)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // --- SCHEDULES ---
    suspend fun fetchSchedulesForUser(userId: String): List<InterviewSchedule> = withContext(Dispatchers.IO) {
        try {
            SupabaseClient.client.from("schedules")
                .select { filter { eq("userId", userId) } }
                .decodeList<InterviewSchedule>()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}