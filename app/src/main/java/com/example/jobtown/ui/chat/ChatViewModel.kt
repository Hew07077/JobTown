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

    private val _eventFlow = MutableSharedFlow<ChatUiEvent>()
    val eventFlow: SharedFlow<ChatUiEvent> = _eventFlow.asSharedFlow()

    private var activeRoomId: String? = null
    private var roomListenerJob: Job? = null
    private var reactionListenerJob: Job? = null
    private var presenceListenerJob: Job? = null

    private val initialQuestionSentForRooms = mutableSetOf<String>()

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

    fun loadMessages(roomId: String, currentUserId: String = "") {
        if (roomId.isBlank()) return

        activeRoomId = roomId
        roomListenerJob?.cancel()
        reactionListenerJob?.cancel()
        presenceListenerJob?.cancel()
        _hasMoreMessages.value = true

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

            // Observe real-time messages
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

        // Observe real-time reactions
        reactionListenerJob = viewModelScope.launch {
            messageRepository.observeReactions(roomId)
                .catch { e -> Log.e(TAG, "Error in reactions stream", e) }
                .collect { reactions ->
                    if (activeRoomId == roomId) {
                        _reactionsList.value = reactions
                    }
                }
        }

        // Observe real-time presence & typing status
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
                    _messagesList.update { current -> olderPage + current }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading older messages", e)
                _eventFlow.emit(ChatUiEvent.ShowToast("Failed to load earlier messages."))
            } finally {
                _isLoadingOlderMessages.value = false
            }
        }
    }

    fun sendInitialQuestionOnce(roomId: String, senderId: String, question: String) {
        if (roomId.isBlank() || question.isBlank()) return
        if (!initialQuestionSentForRooms.add(roomId)) return

        sendMessage(roomId, senderId, question) {
            loadUserChatRooms(senderId)
        }
    }

    private fun mergeIncomingMessage(incoming: ChatMessage) {
        _messagesList.update { current ->
            val index = current.indexOfFirst { it.id == incoming.id }
            if (index != -1) {
                current.toMutableList().apply { set(index, incoming) }
            } else {
                val filtered = current.filterNot {
                    it.id.startsWith("temp_") &&
                            it.senderId == incoming.senderId &&
                            it.text == incoming.text
                }
                val insertIndex = filtered.indexOfFirst { it.timestamp > incoming.timestamp }
                if (insertIndex == -1) {
                    filtered + incoming
                } else {
                    filtered.toMutableList().apply { add(insertIndex, incoming) }
                }
            }
        }
    }

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
                    _eventFlow.emit(ChatUiEvent.ShowToast("Failed to send message."))
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

    fun sendAttachment(
        roomId: String,
        senderId: String,
        bytes: ByteArray,
        fileName: String,
        mimeType: String,
        type: MessageType,
        replyToId: String? = null,
        onResult: (Boolean) -> Unit = {}
    ) {
        if (roomId.isBlank() || bytes.isEmpty()) {
            onResult(false)
            return
        }

        viewModelScope.launch {
            _isUploadingAttachment.value = true
            try {
                val url = messageRepository.uploadChatAttachment(roomId, fileName, bytes, mimeType)
                if (url == null) {
                    _eventFlow.emit(ChatUiEvent.ShowToast("Failed to upload attachment."))
                    onResult(false)
                    return@launch
                }

                _isUploadingAttachment.value = false
                sendMessage(roomId, senderId, url, type, replyToId) { success ->
                    if (success) loadUserChatRooms(senderId)
                    onResult(success)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception while uploading attachment", e)
                _eventFlow.emit(ChatUiEvent.ShowToast("Error uploading attachment."))
                onResult(false)
            } finally {
                _isUploadingAttachment.value = false
            }
        }
    }

    fun toggleReaction(roomId: String, messageId: String, userId: String, emoji: String) {
        if (roomId.isBlank() || messageId.isBlank() || userId.isBlank()) return
        viewModelScope.launch {
            messageRepository.toggleReaction(roomId, messageId, userId, emoji)
        }
    }

    fun sendTypingStatus(roomId: String, userId: String, isTyping: Boolean) {
        if (roomId.isBlank() || userId.isBlank()) return
        viewModelScope.launch {
            messageRepository.sendTypingStatus(roomId, userId, isTyping)
        }
    }

    fun editMessage(roomId: String, messageId: String, newText: String, currentUserId: String = "") {
        val trimmed = newText.trim()
        if (roomId.isBlank() || messageId.isBlank() || trimmed.isBlank()) return

        viewModelScope.launch {
            val originalList = _messagesList.value
            _messagesList.update { current ->
                current.map { if (it.id == messageId) it.copy(text = trimmed, isEdited = true) else it }
            }

            try {
                val success = messageRepository.editMessage(roomId, messageId, trimmed)
                if (success) {
                    if (currentUserId.isNotBlank()) {
                        loadUserChatRooms(currentUserId)
                    }
                } else {
                    _messagesList.value = originalList
                    _eventFlow.emit(ChatUiEvent.ShowToast("Failed to edit message."))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error editing message", e)
                _messagesList.value = originalList
                _eventFlow.emit(ChatUiEvent.ShowToast("Error updating message."))
            }
        }
    }

    fun deleteMessage(roomId: String, messageId: String, currentUserId: String = "") {
        if (roomId.isBlank() || messageId.isBlank()) return

        viewModelScope.launch {
            val originalList = _messagesList.value
            _messagesList.update { current ->
                current.map { if (it.id == messageId) it.copy(text = "This message was deleted", isDeleted = true) else it }
            }

            try {
                val success = messageRepository.deleteMessage(roomId, messageId)
                if (success) {
                    if (currentUserId.isNotBlank()) {
                        loadUserChatRooms(currentUserId)
                    }
                } else {
                    _messagesList.value = originalList
                    _eventFlow.emit(ChatUiEvent.ShowToast("Failed to delete message."))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting message", e)
                _messagesList.value = originalList
                _eventFlow.emit(ChatUiEvent.ShowToast("Error deleting message."))
            }
        }
    }

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