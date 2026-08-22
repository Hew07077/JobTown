package com.example.jobtown.data.repository

import android.util.Log
import com.example.jobtown.data.ActionType
import com.example.jobtown.data.ChatMessage
import com.example.jobtown.data.ChatRoom
import com.example.jobtown.data.MessageReaction
import com.example.jobtown.data.MessageType
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.broadcast
import io.github.jan.supabase.realtime.broadcastFlow
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.presenceChangeFlow
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.realtime.track
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import java.util.UUID

@Serializable
private data class NewChatRoomPayload(
    val seeker_id: String,
    val seeker_name: String,
    val employer_id: String,
    val company_name: String,
    val job_title: String,
    val last_message: String,
    val last_message_time: Long
)

@Serializable
private data class NewChatMessagePayload(
    val chat_room_id: String,
    val sender_id: String,
    val text: String,
    val timestamp: Long,
    val is_read: Boolean,
    val message_type: String,
    val action_type: String,
    val is_edited: Boolean = false,
    val is_deleted: Boolean = false,
    val reply_to_id: String? = null
)

@Serializable
private data class ChatRoomLastMessageUpdate(
    val last_message: String,
    val last_message_time: Long? = null
)

@Serializable
private data class ReadStatusUpdate(
    val is_read: Boolean
)

@Serializable
private data class NewReactionPayload(
    val message_id: String,
    val chat_room_id: String,
    val user_id: String,
    val emoji: String,
    val created_at: Long
)

@Serializable
private data class TypingBroadcast(
    val user_id: String,
    val is_typing: Boolean
)

data class RoomPresence(
    val onlineUserIds: Set<String> = emptySet(),
    val typingUserIds: Set<String> = emptySet()
)

class MessageRepository(private val supabase: SupabaseClient) {

    suspend fun uploadChatAttachment(
        roomId: String,
        fileName: String,
        bytes: ByteArray,
        mimeType: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            var safeName = fileName.ifBlank { "file" }.replace(Regex("[^A-Za-z0-9._-]"), "_")
            if (!safeName.contains(".")) {
                val ext = mimeType.substringAfter("/", "").substringBefore(";")
                if (ext.isNotBlank()) safeName = "$safeName.$ext"
            }
            val path = "$roomId/${UUID.randomUUID()}_$safeName"

            supabase.storage[CHAT_ATTACHMENTS_BUCKET].upload(
                path = path,
                data = bytes,
                upsert = true
            )

            supabase.storage[CHAT_ATTACHMENTS_BUCKET].publicUrl(path)
        } catch (e: Exception) {
            Log.e("MessageRepository", "Error uploading chat attachment: ${e.message}", e)
            null
        }
    }

    suspend fun getOrCreateChatRoom(
        seekerId: String,
        seekerName: String,
        employerId: String,
        companyName: String,
        jobTitle: String
    ): String = withContext(Dispatchers.IO) {
        if (seekerId.isBlank()) return@withContext ""

        try {
            val existingRoom = supabase.postgrest["chat_rooms"]
                .select {
                    filter {
                        eq("seeker_id", seekerId)
                        eq("company_name", companyName)
                        eq("job_title", jobTitle)
                    }
                }
                .decodeList<ChatRoom>()
                .firstOrNull()

            if (existingRoom != null && existingRoom.id.isNotBlank()) {
                return@withContext existingRoom.id
            }
        } catch (e: Exception) {
            Log.e("MessageRepository", "Error looking up existing chat room", e)
        }

        val newRoomPayload = NewChatRoomPayload(
            seeker_id = seekerId,
            seeker_name = seekerName,
            employer_id = employerId.ifBlank { "employer_default" },
            company_name = companyName,
            job_title = jobTitle,
            last_message = "Chat started",
            last_message_time = System.currentTimeMillis()
        )

        try {
            val insertedRoom = supabase.postgrest["chat_rooms"]
                .insert(newRoomPayload) { select() }
                .decodeSingle<ChatRoom>()
            insertedRoom.id
        } catch (e: Exception) {
            Log.e("MessageRepository", "Error inserting new chat room", e)
            throw Exception("Insert failed: ${e.message ?: e.toString()}", e)
        }
    }

    suspend fun getChatRoomsForUser(userId: String): List<ChatRoom> = withContext(Dispatchers.IO) {
        try {
            if (userId.isBlank()) return@withContext emptyList()

            supabase.postgrest["chat_rooms"]
                .select {
                    filter {
                        or {
                            eq("seeker_id", userId)
                            eq("employer_id", userId)
                        }
                    }
                }
                .decodeList<ChatRoom>()
                .sortedByDescending { it.lastMessageTime }
        } catch (e: Exception) {
            Log.e("MessageRepository", "Error in getChatRoomsForUser", e)
            emptyList()
        }
    }

    suspend fun getMessagesForRoom(
        roomId: String,
        limit: Int = DEFAULT_PAGE_SIZE,
        beforeTimestamp: Long? = null
    ): List<ChatMessage> = withContext(Dispatchers.IO) {
        try {
            if (roomId.isBlank()) return@withContext emptyList()

            supabase.postgrest["chat_messages"]
                .select {
                    filter {
                        eq("chat_room_id", roomId)
                        if (beforeTimestamp != null) {
                            lt("timestamp", beforeTimestamp)
                        }
                    }
                    order("timestamp", Order.DESCENDING)
                    limit(limit.toLong())
                }
                .decodeList<ChatMessage>()
                .sortedBy { it.timestamp }
        } catch (e: Exception) {
            Log.e("MessageRepository", "Error in getMessagesForRoom", e)
            emptyList()
        }
    }

    suspend fun getLatestMessage(roomId: String): ChatMessage? = withContext(Dispatchers.IO) {
        try {
            if (roomId.isBlank()) return@withContext null

            supabase.postgrest["chat_messages"]
                .select {
                    filter { eq("chat_room_id", roomId) }
                    order("timestamp", Order.DESCENDING)
                    limit(1)
                }
                .decodeList<ChatMessage>()
                .firstOrNull()
        } catch (e: Exception) {
            Log.e("MessageRepository", "Error in getLatestMessage", e)
            null
        }
    }

    suspend fun searchMessagesInRoom(roomId: String, query: String): List<ChatMessage> =
        withContext(Dispatchers.IO) {
            try {
                if (roomId.isBlank() || query.isBlank()) return@withContext emptyList()

                supabase.postgrest["chat_messages"]
                    .select {
                        filter {
                            eq("chat_room_id", roomId)
                            eq("is_deleted", false)
                            ilike("text", "%$query%")
                        }
                        order("timestamp", Order.DESCENDING)
                        limit(50)
                    }
                    .decodeList<ChatMessage>()
            } catch (e: Exception) {
                Log.e("MessageRepository", "Error searching messages", e)
                emptyList()
            }
        }

    suspend fun sendMessage(
        roomId: String,
        senderId: String,
        content: String,
        type: MessageType = MessageType.TEXT,
        replyToId: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            if (roomId.isBlank() || content.isBlank()) return@withContext false
            val now = System.currentTimeMillis()

            val messagePayload = NewChatMessagePayload(
                chat_room_id = roomId,
                sender_id = senderId,
                text = content,
                timestamp = now,
                is_read = false,
                message_type = type.name,
                action_type = ActionType.NONE.name,
                reply_to_id = replyToId?.takeIf { it.isNotBlank() }
            )

            supabase.postgrest["chat_messages"].insert(messagePayload)

            val snippet = when (type) {
                MessageType.IMAGE -> "[Image]"
                MessageType.FILE -> "[Document]"
                MessageType.VOICE -> "[Voice Note]"
                else -> content
            }

            supabase.postgrest["chat_rooms"].update(
                ChatRoomLastMessageUpdate(
                    last_message = snippet,
                    last_message_time = now
                )
            ) {
                filter { eq("id", roomId) }
            }

            true
        } catch (e: Exception) {
            Log.e("MessageRepository", "Error in sendMessage: ${e.message}", e)
            false
        }
    }

    suspend fun editMessage(roomId: String, messageId: String, newText: String): Boolean = withContext(Dispatchers.IO) {
        if (messageId.isBlank() || roomId.isBlank() || newText.isBlank()) return@withContext false

        try {
            supabase.postgrest["chat_messages"].update(
                mapOf(
                    "text" to newText,
                    "is_edited" to true
                )
            ) {
                filter {
                    eq("id", messageId)
                    eq("chat_room_id", roomId)
                }
            }

            val latest = getLatestMessage(roomId)
            if (latest != null && latest.id == messageId) {
                val snippet = when (latest.messageType) {
                    MessageType.IMAGE -> "[Image]"
                    MessageType.FILE -> "[Document]"
                    MessageType.VOICE -> "[Voice Note]"
                    else -> newText
                }
                supabase.postgrest["chat_rooms"].update(
                    mapOf("last_message" to snippet)
                ) {
                    filter { eq("id", roomId) }
                }
            }

            true
        } catch (e: Exception) {
            Log.e("MessageRepository", "Error editing message $messageId: ${e.message}", e)
            false
        }
    }

    suspend fun deleteMessage(roomId: String, messageId: String): Boolean = withContext(Dispatchers.IO) {
        if (messageId.isBlank() || roomId.isBlank()) return@withContext false

        try {
            val deletedText = "This message was deleted"
            supabase.postgrest["chat_messages"].update(
                mapOf(
                    "text" to deletedText,
                    "is_deleted" to true,
                    "is_edited" to false
                )
            ) {
                filter {
                    eq("id", messageId)
                    eq("chat_room_id", roomId)
                }
            }

            val latest = getLatestMessage(roomId)
            if (latest != null && latest.id == messageId) {
                supabase.postgrest["chat_rooms"].update(
                    mapOf("last_message" to deletedText)
                ) {
                    filter { eq("id", roomId) }
                }
            }

            true
        } catch (e: Exception) {
            Log.e("MessageRepository", "Error deleting message $messageId: ${e.message}", e)
            false
        }
    }

    suspend fun markMessagesAsRead(roomId: String, viewerId: String) = withContext(Dispatchers.IO) {
        if (roomId.isBlank() || viewerId.isBlank()) return@withContext

        try {
            supabase.postgrest["chat_messages"].update(
                ReadStatusUpdate(is_read = true)
            ) {
                filter {
                    eq("chat_room_id", roomId)
                    neq("sender_id", viewerId)
                    eq("is_read", false)
                }
            }
        } catch (e: Exception) {
            Log.e("MessageRepository", "Error marking messages as read", e)
        }
    }

    suspend fun getReactionsForRoom(roomId: String): List<MessageReaction> = withContext(Dispatchers.IO) {
        try {
            if (roomId.isBlank()) return@withContext emptyList()

            supabase.postgrest["message_reactions"]
                .select { filter { eq("chat_room_id", roomId) } }
                .decodeList<MessageReaction>()
        } catch (e: Exception) {
            Log.e("MessageRepository", "Error fetching reactions", e)
            emptyList()
        }
    }

    suspend fun toggleReaction(
        roomId: String,
        messageId: String,
        userId: String,
        emoji: String
    ): Boolean = withContext(Dispatchers.IO) {
        if (roomId.isBlank() || messageId.isBlank() || userId.isBlank() || emoji.isBlank()) {
            return@withContext false
        }

        try {
            val existing = supabase.postgrest["message_reactions"]
                .select {
                    filter {
                        eq("message_id", messageId)
                        eq("user_id", userId)
                        eq("emoji", emoji)
                    }
                }
                .decodeList<MessageReaction>()
                .firstOrNull()

            if (existing != null) {
                supabase.postgrest["message_reactions"].delete {
                    filter { eq("id", existing.id) }
                }
            } else {
                supabase.postgrest["message_reactions"].insert(
                    NewReactionPayload(
                        message_id = messageId,
                        chat_room_id = roomId,
                        user_id = userId,
                        emoji = emoji,
                        created_at = System.currentTimeMillis()
                    )
                )
            }
            true
        } catch (e: Exception) {
            Log.e("MessageRepository", "Error toggling reaction", e)
            false
        }
    }

    fun observeReactions(roomId: String): Flow<List<MessageReaction>> = callbackFlow {
        if (roomId.isBlank()) {
            close()
            return@callbackFlow
        }

        val channel = supabase.channel("chat_reactions_${roomId}_${System.currentTimeMillis()}")
        val collectJob = launch {
            channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "message_reactions"
                filter = "chat_room_id=eq.$roomId"
            }.collect {
                try {
                    trySend(getReactionsForRoom(roomId))
                } catch (e: Exception) {
                    Log.e("MessageRepository", "Error refetching reactions", e)
                }
            }
        }

        try {
            channel.subscribe()
        } catch (e: Exception) {
            Log.e("MessageRepository", "Error subscribing to reactions channel", e)
        }

        awaitClose {
            collectJob.cancel()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    channel.unsubscribe()
                    supabase.realtime.removeChannel(channel)
                } catch (e: Exception) {
                    Log.e("MessageRepository", "Error tearing down reactions channel", e)
                }
            }
        }
    }

    fun observeRoomPresence(roomId: String, selfUserId: String): Flow<RoomPresence> = callbackFlow {
        if (roomId.isBlank() || selfUserId.isBlank()) {
            close()
            return@callbackFlow
        }

        val channel = supabase.channel("presence_room_$roomId")
        val onlineUsers = mutableSetOf<String>()
        val typingUsers = mutableSetOf<String>()

        fun emitPresence() {
            trySend(RoomPresence(onlineUserIds = onlineUsers.toSet(), typingUserIds = typingUsers.toSet()))
        }

        val presenceJob = launch {
            try {
                channel.presenceChangeFlow().collect { change ->
                    change.joins.keys.forEach { onlineUsers.add(it) }
                    change.leaves.keys.forEach { onlineUsers.remove(it) }
                    emitPresence()
                }
            } catch (e: Exception) {
                Log.e("MessageRepository", "Error in presence flow", e)
            }
        }

        val typingJob = launch {
            try {
                channel.broadcastFlow<TypingBroadcast>(event = "typing").collect { payload ->
                    if (payload.user_id == selfUserId) return@collect
                    if (payload.is_typing) typingUsers.add(payload.user_id)
                    else typingUsers.remove(payload.user_id)
                    emitPresence()
                }
            } catch (e: Exception) {
                Log.e("MessageRepository", "Error in typing broadcast flow", e)
            }
        }

        try {
            channel.subscribe(blockUntilSubscribed = false)
            channel.track(mapOf("user_id" to JsonPrimitive(selfUserId) as JsonElement))
        } catch (e: Exception) {
            Log.e("MessageRepository", "Error subscribing to presence channel", e)
        }

        awaitClose {
            presenceJob.cancel()
            typingJob.cancel()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    channel.unsubscribe()
                    supabase.realtime.removeChannel(channel)
                } catch (e: Exception) {
                    Log.e("MessageRepository", "Error tearing down presence channel", e)
                }
            }
        }
    }

    suspend fun sendTypingStatus(roomId: String, userId: String, isTyping: Boolean) = withContext(Dispatchers.IO) {
        if (roomId.isBlank() || userId.isBlank()) return@withContext
        try {
            val targetTopic = "presence_room_$roomId"
            val channel = supabase.realtime.subscriptions.values.firstOrNull {
                it.topic.contains(targetTopic)
            } ?: supabase.channel(targetTopic).apply { subscribe(blockUntilSubscribed = false) }

            channel.broadcast(event = "typing", TypingBroadcast(user_id = userId, is_typing = isTyping))
        } catch (e: Exception) {
            Log.e("MessageRepository", "Error sending typing status", e)
        }
    }

    fun observeNewMessages(roomId: String): Flow<ChatMessage> = callbackFlow {
        if (roomId.isBlank()) {
            close()
            return@callbackFlow
        }

        val channel = supabase.channel("chat_messages_${roomId}_${System.currentTimeMillis()}")
        val collectJob = launch {
            channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "chat_messages"
                filter = "chat_room_id=eq.$roomId"
            }.collect { action ->
                when (action) {
                    is PostgresAction.Insert, is PostgresAction.Update -> {
                        try {
                            val message = action.decodeRecord<ChatMessage>()
                            if (message.chatRoomId == roomId) {
                                trySend(message)
                            }
                        } catch (e: Exception) {
                            Log.e("MessageRepository", "Error decoding realtime message", e)
                        }
                    }
                    else -> {}
                }
            }
        }

        try {
            channel.subscribe()
        } catch (e: Exception) {
            Log.e("MessageRepository", "Error subscribing to realtime messages channel", e)
        }

        awaitClose {
            collectJob.cancel()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    channel.unsubscribe()
                    supabase.realtime.removeChannel(channel)
                } catch (e: Exception) {
                    Log.e("MessageRepository", "Error tearing down realtime channel", e)
                }
            }
        }
    }

    companion object {
        const val CHAT_ATTACHMENTS_BUCKET = "chat-attachments"
        const val DEFAULT_PAGE_SIZE = 40
    }
}