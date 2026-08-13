package com.example.jobtown.data.repository

import android.util.Log
import com.example.jobtown.data.ChatMessage
import com.example.jobtown.data.ChatRoom
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

// kotlinx.serialization needs a concrete, compile-time-known type for anything
// passed to .insert()/.update(). A raw mapOf(...) that mixes String, Long, and
// Boolean values gets inferred as Map<String, Any> -- and there's no serializer
// for Any, so that call fails immediately client-side with:
//   "Serializer for class 'Any' is not found."
// before it ever reaches the network. Using small @Serializable data classes
// (one per payload shape) instead of mapOf(...) avoids that entirely.

@Serializable
private data class NewChatRoomPayload(
    val seeker_id: String,
    val seeker_name: String,
    val employer_id: String,
    val company_name: String,
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
    val action_type: String
)

@Serializable
private data class ChatRoomLastMessageUpdate(
    val last_message: String,
    val last_message_time: Long
)

@Serializable
private data class ReadStatusUpdate(
    val is_read: Boolean
)

class MessageRepository(private val supabase: SupabaseClient) {

    // NOTE: this now RETHROWS after logging, instead of catching and returning "".
    // The caller (AppNavGraph) shows e.message directly in a Snackbar -- that way
    // the actual Postgrest/Supabase error text shows up on-screen, no adb/Logcat
    // needed to see what actually went wrong.
    suspend fun getOrCreateChatRoom(
        seekerId: String,
        seekerName: String,
        employerId: String,
        companyName: String
    ): String = withContext(Dispatchers.IO) {
        if (seekerId.isBlank()) {
            Log.e("MessageRepository", "getOrCreateChatRoom called with blank seekerId")
            return@withContext ""
        }

        Log.d("MessageRepository", "Looking up existing chat room for seekerId=$seekerId company=$companyName")

        try {
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
                Log.d("MessageRepository", "Reusing existing chat room id=${existingRoom.id}")
                return@withContext existingRoom.id
            }
        } catch (e: Exception) {
            Log.e("MessageRepository", "Error looking up existing chat room", e)
            throw Exception("Lookup failed: ${e.message ?: e.toString()}", e)
        }

        Log.d("MessageRepository", "No existing room found, inserting a new one")

        val newRoomPayload = NewChatRoomPayload(
            seeker_id = seekerId,
            seeker_name = seekerName,
            employer_id = employerId.ifBlank { "employer_default" },
            company_name = companyName,
            last_message = "Chat started",
            last_message_time = System.currentTimeMillis()
        )

        try {
            val insertedRoom = supabase.postgrest["chat_rooms"]
                .insert(newRoomPayload) {
                    select()
                }
                .decodeSingle<ChatRoom>()

            Log.d("MessageRepository", "Created new chat room id=${insertedRoom.id}")
            insertedRoom.id
        } catch (e: Exception) {
            // Common real-world causes if this fires: the "chat_rooms" table doesn't
            // exist yet in Supabase, a required column is missing/NOT NULL without a
            // default, or a Row Level Security policy is silently blocking the
            // insert/select for this user (RLS blocks insert() { select() } if there's
            // no SELECT policy, even when INSERT is allowed).
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

    suspend fun sendMessage(roomId: String, senderId: String, content: String): Boolean = withContext(Dispatchers.IO) {
        try {
            if (roomId.isBlank() || content.isBlank()) {
                Log.e("MessageRepository", "sendMessage called with blank roomId or content")
                return@withContext false
            }
            val now = System.currentTimeMillis()

            val messagePayload = NewChatMessagePayload(
                chat_room_id = roomId,
                sender_id = senderId,
                text = content,
                timestamp = now,
                is_read = false,
                message_type = "TEXT", // chat_messages.message_type is a required enum column
                action_type = "NONE"
            )

            Log.d("MessageRepository", "Inserting message into room=$roomId sender=$senderId")
            supabase.postgrest["chat_messages"].insert(messagePayload)

            supabase.postgrest["chat_rooms"].update(
                ChatRoomLastMessageUpdate(
                    last_message = content,
                    last_message_time = now
                )
            ) {
                filter { eq("id", roomId) }
            }

            Log.d("MessageRepository", "sendMessage succeeded")
            true
        } catch (e: Exception) {
            Log.e("MessageRepository", "Error in sendMessage", e)
            false
        }
    }

    // Marks every message in a room that wasn't sent by the current viewer as read.
    // Previously nothing ever set is_read back to true after insert, so it stayed
    // false forever -- harmless today since no UI reads it yet, but it silently
    // broke the one piece of data (real read receipts / unread counts) any future
    // "unread" badge on the chat list would need.
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

    // Live stream of messages inserted into [roomId] after this is collected.
    // Chat previously only ever refreshed on YOUR OWN send -- if the other person
    // replied while you had the room open, you wouldn't see it until you backed
    // out and reopened the chat. This pushes new rows in as they're inserted.
    //
    // Requires Realtime replication to be turned on for "chat_messages" in the
    // Supabase dashboard (Database > Replication) -- if it isn't, this flow simply
    // never emits and the screen falls back to the existing refresh-on-send/reopen
    // behavior, so it's safe to add even before that's configured.
    fun observeNewMessages(roomId: String): Flow<ChatMessage> = callbackFlow {
        if (roomId.isBlank()) {
            close()
            return@callbackFlow
        }

        // Suffix with the current time so reopening the same room quickly doesn't
        // collide with a previous channel of the same name still tearing down.
        val channel = supabase.channel("chat_messages_${roomId}_${System.currentTimeMillis()}")

        val collectJob = launch {
            channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "chat_messages"
            }.collect { action ->
                if (action is PostgresAction.Insert) {
                    try {
                        val message = action.decodeRecord<ChatMessage>()
                        if (message.chatRoomId == roomId) {
                            trySend(message)
                        }
                    } catch (e: Exception) {
                        Log.e("MessageRepository", "Error decoding realtime chat message", e)
                    }
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
            // unsubscribe/removeChannel are suspend calls; awaitClose's cleanup
            // lambda isn't, so fire them on a short-lived scope instead of blocking.
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