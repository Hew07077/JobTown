package com.example.jobtown.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class UserRole {
    @SerialName("JOB_SEEKER")
    JOB_SEEKER,

    @SerialName("EMPLOYER")
    EMPLOYER
}

@Serializable
data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val role: UserRole = UserRole.JOB_SEEKER,
    val bio: String = "",
    val phone: String = "",
    val location: String = "",
    val avatarUrl: String = "",
    val createdAt: String = ""
)