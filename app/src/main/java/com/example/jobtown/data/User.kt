package com.example.jobtown.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
enum class UserRole {
    @SerialName("JOB_SEEKER")
    JOB_SEEKER,

    @SerialName("EMPLOYER")
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

    // NOT sent to / read from the "users" table -- there is no "password"
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

    @SerialName("created_at")
    val createdAt: String = ""
)