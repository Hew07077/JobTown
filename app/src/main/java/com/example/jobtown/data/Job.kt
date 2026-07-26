package com.example.jobtown.data

import kotlinx.serialization.Serializable

@Serializable
data class Job(
    val id: String = "",
    val title: String = "",
    val company: String = "",
    val location: String = "",
    val salary: String = "",
    val salaryRange: String = "",
    val type: String = "Full-time",
    val description: String = "",
    val requirements: List<String> = emptyList(),
    val skills: List<String> = emptyList(),
    val isFeatured: Boolean = false,
    val employerId: String = "",
    val postedByUserId: String = "",
    val createdAt: String = ""
)