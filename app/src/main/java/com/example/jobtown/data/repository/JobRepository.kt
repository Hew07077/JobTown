package com.example.jobtown.data.repository

import android.util.Log
import com.example.jobtown.data.Job
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class NewJobPayload(
    @SerialName("title") val title: String,
    @SerialName("company") val company: String,
    @SerialName("location") val location: String,
    @SerialName("salary") val salary: String,
    @SerialName("salary_range") val salaryRange: String?,
    @SerialName("type") val type: String,
    @SerialName("description") val description: String,
    @SerialName("requirements") val requirements: List<String>,
    @SerialName("skills") val skills: List<String>,
    @SerialName("is_featured") val isFeatured: Boolean,
    @SerialName("employer_id") val employerId: String?,
    @SerialName("posted_by_user_id") val postedByUserId: String?
)

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

    suspend fun insertJob(job: Job): Job? = withContext(Dispatchers.IO) {
        try {
            val payload = NewJobPayload(
                title = job.title.trim(),
                company = job.company.trim(),
                location = job.location.trim(),
                salary = job.salary.trim(),
                salaryRange = job.salaryRange?.trim(),
                type = job.type.ifBlank { "Full-time" },
                description = job.description.trim(),
                requirements = job.requirements.orEmpty(),
                skills = job.skills.orEmpty(),
                isFeatured = job.isFeatured ?: false,
                employerId = job.employerId?.ifBlank { null },
                postedByUserId = job.postedByUserId?.ifBlank { null }
            )

            val inserted = supabaseClient.postgrest["jobs"]
                .insert(payload) { select() }
                .decodeSingle<Job>()

            Log.d("JobRepository", "SUCCESS: Inserted job '${inserted.title}' with id=${inserted.id}")
            inserted
        } catch (e: Exception) {
            Log.e("JobRepository", "ERROR INSERTING JOB: ${e.message}", e)
            null
        }
    }
}