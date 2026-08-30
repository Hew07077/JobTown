@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)

package com.example.jobtown.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

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
    val bio: String? = null,

    @SerialName("experience_entries")
    val experienceEntries: List<ProfileEntry> = emptyList(),

    @SerialName("education_entries")
    val educationEntries: List<ProfileEntry> = emptyList(),

    @SerialName("certification_entries")
    val certificationEntries: List<ProfileEntry> = emptyList()
)

@Serializable
data class ProfileEntry(
    @SerialName("id")
    val id: String = UUID.randomUUID().toString(),
    @SerialName("title")
    val title: String = "",
    @SerialName("subtitle")
    val subtitle: String = "",
    @SerialName("period")
    val period: String = "",
    @SerialName("description")
    val description: String = "",
    @SerialName("file_url")
    val fileUrl: String = ""
)

@Serializable
internal data class UserProfileCore(
    @SerialName("id") val id: String,
    @SerialName("phone") val phone: String? = null,
    @SerialName("location") val location: String? = null,
    @SerialName("tagline") val tagline: String? = null,
    @SerialName("website_url") val websiteUrl: String? = null,
    @SerialName("perks") val perks: List<String> = emptyList(),
    @SerialName("skills") val skills: String? = null,
    @SerialName("experience_level") val experienceLevel: String? = null,
    @SerialName("portfolio_url") val portfolioUrl: String? = null,
    @SerialName("bio") val bio: String? = null
)