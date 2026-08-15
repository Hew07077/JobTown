package com.example.jobtown.data.repository

import android.util.Log
import com.example.jobtown.data.Job
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class JobRepository(private val supabaseClient: SupabaseClient) {

    suspend fun getAllJobs(): List<Job> = withContext(Dispatchers.IO) {
        try {
            val result = supabaseClient.postgrest["jobs"]
                .select()
                .decodeList<Job>()

            Log.d("JobRepository", "SUCCESS: Loaded ${result.size} jobs from Supabase!")
            result
        } catch (e: Exception) {
            Log.e("JobRepository", "ERROR DECODING JOBS: ${e.message}", e)
            emptyList()
        }
    }

    // Fetch only jobs posted by the current logged-in user
    suspend fun getJobsByUserId(userId: String): List<Job> = withContext(Dispatchers.IO) {
        try {
            val result = supabaseClient.postgrest["jobs"]
                .select {
                    filter {
                        eq("posted_by_user_id", userId)
                    }
                }
                .decodeList<Job>()

            Log.d("JobRepository", "SUCCESS: Loaded ${result.size} user posted jobs!")
            result
        } catch (e: Exception) {
            Log.e("JobRepository", "ERROR FETCHING USER JOBS: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun addJob(job: Job) = withContext(Dispatchers.IO) {
        try {
            supabaseClient.postgrest["jobs"].insert(job)
            Log.d("JobRepository", "SUCCESS: Added job '${job.title}' to Supabase!")
        } catch (e: Exception) {
            Log.e("JobRepository", "ERROR ADDING JOB: ${e.message}", e)
            throw e
        }
    }
}