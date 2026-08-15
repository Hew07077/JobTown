package com.example.jobtown.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatRoom(
    @SerialName("id")
    val id: String = "",

    @SerialName("application_id")
    val applicationId: String = "",

    @SerialName("job_title")
    val jobTitle: String = "",

    @SerialName("seeker_id")
    val seekerId: String = "",

    @SerialName("seeker_name")
    val seekerName: String = "",

    @SerialName("employer_id")
    val employerId: String = "",

    @SerialName("company_name")
    val companyName: String = "",

    @SerialName("last_message")
    val lastMessage: String = "",

    @SerialName("last_message_time")
    val lastMessageTime: Long = System.currentTimeMillis(),

    @SerialName("unread_count")
    val unreadCount: Int = 0
)