package com.example.jobtown.data

import kotlinx.serialization.Serializable

@Serializable
data class InterviewSchedule(
    val id: String = "",
    val userId: String = "",
    val employerId: String = "",
    val jobId: String = "",
    val title: String = "",
    val company: String = "",
    val date: String = "",
    val time: String = "",
    val locationOrLink: String = "",
    val status: String = "Scheduled",
    val notes: String = ""
)