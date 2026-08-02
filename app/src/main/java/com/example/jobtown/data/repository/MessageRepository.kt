package com.example.jobtown.data.repository

import android.util.Log
import com.example.jobtown.data.ChatMessage
import com.example.jobtown.data.ChatRoom
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MessageRepository(private val supabase: SupabaseClient) {

    suspend fun getOrCreateChatRoom(
        seekerId: String,
        seekerName: String,
        employerId: String,
        companyName: String
    ): String = withContext(Dispatchers.IO) {
        try {
            if (seekerId.isBlank()) return@withContext ""

            val existingRoom = supabase.postgrest["chat_rooms"]
                .select {
                    filter {
                        eq("seeker_id", seekerId)
                        eq("company_name", companyName)
                    }
                }
                .decodeList<ChatRoom>()
                .firstOrNull()

            if (existingRoom != null && existingRoom.id.isNotBlank()) {
                return@withContext existingRoom.id
            }

            val newRoomPayload = mapOf(
                "seeker_id" to seekerId,
                "seeker_name" to seekerName,
                "employer_id" to employerId.ifBlank { "employer_default" },
                "company_name" to companyName,
                "last_message" to "Chat started",
                "last_message_time" to System.currentTimeMillis()
            )

            val insertedRoom = supabase.postgrest["chat_rooms"]
                .insert(newRoomPayload) {
                    select()
                }
                .decodeSingle<ChatRoom>()

            insertedRoom.id
        } catch (e: Exception) {
            Log.e("MessageRepository", "Error in getOrCreateChatRoom", e)
            ""
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

    suspend fun sendMessage(roomId: String, senderId: String, content: String): Boolean = withContext(Dispatchers.IO) {
        try {
            if (roomId.isBlank() || content.isBlank()) return@withContext false
            val now = System.currentTimeMillis()

            val messagePayload = mapOf(
                "chat_room_id" to roomId,
                "sender_id" to senderId,
                "text" to content,
                "timestamp" to now,
                "is_read" to false,
                "action_type" to "NONE" // Sent as string to match database text column mapping
            )

            supabase.postgrest["chat_messages"].insert(messagePayload)

            supabase.postgrest["chat_rooms"].update(
                mapOf(
                    "last_message" to content,
                    "last_message_time" to now
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
}