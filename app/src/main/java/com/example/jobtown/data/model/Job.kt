@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)

package com.example.jobtown.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class Job(
    @SerialName("id")
    val id: String = "",

    @SerialName("title")
    val title: String = "",

    @SerialName("company")
    val company: String = "",

    @SerialName("company_image_url")
    val companyImageUrl: String? = null,

    @SerialName("location")
    val location: String = "",

    @SerialName("salary")
    val salary: String = "",

    @SerialName("salary_range")
    val salaryRange: String? = null,

    @SerialName("type")
    val type: String = "Full-time",

    @SerialName("description")
    val description: String = "",

    @SerialName("requirements")
    val requirements: List<String>? = emptyList(),

    @SerialName("skills")
    val skills: List<String>? = emptyList(),

    @SerialName("is_featured")
    val isFeatured: Boolean? = false,

    @SerialName("is_oku_friendly")
    val isOkuFriendly: Boolean? = false,

    @SerialName("employer_id")
    val employerId: String? = null,

    @SerialName("posted_by_user_id")
    val postedByUserId: String? = null,

    @SerialName("status")
    val status: String? = "active",

    @SerialName("expired_at")
    val expiredAt: String? = null,

    @SerialName("created_at")
    val createdAt: String? = null
) {
    @Transient
    val companyName: String
        get() = company.ifBlank { "Unknown Company" }

    @Transient
    val jobType: String
        get() = type.ifBlank { "Full-time" }

    @Transient
    val isExpired: Boolean
        get() = status?.equals("expired", ignoreCase = true) == true
}