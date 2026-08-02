package com.example.jobtown.data.repository

import com.example.jobtown.data.JobApplication
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ApplicationRepository(private val supabase: SupabaseClient) {

    suspend fun getApplicationsForUser(userId: String): List<JobApplication> = withContext(Dispatchers.IO) {
        try {
            println("DEBUG_SUPABASE: Fetching applications for userId = '$userId'")

            val results = supabase.postgrest["applications"]
                .select {
                    filter {
                        eq("user_id", userId)
                    }
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