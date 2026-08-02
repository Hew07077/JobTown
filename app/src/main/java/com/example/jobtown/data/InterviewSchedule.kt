package com.example.jobtown.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InterviewSchedule(
    @SerialName("id")
    val id: String = "",

    @SerialName("applicant_id")
    val userId: String = "",

    @SerialName("employer_id")
    val employerId: String = "",

    @SerialName("job_id")
    val jobId: String = "",

    @SerialName("title")
    val title: String = "",

    @SerialName("company")
    val company: String = "",

    @SerialName("date")
    val date: String = "",

    @SerialName("time")
    val time: String = "",

    @SerialName("location_or_link")
    val locationOrLink: String = "",

    @SerialName("status")
    val status: String = "Scheduled",

    @SerialName("notes")
    val notes: String = ""
)