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
}