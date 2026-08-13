package com.example.jobtown.ui.chat

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.jobtown.data.ChatMessage
import com.example.jobtown.data.ChatRoom
import com.example.jobtown.data.repository.MessageRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ChatViewModel(private val messageRepository: MessageRepository) : ViewModel() {

    var chatRooms = mutableStateOf<List<ChatRoom>>(emptyList())
        private set

    var messagesList = mutableStateOf<List<ChatMessage>>(emptyList())
        private set

    var isLoadingRooms by mutableStateOf(false)
        private set

    var isLoadingMessages by mutableStateOf(false)
        private set

    var isSendingMessage by mutableStateOf(false)
        private set

    // Tracks which room the current realtime subscription belongs to, so a stray
    // late event from a room the user has since left doesn't get applied to the
    // wrong screen.
    private var activeRoomId: String? = null
    private var realtimeJob: Job? = null

    fun loadUserChatRooms(userId: String) {
        if (userId.isBlank()) return
        viewModelScope.launch {
            isLoadingRooms = true
            try {
                chatRooms.value = messageRepository.getChatRoomsForUser(userId)
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error loading chat rooms", e)
            } finally {
                isLoadingRooms = false
            }
        }
    }

    // currentUserId is optional so existing call sites keep compiling, but passing
    // it lets us mark the other person's messages as read as soon as this room is
    // opened, and is required for the live-message subscription to know which
    // room it's listening for.
    fun loadMessages(roomId: String, currentUserId: String = "") {
        if (roomId.isBlank()) return

        activeRoomId = roomId
        realtimeJob?.cancel()

        viewModelScope.launch {
            isLoadingMessages = true
            try {
                messagesList.value = messageRepository.getMessagesForRoom(roomId)
                if (currentUserId.isNotBlank()) {
                    messageRepository.markMessagesAsRead(roomId, currentUserId)
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error loading messages", e)
            } finally {
                isLoadingMessages = false
            }
        }

        realtimeJob = viewModelScope.launch {
            messageRepository.observeNewMessages(roomId).collect { incoming ->
                if (activeRoomId != roomId) return@collect
                mergeIncomingMessage(incoming)
                if (currentUserId.isNotBlank() && incoming.senderId != currentUserId) {
                    messageRepository.markMessagesAsRead(roomId, currentUserId)
                }
            }
        }
    }

    private fun mergeIncomingMessage(incoming: ChatMessage) {
        val current = messagesList.value
        if (current.any { it.id == incoming.id }) return

        // Drop the optimistic "temp_" placeholder this message was standing in
        // for (added by sendMessage below) so it isn't shown twice.
        val withoutTemp = current.filterNot {
            it.id.startsWith("temp_") && it.senderId == incoming.senderId && it.text == incoming.text
        }
        messagesList.value = (withoutTemp + incoming).sortedBy { it.timestamp }
    }

    fun sendMessage(
        roomId: String,
        senderId: String,
        content: String,
        onResult: (Boolean) -> Unit = {}
    ) {
        val trimmed = content.trim()
        if (roomId.isBlank() || trimmed.isBlank()) {
            onResult(false)
            return
        }

        viewModelScope.launch {
            isSendingMessage = true
            val now = System.currentTimeMillis()

            val tempMessage = ChatMessage(
                id = "temp_$now",
                chatRoomId = roomId,
                senderId = senderId,
                text = trimmed,
                timestamp = now,
                isRead = false
            )

            messagesList.value = messagesList.value + tempMessage

            val success = messageRepository.sendMessage(roomId, senderId, trimmed)

            if (success) {
                // Belt-and-braces refresh in addition to the realtime subscription:
                // if Realtime replication isn't enabled for chat_messages yet, this
                // still guarantees the sender sees their own confirmed message.
                messagesList.value = messageRepository.getMessagesForRoom(roomId)
                loadUserChatRooms(senderId)
            } else {
                // Remove the optimistic bubble so a failed send doesn't leave a
                // message on screen that was never actually delivered.
                messagesList.value = messagesList.value.filterNot { it.id == tempMessage.id }
                Log.e("ChatViewModel", "Failed to send message to repository.")
            }

            isSendingMessage = false
            onResult(success)
        }
    }

    override fun onCleared() {
        super.onCleared()
        realtimeJob?.cancel()
    }
}

class ChatViewModelFactory(private val messageRepository: MessageRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(messageRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}