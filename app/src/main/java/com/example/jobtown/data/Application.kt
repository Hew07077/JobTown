package com.example.jobtown.data

import kotlinx.serialization.Serializable

@Serializable
data class JobApplication(
    val id: String = "",
    val jobId: String = "",
    val userId: String = "",
    val jobTitle: String = "",
    val companyName: String = "",
    val applicantName: String = "",
    val applicantEmail: String = "",
    val resumeUrl: String = "",
    val coverLetter: String = "",
    val status: String = "Pending",
    val appliedAt: String = ""
)