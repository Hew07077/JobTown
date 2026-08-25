package com.example.jobtown.data.repository

import android.util.Log
import com.example.jobtown.data.ActionType
import com.example.jobtown.data.ChatMessage
import com.example.jobtown.data.ChatRoom
import com.example.jobtown.data.MessageReaction
import com.example.jobtown.data.MessageType
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.broadcast
import io.github.jan.supabase.realtime.broadcastFlow
import io.github.jan.supabase.realtime.channel
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
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

private const val CHAT_ATTACHMENTS_BUCKET = "chat_attachments"

@Serializable
private data class NewChatRoomPayload(
    @SerialName("application_id") val application_id: String = "",
    @SerialName("seeker_id") val seeker_id: String,
    @SerialName("seeker_name") val seeker_name: String,
    @SerialName("employer_id") val employer_id: String,
    @SerialName("company_name") val company_name: String,
    @SerialName("job_title") val job_title: String,
    @SerialName("last_message") val last_message: String,
    @SerialName("last_message_time") val last_message_time: Long,
    @SerialName("unread_count") val unread_count: Int = 0


)

@Serializable
private data class NewChatMessagePayload(
    @SerialName("chat_room_id") val chat_room_id: String,
    @SerialName("sender_id") val sender_id: String,
    @SerialName("text") val text: String,
    @SerialName("timestamp") val timestamp: Long,
    @SerialName("is_read") val is_read: Boolean = false,
    @SerialName("message_type") val message_type: String = "TEXT",
    @SerialName("action_type") val action_type: String = "NONE",
    @SerialName("is_edited") val is_edited: Boolean = false,
    @SerialName("is_deleted") val is_deleted: Boolean = false,
    @SerialName("reply_to_id") val reply_to_id: String? = null
)

@Serializable
private data class ReadStatusUpdate(
    @SerialName("is_read") val is_read: Boolean
)

@Serializable
private data class NewReactionPayload(
    @SerialName("message_id") val message_id: String,
    @SerialName("chat_room_id") val chat_room_id: String,
    @SerialName("user_id") val user_id: String,
    @SerialName("emoji") val emoji: String,
    @SerialName("created_at") val created_at: Long
)

@Serializable
private data class TypingBroadcast(
    @SerialName("user_id") val user_id: String,
    @SerialName("is_typing") val is_typing: Boolean
)

data class RoomPresence(
    val onlineUserIds: Set<String> = emptySet(),
    val typingUserIds: Set<String> = emptySet()
)

class MessageRepository(private val supabase: SupabaseClient) {

    private val json = Json { ignoreUnknownKeys = true }
    private val activePresenceChannels = mutableMapOf<String, RealtimeChannel>()

    companion object {
        const val DEFAULT_PAGE_SIZE = 30
        private const val TAG = "MessageRepository"
    }

    suspend fun uploadChatAttachment(
        roomId: String,
        fileName: String,
        bytes: ByteArray,
        mimeType: String
    ): String = withContext(Dispatchers.IO) {
        var safeName = fileName.ifBlank { "file" }.replace(Regex("[^A-Za-z0-9._-]"), "_")
        if (!safeName.contains(".")) {
            val ext = mimeType.substringAfter("/", "").substringBefore(";")
            if (ext.isNotBlank()) safeName = "$safeName.$ext"
        }
        val path = "$roomId/${java.util.UUID.randomUUID()}_$safeName"
        val bucket = supabase.storage[CHAT_ATTACHMENTS_BUCKET]

        try {
            bucket.upload(path = path, data = bytes, upsert = true)
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading attachment: ${e.message}", e)
            throw Exception("Upload failed: ${e.message}", e)
        }

        bucket.publicUrl(path)
    }

    suspend fun getOrCreateChatRoom(
        applicationId: String = "",
        seekerId: String,
        seekerName: String,
        employerId: String,
        companyName: String,
        jobTitle: String
    ): String = withContext(Dispatchers.IO) {
        if (seekerId.isBlank()) return@withContext ""

        try {
            val existingRoom = supabase.from("chat_rooms")
                .select {
                    filter {
                        eq("seeker_id", seekerId)
                        eq("company_name", companyName)
                        eq("job_title", jobTitle)
                        if (applicationId.isNotBlank()) {
                            eq("application_id", applicationId)
                        }
                    }
                }
                .decodeList<ChatRoom>()
                .firstOrNull()

            if (existingRoom != null && existingRoom.id.isNotBlank()) {
                return@withContext existingRoom.id
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking existing room: ${e.localizedMessage}", e)
        }

        val initialText = "Chat started"
        val now = System.currentTimeMillis()

        val newRoomPayload = NewChatRoomPayload(
            application_id = applicationId,
            seeker_id = seekerId,
            seeker_name = seekerName,
            employer_id = employerId.ifBlank { "employer_default" },
            company_name = companyName,
            job_title = jobTitle,
            last_message = initialText,
            last_message_time = now,
            unread_count = 0
        )

        try {
            val insertedRoom = supabase.from("chat_rooms")
                .insert(newRoomPayload) { select() }
                .decodeSingle<ChatRoom>()

            sendMessage(
                roomId = insertedRoom.id,
                senderId = seekerId,
                content = initialText,
                type = MessageType.TEXT,
                actionType = ActionType.NONE
            )

            insertedRoom.id
        } catch (e: Exception) {
            Log.e(TAG, "Error creating chat room: ${e.localizedMessage}", e)
            ""
        }
    }

    suspend fun getChatRoomsForUser(userId: String): List<ChatRoom> = withContext(Dispatchers.IO) {
        try {
            if (userId.isBlank()) return@withContext emptyList()

            val rooms = supabase.from("chat_rooms")
                .select {
                    filter {
                        or {
                            eq("seeker_id", userId)
                            eq("employer_id", userId)
                        }
                    }
                    order("last_message_time", Order.DESCENDING)
                }
                .decodeList<ChatRoom>()

            applyUnreadCounts(rooms, userId).sortedByDescending { it.lastMessageTime }
        } catch (e: Exception) {
            Log.e(TAG, "Error in getChatRoomsForUser: ${e.localizedMessage}", e)
            emptyList()
        }
    }

    private suspend fun applyUnreadCounts(rooms: List<ChatRoom>, userId: String): List<ChatRoom> {
        if (rooms.isEmpty()) return rooms
        return try {
            val unreadMessages = supabase.from("chat_messages")
                .select {
                    filter {
                        eq("is_read", false)
                        neq("sender_id", userId)
                    }
                }
                .decodeList<ChatMessage>()

            val countsByRoom = unreadMessages.groupingBy { it.chatRoomId }.eachCount()
            rooms.map { room -> room.copy(unreadCount = countsByRoom[room.id] ?: 0) }
        } catch (e: Exception) {
            rooms
        }
    }

    fun observeUserChatRooms(userId: String): Flow<List<ChatRoom>> = callbackFlow {
        if (userId.isBlank()) {
            close()
            return@callbackFlow
        }

        suspend fun refresh() {
            try {
                trySend(getChatRoomsForUser(userId))
            } catch (e: Exception) {
                Log.e(TAG, "Error refetching rooms", e)
            }
        }

        val roomsChannel = supabase.channel("user_rooms_all_$userId")
        val roomsJob = launch {
            roomsChannel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "chat_rooms"
            }.collect { refresh() }
        }

        try { roomsChannel.subscribe() } catch (e: Exception) { Log.e(TAG, "Error subscribing rooms", e) }

        awaitClose {
            roomsJob.cancel()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    roomsChannel.unsubscribe()
                    supabase.realtime.removeChannel(roomsChannel)
                } catch (e: Exception) {}
            }
        }
    }

    suspend fun getMessagesForRoom(
        roomId: String,
        limit: Int = DEFAULT_PAGE_SIZE,
        beforeTimestamp: Long? = null
    ): List<ChatMessage> = withContext(Dispatchers.IO) {
        try {
            if (roomId.isBlank()) return@withContext emptyList()

            supabase.from("chat_messages")
                .select {
                    filter {
                        eq("chat_room_id", roomId)
                        if (beforeTimestamp != null) {
                            lte("timestamp", beforeTimestamp)
                        }
                    }
                    order("timestamp", Order.DESCENDING)
                    limit(limit.toLong())
                }
                .decodeList<ChatMessage>()
                .sortedBy { it.timestamp }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching messages: ${e.localizedMessage}", e)
            emptyList()
        }
    }

    suspend fun getMessageById(messageId: String): ChatMessage? = withContext(Dispatchers.IO) {
        try {
            if (messageId.isBlank()) return@withContext null
            supabase.from("chat_messages")
                .select { filter { eq("id", messageId) } }
                .decodeList<ChatMessage>()
                .firstOrNull()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun sendMessage(
        roomId: String,
        senderId: String,
        content: String,
        type: MessageType = MessageType.TEXT,
        actionType: ActionType = ActionType.NONE,
        replyToId: String? = null
    ): ChatMessage? = withContext(Dispatchers.IO) {
        if (roomId.isBlank() || content.isBlank()) {
            Log.e(TAG, "sendMessage aborted: Empty roomId or content")
            return@withContext null
        }
        val now = System.currentTimeMillis()

        val messagePayload = NewChatMessagePayload(
            chat_room_id = roomId,
            sender_id = senderId,
            text = content,
            timestamp = now,
            is_read = false,
            message_type = type.name,
            action_type = actionType.name,
            reply_to_id = replyToId?.takeIf { it.isNotBlank() }
        )

        try {
            val inserted = supabase.from("chat_messages")
                .insert(messagePayload) { select() }
                .decodeSingle<ChatMessage>()

            val snippet = when (type) {
                MessageType.IMAGE -> "[Photo]"
                MessageType.FILE -> "[Document]"
                MessageType.SYSTEM -> content
                else -> when (actionType) {
                    ActionType.INTERVIEW_REQUEST -> "📅 Interview Request"
                    ActionType.RESUME_REQUEST -> "📄 Resume Requested"
                    ActionType.OFFER_LETTER -> "🎉 Offer Letter"
                    else -> content
                }
            }

            supabase.from("chat_rooms").update(
                mapOf(
                    "last_message" to snippet,
                    "last_message_time" to now
                )
            ) { filter { eq("id", roomId) } }

            inserted
        } catch (e: Exception) {
            Log.e(TAG, "CRITICAL ERROR SAVING MESSAGE: ${e.localizedMessage}", e)
            null
        }
    }

    suspend fun editMessage(roomId: String, messageId: String, newText: String): Boolean = withContext(Dispatchers.IO) {
        if (messageId.isBlank() || roomId.isBlank() || newText.isBlank()) return@withContext false
        try {
            supabase.from("chat_messages").update(
                mapOf("text" to newText, "is_edited" to true)
            ) { filter { eq("id", messageId); eq("chat_room_id", roomId) } }

            supabase.from("chat_rooms").update(
                mapOf(
                    "last_message" to newText,
                    "last_message_time" to System.currentTimeMillis()
                )
            ) { filter { eq("id", roomId) } }

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error editing message: ${e.localizedMessage}", e)
            false
        }
    }

    suspend fun deleteMessage(roomId: String, messageId: String): Boolean = withContext(Dispatchers.IO) {
        if (messageId.isBlank() || roomId.isBlank()) return@withContext false
        try {
            val deletedText = "This message was deleted"
            supabase.from("chat_messages").update(
                mapOf("text" to deletedText, "is_deleted" to true, "is_edited" to false)
            ) { filter { eq("id", messageId); eq("chat_room_id", roomId) } }

            supabase.from("chat_rooms").update(
                mapOf(
                    "last_message" to deletedText,
                    "last_message_time" to System.currentTimeMillis()
                )
            ) { filter { eq("id", roomId) } }

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting message: ${e.localizedMessage}", e)
            false
        }
    }

    suspend fun markMessagesAsRead(roomId: String, viewerId: String) = withContext(Dispatchers.IO) {
        if (roomId.isBlank() || viewerId.isBlank()) return@withContext
        try {
            supabase.from("chat_messages").update(ReadStatusUpdate(is_read = true)) {
                filter {
                    eq("chat_room_id", roomId)
                    neq("sender_id", viewerId)
                    eq("is_read", false)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error marking read: ${e.localizedMessage}", e)
        }
    }

    suspend fun getReactionsForRoom(roomId: String): List<MessageReaction> = withContext(Dispatchers.IO) {
        try {
            if (roomId.isBlank()) return@withContext emptyList()
            supabase.from("message_reactions")
                .select { filter { eq("chat_room_id", roomId) } }
                .decodeList<MessageReaction>()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun toggleReaction(
        roomId: String,
        messageId: String,
        userId: String,
        emoji: String
    ): Boolean = withContext(Dispatchers.IO) {
        if (roomId.isBlank() || messageId.isBlank() || userId.isBlank() || emoji.isBlank()) return@withContext false
        try {
            val existing = supabase.from("message_reactions").select {
                filter {
                    eq("message_id", messageId)
                    eq("user_id", userId)
                    eq("emoji", emoji)
                }
            }.decodeList<MessageReaction>().firstOrNull()

            if (existing != null) {
                supabase.from("message_reactions").delete {
                    filter {
                        eq("message_id", messageId)
                        eq("user_id", userId)
                        eq("emoji", emoji)
                    }
                }
            } else {
                supabase.from("message_reactions").insert(
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
            false
        }
    }

    fun observeReactions(roomId: String): Flow<List<MessageReaction>> = callbackFlow {
        if (roomId.isBlank()) { close(); return@callbackFlow }
        val channel = supabase.channel("chat_reactions_${roomId}_${System.currentTimeMillis()}")
        val job = launch {
            channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "message_reactions"
                filter = "chat_room_id=eq.$roomId"
            }.collect { trySend(getReactionsForRoom(roomId)) }
        }
        try { channel.subscribe() } catch (e: Exception) {}
        awaitClose {
            job.cancel()
            CoroutineScope(Dispatchers.IO).launch {
                try { channel.unsubscribe(); supabase.realtime.removeChannel(channel) } catch (e: Exception) {}
            }
        }
    }

    fun observeRoomPresence(roomId: String, selfUserId: String): Flow<RoomPresence> = callbackFlow {
        if (roomId.isBlank() || selfUserId.isBlank()) { close(); return@callbackFlow }

        val channel = supabase.channel("presence_room_$roomId")
        activePresenceChannels[roomId] = channel
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
            } catch (e: Exception) {}
        }

        val typingJob = launch {
            try {
                channel.broadcastFlow<TypingBroadcast>(event = "typing").collect { payload ->
                    if (payload.user_id == selfUserId) return@collect
                    if (payload.is_typing) typingUsers.add(payload.user_id)
                    else typingUsers.remove(payload.user_id)
                    emitPresence()
                }
            } catch (e: Exception) {}
        }

        try {
            channel.subscribe(blockUntilSubscribed = false)
            channel.track(mapOf("user_id" to JsonPrimitive(selfUserId) as JsonElement))
        } catch (e: Exception) {}

        awaitClose {
            if (activePresenceChannels[roomId] === channel) activePresenceChannels.remove(roomId)
            presenceJob.cancel()
            typingJob.cancel()
            CoroutineScope(Dispatchers.IO).launch {
                try { channel.unsubscribe(); supabase.realtime.removeChannel(channel) } catch (e: Exception) {}
            }
        }
    }

    suspend fun sendTypingStatus(roomId: String, userId: String, isTyping: Boolean) = withContext(Dispatchers.IO) {
        if (roomId.isBlank() || userId.isBlank()) return@withContext
        val channel = activePresenceChannels[roomId] ?: return@withContext
        try {
            channel.broadcast(event = "typing", TypingBroadcast(user_id = userId, is_typing = isTyping))
        } catch (e: Exception) {}
    }

    fun observeNewMessages(roomId: String): Flow<ChatMessage> = callbackFlow {
        if (roomId.isBlank()) { close(); return@callbackFlow }

        val channel = supabase.channel("chat_messages_room_$roomId")
        val collectJob = launch {
            channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "chat_messages"
                filter = "chat_room_id=eq.$roomId"
            }.collect { action ->
                try {
                    when (action) {
                        is PostgresAction.Insert -> {
                            val decoded = json.decodeFromJsonElement(ChatMessage.serializer(), action.record)
                            trySend(decoded)
                        }
                        is PostgresAction.Update -> {
                            val updatedId = action.record["id"]?.toString()?.trim('"')
                            if (!updatedId.isNullOrBlank()) {
                                val freshMessage = getMessageById(updatedId)
                                if (freshMessage != null) trySend(freshMessage)
                                else trySend(json.decodeFromJsonElement(ChatMessage.serializer(), action.record))
                            }
                        }
                        is PostgresAction.Delete -> {
                            val deletedId = action.oldRecord["id"]?.toString()?.trim('"')
                            if (!deletedId.isNullOrBlank()) {
                                trySend(
                                    ChatMessage(
                                        id = deletedId,
                                        chatRoomId = roomId,
                                        senderId = "",
                                        text = "This message was deleted",
                                        timestamp = System.currentTimeMillis(),
                                        isDeleted = true
                                    )
                                )
                            }
                        }
                        else -> {}
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing realtime event", e)
                }
            }
        }

        try { channel.subscribe() } catch (e: Exception) {}

        awaitClose {
            collectJob.cancel()
            CoroutineScope(Dispatchers.IO).launch {
                try { channel.unsubscribe(); supabase.realtime.removeChannel(channel) } catch (e: Exception) {}
            }
        }
    }
}