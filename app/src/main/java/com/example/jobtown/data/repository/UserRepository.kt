package com.example.jobtown.data.repository

import com.example.jobtown.data.InterviewSchedule
import com.example.jobtown.data.Job
import com.example.jobtown.data.JobApplication
import com.example.jobtown.data.SupabaseClient
import com.example.jobtown.data.User
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object UserRepository {

    // --- USERS ---
    suspend fun saveUserToSupabase(user: User): Boolean = withContext(Dispatchers.IO) {
        try {
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
    // Fetch all active jobs for Job Seekers
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

    // NEW: Fetch only jobs posted by a specific Employer
    suspend fun fetchJobsByEmployer(employerId: String): List<Job> = withContext(Dispatchers.IO) {
        try {
            SupabaseClient.client.from("jobs")
                .select {
                    filter {
                        eq("employer_id", employerId)
                    }
                }
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
    suspend fun fetchApplicationsForUser(userId: String, isEmployer: Boolean): List<JobApplication> =
        withContext(Dispatchers.IO) {
            try {
                if (isEmployer) {
                    // Fixed: Filters applications by the employer's ID so they don't see ALL applications in the app
                    SupabaseClient.client.from("applications")
                        .select {
                            filter { eq("employer_id", userId) }
                        }
                        .decodeList<JobApplication>()
                } else {
                    // Job Seekers see applications where they are the applicant
                    SupabaseClient.client.from("applications")
                        .select {
                            filter { eq("user_id", userId) }
                        }
                        .decodeList<JobApplication>()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }

    suspend fun saveApplicationToSupabase(application: JobApplication): Boolean =
        withContext(Dispatchers.IO) {
            try {
                SupabaseClient.client.from("applications").insert(application)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

    // NEW: Allow Employers to update application status (e.g., Pending -> Shortlisted / Rejected)
    suspend fun updateApplicationStatus(applicationId: String, newStatus: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                SupabaseClient.client.from("applications").update(
                    mapOf("status" to newStatus)
                ) {
                    filter { eq("id", applicationId) }
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

    // --- SCHEDULES ---
    suspend fun fetchSchedulesForUser(userId: String, isEmployer: Boolean): List<InterviewSchedule> =
        withContext(Dispatchers.IO) {
            try {
                SupabaseClient.client.from("schedules")
                    .select {
                        filter {
                            if (isEmployer) eq("employer_id", userId) else eq(
                                "applicant_id",
                                userId
                            )
                        }
                    }
                    .decodeList<InterviewSchedule>()
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }

    suspend fun saveScheduleToSupabase(schedule: InterviewSchedule): Boolean =
        withContext(Dispatchers.IO) {
            try {
                SupabaseClient.client.from("schedules").insert(schedule)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
}