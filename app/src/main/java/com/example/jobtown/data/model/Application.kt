@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)

package com.example.jobtown.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class JobApplication(
    @SerialName("id")
    val id: String = "",

    @SerialName("job_id")
    val jobId: String = "",

    @SerialName("user_id")
    val userId: String = "",

    @SerialName("job_title")
    val jobTitle: String = "",

    @SerialName("company_name")
    val companyName: String = "",

    @SerialName("employer_id")
    val employerId: String = "",

    @SerialName("applicant_name")
    val applicantName: String = "",

    @SerialName("applicant_email")
    val applicantEmail: String = "",

    @SerialName("resume_url")
    val resumeUrl: String = "",

    @SerialName("cover_letter")
    val coverLetter: String = "",

    @SerialName("status")
    val status: String = "Pending",

    @SerialName("location")
    val location: String = "",

    @SerialName("applied_at")
    val appliedAt: String = ""
) {
    val appliedDate: String
        get() = appliedAt.ifBlank { "Recently" }

    val displayLocation: String
        get() = location.ifBlank { "Not specified" }
}