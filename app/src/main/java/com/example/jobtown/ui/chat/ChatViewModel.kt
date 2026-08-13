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
import com.example.jobtown.data.MessageType
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
        val index = current.indexOfFirst { it.id == incoming.id }

        if (index != -1) {
            val updatedList = current.toMutableList()
            updatedList[index] = incoming
            messagesList.value = updatedList.sortedBy { it.timestamp }
        } else {
            val withoutTemp = current.filterNot {
                it.id.startsWith("temp_") && it.senderId == incoming.senderId && it.text == incoming.text
            }
            messagesList.value = (withoutTemp + incoming).sortedBy { it.timestamp }
        }
    }

    fun sendMessage(
        roomId: String,
        senderId: String,
        content: String,
        type: MessageType = MessageType.TEXT,
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
                isRead = false,
                messageType = type
            )

            messagesList.value = messagesList.value + tempMessage

            val success = messageRepository.sendMessage(roomId, senderId, trimmed, type)

            if (success) {
                messagesList.value = messageRepository.getMessagesForRoom(roomId)
                loadUserChatRooms(senderId)
            } else {
                messagesList.value = messagesList.value.filterNot { it.id == tempMessage.id }
                Log.e("ChatViewModel", "Failed to send message to repository.")
            }

            isSendingMessage = false
            onResult(success)
        }
    }

    fun editMessage(roomId: String, messageId: String, newText: String) {
        val trimmed = newText.trim()
        if (roomId.isBlank() || messageId.isBlank() || trimmed.isBlank()) return

        viewModelScope.launch {
            // Optimistic update locally
            messagesList.value = messagesList.value.map {
                if (it.id == messageId) it.copy(text = trimmed, isEdited = true) else it
            }

            try {
                val success = messageRepository.editMessage(roomId, messageId, trimmed)
                if (success) {
                    messagesList.value = messageRepository.getMessagesForRoom(roomId)
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error editing message", e)
                messagesList.value = messageRepository.getMessagesForRoom(roomId)
            }
        }
    }

    fun deleteMessage(roomId: String, messageId: String) {
        if (roomId.isBlank() || messageId.isBlank()) return

        viewModelScope.launch {
            // Optimistic update locally
            messagesList.value = messagesList.value.map {
                if (it.id == messageId) it.copy(text = "This message was deleted", isDeleted = true) else it
            }

            try {
                val success = messageRepository.deleteMessage(roomId, messageId)
                if (success) {
                    messagesList.value = messageRepository.getMessagesForRoom(roomId)
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error deleting message", e)
                messagesList.value = messageRepository.getMessagesForRoom(roomId)
            }
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