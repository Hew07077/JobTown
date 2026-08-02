package com.example.jobtown.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val id: String,
    val phone: String? = null,
    val location: String? = null,
    val skills: List<String> = emptyList(),
    @SerialName("experience_level")
    val experienceLevel: String? = null,
    @SerialName("portfolio_url")
    val portfolioUrl: String? = null,
    val bio: String? = null
)