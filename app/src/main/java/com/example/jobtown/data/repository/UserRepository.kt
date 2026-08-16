package com.example.jobtown.data.repository

import com.example.jobtown.data.InterviewSchedule
import com.example.jobtown.data.Job
import com.example.jobtown.data.JobApplication
import com.example.jobtown.data.SupabaseClient
import com.example.jobtown.data.User
import com.example.jobtown.data.UserProfile
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object UserRepository {

    // --- USERS ---
    suspend fun saveUserToSupabase(user: User): Boolean = withContext(Dispatchers.IO) {
        try {
            // Manually check whether a row with this email already exists,
            // then either UPDATE or INSERT accordingly. This avoids relying
            // on upsert(...) { onConflict = "email" }, whose exact syntax
            // depends on the installed supabase-kt version -- update()/
            // insert() below use the same proven syntax already working
            // elsewhere in this file.
            //
            // This matters because a user's Auth account can end up created
            // (with a fresh id) before their profile row exists -- e.g. a
            // previous attempt crashed between signUpWith() and this call.
            // Without this check, retrying would try to INSERT a second row
            // with the same email and hit the unique constraint
            // ("duplicate key value violates unique constraint users_email_key")
            // instead of updating the existing row.
            val existing = findUserByEmail(user.email)
            if (existing != null) {
                SupabaseClient.client.from("users").update(user) {
                    filter { eq("email", user.email) }
                }
            } else {
                SupabaseClient.client.from("users").insert(user)
            }
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

    // Fetches the skills/location/experience fields job matching relies on.
    // These live as extra columns on the same "users" row (see ProfileViewModel.saveProfile).
    suspend fun fetchUserProfile(userId: String): UserProfile? = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext null
        try {
            SupabaseClient.client.from("users")
                .select {
                    filter { eq("id", userId) }
                }
                .decodeSingleOrNull<UserProfile>()
        } catch (e: Exception) {
            e.printStackTrace()
            null
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