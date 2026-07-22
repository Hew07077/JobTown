package com.example.jobtown.data

data class ChatRoom(
    val id: String = "",
    val applicationId: String = "",
    val jobTitle: String = "",
    val seekerId: String = "",
    val seekerName: String = "",
    val companyId: String = "",
    val companyName: String = "",
    val lastMessage: String = "",
    val lastMessageTime: Long = System.currentTimeMillis()
)