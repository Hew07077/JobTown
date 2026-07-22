package com.example.jobtown.data

data class ChatMessage(
    val id: String,
    val senderId: String,
    val senderName: String,
    val messageText: String,
    val timestamp: String,
    val isFromUser: Boolean = false
)