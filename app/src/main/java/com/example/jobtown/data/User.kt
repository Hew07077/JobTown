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

    @Transient
    val password: String = "",

    @SerialName("role")
    val role: UserRole = UserRole.JOB_SEEKER,

    @SerialName("company_name")
    val companyName: String = "",

    @SerialName("bio")
    val bio: String = "",

    @SerialName("phone")
    val phone: String = "",

    @SerialName("location")
    val location: String = "",

    @SerialName("avatar_url")
    val avatarUrl: String = "",

    @SerialName("created_at")
    val createdAt: String = ""
)
