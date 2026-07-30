package com.example.jobtown.ui.chat

import androidx.lifecycle.ViewModel
import com.example.jobtown.data.ActionType
import com.example.jobtown.data.ChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ChatViewModel : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    init {
        // Mock conversation initialization
        _messages.value = listOf(
            ChatMessage(
                id = "1",
                senderId = "2",
                text = "Hello! We reviewed your application and would love to connect.",
                timestamp = System.currentTimeMillis() - 3600000,
                isRead = true
            ),
            ChatMessage(
                id = "2",
                senderId = "1",
                text = "Hi! Thank you for getting back to me. I'm very excited about this opportunity.",
                timestamp = System.currentTimeMillis() - 1800000,
                isRead = true
            )
        )
    }

    fun sendMessage(currentUserId: String, text: String, actionType: ActionType = ActionType.NONE) {
        if (text.isBlank()) return
        val newMsg = ChatMessage(
            id = System.currentTimeMillis().toString(),
            senderId = currentUserId,
            text = text,
            timestamp = System.currentTimeMillis(),
            isRead = false,
            actionType = actionType
        )
        _messages.value = _messages.value + newMsg
    }
}0