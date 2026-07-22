package com.example.jobtown.data

data class JobApplication(
    val id: String,
    val jobId: String,
    val applicantUid: String,
    val applicantName: String,
    val jobTitle: String = "",
    val companyName: String = "",
    val status: String = "Pending",
    val appliedDate: String = "Today",
    val appliedAt: Long = System.currentTimeMillis(),
    val matchScore: Int = 80
)