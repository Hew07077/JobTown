package com.example.jobtown.data

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val actionType: ActionType = ActionType.NONE
)

enum class ActionType {
    NONE,
    INTERVIEW_REQUEST,
    RESUME_REQUEST
}