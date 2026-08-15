package com.example.jobtown.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ActionType {
    @SerialName("NONE") NONE,
    @SerialName("INTERVIEW_REQUEST") INTERVIEW_REQUEST,
    @SerialName("RESUME_REQUEST") RESUME_REQUEST,
    @SerialName("OFFER_LETTER") OFFER_LETTER
}

@Serializable
enum class MessageType {
    @SerialName("TEXT") TEXT,
    @SerialName("IMAGE") IMAGE,
    @SerialName("FILE") FILE,
    @SerialName("SYSTEM") SYSTEM
}

@Serializable
data class ChatMessage(
    @SerialName("id") val id: String = "",
    @SerialName("chat_room_id") val chatRoomId: String = "",
    @SerialName("sender_id") val senderId: String = "",
    @SerialName("text") val text: String = "",
    @SerialName("timestamp") val timestamp: Long = 0L,
    @SerialName("is_read") val isRead: Boolean = false,
    @SerialName("message_type") val messageType: MessageType = MessageType.TEXT,
    @SerialName("action_type") val actionType: ActionType = ActionType.NONE,
    @SerialName("is_edited") val isEdited: Boolean = false,
    @SerialName("is_deleted") val isDeleted: Boolean = false,
    @SerialName("reply_to_id") val replyToId: String? = null
)

@Serializable
data class MessageReaction(
    @SerialName("id") val id: String = "",
    @SerialName("message_id") val messageId: String = "",
    @SerialName("chat_room_id") val chatRoomId: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("emoji") val emoji: String = "",
    @SerialName("created_at") val createdAt: Long = 0L
)

data class ReactionGroup(
    val emoji: String,
    val count: Int,
    val reactedByMe: Boolean
)

fun List<MessageReaction>.toReactionGroups(currentUserId: String): List<ReactionGroup> {
    return this.groupBy { it.emoji }
        .map { (emoji, reactions) ->
            ReactionGroup(
                emoji = emoji,
                count = reactions.size,
                reactedByMe = reactions.any { it.userId == currentUserId }
            )
        }
        .sortedBy { it.emoji }
}