@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)

package com.example.jobtown.data.repository

import com.example.jobtown.data.SupabaseClient
import com.example.jobtown.data.model.InterviewSchedule
import com.example.jobtown.data.model.Job
import com.example.jobtown.data.model.JobApplication
import com.example.jobtown.data.model.User
import com.example.jobtown.data.model.UserProfile
import com.example.jobtown.data.model.UserProfileCore
import com.example.jobtown.data.model.UserWritePayload
import com.example.jobtown.data.model.toUserProfile
import com.example.jobtown.data.model.toUserWritePayload
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

// One previously-uploaded profile photo, as returned by
// UserRepository.listAvatarHistory(). `path` is the full Storage path
// (needed for deleteAvatar), `url` is the ready-to-display public URL.
data class AvatarHistoryItem(
    val fileName: String,
    val path: String,
    val url: String
)

object UserRepository {

    // public.users holds the account row (same format as the original app).
    // Extra match fields and certificates go to public.user_profiles.

    var lastUserSaveError: String? = null
        private set

    suspend fun saveUserToSupabase(user: User): Boolean = withContext(Dispatchers.IO) {
        lastUserSaveError = null
        try {
            if (user.id.isBlank()) {
                lastUserSaveError = "Could not create your account session. Please try again."
                return@withContext false
            }
            persistUserRow(user.toUserWritePayload(), findUserByEmail(user.email) ?: fetchUserById(user.id))
            if (!upsertUserProfileRow(user.toUserProfile())) {
                lastUserSaveError = "Account saved, but extra profile details were blocked by the database (RLS on user_profiles)."
                return@withContext false
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            lastUserSaveError = describeDbError(e)
            false
        }
    }

    suspend fun findUserByEmail(email: String): User? = withContext(Dispatchers.IO) {
        val trimmed = email.trim()
        if (trimmed.isBlank()) return@withContext null
        fetchUserByEmailExact(trimmed) ?: fetchUserByEmailExact(trimmed.lowercase())
    }

    suspend fun resolveUserForSession(authUserId: String?, email: String): User? = withContext(Dispatchers.IO) {
        val cleanId = authUserId?.trim().orEmpty()
        val cleanEmail = email.trim()
        if (cleanId.isNotBlank()) {
            fetchUserById(cleanId)?.let { return@withContext mergeProfile(it) }
        }
        if (cleanEmail.isNotBlank()) {
            findUserByEmail(cleanEmail)?.let { return@withContext mergeProfile(it) }
        }
        null
    }

    suspend fun updateUserInSupabase(user: User): Boolean = withContext(Dispatchers.IO) {
        lastUserSaveError = null
        try {
            SupabaseClient.client.from("users").update(user.toUserWritePayload()) {
                filter { eq("id", user.id) }
            }
            upsertUserProfileRow(user.toUserProfile())
            true
        } catch (e: Exception) {
            e.printStackTrace()
            lastUserSaveError = describeDbError(e)
            false
        }
    }

    private suspend fun fetchUserByEmailExact(email: String): User? = try {
        SupabaseClient.client.from("users")
            .select { filter { eq("email", email) } }
            .decodeSingleOrNull<User>()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }

    private suspend fun mergeProfile(user: User): User {
        val profile = fetchProfileRow(user.id) ?: return user
        return user.copy(
            phone = profile.phone ?: user.phone,
            location = profile.location ?: user.location,
            tagline = profile.tagline ?: user.tagline,
            websiteUrl = profile.websiteUrl ?: user.websiteUrl,
            perks = profile.perks.ifEmpty { user.perks },
            skills = profile.skills ?: user.skills,
            experienceLevel = profile.experienceLevel ?: user.experienceLevel,
            portfolioUrl = profile.portfolioUrl ?: user.portfolioUrl,
            bio = profile.bio ?: user.bio,
            experienceEntries = profile.experienceEntries.ifEmpty { user.experienceEntries },
            educationEntries = profile.educationEntries.ifEmpty { user.educationEntries },
            certificationEntries = profile.certificationEntries.ifEmpty { user.certificationEntries }
        )
    }

    private suspend fun persistUserRow(payload: UserWritePayload, existing: User?) {
        if (existing != null) {
            SupabaseClient.client.from("users").update(payload) {
                filter { eq("id", existing.id) }
            }
        } else {
            SupabaseClient.client.from("users").insert(payload)
        }
    }

    private fun isJwtDecodeError(e: Exception): Boolean {
        val message = e.message.orEmpty()
        return message.contains("PGRST301", ignoreCase = true) ||
            message.contains("No suitable key", ignoreCase = true) ||
            message.contains("wrong key type", ignoreCase = true)
    }

    private fun describeDbError(e: Exception): String {
        val message = e.message.orEmpty()
        val firstLine = message.lineSequence().firstOrNull { it.isNotBlank() }.orEmpty()
        return when {
            isJwtDecodeError(e) ->
                "Signed in, but the database could not verify the login token. Try Save again."
            message.contains("row-level security", ignoreCase = true) || message.contains("42501") ->
                "Signed in, but saving was blocked by the database (RLS on users)."
            message.contains("PGRST205", ignoreCase = true) || message.contains("Could not find the table", ignoreCase = true) ->
                "This Supabase project does not have a users table."
            message.contains("PGRST204", ignoreCase = true) ||
                (message.contains("Could not find", ignoreCase = true) && message.contains("column", ignoreCase = true)) ->
                "A profile field is not in this database schema."
            message.contains("duplicate key", ignoreCase = true) || message.contains("unique constraint", ignoreCase = true) ->
                "This email already has a profile. Go back and log in."
            message.contains("invalid input syntax", ignoreCase = true) ->
                "A profile value does not match this database column type."
            firstLine.isNotBlank() &&
                firstLine.length < 180 &&
                !firstLine.contains("apikey", ignoreCase = true) &&
                !firstLine.contains("Request:", ignoreCase = true) &&
                !firstLine.contains("sb_publishable", ignoreCase = true) ->
                firstLine
            else -> "Failed to save profile details. Please try again."
        }
    }

    private suspend fun upsertUserProfileRow(profile: UserProfile): Boolean {
        if (profile.id.isBlank()) return false
        val core = UserProfileCore(
            id = profile.id,
            phone = profile.phone,
            location = profile.location,
            tagline = profile.tagline,
            websiteUrl = profile.websiteUrl,
            perks = profile.perks,
            skills = profile.skills,
            experienceLevel = profile.experienceLevel,
            portfolioUrl = profile.portfolioUrl,
            bio = profile.bio
        )
        return try {
            SupabaseClient.client.from("user_profiles").upsert(core)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private suspend fun fetchProfileRow(userId: String): UserProfile? = try {
        if (userId.isBlank()) null
        else SupabaseClient.client.from("user_profiles")
            .select { filter { eq("id", userId) } }
            .decodeSingleOrNull<UserProfile>()
    } catch (e: Exception) {
        e.printStackTrace()
        try {
            SupabaseClient.client.from("user_profiles")
                .select { filter { eq("id", userId) } }
                .decodeSingleOrNull<UserProfileCore>()
                ?.let {
                    UserProfile(
                        id = it.id,
                        phone = it.phone,
                        location = it.location,
                        tagline = it.tagline,
                        websiteUrl = it.websiteUrl,
                        perks = it.perks,
                        skills = it.skills,
                        experienceLevel = it.experienceLevel,
                        portfolioUrl = it.portfolioUrl,
                        bio = it.bio
                    )
                }
        } catch (fallback: Exception) {
            fallback.printStackTrace()
            null
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

    // --- RESUME ---
    // Uploads a PDF to the "resumes" Storage bucket (create it in the
    // Supabase dashboard, set to Public, before this will work) and returns
    // its public URL, or null on failure. Path is keyed by userId with
    // upsert = true, so re-uploading replaces the previous resume in place
    // rather than accumulating old versions (unlike the avatar history
    // feature above -- a resume only needs to keep the latest copy).
    suspend fun uploadResume(userId: String, bytes: ByteArray): String? =
        withContext(Dispatchers.IO) {
            try {
                val bucket = SupabaseClient.client.storage.from("resumes")
                val path = "$userId.pdf"
                bucket.upload(path, bytes, upsert = true)
                bucket.publicUrl(path)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

    // --- CERTIFICATES ---
    // Uploads a certificate PDF or image to the "certificates" bucket.
    // Path is {userId}/{uuid}.ext so RLS (name LIKE auth.uid() || '/%') matches.
    suspend fun uploadCertificate(userId: String, bytes: ByteArray, fileExtension: String): String? =
        withContext(Dispatchers.IO) {
            try {
                val bucket = SupabaseClient.client.storage.from("certificates")
                val ext = fileExtension.trim().lowercase().ifBlank { "pdf" }
                val path = "$userId/${UUID.randomUUID()}.$ext"
                bucket.upload(path, bytes, upsert = false)
                bucket.publicUrl(path)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

    // Fetches extra profile fields from user_profiles, falling back to the users row.
    suspend fun fetchUserProfile(userId: String): UserProfile? = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext null
        fetchProfileRow(userId) ?: fetchUserById(userId)?.toUserProfile()
    }

    suspend fun updateUserProfile(profile: UserProfile): Boolean = withContext(Dispatchers.IO) {
        upsertUserProfileRow(profile)
    }

    suspend fun fetchUserById(userId: String): User? = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext null
        try {
            val row = SupabaseClient.client.from("users")
                .select { filter { eq("id", userId) } }
                .decodeSingleOrNull<User>()
            row?.let { mergeProfile(it) }
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
    // Live table is public.interview_schedules (not "schedules").
    // Seeker column is user_id (not applicant_id).
    suspend fun fetchSchedulesForUser(userId: String, isEmployer: Boolean): List<InterviewSchedule> =
        withContext(Dispatchers.IO) {
            try {
                val column = if (isEmployer) "employer_id" else "user_id"
                SupabaseClient.client.from("interview_schedules")
                    .select {
                        filter { eq(column, userId) }
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
                SupabaseClient.client.from("interview_schedules").insert(schedule)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
}