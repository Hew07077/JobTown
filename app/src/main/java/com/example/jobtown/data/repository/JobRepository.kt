@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)

package com.example.jobtown.data.repository

import android.util.Log
import com.example.jobtown.data.model.Job
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

sealed interface JobListingEvent {
    data class Upserted(val job: Job) : JobListingEvent
    data class Removed(val jobId: String) : JobListingEvent
}

@Serializable
private data class SavedJobRow(
    @SerialName("id") val id: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("job_id") val jobId: String = ""
)

@Serializable
private data class NewSavedJobPayload(
    @SerialName("user_id") val userId: String,
    @SerialName("job_id") val jobId: String
)

@Serializable
private data class NewJobPayload(
    @SerialName("title") val title: String,
    @SerialName("company") val company: String,
    @SerialName("company_image_url") val companyImageUrl: String?,
    @SerialName("location") val location: String,
    @SerialName("salary") val salary: String,
    @SerialName("salary_range") val salaryRange: String?,
    @SerialName("type") val type: String,
    @SerialName("description") val description: String,
    @SerialName("requirements") val requirements: List<String>,
    @SerialName("skills") val skills: List<String>,
    @SerialName("is_featured") val isFeatured: Boolean,
    @SerialName("employer_id") val employerId: String?,
    @SerialName("posted_by_user_id") val postedByUserId: String?,
    @SerialName("status") val status: String? = "active",
    @SerialName("expired_at") val expiredAt: String? = null


)

@Serializable
private data class UpsertJobPayload(
    @SerialName("id") val id: String,
    @SerialName("title") val title: String,
    @SerialName("company") val company: String,
    @SerialName("company_image_url") val companyImageUrl: String?,
    @SerialName("location") val location: String,
    @SerialName("salary") val salary: String,
    @SerialName("salary_range") val salaryRange: String?,
    @SerialName("type") val type: String,
    @SerialName("description") val description: String,
    @SerialName("requirements") val requirements: List<String>,
    @SerialName("skills") val skills: List<String>,
    @SerialName("is_featured") val isFeatured: Boolean,
    @SerialName("employer_id") val employerId: String?,
    @SerialName("posted_by_user_id") val postedByUserId: String?,
    @SerialName("status") val status: String? = "active",
    @SerialName("expired_at") val expiredAt: String? = null
)

class JobRepository(private val supabaseClient: SupabaseClient) {

    suspend fun getSavedJobIds(userId: String): Set<String> = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext emptySet()
        try {
            supabaseClient.postgrest["saved_jobs"]
                .select { filter { eq("user_id", userId) } }
                .decodeList<SavedJobRow>()
                .map { it.jobId }
                .toSet()
        } catch (e: Exception) {
            Log.e("JobRepository", "Error loading saved jobs: ${e.message}", e)
            emptySet()
        }
    }

    /** Toggles the saved state for a job and returns the new state (true = now saved). */
    suspend fun toggleSavedJob(userId: String, jobId: String): Boolean = withContext(Dispatchers.IO) {
        if (userId.isBlank() || jobId.isBlank()) return@withContext false
        try {
            val existing = supabaseClient.postgrest["saved_jobs"]
                .select { filter { eq("user_id", userId); eq("job_id", jobId) } }
                .decodeList<SavedJobRow>()
                .firstOrNull()

            if (existing != null) {
                supabaseClient.postgrest["saved_jobs"].delete {
                    filter { eq("user_id", userId); eq("job_id", jobId) }
                }
                false
            } else {
                supabaseClient.postgrest["saved_jobs"].insert(
                    NewSavedJobPayload(userId = userId, jobId = jobId)
                )
                true
            }
        } catch (e: Exception) {
            Log.e("JobRepository", "Error toggling saved job: ${e.message}", e)
            // Report unchanged state on failure so the UI can revert its optimistic update.
            throw e
        }
    }

    suspend fun getAllJobs(): List<Job> = withContext(Dispatchers.IO) {
        try {
            val result = supabaseClient.postgrest["jobs"]
                .select {
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<Job>()

            Log.d("JobRepository", "SUCCESS: Loaded ${result.size} jobs from Supabase!")
            result
        } catch (e: Exception) {
            Log.e("JobRepository", "ERROR DECODING JOBS: ${e.message}", e)
            emptyList()
        }
    }

    fun observeJobs(): Flow<JobListingEvent> = callbackFlow {
        val channel = supabaseClient.realtime.channel("jobs_feed_${System.currentTimeMillis()}")
        val collectJob = launch {
            channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "jobs"
            }.collect { action ->
                try {
                    when (action) {
                        is PostgresAction.Insert, is PostgresAction.Update -> {
                            trySend(JobListingEvent.Upserted(action.decodeRecord<Job>()))
                        }
                        is PostgresAction.Delete -> {
                            val id = action.oldRecord["id"]?.jsonPrimitive?.contentOrNull
                            if (!id.isNullOrBlank()) trySend(JobListingEvent.Removed(id))
                        }
                        else -> {}
                    }
                } catch (e: Exception) {
                    Log.e("JobRepository", "Error decoding realtime job event", e)
                }
            }
        }

        try {
            channel.subscribe()
        } catch (e: Exception) {
            Log.e("JobRepository", "Error subscribing to jobs realtime channel", e)
        }

        awaitClose {
            collectJob.cancel()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    channel.unsubscribe()
                    supabaseClient.realtime.removeChannel(channel)
                } catch (e: Exception) {
                    Log.e("JobRepository", "Error tearing down jobs realtime channel", e)
                }
            }
        }
    }

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

    suspend fun insertJob(job: Job): Job = withContext(Dispatchers.IO) {
        val cleanEmployerId = job.employerId?.trim()?.ifEmpty { null }
        val cleanUserId = job.postedByUserId?.trim()?.ifEmpty { null } ?: cleanEmployerId

        val payload = NewJobPayload(
            title = job.title.trim(),
            company = job.company.trim(),
            companyImageUrl = job.companyImageUrl?.trim()?.ifEmpty { null },
            location = job.location.trim(),
            salary = job.salary.trim(),
            salaryRange = job.salaryRange?.trim()?.ifEmpty { null },
            type = job.type.ifBlank { "Full-time" },
            description = job.description.trim(),
            requirements = job.requirements.orEmpty(),
            skills = job.skills.orEmpty(),
            isFeatured = job.isFeatured ?: false,
            employerId = cleanEmployerId,
            postedByUserId = cleanUserId,
            status = job.status ?: "active",
            expiredAt = job.expiredAt
        )

        val inserted = supabaseClient.postgrest["jobs"]
            .insert(payload) { select() }
            .decodeSingle<Job>()

        Log.d("JobRepository", "SUCCESS: Inserted job '${inserted.title}' with id=${inserted.id}")
        inserted
    }

    suspend fun updateJob(job: Job): Job = withContext(Dispatchers.IO) {
        val cleanEmployerId = job.employerId?.trim()?.ifEmpty { null }
        val cleanUserId = job.postedByUserId?.trim()?.ifEmpty { null } ?: cleanEmployerId

        val payload = UpsertJobPayload(
            id = job.id,
            title = job.title.trim(),
            company = job.company.trim(),
            companyImageUrl = job.companyImageUrl?.trim()?.ifEmpty { null },
            location = job.location.trim(),
            salary = job.salary.trim(),
            salaryRange = job.salaryRange?.trim()?.ifEmpty { null },
            type = job.type.ifBlank { "Full-time" },
            description = job.description.trim(),
            requirements = job.requirements.orEmpty(),
            skills = job.skills.orEmpty(),
            isFeatured = job.isFeatured ?: false,
            employerId = cleanEmployerId,
            postedByUserId = cleanUserId,
            status = job.status ?: "active",
            expiredAt = job.expiredAt
        )

        val updated = supabaseClient.postgrest["jobs"]
            .upsert(payload) { select() }
            .decodeSingle<Job>()

        Log.d("JobRepository", "SUCCESS: Updated job '${updated.title}' with id=${updated.id}")
        updated
    }

    suspend fun postJob(job: Job, isNewJob: Boolean = true): Result<Job> = withContext(Dispatchers.IO) {
        try {
            val result = if (isNewJob) insertJob(job) else updateJob(job)
            Result.success(result)
        } catch (e: Exception) {
            Log.e("JobRepository", "ERROR POSTING JOB TO SUPABASE: ${e.message}", e)
            Result.failure(e)
        }
    }
}