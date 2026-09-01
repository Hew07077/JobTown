@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)

package com.example.jobtown.data.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Databases in this project have stored role as EMPLOYER, employer, Employer,
 * or even "company". With coerceInputValues, an unknown enum silently became
 * JOB_SEEKER — so employers landed on the job-seeker home screen after login.
 */
object UserRoleSerializer : KSerializer<UserRole> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("UserRole", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: UserRole) {
        encoder.encodeString(
            when (value) {
                UserRole.EMPLOYER -> "EMPLOYER"
                UserRole.JOB_SEEKER -> "JOB_SEEKER"
            }
        )
    }

    override fun deserialize(decoder: Decoder): UserRole {
        val normalized = decoder.decodeString().trim().uppercase()
            .replace(' ', '_')
            .replace('-', '_')
        return when {
            normalized == "EMPLOYER" ||
                normalized.contains("EMPLOY") ||
                normalized == "COMPANY" ||
                normalized == "RECRUITER" -> UserRole.EMPLOYER
            else -> UserRole.JOB_SEEKER
        }
    }
}

@Serializable(with = UserRoleSerializer::class)
enum class UserRole {
    JOB_SEEKER,
    EMPLOYER
}

@Serializable
data class User(
    @SerialName("id")
    val id: String = "",

    @SerialName("full_name")
    val name: String = "",

    @SerialName("email")
    val email: String = "",

    // NOT sent to the job_seekers / employers tables -- there is no "password"
    // column there (and there shouldn't be one: Supabase Auth already
    // stores passwords securely). This field only exists so the in-memory
    // User object can be passed straight into signUpWith(Email) / signInWith(Email),
    // which read it directly as a Kotlin property, not via serialization.
    @Transient
    val password: String = "",

    @SerialName("role")
    val role: UserRole = UserRole.JOB_SEEKER,

    @SerialName("company_name")
    val companyName: String = "",

    @SerialName("company_size")
    val companySize: String = "",

    @SerialName("industry")
    val industry: String = "",

    @SerialName("tagline")
    val tagline: String = "",

    @SerialName("website_url")
    val websiteUrl: String = "",

    @SerialName("perks")
    val perks: List<String> = emptyList(),

    @SerialName("skills")
    val skills: String = "",

    @SerialName("experience_level")
    val experienceLevel: String = "",

    @SerialName("portfolio_url")
    val portfolioUrl: String = "",

    @SerialName("bio")
    val bio: String = "",

    @SerialName("phone")
    val phone: String = "",

    @SerialName("location")
    val location: String = "",

    @SerialName("avatar_url")
    val avatarUrl: String = "",

    @SerialName("resume_url")
    val resumeUrl: String = "",

    @SerialName("is_oku")
    val isOku: Boolean = false,

    @Transient
    val experienceEntries: List<ProfileEntry> = emptyList(),

    @Transient
    val educationEntries: List<ProfileEntry> = emptyList(),

    @Transient
    val certificationEntries: List<ProfileEntry> = emptyList(),

    @SerialName("created_at")
    val createdAt: String = ""
)

@Serializable
internal data class UserWritePayload(
    @SerialName("id") val id: String = "",
    @SerialName("email") val email: String = "",
    @SerialName("full_name") val fullName: String = "",
    @SerialName("role") val role: UserRole = UserRole.JOB_SEEKER,
    @SerialName("company_name") val companyName: String = "",
    @SerialName("company_size") val companySize: String = "",
    @SerialName("industry") val industry: String = "",
    @SerialName("tagline") val tagline: String = "",
    @SerialName("website_url") val websiteUrl: String = "",
    @SerialName("perks") val perks: List<String> = emptyList(),
    @SerialName("skills") val skills: String = "",
    @SerialName("experience_level") val experienceLevel: String = "",
    @SerialName("portfolio_url") val portfolioUrl: String = "",
    @SerialName("bio") val bio: String = "",
    @SerialName("phone") val phone: String = "",
    @SerialName("location") val location: String = "",
    @SerialName("avatar_url") val avatarUrl: String = "",
    @SerialName("resume_url") val resumeUrl: String = "",
    @SerialName("is_oku") val isOku: Boolean = false
)

internal fun User.toUserWritePayload(): UserWritePayload = UserWritePayload(
    id = id,
    email = email.trim().lowercase(),
    // For Employers, the "full_name" column in public.users is often used for the company name 
    // if a separate company_name column doesn't exist or is redundant.
    fullName = if (role == UserRole.EMPLOYER && name.isBlank()) companyName else name,
    role = role,
    companyName = companyName,
    companySize = companySize,
    industry = industry,
    tagline = tagline,
    websiteUrl = websiteUrl,
    perks = perks,
    skills = skills,
    experienceLevel = experienceLevel,
    portfolioUrl = portfolioUrl,
    bio = bio,
    phone = phone,
    location = location,
    avatarUrl = avatarUrl,
    resumeUrl = resumeUrl,
    isOku = isOku
)

internal fun User.toUserProfile(): UserProfile = UserProfile(
    id = id,
    phone = phone,
    location = location,
    tagline = tagline,
    websiteUrl = websiteUrl,
    perks = perks,
    skills = skills,
    experienceLevel = experienceLevel,
    portfolioUrl = portfolioUrl,
    bio = bio,
    experienceEntries = experienceEntries,
    educationEntries = educationEntries,
    certificationEntries = certificationEntries
)