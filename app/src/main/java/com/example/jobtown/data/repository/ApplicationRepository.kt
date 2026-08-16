package com.example.jobtown.data.repository

import android.util.Log
import com.example.jobtown.data.JobApplication
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
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

/** Emitted by [ApplicationRepository.observeApplicationsForUser] for live tracking. */
sealed interface ApplicationTrackingEvent {
    data class Upserted(val application: JobApplication) : ApplicationTrackingEvent
    data class Removed(val applicationId: String) : ApplicationTrackingEvent
}

class ApplicationRepository(private val supabase: SupabaseClient) {

    suspend fun getApplicationsForUser(userId: String): List<JobApplication> = withContext(Dispatchers.IO) {
        try {
            println("DEBUG_SUPABASE: Fetching applications for userId = '$userId'")

            val results = supabase.postgrest["applications"]
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                    order("applied_at", Order.DESCENDING)
                }
                .decodeList<JobApplication>()

            println("DEBUG_SUPABASE: Successfully retrieved ${results.size} application(s).")
            results
        } catch (e: Exception) {
            println("DEBUG_SUPABASE_ERROR (Fetch): ${e.localizedMessage}")
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Live tracking stream: emits every insert/update/delete on this user's rows in the
     * `applications` table (e.g. an employer moving a status from "Pending" to
     * "Shortlisted" or "Rejected") so "My Applications" updates the instant it happens,
     * without the user needing to pull-to-refresh.
     */
    fun observeApplicationsForUser(userId: String): Flow<ApplicationTrackingEvent> = callbackFlow {
        if (userId.isBlank()) {
            close()
            return@callbackFlow
        }

        val channel = supabase.realtime.channel("applications_tracking_${userId}_${System.currentTimeMillis()}")
        val collectJob = launch {
            channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "applications"
                filter = "user_id=eq.$userId"
            }.collect { action ->
                try {
                    when (action) {
                        is PostgresAction.Insert, is PostgresAction.Update -> {
                            trySend(ApplicationTrackingEvent.Upserted(action.decodeRecord<JobApplication>()))
                        }
                        is PostgresAction.Delete -> {
                            val id = action.oldRecord["id"]?.jsonPrimitive?.contentOrNull
                            if (!id.isNullOrBlank()) trySend(ApplicationTrackingEvent.Removed(id))
                        }
                        else -> {}
                    }
                } catch (e: Exception) {
                    Log.e("ApplicationRepository", "Error decoding realtime application event", e)
                }
            }
        }

        try {
            channel.subscribe()
        } catch (e: Exception) {
            Log.e("ApplicationRepository", "Error subscribing to applications realtime channel", e)
        }

        awaitClose {
            collectJob.cancel()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    channel.unsubscribe()
                    supabase.realtime.removeChannel(channel)
                } catch (e: Exception) {
                    Log.e("ApplicationRepository", "Error tearing down applications realtime channel", e)
                }
            }
        }
    }

    /** Lets an employer move an application through the pipeline (Pending -> Shortlisted / Interview / Rejected / Accepted). */
    suspend fun updateApplicationStatus(applicationId: String, newStatus: String): Boolean = withContext(Dispatchers.IO) {
        try {
            supabase.postgrest["applications"].update(
                mapOf("status" to newStatus)
            ) {
                filter { eq("id", applicationId) }
            }
            true
        } catch (e: Exception) {
            println("DEBUG_SUPABASE_ERROR (Status update): ${e.localizedMessage}")
            e.printStackTrace()
            false
        }
    }

    suspend fun applyForJob(application: JobApplication): Boolean = withContext(Dispatchers.IO) {
        try {
            println("DEBUG_SUPABASE: Inserting application for userId = '${application.userId}'")

            // Map fields explicitly to prevent empty string UUID errors on 'id'
            supabase.postgrest["applications"].insert(
                buildMap {
                    put("job_id", application.jobId)
                    put("user_id", application.userId)
                    put("job_title", application.jobTitle)
                    put("company_name", application.companyName)
                    if (application.employerId.isNotBlank()) {
                        put("employer_id", application.employerId)
                    }
                    put("applicant_name", application.applicantName)
                    put("applicant_email", application.applicantEmail)
                    put("cover_letter", application.coverLetter)
                    put("resume_url", application.resumeUrl)
                    put("status", application.status)
                    if (application.appliedAt.isNotBlank()) {
                        put("applied_at", application.appliedAt)
                    }
                }
            )

            println("DEBUG_SUPABASE: Insert successful!")
            true
        } catch (e: Exception) {
            println("DEBUG_SUPABASE_ERROR (Insert): ${e.localizedMessage}")
            e.printStackTrace()
            false
        }
    }

    // Alias helper function
    suspend fun submitApplication(application: JobApplication): Boolean {
        return applyForJob(application)
    }
}