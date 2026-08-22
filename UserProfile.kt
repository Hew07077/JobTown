package com.example.jobtown.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    @SerialName("id")
    val id: String,

    @SerialName("phone")
    val phone: String? = null,

    @SerialName("location")
    val location: String? = null,

    @SerialName("tagline")
    val tagline: String? = null,

    @SerialName("website_url")
    val websiteUrl: String? = null,

    @SerialName("perks")
    val perks: List<String> = emptyList(),

    @SerialName("skills")
    val skills: String? = null,

    @SerialName("experience_level")
    val experienceLevel: String? = null,

    @SerialName("portfolio_url")
    val portfolioUrl: String? = null,

    @SerialName("bio")
    val bio: String? = null
)