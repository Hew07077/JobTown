package com.example.jobtown.data.repository

import com.example.jobtown.data.ActionType
import com.example.jobtown.data.ChatMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class ChatRepository {
    private val messagesStore = MutableStateFlow<List<ChatMessage>>(emptyList())

    fun getMessagesForRoom(roomId: String): Flow<List<ChatMessage>> {
        return messagesStore.map { list ->
            list.filter { it.chatRoomId == roomId }.sortedBy { it.timestamp }
        }
    }

    suspend fun sendMessage(
        roomId: String,
        senderId: String,
        text: String,
        actionType: ActionType = ActionType.NONE
    ) {
        val newMessage = ChatMessage(
            id = "msg_${System.currentTimeMillis()}",
            chatRoomId = roomId,
            senderId = senderId,
            text = text,
            timestamp = System.currentTimeMillis(),
            isRead = false,
            actionType = actionType
        )
        messagesStore.value = messagesStore.value + newMessage
    }
}