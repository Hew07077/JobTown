package com.example.jobtown.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ActionType {
    @SerialName("NONE")
    NONE,

    @SerialName("INTERVIEW_REQUEST")
    INTERVIEW_REQUEST,

    @SerialName("RESUME_REQUEST")
    RESUME_REQUEST,

    @SerialName("OFFER_LETTER")
    OFFER_LETTER
}

// Matches the "message_type" enum column on chat_messages in Supabase.
@Serializable
enum class MessageType {
    @SerialName("TEXT")
    TEXT,

    @SerialName("IMAGE")
    IMAGE,

    @SerialName("FILE")
    FILE,

    @SerialName("SYSTEM")
    SYSTEM
}

@Serializable
data class ChatMessage(
    @SerialName("id")
    val id: String = "",

    @SerialName("chat_room_id")
    val chatRoomId: String = "",

    @SerialName("sender_id")
    val senderId: String = "",

    @SerialName("text")
    val text: String = "",

    @SerialName("timestamp")
    val timestamp: Long = 0L,

    @SerialName("is_read")
    val isRead: Boolean = false,

    @SerialName("message_type")
    val messageType: MessageType = MessageType.TEXT,

    @SerialName("action_type")
    val actionType: ActionType = ActionType.NONE
)