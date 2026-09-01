@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)

package com.example.jobtown.data.repository

import android.util.Log
import com.example.jobtown.data.SupabaseClient
import com.example.jobtown.data.model.InterviewSchedule
import com.example.jobtown.data.model.Job
import com.example.jobtown.data.model.JobApplication
import com.example.jobtown.data.model.User
import com.example.jobtown.data.model.UserProfile
import com.example.jobtown.data.model.UserProfileCore
import com.example.jobtown.data.model.UserRole
import com.example.jobtown.data.model.UserWritePayload
import com.example.jobtown.data.model.toUserProfile
import com.example.jobtown.data.model.toUserWritePayload
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
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

            var saved = false
            var lastError: Exception? = null

            try {
                persistUserRow(user.toUserWritePayload())
                saved = true
            } catch (e: Exception) {
                lastError = e
                if (!isMissingRelation(e)) {
                    lastUserSaveError = describeDbError(e)
                }
            }

            try {
                if (user.role == UserRole.EMPLOYER) {
                    persistEmployerRow(user)
                } else {
                    persistJobSeekerRow(user)
                }
                saved = true
            } catch (e: Exception) {
                lastError = e
                if (!saved && !isMissingRelation(e)) {
                    lastUserSaveError = describeDbError(e)
                }
            }

            if (!saved) {
                lastUserSaveError = lastUserSaveError
                    ?: lastError?.let { describeDbError(it) }
                    ?: "Failed to save profile details. Please try again."
            }
            saved
        } catch (e: Exception) {
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

        // Employers table wins: some databases keep a users row with the
        // default JOB_SEEKER role from the auth trigger, even after signup
        // as a company.
        if (cleanId.isNotBlank()) {
            fetchEmployerById(cleanId)?.let { return@withContext it }
            fetchUserById(cleanId)?.let { return@withContext it }
            fetchJobSeekerById(cleanId)?.let { return@withContext it }
        }
        if (cleanEmail.isNotBlank()) {
            fetchEmployerByEmail(cleanEmail)?.let { return@withContext it }
            findUserByEmail(cleanEmail)?.let { return@withContext it }
            fetchJobSeekerByEmail(cleanEmail)?.let { return@withContext it }
        }
        null
    }

    suspend fun updateUserInSupabase(user: User): Boolean = withContext(Dispatchers.IO) {
        saveUserToSupabase(user)
    }

    private suspend fun fetchUserByEmailExact(email: String): User? = try {
        SupabaseClient.client.from("users")
            .select { filter { eq("email", email) } }
            .decodeSingleOrNull<User>()
    } catch (e: Exception) {
        if (!isMissingRelation(e)) e.printStackTrace()
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

    private suspend fun persistUserRow(payload: UserWritePayload) {
        try {
            // Using upsert is safer as it handles both insert and update based on the primary key (id)
            SupabaseClient.client.from("users").upsert(payload)
        } catch (e: Exception) {
            throw e
        }
    }

    private fun isMissingRelation(e: Exception): Boolean {
        val message = e.message.orEmpty()
        return message.contains("42P01") ||
            message.contains("does not exist", ignoreCase = true) ||
            message.contains("PGRST205", ignoreCase = true) ||
            message.contains("Could not find the table", ignoreCase = true)
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
            message.contains("PGRST301", ignoreCase = true) ->
                "Database session expired or token invalid. Please try logging in again."
            message.contains("PGRST204", ignoreCase = true) ->
                "Schema mismatch: One of the profile fields doesn't exist in the database table."
            message.contains("42501") || message.contains("row-level security", ignoreCase = true) ->
                "Permission denied: The database RLS policies are blocking this write."
            message.contains("23505") || message.contains("duplicate key", ignoreCase = true) ->
                "An account with this email already has a profile."
            isJwtDecodeError(e) ->
                "Signed in, but the database could not verify the login token. Try Save again."
            firstLine.isNotBlank() && firstLine.length < 180 && !firstLine.contains("apikey") ->
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
            SupabaseClient.client.from("profiles").upsert(core)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private suspend fun fetchProfileRow(userId: String): UserProfile? = try {
        if (userId.isBlank()) null
        else SupabaseClient.client.from("profiles")
            .select { filter { eq("id", userId) } }
            .decodeSingleOrNull<UserProfile>()
    } catch (e: Exception) {
        e.printStackTrace()
        try {
            SupabaseClient.client.from("profiles")
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
        // Since everything is in the 'users' table now, fetch the user and convert it
        fetchUserById(userId)?.toUserProfile()
    }

    suspend fun updateUserProfile(profile: UserProfile): Boolean = withContext(Dispatchers.IO) {
        try {
            // Map UserProfile back to the users table payload if needed, 
            // but saveUserToSupabase is already doing this.
            val user = fetchUserById(profile.id) ?: return@withContext false
            val updatedUser = user.copy(
                phone = profile.phone ?: user.phone,
                location = profile.location ?: user.location,
                tagline = profile.tagline ?: user.tagline,
                websiteUrl = profile.websiteUrl ?: user.websiteUrl,
                perks = profile.perks.ifEmpty { user.perks },
                skills = profile.skills ?: user.skills,
                experienceLevel = profile.experienceLevel ?: user.experienceLevel,
                portfolioUrl = profile.portfolioUrl ?: user.portfolioUrl,
                bio = profile.bio ?: user.bio
            )
            saveUserToSupabase(updatedUser)
        } catch (e: Exception) {
            Log.e("UserRepository", "Error updating user profile", e)
            false
        }
    }

    suspend fun fetchUserById(userId: String): User? = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext null
        fetchEmployerById(userId)
            ?: fetchUsersTableById(userId)
            ?: fetchJobSeekerById(userId)
    }

    private suspend fun fetchUsersTableById(userId: String): User? = try {
        SupabaseClient.client.from("users")
            .select { filter { eq("id", userId) } }
            .decodeSingleOrNull<User>()
    } catch (e: Exception) {
        if (!isMissingRelation(e)) {
            Log.e("UserRepository", "Error fetching user by ID: $userId", e)
        }
        null
    }

    private suspend fun fetchEmployerById(userId: String): User? = try {
        SupabaseClient.client.from("employers")
            .select { filter { eq("id", userId) } }
            .decodeSingleOrNull<EmployerTableRecord>()
            ?.toUser()
    } catch (e: Exception) {
        if (!isMissingRelation(e)) {
            Log.e("UserRepository", "Error fetching employer by ID: $userId", e)
        }
        null
    }

    private suspend fun fetchEmployerByEmail(email: String): User? = try {
        val trimmed = email.trim()
        SupabaseClient.client.from("employers")
            .select { filter { eq("email", trimmed) } }
            .decodeSingleOrNull<EmployerTableRecord>()
            ?.toUser()
            ?: if (trimmed != trimmed.lowercase()) {
                SupabaseClient.client.from("employers")
                    .select { filter { eq("email", trimmed.lowercase()) } }
                    .decodeSingleOrNull<EmployerTableRecord>()
                    ?.toUser()
            } else null
    } catch (e: Exception) {
        if (!isMissingRelation(e)) e.printStackTrace()
        null
    }

    private suspend fun fetchJobSeekerById(userId: String): User? = try {
        SupabaseClient.client.from("job_seekers")
            .select { filter { eq("id", userId) } }
            .decodeSingleOrNull<JobSeekerTableRecord>()
            ?.toUser()
    } catch (e: Exception) {
        if (!isMissingRelation(e)) {
            Log.e("UserRepository", "Error fetching job seeker by ID: $userId", e)
        }
        null
    }

    private suspend fun fetchJobSeekerByEmail(email: String): User? = try {
        val trimmed = email.trim()
        SupabaseClient.client.from("job_seekers")
            .select { filter { eq("email", trimmed) } }
            .decodeSingleOrNull<JobSeekerTableRecord>()
            ?.toUser()
            ?: if (trimmed != trimmed.lowercase()) {
                SupabaseClient.client.from("job_seekers")
                    .select { filter { eq("email", trimmed.lowercase()) } }
                    .decodeSingleOrNull<JobSeekerTableRecord>()
                    ?.toUser()
            } else null
    } catch (e: Exception) {
        if (!isMissingRelation(e)) e.printStackTrace()
        null
    }

    private suspend fun persistEmployerRow(user: User) {
        SupabaseClient.client.from("employers").upsert(
            EmployerWritePayload(
                id = user.id,
                email = user.email.trim().lowercase(),
                companyName = user.companyName.ifBlank { user.name },
                companySize = user.companySize,
                industry = user.industry,
                tagline = user.tagline,
                websiteUrl = user.websiteUrl.ifBlank { user.portfolioUrl },
                perks = user.perks,
                phone = user.phone,
                location = user.location,
                bio = user.bio,
                avatarUrl = user.avatarUrl
            )
        )
    }

    private suspend fun persistJobSeekerRow(user: User) {
        SupabaseClient.client.from("job_seekers").upsert(
            JobSeekerWritePayload(
                id = user.id,
                email = user.email.trim().lowercase(),
                fullName = user.name,
                phone = user.phone,
                location = user.location,
                skills = user.skills,
                experienceLevel = user.experienceLevel,
                portfolioUrl = user.portfolioUrl,
                bio = user.bio,
                avatarUrl = user.avatarUrl,
                resumeUrl = user.resumeUrl,
                isOku = user.isOku
            )
        )
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

@Serializable
private data class EmployerTableRecord(
    val id: String = "",
    val email: String = "",
    @SerialName("company_name") val companyName: String = "",
    @SerialName("company_size") val companySize: String = "",
    val industry: String = "",
    val tagline: String = "",
    @SerialName("website_url") val websiteUrl: String = "",
    val perks: List<String> = emptyList(),
    val phone: String = "",
    val location: String = "",
    val bio: String = "",
    @SerialName("avatar_url") val avatarUrl: String = "",
    @SerialName("created_at") val createdAt: String = ""
)

private fun EmployerTableRecord.toUser() = User(
    id = id,
    email = email,
    name = companyName,
    role = UserRole.EMPLOYER,
    companyName = companyName,
    companySize = companySize,
    industry = industry,
    tagline = tagline,
    websiteUrl = websiteUrl,
    perks = perks,
    bio = bio,
    phone = phone,
    location = location,
    avatarUrl = avatarUrl,
    createdAt = createdAt
)

@Serializable
private data class EmployerWritePayload(
    val id: String,
    val email: String,
    @SerialName("company_name") val companyName: String = "",
    @SerialName("company_size") val companySize: String = "",
    val industry: String = "",
    val tagline: String = "",
    @SerialName("website_url") val websiteUrl: String = "",
    val perks: List<String> = emptyList(),
    val phone: String = "",
    val location: String = "",
    val bio: String = "",
    @SerialName("avatar_url") val avatarUrl: String = ""
)

@Serializable
private data class JobSeekerTableRecord(
    val id: String = "",
    val email: String = "",
    @SerialName("full_name") val fullName: String = "",
    val phone: String = "",
    val location: String = "",
    val skills: String = "",
    @SerialName("experience_level") val experienceLevel: String = "",
    @SerialName("portfolio_url") val portfolioUrl: String = "",
    val bio: String = "",
    @SerialName("avatar_url") val avatarUrl: String = "",
    @SerialName("resume_url") val resumeUrl: String = "",
    @SerialName("is_oku") val isOku: Boolean = false,
    @SerialName("created_at") val createdAt: String = ""
)

private fun JobSeekerTableRecord.toUser() = User(
    id = id,
    email = email,
    name = fullName,
    role = UserRole.JOB_SEEKER,
    phone = phone,
    location = location,
    skills = skills,
    experienceLevel = experienceLevel,
    portfolioUrl = portfolioUrl,
    bio = bio,
    avatarUrl = avatarUrl,
    resumeUrl = resumeUrl,
    isOku = isOku,
    createdAt = createdAt
)

@Serializable
private data class JobSeekerWritePayload(
    val id: String,
    val email: String,
    @SerialName("full_name") val fullName: String = "",
    val phone: String = "",
    val location: String = "",
    val skills: String = "",
    @SerialName("experience_level") val experienceLevel: String = "",
    @SerialName("portfolio_url") val portfolioUrl: String = "",
    val bio: String = "",
    @SerialName("avatar_url") val avatarUrl: String = "",
    @SerialName("resume_url") val resumeUrl: String = "",
    @SerialName("is_oku") val isOku: Boolean = false
)