package com.example.jobtown.data.repository

import com.example.jobtown.data.InterviewSchedule
import com.example.jobtown.data.Job
import com.example.jobtown.data.JobApplication
import com.example.jobtown.data.SupabaseClient
import com.example.jobtown.data.User
import com.example.jobtown.data.UserProfile
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// One previously-uploaded profile photo, as returned by
// UserRepository.listAvatarHistory(). `path` is the full Storage path
// (needed for deleteAvatar), `url` is the ready-to-display public URL.
data class AvatarHistoryItem(
    val fileName: String,
    val path: String,
    val url: String
)

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

    // --- AVATAR ---
    // Uploads image bytes to the "avatars" Storage bucket (must already exist
    // and be set to Public in the Supabase dashboard) and returns its public
    // URL, or null on failure. Stored under "logos/{userId}/" (same bucket
    // company logos use) so both job-seeker avatars and employer logos live
    // together. Unlike before, this does NOT overwrite the previous photo --
    // each upload gets its own timestamped filename inside the user's own
    // subfolder, so old photos stay in Storage and the user can switch back
    // to one later via listAvatarHistory() / can remove one via deleteAvatar().
    suspend fun uploadAvatar(userId: String, bytes: ByteArray, fileExtension: String): String? =
        withContext(Dispatchers.IO) {
            try {
                val bucket = SupabaseClient.client.storage.from("avatars")
                val path = "logos/$userId/${System.currentTimeMillis()}.$fileExtension"
                // NOTE: storage-kt 2.0.0 does NOT have the { upsert = true }
                // options DSL -- that was added in 3.0.0. In 2.0.0, upsert is
                // a plain named Boolean parameter on upload() itself. upsert
                // is false here since every filename is already unique.
                bucket.upload(path, bytes, upsert = false)
                bucket.publicUrl(path)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

    // Lists every photo previously uploaded for this user (newest first --
    // filenames are millis-since-epoch, so a plain descending string sort
    // works), so the profile screen can offer "switch back to an old photo"
    // instead of only ever letting you upload a brand new one.
    suspend fun listAvatarHistory(userId: String): List<AvatarHistoryItem> = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext emptyList()
        try {
            val bucket = SupabaseClient.client.storage.from("avatars")
            val folder = "logos/$userId"
            // Positional arg (not named) -- avoids relying on the exact
            // parameter name, which isn't confirmed for this pinned version.
            bucket.list(folder)
                .mapNotNull { item -> item.name?.takeIf { it.isNotBlank() } }
                .sortedDescending()
                .map { fileName ->
                    val fullPath = "$folder/$fileName"
                    AvatarHistoryItem(fileName = fileName, path = fullPath, url = bucket.publicUrl(fullPath))
                }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // Permanently removes one previously-uploaded photo from Storage.
    suspend fun deleteAvatar(path: String): Boolean = withContext(Dispatchers.IO) {
        try {
            SupabaseClient.client.storage.from("avatars").delete(path)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Fetches the profile fields (skills, location, tagline, website, perks, etc.)
    // from the "users" table.
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

    // Updates extra profile fields (tagline, website_url, perks, skills, etc.) for an existing user row.
    suspend fun updateUserProfile(profile: UserProfile): Boolean = withContext(Dispatchers.IO) {
        if (profile.id.isBlank()) return@withContext false
        try {
            SupabaseClient.client.from("users").update(profile) {
                filter { eq("id", profile.id) }
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

    // Fetch only jobs posted by a specific Employer
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
                    // Filters applications by the employer's ID
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

    // Allow Employers to update application status (e.g., Pending -> Shortlisted / Rejected)
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

    // Fetch a full User row by ID
    suspend fun fetchUserById(userId: String): User? = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext null
        try {
            SupabaseClient.client.from("users")
                .select {
                    filter { eq("id", userId) }
                }
                .decodeSingleOrNull<User>()
        } catch (e: Exception) {
            e.printStackTrace()
            null
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