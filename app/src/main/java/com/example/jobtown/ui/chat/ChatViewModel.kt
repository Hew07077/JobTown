package com.example.jobtown.ui.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.jobtown.data.ChatMessage
import com.example.jobtown.data.ChatRoom
import com.example.jobtown.data.MessageReaction
import com.example.jobtown.data.MessageType
import com.example.jobtown.data.repository.MessageRepository
import com.example.jobtown.data.repository.RoomPresence
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ChatUiEvent {
    data class ShowToast(val message: String) : ChatUiEvent
}

class ChatViewModel(private val messageRepository: MessageRepository) : ViewModel() {

    private val _chatRooms = MutableStateFlow<List<ChatRoom>>(emptyList())
    val chatRooms: StateFlow<List<ChatRoom>> = _chatRooms.asStateFlow()

    private val _messagesList = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messagesList: StateFlow<List<ChatMessage>> = _messagesList.asStateFlow()

    private val _reactionsList = MutableStateFlow<List<MessageReaction>>(emptyList())
    val reactionsList: StateFlow<List<MessageReaction>> = _reactionsList.asStateFlow()

    private val _roomPresence = MutableStateFlow(RoomPresence())
    val roomPresence: StateFlow<RoomPresence> = _roomPresence.asStateFlow()

    // UI State flags
    private val _isLoadingRooms = MutableStateFlow(false)
    val isLoadingRooms: StateFlow<Boolean> = _isLoadingRooms.asStateFlow()

    private val _isLoadingMessages = MutableStateFlow(false)
    val isLoadingMessages: StateFlow<Boolean> = _isLoadingMessages.asStateFlow()

    private val _isSendingMessage = MutableStateFlow(false)
    val isSendingMessage: StateFlow<Boolean> = _isSendingMessage.asStateFlow()

    private val _isUploadingAttachment = MutableStateFlow(false)
    val isUploadingAttachment: StateFlow<Boolean> = _isUploadingAttachment.asStateFlow()

    private val _isLoadingOlderMessages = MutableStateFlow(false)
    val isLoadingOlderMessages: StateFlow<Boolean> = _isLoadingOlderMessages.asStateFlow()

    private val _hasMoreMessages = MutableStateFlow(true)
    val hasMoreMessages: StateFlow<Boolean> = _hasMoreMessages.asStateFlow()

    // Advanced UI filters & archiving
    private val _pinnedRoomIds = MutableStateFlow<Set<String>>(emptySet())
    val pinnedRoomIds: StateFlow<Set<String>> = _pinnedRoomIds.asStateFlow()

    private val _archivedRoomIds = MutableStateFlow<Set<String>>(emptySet())
    val archivedRoomIds: StateFlow<Set<String>> = _archivedRoomIds.asStateFlow()

    private val _messageSearchQuery = MutableStateFlow("")
    val messageSearchQuery: StateFlow<String> = _messageSearchQuery.asStateFlow()

    private val _eventFlow = MutableSharedFlow<ChatUiEvent>()
    val eventFlow: SharedFlow<ChatUiEvent> = _eventFlow.asSharedFlow()

    private var activeRoomId: String? = null
    private var roomListenerJob: Job? = null
    private var reactionListenerJob: Job? = null
    private var presenceListenerJob: Job? = null

    private val initialQuestionSentForRooms = mutableSetOf<String>()

    // ==================== Room Management ====================

    fun loadUserChatRooms(userId: String) {
        if (userId.isBlank()) return
        viewModelScope.launch {
            _isLoadingRooms.value = true
            try {
                _chatRooms.value = messageRepository.getChatRoomsForUser(userId)
            } catch (e: Exception) {
                Log.e(TAG, "Error loading chat rooms", e)
                _eventFlow.emit(ChatUiEvent.ShowToast("Failed to load chat conversations."))
            } finally {
                _isLoadingRooms.value = false
            }
        }
    }

    suspend fun getOrCreateChatRoom(
        seekerId: String,
        seekerName: String,
        employerId: String,
        companyName: String,
        jobTitle: String
    ): String {
        return messageRepository.getOrCreateChatRoom(
            seekerId = seekerId,
            seekerName = seekerName,
            employerId = employerId,
            companyName = companyName,
            jobTitle = jobTitle
        )
    }

    fun togglePinRoom(roomId: String) {
        if (roomId.isBlank()) return
        _pinnedRoomIds.update { current ->
            if (current.contains(roomId)) current - roomId else current + roomId
        }
    }

    fun toggleArchiveRoom(roomId: String) {
        if (roomId.isBlank()) return
        _archivedRoomIds.update { current ->
            if (current.contains(roomId)) current - roomId else current + roomId
        }
    }

    // ==================== Message Loading ====================

    fun loadMessages(roomId: String, currentUserId: String = "") {
        if (roomId.isBlank()) return

        activeRoomId = roomId
        roomListenerJob?.cancel()
        reactionListenerJob?.cancel()
        presenceListenerJob?.cancel()
        _hasMoreMessages.value = true
        _messageSearchQuery.value = ""

        roomListenerJob = viewModelScope.launch {
            _isLoadingMessages.value = true
            try {
                val page = messageRepository.getMessagesForRoom(roomId)
                _messagesList.value = page
                _reactionsList.value = messageRepository.getReactionsForRoom(roomId)
                _hasMoreMessages.value = page.size >= MessageRepository.DEFAULT_PAGE_SIZE
                if (currentUserId.isNotBlank()) {
                    messageRepository.markMessagesAsRead(roomId, currentUserId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading initial messages", e)
                _eventFlow.emit(ChatUiEvent.ShowToast("Failed to load message history."))
            } finally {
                _isLoadingMessages.value = false
            }

            messageRepository.observeNewMessages(roomId)
                .catch { e -> Log.e(TAG, "Error in realtime message stream", e) }
                .collect { incoming ->
                    if (activeRoomId != roomId) return@collect
                    mergeIncomingMessage(incoming)
                    if (currentUserId.isNotBlank() && incoming.senderId != currentUserId) {
                        messageRepository.markMessagesAsRead(roomId, currentUserId)
                    }
                }
        }

        reactionListenerJob = viewModelScope.launch {
            messageRepository.observeReactions(roomId)
                .catch { e -> Log.e(TAG, "Error in reactions stream", e) }
                .collect { reactions ->
                    if (activeRoomId == roomId) {
                        _reactionsList.value = reactions
                    }
                }
        }

        if (currentUserId.isNotBlank()) {
            presenceListenerJob = viewModelScope.launch {
                messageRepository.observeRoomPresence(roomId, currentUserId)
                    .catch { e -> Log.e(TAG, "Error in presence stream", e) }
                    .collect { presence ->
                        if (activeRoomId == roomId) {
                            _roomPresence.value = presence
                        }
                    }
            }
        }
    }

    fun loadOlderMessages(roomId: String) {
        if (roomId.isBlank() || _isLoadingOlderMessages.value || !_hasMoreMessages.value) return
        val oldestTimestamp = _messagesList.value.firstOrNull()?.timestamp ?: return

        viewModelScope.launch {
            _isLoadingOlderMessages.value = true
            try {
                val olderPage = messageRepository.getMessagesForRoom(roomId, beforeTimestamp = oldestTimestamp)
                if (olderPage.isEmpty()) {
                    _hasMoreMessages.value = false
                } else {
                    _hasMoreMessages.value = olderPage.size >= MessageRepository.DEFAULT_PAGE_SIZE
                    _messagesList.update { current ->
                        val existingIds = current.map { it.id }.toSet()
                        val newUnique = olderPage.filterNot { existingIds.contains(it.id) }
                        newUnique + current
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading older messages", e)
                _eventFlow.emit(ChatUiEvent.ShowToast("Failed to load earlier messages."))
            } finally {
                _isLoadingOlderMessages.value = false
            }
        }
    }

    fun updateMessageSearchQuery(query: String) {
        _messageSearchQuery.value = query
    }

    // ==================== Sending Messages ====================

    fun sendMessage(
        roomId: String,
        senderId: String,
        content: String,
        type: MessageType = MessageType.TEXT,
        replyToId: String? = null,
        onResult: (Boolean) -> Unit = {}
    ) {
        val trimmed = content.trim()
        if (roomId.isBlank() || trimmed.isBlank()) {
            onResult(false)
            return
        }

        viewModelScope.launch {
            _isSendingMessage.value = true
            val now = System.currentTimeMillis()
            val tempMessage = ChatMessage(
                id = "temp_$now",
                chatRoomId = roomId,
                senderId = senderId,
                text = trimmed,
                timestamp = now,
                isRead = false,
                messageType = type,
                replyToId = replyToId
            )

            _messagesList.update { it + tempMessage }

            try {
                val success = messageRepository.sendMessage(roomId, senderId, trimmed, type, replyToId)
                if (success) {
                    loadUserChatRooms(senderId)
                } else {
                    _messagesList.update { list -> list.filterNot { it.id == tempMessage.id } }
                }
                onResult(success)
            } catch (e: Exception) {
                Log.e(TAG, "Exception while sending message", e)
                _messagesList.update { list -> list.filterNot { it.id == tempMessage.id } }
                _eventFlow.emit(ChatUiEvent.ShowToast("Error sending message."))
                onResult(false)
            } finally {
                _isSendingMessage.value = false
            }
        }
    }

    fun sendInitialQuestionOnce(roomId: String, userId: String, initialQuestion: String) {
        if (roomId.isBlank() || userId.isBlank() || initialQuestion.isBlank()) return
        val key = "$roomId:$userId"
        if (initialQuestionSentForRooms.contains(key)) return

        viewModelScope.launch {
            try {
                val existingMessages = messageRepository.getMessagesForRoom(roomId, limit = 1)
                if (existingMessages.isNotEmpty()) {
                    initialQuestionSentForRooms.add(key)
                    return@launch
                }

                val success = messageRepository.sendMessage(
                    roomId = roomId,
                    senderId = userId,
                    content = initialQuestion,
                    type = MessageType.TEXT
                )
                if (success) {
                    initialQuestionSentForRooms.add(key)
                    loadUserChatRooms(userId)
                    loadMessages(roomId, userId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending initial question", e)
            }
        }
    }

    fun sendTypingStatus(roomId: String, userId: String, isTyping: Boolean) {
        if (roomId.isBlank() || userId.isBlank()) return
        viewModelScope.launch {
            messageRepository.sendTypingStatus(roomId, userId, isTyping)
        }
    }

    fun sendAttachment(
        roomId: String,
        senderId: String,
        bytes: ByteArray,
        fileName: String,
        mimeType: String,
        type: MessageType,
        replyToId: String? = null,
        onComplete: (Boolean) -> Unit = {}
    ) {
        if (roomId.isBlank() || senderId.isBlank() || bytes.isEmpty()) {
            onComplete(false)
            return
        }

        viewModelScope.launch {
            _isUploadingAttachment.value = true
            try {
                val url = messageRepository.uploadChatAttachment(
                    roomId = roomId,
                    fileName = fileName,
                    bytes = bytes,
                    mimeType = mimeType
                )

                if (url == null) {
                    _eventFlow.emit(ChatUiEvent.ShowToast("Failed to upload attachment."))
                    onComplete(false)
                    return@launch
                }

                val success = messageRepository.sendMessage(
                    roomId = roomId,
                    senderId = senderId,
                    content = url,
                    type = type,
                    replyToId = replyToId
                )

                if (success) {
                    loadUserChatRooms(senderId)
                } else {
                    _eventFlow.emit(ChatUiEvent.ShowToast("Failed to send attachment."))
                }
                onComplete(success)
            } catch (e: Exception) {
                Log.e(TAG, "Error sending attachment", e)
                _eventFlow.emit(ChatUiEvent.ShowToast("Error sending attachment."))
                onComplete(false)
            } finally {
                _isUploadingAttachment.value = false
            }
        }
    }

    // ==================== Message Management ====================

    fun editMessage(roomId: String, messageId: String, newText: String, currentUserId: String = "") {
        val trimmed = newText.trim()
        if (roomId.isBlank() || messageId.isBlank() || trimmed.isBlank()) return

        viewModelScope.launch {
            val previousList = _messagesList.value

            // Optimistic update
            _messagesList.update { current ->
                current.map { msg ->
                    if (msg.id == messageId) {
                        msg.copy(text = trimmed, isEdited = true)
                    } else msg
                }
            }

            val success = messageRepository.editMessage(roomId, messageId, trimmed)
            if (success) {
                if (currentUserId.isNotBlank()) loadUserChatRooms(currentUserId)
            } else {
                // Revert on failure
                _messagesList.value = previousList
                _eventFlow.emit(ChatUiEvent.ShowToast("Failed to update message."))
            }
        }
    }

    fun deleteMessage(roomId: String, messageId: String, currentUserId: String = "") {
        if (roomId.isBlank() || messageId.isBlank()) return

        viewModelScope.launch {
            val previousList = _messagesList.value

            // Optimistic update
            _messagesList.update { current ->
                current.map { msg ->
                    if (msg.id == messageId) {
                        msg.copy(text = "This message was deleted", isDeleted = true, isEdited = false)
                    } else msg
                }
            }

            val success = messageRepository.deleteMessage(roomId, messageId)
            if (success) {
                if (currentUserId.isNotBlank()) loadUserChatRooms(currentUserId)
            } else {
                // Revert on failure
                _messagesList.value = previousList
                _eventFlow.emit(ChatUiEvent.ShowToast("Failed to delete message."))
            }
        }
    }

    fun toggleReaction(roomId: String, messageId: String, userId: String, emoji: String) {
        if (roomId.isBlank() || messageId.isBlank() || userId.isBlank() || emoji.isBlank()) return

        viewModelScope.launch {
            val previousReactions = _reactionsList.value
            val existing = previousReactions.firstOrNull {
                it.messageId == messageId && it.userId == userId && it.emoji == emoji
            }

            // Optimistic update so the tap feels instant instead of waiting on the
            // realtime round-trip (same pattern used for edit/delete).
            _reactionsList.update { current ->
                if (existing != null) {
                    current.filterNot { it.id == existing.id }
                } else {
                    current + MessageReaction(
                        id = "temp_reaction_${System.currentTimeMillis()}",
                        messageId = messageId,
                        chatRoomId = roomId,
                        userId = userId,
                        emoji = emoji,
                        createdAt = System.currentTimeMillis()
                    )
                }
            }

            try {
                val success = messageRepository.toggleReaction(roomId, messageId, userId, emoji)
                if (!success) {
                    _reactionsList.value = previousReactions
                    _eventFlow.emit(ChatUiEvent.ShowToast("Failed to update reaction."))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error toggling reaction", e)
                _reactionsList.value = previousReactions
                _eventFlow.emit(ChatUiEvent.ShowToast("Failed to update reaction."))
            }
        }
    }

    // ==================== Private Helpers ====================

    private fun mergeIncomingMessage(incoming: ChatMessage) {
        _messagesList.update { current ->
            // 1. Check if the exact message ID exists (covers existing message update/delete realtime broadcast)
            val index = current.indexOfFirst { it.id == incoming.id }
            if (index != -1) {
                return@update current.toMutableList().apply {
                    set(index, incoming)
                }
            }

            // 2. Clear out temp messages if real message arrived
            val filtered = current.filterNot {
                it.id.startsWith("temp_") && it.senderId == incoming.senderId && it.text == incoming.text
            }

            // 3. Insert or append based on timestamp order
            val insertIndex = filtered.indexOfFirst { it.timestamp > incoming.timestamp }
            if (insertIndex == -1) {
                filtered + incoming
            } else {
                filtered.toMutableList().apply { add(insertIndex, incoming) }
            }
        }
    }

    // ==================== Cleanup ====================

    override fun onCleared() {
        super.onCleared()
        roomListenerJob?.cancel()
        reactionListenerJob?.cancel()
        presenceListenerJob?.cancel()
    }

    companion object {
        private const val TAG = "ChatViewModel"
    }
}

class ChatViewModelFactory(
    private val messageRepository: MessageRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(messageRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}