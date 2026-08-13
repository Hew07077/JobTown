package com.example.jobtown.data.repository

import android.util.Log
import com.example.jobtown.data.ActionType
import com.example.jobtown.data.ChatMessage
import com.example.jobtown.data.ChatRoom
import com.example.jobtown.data.MessageType
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

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
    val is_deleted: Boolean = false
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
private data class EditMessagePayload(
    val text: String,
    val is_edited: Boolean
)

@Serializable
private data class DeleteMessagePayload(
    val text: String,
    val is_deleted: Boolean
)

class MessageRepository(private val supabase: SupabaseClient) {

    suspend fun getOrCreateChatRoom(
        seekerId: String,
        seekerName: String,
        employerId: String,
        companyName: String,
        jobTitle: String
    ): String = withContext(Dispatchers.IO) {
        if (seekerId.isBlank()) {
            Log.e("MessageRepository", "getOrCreateChatRoom called with blank seekerId")
            return@withContext ""
        }

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
                .insert(newRoomPayload) {
                    select()
                }
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

    suspend fun getMessagesForRoom(roomId: String): List<ChatMessage> = withContext(Dispatchers.IO) {
        try {
            if (roomId.isBlank()) return@withContext emptyList()

            supabase.postgrest["chat_messages"]
                .select {
                    filter {
                        eq("chat_room_id", roomId)
                    }
                }
                .decodeList<ChatMessage>()
                .sortedBy { it.timestamp }
        } catch (e: Exception) {
            Log.e("MessageRepository", "Error in getMessagesForRoom", e)
            emptyList()
        }
    }

    suspend fun sendMessage(
        roomId: String,
        senderId: String,
        content: String,
        type: MessageType = MessageType.TEXT
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
                action_type = ActionType.NONE.name
            )

            supabase.postgrest["chat_messages"].insert(messagePayload)

            val snippet = when (type) {
                MessageType.IMAGE -> "[Image]"
                MessageType.FILE -> "[Document]"
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
            Log.e("MessageRepository", "Error in sendMessage", e)
            false
        }
    }

    suspend fun editMessage(roomId: String, messageId: String, newText: String): Boolean = withContext(Dispatchers.IO) {
        if (messageId.isBlank() || newText.isBlank()) return@withContext false

        try {
            // 1. Permanently update message row in Supabase
            supabase.postgrest["chat_messages"].update(
                EditMessagePayload(
                    text = newText,
                    is_edited = true
                )
            ) {
                filter { eq("id", messageId) }
            }

            // 2. Refresh last_message in chat_rooms if it's the latest message
            val messages = getMessagesForRoom(roomId)
            val latest = messages.lastOrNull()
            if (latest != null && latest.id == messageId) {
                supabase.postgrest["chat_rooms"].update(
                    ChatRoomLastMessageUpdate(last_message = newText)
                ) {
                    filter { eq("id", roomId) }
                }
            }

            true
        } catch (e: Exception) {
            Log.e("MessageRepository", "Error editing message $messageId", e)
            false
        }
    }

    suspend fun deleteMessage(roomId: String, messageId: String): Boolean = withContext(Dispatchers.IO) {
        if (messageId.isBlank()) return@withContext false

        try {
            val deletedText = "This message was deleted"

            // 1. Mark message as deleted in Supabase
            supabase.postgrest["chat_messages"].update(
                DeleteMessagePayload(
                    text = deletedText,
                    is_deleted = true
                )
            ) {
                filter { eq("id", messageId) }
            }

            // 2. Refresh last_message in chat_rooms if it was the latest message
            val messages = getMessagesForRoom(roomId)
            val latest = messages.lastOrNull()
            if (latest != null && latest.id == messageId) {
                supabase.postgrest["chat_rooms"].update(
                    ChatRoomLastMessageUpdate(last_message = deletedText)
                ) {
                    filter { eq("id", roomId) }
                }
            }

            true
        } catch (e: Exception) {
            Log.e("MessageRepository", "Error deleting message $messageId", e)
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

    fun observeNewMessages(roomId: String): Flow<ChatMessage> = callbackFlow {
        if (roomId.isBlank()) {
            close()
            return@callbackFlow
        }

        val channel = supabase.channel("chat_messages_${roomId}_${System.currentTimeMillis()}")

        val collectJob = launch {
            channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "chat_messages"
            }.collect { action ->
                when (action) {
                    is PostgresAction.Insert -> {
                        try {
                            val message = action.decodeRecord<ChatMessage>()
                            if (message.chatRoomId == roomId) trySend(message)
                        } catch (e: Exception) {
                            Log.e("MessageRepository", "Error decoding realtime insert", e)
                        }
                    }
                    is PostgresAction.Update -> {
                        try {
                            val message = action.decodeRecord<ChatMessage>()
                            if (message.chatRoomId == roomId) trySend(message)
                        } catch (e: Exception) {
                            Log.e("MessageRepository", "Error decoding realtime update", e)
                        }
                    }
                    else -> {}
                }
            }
        }

        try {
            channel.subscribe()
        } catch (e: Exception) {
            Log.e("MessageRepository", "Error subscribing to realtime channel for room=$roomId", e)
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
}