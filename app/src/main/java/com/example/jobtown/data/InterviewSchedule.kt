package com.example.jobtown.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class InterviewSchedule(
    @SerialName("id")
    val id: String = "",

    @SerialName("user_id")
    val userId: String = "",

    // Marked as @Transient so KotlinX Serialization excludes it from Supabase PostgREST queries
    @Transient
    val seekerName: String = "",

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
    val status: String = "Pending",

    @SerialName("notes")
    val notes: String = "",

    @SerialName("reschedule_reason")
    val rescheduleReason: String = "",

    @SerialName("preferred_time")
    val preferredTime: String = ""
)