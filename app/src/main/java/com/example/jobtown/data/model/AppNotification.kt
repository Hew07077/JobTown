@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)

package com.example.jobtown.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppNotification(
    @SerialName("id")
    val id: String = "",

    @SerialName("user_id")
    val userId: String = "",

    @SerialName("title")
    val title: String = "",

    @SerialName("body")
    val body: String = "",

    @SerialName("type")
    val type: String = "info",

    @SerialName("related_id")
    val relatedId: String = "",

    @SerialName("is_read")
    val isRead: Boolean = false,

    @SerialName("created_at")
    val createdAt: String = ""
)

object NotificationType {
    const val APPLICATION_VIEWED = "application_viewed"
    const val INTERVIEW_SCHEDULED = "interview_scheduled"
    const val APPLICATION_STATUS = "application_status"
}
