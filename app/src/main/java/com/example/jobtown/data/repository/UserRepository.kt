@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)

package com.example.jobtown.data.repository

import android.util.Log
import com.example.jobtown.data.SupabaseClient
import com.example.jobtown.data.model.InterviewSchedule
import com.example.jobtown.data.model.Job
import com.example.jobtown.data.model.JobApplication
import com.example.jobtown.data.model.ProfileEntry
import com.example.jobtown.data.model.User
import com.example.jobtown.data.model.UserProfile
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

data class AvatarHistoryItem(
    val fileName: String,
    val path: String,
    val url: String
)

object UserRepository {

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

            if (user.role != UserRole.EMPLOYER) {
                if (persistProfileEntries(user)) {
                    saved = true
                } else if (saved) {
                    lastUserSaveError = lastUserSaveError
                        ?: "Account saved, but education/experience/certificates could not be saved."
                    return@withContext false
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

        if (cleanId.isNotBlank()) {
            fetchEmployerById(cleanId)?.let { return@withContext mergeProfile(it) }
            fetchUserById(cleanId)?.let { return@withContext it }
            fetchJobSeekerById(cleanId)?.let { return@withContext mergeProfile(it) }
        }
        if (cleanEmail.isNotBlank()) {
            fetchEmployerByEmail(cleanEmail)?.let { return@withContext mergeProfile(it) }
            findUserByEmail(cleanEmail)?.let { return@withContext mergeProfile(it) }
            fetchJobSeekerByEmail(cleanEmail)?.let { return@withContext mergeProfile(it) }
        }
        null
    }

    suspend fun updateUserInSupabase(user: User): Boolean = withContext(Dispatchers.IO) {
        saveUserToSupabase(user)
    }

    suspend fun fetchSavedAddresses(userId: String): List<String> = withContext(Dispatchers.IO) {
        val user = fetchUserById(userId) ?: return@withContext emptyList()
        if (user.location.isBlank()) emptyList()
        else user.location.split(";").map { it.trim() }.filter { it.isNotBlank() }
    }

    suspend fun addSavedAddressToEmployer(userId: String, newAddress: String): Boolean = withContext(Dispatchers.IO) {
        val trimmedAddress = newAddress.trim()
        if (trimmedAddress.isBlank() || userId.isBlank()) return@withContext false

        try {
            val user = fetchUserById(userId) ?: return@withContext false
            val existingAddresses = user.location.split(";")
                .map { it.trim() }
                .filter { it.isNotBlank() }

            if (!existingAddresses.any { it.equals(trimmedAddress, ignoreCase = true) }) {
                val updatedAddressesList = existingAddresses + trimmedAddress
                val updatedLocationString = updatedAddressesList.joinToString("; ")
                val updatedUser = user.copy(location = updatedLocationString)
                return@withContext saveUserToSupabase(updatedUser)
            }
            true
        } catch (e: Exception) {
            Log.e("UserRepository", "Error saving new location address", e)
            false
        }
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
        val experience = fetchExperienceEntries(user.id)
        val education = fetchEducationEntries(user.id)
        val certifications = fetchCertificationEntries(user.id)
        if (experience.isEmpty() && education.isEmpty() && certifications.isEmpty()) return user
        return user.copy(
            experienceEntries = experience.ifEmpty { user.experienceEntries },
            educationEntries = education.ifEmpty { user.educationEntries },
            certificationEntries = certifications.ifEmpty { user.certificationEntries }
        )
    }

    private suspend fun persistUserRow(payload: UserWritePayload) {
        try {
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

    private suspend fun persistProfileEntries(user: User): Boolean {
        if (user.id.isBlank()) return false
        return try {
            replaceExperienceEntries(user.id, user.experienceEntries)
            replaceEducationEntries(user.id, user.educationEntries)
            replaceCertificationEntries(user.id, user.certificationEntries)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            lastUserSaveError = describeDbError(e)
            false
        }
    }

    private suspend fun replaceExperienceEntries(userId: String, entries: List<ProfileEntry>) {
        val table = SupabaseClient.client.from("profile_experiences")
        table.delete { filter { eq("user_id", userId) } }
        if (entries.isEmpty()) return
        table.insert(entries.map { entry ->
            ProfileExperienceRow(
                id = entry.id.ifBlank { UUID.randomUUID().toString() },
                userId = userId,
                title = entry.title,
                company = entry.subtitle,
                period = entry.period,
                description = entry.description
            )
        })
    }

    private suspend fun replaceEducationEntries(userId: String, entries: List<ProfileEntry>) {
        val table = SupabaseClient.client.from("profile_educations")
        table.delete { filter { eq("user_id", userId) } }
        if (entries.isEmpty()) return
        table.insert(entries.map { entry ->
            ProfileEducationRow(
                id = entry.id.ifBlank { UUID.randomUUID().toString() },
                userId = userId,
                qualification = entry.title,
                institution = entry.subtitle,
                period = entry.period,
                description = entry.description
            )
        })
    }

    private suspend fun replaceCertificationEntries(userId: String, entries: List<ProfileEntry>) {
        val table = SupabaseClient.client.from("profile_certifications")
        table.delete { filter { eq("user_id", userId) } }
        if (entries.isEmpty()) return
        table.insert(entries.map { entry ->
            ProfileCertificationRow(
                id = entry.id.ifBlank { UUID.randomUUID().toString() },
                userId = userId,
                title = entry.title,
                issueBy = entry.subtitle,
                valid = entry.period,
                fileUrl = entry.fileUrl
            )
        })
    }

    private suspend fun fetchExperienceEntries(userId: String): List<ProfileEntry> = try {
        if (userId.isBlank()) emptyList()
        else SupabaseClient.client.from("profile_experiences")
            .select { filter { eq("user_id", userId) } }
            .decodeList<ProfileExperienceRow>()
            .map { it.toProfileEntry() }
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }

    private suspend fun fetchEducationEntries(userId: String): List<ProfileEntry> = try {
        if (userId.isBlank()) emptyList()
        else SupabaseClient.client.from("profile_educations")
            .select { filter { eq("user_id", userId) } }
            .decodeList<ProfileEducationRow>()
            .map { it.toProfileEntry() }
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }

    private suspend fun fetchCertificationEntries(userId: String): List<ProfileEntry> = try {
        if (userId.isBlank()) emptyList()
        else SupabaseClient.client.from("profile_certifications")
            .select { filter { eq("user_id", userId) } }
            .decodeList<ProfileCertificationRow>()
            .map { it.toProfileEntry() }
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }

    suspend fun uploadAvatar(userId: String, bytes: ByteArray, fileExtension: String): String? =
        withContext(Dispatchers.IO) {
            try {
                val bucket = SupabaseClient.client.storage.from("avatars")
                val path = "logos/$userId/${System.currentTimeMillis()}.$fileExtension"
                bucket.upload(path, bytes, upsert = false)
                bucket.publicUrl(path)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

    suspend fun listAvatarHistory(userId: String): List<AvatarHistoryItem> = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext emptyList()
        try {
            val bucket = SupabaseClient.client.storage.from("avatars")
            val folder = "logos/$userId"
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

    suspend fun deleteAvatar(path: String): Boolean = withContext(Dispatchers.IO) {
        try {
            SupabaseClient.client.storage.from("avatars").delete(path)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

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

    suspend fun fetchUserProfile(userId: String): UserProfile? = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext null
        fetchUserById(userId)?.toUserProfile()
    }

    suspend fun updateUserProfile(profile: UserProfile): Boolean = withContext(Dispatchers.IO) {
        try {
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
        val user = fetchEmployerById(userId)
            ?: fetchUsersTableById(userId)
            ?: fetchJobSeekerById(userId)
        user?.let { mergeProfile(it) }
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
            SupabaseClient.client.from("jobs").upsert(job)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun updateJob(job: Job): Boolean = withContext(Dispatchers.IO) {
        saveJobToSupabase(job)
    }

    suspend fun deleteJob(jobId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            SupabaseClient.client.from("jobs").delete {
                filter { eq("id", jobId) }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun fetchApplicationsForUser(userId: String, isEmployer: Boolean): List<JobApplication> =
        withContext(Dispatchers.IO) {
            try {
                if (isEmployer) {
                    SupabaseClient.client.from("applications")
                        .select {
                            filter { eq("employer_id", userId) }
                        }
                        .decodeList<JobApplication>()
                } else {
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

@Serializable
private data class ProfileExperienceRow(
    val id: String,
    @SerialName("user_id") val userId: String,
    val title: String = "",
    val company: String = "",
    val period: String = "",
    val description: String = ""
)

private fun ProfileExperienceRow.toProfileEntry() = ProfileEntry(
    id = id,
    title = title,
    subtitle = company,
    period = period,
    description = description
)

@Serializable
private data class ProfileEducationRow(
    val id: String,
    @SerialName("user_id") val userId: String,
    val qualification: String = "",
    val institution: String = "",
    val period: String = "",
    val description: String = ""
)

private fun ProfileEducationRow.toProfileEntry() = ProfileEntry(
    id = id,
    title = qualification,
    subtitle = institution,
    period = period,
    description = description
)

@Serializable
private data class ProfileCertificationRow(
    val id: String,
    @SerialName("user_id") val userId: String,
    val title: String = "",
    @SerialName("issue_by") val issueBy: String = "",
    val valid: String = "",
    @SerialName("file_url") val fileUrl: String = ""
)

private fun ProfileCertificationRow.toProfileEntry() = ProfileEntry(
    id = id,
    title = title,
    subtitle = issueBy,
    period = valid,
    fileUrl = fileUrl
)