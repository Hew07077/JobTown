package com.example.jobtown.data

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

    @SerialName("applied_at")
    val appliedAt: String = ""
) {
    // Custom getters so UI code can reference .location and .appliedDate seamlessly
    val location: String
        get() = "Remote" // Fallback or retrieve from parent Job model if available

    val appliedDate: String
        get() = appliedAt.ifBlank { "Recently" }
}