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

    fun loadMessages(roomId: String) {
        if (roomId.isBlank()) return
        viewModelScope.launch {
            isLoadingMessages = true
            try {
                messagesList.value = messageRepository.getMessagesForRoom(roomId)
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error loading messages", e)
            } finally {
                isLoadingMessages = false
            }
        }
    }

    fun sendMessage(
        roomId: String,
        senderId: String,
        content: String,
        onResult: (Boolean) -> Unit = {}
    ) {
        if (roomId.isBlank() || content.isBlank()) {
            onResult(false)
            return
        }

        viewModelScope.launch {
            val now = System.currentTimeMillis()

            // Omitted custom fields like actionType to prevent type/unresolved reference errors.
            // Adjust properties here to match your exact ChatMessage constructor if it uses defaults.
            val tempMessage = ChatMessage(
                id = "temp_$now",
                chatRoomId = roomId,
                senderId = senderId,
                text = content,
                timestamp = now,
                isRead = false
            )

            messagesList.value = messagesList.value + tempMessage

            val success = messageRepository.sendMessage(roomId, senderId, content)

            if (success) {
                messagesList.value = messageRepository.getMessagesForRoom(roomId)
                loadUserChatRooms(senderId)
            } else {
                Log.e("ChatViewModel", "Failed to send message to repository.")
            }

            onResult(success)
        }
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