@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)

package com.example.jobtown.data.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object MessageTypeSerializer : KSerializer<MessageType> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("MessageType", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: MessageType) = encoder.encodeString(value.name)
    override fun deserialize(decoder: Decoder): MessageType {
        return runCatching { MessageType.valueOf(decoder.decodeString().uppercase()) }.getOrDefault(MessageType.TEXT)
    }
}
object ActionTypeSerializer : KSerializer<ActionType> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ActionType", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: ActionType) = encoder.encodeString(value.name)
    override fun deserialize(decoder: Decoder): ActionType {
        return runCatching { ActionType.valueOf(decoder.decodeString().uppercase()) }.getOrDefault(ActionType.NONE)
    }
}

@Serializable(with = ActionTypeSerializer::class)
enum class ActionType {
    NONE,
    INTERVIEW_REQUEST,
    RESUME_REQUEST,
    OFFER_LETTER
}

@Serializable(with = MessageTypeSerializer::class)
enum class MessageType {
    TEXT,
    IMAGE,
    FILE,
    SYSTEM
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
    @SerialName("reply_to_id") val replyToId: String? = null,
    @Transient val isFailed: Boolean = false
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