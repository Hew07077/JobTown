package com.example.jobtown.ui.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.jobtown.data.ActionType
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
    private var roomsListenerJob: Job? = null

    private val initialQuestionSentForRooms = mutableSetOf<String>()

    fun loadUserChatRooms(userId: String) {
        if (userId.isBlank()) return

        roomsListenerJob?.cancel()

        viewModelScope.launch {
            _isLoadingRooms.value = true
            try {
                _chatRooms.value = messageRepository.getChatRoomsForUser(userId)
            } catch (e: Exception) {
                Log.e(TAG, "Error loading chat rooms", e)
            } finally {
                _isLoadingRooms.value = false
            }
        }

        roomsListenerJob = viewModelScope.launch {
            messageRepository.observeUserChatRooms(userId)
                .catch { e -> Log.e(TAG, "Error in rooms stream", e) }
                .collect { updatedRooms ->
                    _chatRooms.update { currentRooms ->
                        if (currentRooms.isEmpty()) {
                            updatedRooms
                        } else {
                            val map = currentRooms.associateBy { it.id }.toMutableMap()
                            updatedRooms.forEach { room ->
                                val existing = map[room.id]
                                if (existing == null || room.lastMessageTime >= existing.lastMessageTime) {
                                    map[room.id] = room
                                }
                            }
                            map.values.sortedByDescending { it.lastMessageTime }
                        }
                    }
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

    fun loadMessages(roomId: String, currentUserId: String = "") {
        if (roomId.isBlank()) return

        if (activeRoomId != roomId) {
            _messagesList.value = emptyList()
        }

        activeRoomId = roomId
        roomListenerJob?.cancel()
        reactionListenerJob?.cancel()
        presenceListenerJob?.cancel()
        _hasMoreMessages.value = true
        _messageSearchQuery.value = ""

        roomListenerJob = viewModelScope.launch {
            _isLoadingMessages.value = true
            try {
                val fetchedMessages = messageRepository.getMessagesForRoom(roomId)
                if (fetchedMessages.isNotEmpty()) {
                    _messagesList.value = fetchedMessages
                }
                _reactionsList.value = messageRepository.getReactionsForRoom(roomId)
                _hasMoreMessages.value = fetchedMessages.size >= MessageRepository.DEFAULT_PAGE_SIZE

                if (currentUserId.isNotBlank()) {
                    messageRepository.markMessagesAsRead(roomId, currentUserId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading messages", e)
            } finally {
                _isLoadingMessages.value = false
            }

            messageRepository.observeNewMessages(roomId)
                .catch { e -> Log.e(TAG, "Error in realtime message stream", e) }
                .collect { incoming ->
                    if (activeRoomId != roomId) return@collect
                    mergeIncomingMessage(incoming)

                    // Only refresh the chat-list preview if this message is actually the
                    // latest one in the room - otherwise an edit/delete on an older message
                    // would incorrectly overwrite the preview shown in the chat list.
                    if (isLatestMessageInRoom(incoming.id)) {
                        val snippet = when (incoming.messageType) {
                            MessageType.IMAGE -> "[Photo]"
                            MessageType.FILE -> "[Document]"
                            else -> incoming.text
                        }
                        updateLocalRoomPreview(roomId, snippet, incoming.timestamp)
                    }

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
            } finally {
                _isLoadingOlderMessages.value = false
            }
        }
    }

    fun updateMessageSearchQuery(query: String) {
        _messageSearchQuery.value = query
    }

    fun sendMessage(
        roomId: String,
        senderId: String,
        content: String,
        type: MessageType = MessageType.TEXT,
        actionType: ActionType = ActionType.NONE,
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
            val tempId = "temp_$now"

            val tempMessage = ChatMessage(
                id = tempId,
                chatRoomId = roomId,
                senderId = senderId,
                text = trimmed,
                timestamp = now,
                isRead = false,
                messageType = type,
                actionType = actionType,
                replyToId = replyToId,
                isFailed = false
            )

            val snippet = when (type) {
                MessageType.IMAGE -> "[Photo]"
                MessageType.FILE -> "[Document]"
                else -> trimmed
            }

            _messagesList.update { it + tempMessage }
            updateLocalRoomPreview(roomId, snippet, now)

            try {
                val confirmed = messageRepository.sendMessage(
                    roomId = roomId,
                    senderId = senderId,
                    content = trimmed,
                    type = type,
                    actionType = actionType,
                    replyToId = replyToId
                )
                if (confirmed != null) {
                    replaceTempMessage(tempId, confirmed)
                    onResult(true)
                } else {
                    _messagesList.update { current ->
                        current.map { if (it.id == tempId) it.copy(isFailed = true) else it }
                    }
                    _eventFlow.emit(ChatUiEvent.ShowToast("Failed to post message to database"))
                    onResult(false)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception while sending message", e)
                _messagesList.update { current ->
                    current.map { if (it.id == tempId) it.copy(isFailed = true) else it }
                }
                _eventFlow.emit(ChatUiEvent.ShowToast("Error: ${e.localizedMessage}"))
                onResult(false)
            } finally {
                _isSendingMessage.value = false
            }
        }
    }

    fun retryMessage(roomId: String, senderId: String, failedMessage: ChatMessage) {
        if (failedMessage.id.isBlank()) return
        viewModelScope.launch {
            _messagesList.update { current ->
                current.map { if (it.id == failedMessage.id) it.copy(isFailed = false) else it }
            }
            try {
                val confirmed = messageRepository.sendMessage(
                    roomId = roomId,
                    senderId = senderId,
                    content = failedMessage.text,
                    type = failedMessage.messageType,
                    actionType = failedMessage.actionType,
                    replyToId = failedMessage.replyToId
                )
                if (confirmed != null) {
                    replaceTempMessage(failedMessage.id, confirmed)
                    val snippet = when (failedMessage.messageType) {
                        MessageType.IMAGE -> "[Photo]"
                        MessageType.FILE -> "[Document]"
                        else -> failedMessage.text
                    }
                    updateLocalRoomPreview(roomId, snippet, confirmed.timestamp)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception while retrying message", e)
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

                val confirmed = messageRepository.sendMessage(
                    roomId = roomId,
                    senderId = userId,
                    content = initialQuestion,
                    type = MessageType.TEXT,
                    actionType = ActionType.NONE
                )
                if (confirmed != null) {
                    initialQuestionSentForRooms.add(key)
                    updateLocalRoomPreview(roomId, initialQuestion, confirmed.timestamp)
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
            val now = System.currentTimeMillis()
            val tempId = "temp_att_$now"

            val tempAttachmentMessage = ChatMessage(
                id = tempId,
                chatRoomId = roomId,
                senderId = senderId,
                text = "Uploading attachment...",
                timestamp = now,
                isRead = false,
                messageType = type,
                actionType = ActionType.NONE,
                replyToId = replyToId,
                isFailed = false
            )

            _messagesList.update { it + tempAttachmentMessage }

            try {
                val publicUrl = messageRepository.uploadChatAttachment(
                    roomId = roomId,
                    fileName = fileName,
                    bytes = bytes,
                    mimeType = mimeType
                )

                if (publicUrl.isNotBlank()) {
                    val confirmed = messageRepository.sendMessage(
                        roomId = roomId,
                        senderId = senderId,
                        content = publicUrl,
                        type = type,
                        actionType = ActionType.NONE,
                        replyToId = replyToId
                    )

                    val snippet = when (type) {
                        MessageType.IMAGE -> "[Photo]"
                        MessageType.FILE -> "[Document]"
                        else -> "Attachment"
                    }

                    if (confirmed != null) {
                        replaceTempMessage(tempId, confirmed)
                        updateLocalRoomPreview(roomId, snippet, confirmed.timestamp)
                    } else {
                        _messagesList.update { list ->
                            list.map {
                                if (it.id == tempId) {
                                    it.copy(id = "sent_att_$now", text = publicUrl, messageType = type)
                                } else it
                            }
                        }
                    }
                    onComplete(true)
                } else {
                    throw IllegalStateException("Failed to obtain public URL for attachment")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error uploading and sending attachment", e)
                _messagesList.update { list ->
                    list.map {
                        if (it.id == tempId) it.copy(isFailed = true, text = "Failed to send attachment") else it
                    }
                }
                onComplete(false)
            } finally {
                _isUploadingAttachment.value = false
            }
        }
    }

    fun editMessage(roomId: String, messageId: String, newText: String, currentUserId: String = "") {
        val trimmed = newText.trim()
        if (roomId.isBlank() || messageId.isBlank() || trimmed.isBlank()) return

        viewModelScope.launch {
            val wasLatest = isLatestMessageInRoom(messageId)

            _messagesList.update { current ->
                current.map { msg ->
                    if (msg.id == messageId) {
                        msg.copy(text = trimmed, isEdited = true)
                    } else msg
                }
            }

            if (wasLatest) {
                updateLocalRoomPreview(roomId, trimmed, System.currentTimeMillis())
            }

            try {
                messageRepository.editMessage(roomId, messageId, trimmed)
            } catch (e: Exception) {
                Log.e(TAG, "Exception while editing message", e)
            }
        }
    }

    fun deleteMessage(roomId: String, messageId: String, currentUserId: String = "") {
        if (roomId.isBlank() || messageId.isBlank()) return

        viewModelScope.launch {
            val wasLatest = isLatestMessageInRoom(messageId)
            val deletedText = "This message was deleted"
            _messagesList.update { current ->
                current.map { msg ->
                    if (msg.id == messageId) {
                        msg.copy(text = deletedText, isDeleted = true, isEdited = false)
                    } else msg
                }
            }

            if (wasLatest) {
                updateLocalRoomPreview(roomId, deletedText, System.currentTimeMillis())
            }

            try {
                messageRepository.deleteMessage(roomId, messageId)
            } catch (e: Exception) {
                Log.e(TAG, "Exception while deleting message", e)
            }
        }
    }

    /** True if [messageId] is the chronologically latest message currently loaded for its room. */
    private fun isLatestMessageInRoom(messageId: String): Boolean {
        val messages = _messagesList.value
        val target = messages.firstOrNull { it.id == messageId } ?: return false
        val latest = messages.maxByOrNull { it.timestamp } ?: return false
        return latest.id == target.id
    }

    fun toggleReaction(roomId: String, messageId: String, userId: String, emoji: String) {
        if (roomId.isBlank() || messageId.isBlank() || userId.isBlank() || emoji.isBlank()) return

        viewModelScope.launch {
            val previousReactions = _reactionsList.value
            val existing = previousReactions.firstOrNull {
                it.messageId == messageId && it.userId == userId && it.emoji == emoji
            }

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
                messageRepository.toggleReaction(roomId, messageId, userId, emoji)
            } catch (e: Exception) {
                Log.e(TAG, "Error toggling reaction", e)
            }
        }
    }

    private fun updateLocalRoomPreview(roomId: String, snippet: String, timestamp: Long) {
        _chatRooms.update { rooms ->
            rooms.map { room ->
                if (room.id == roomId) {
                    room.copy(
                        lastMessage = snippet,
                        lastMessageTime = timestamp
                    )
                } else room
            }.sortedByDescending { it.lastMessageTime }
        }
    }

    private fun replaceTempMessage(tempId: String, confirmed: ChatMessage) {
        _messagesList.update { current ->
            val hasConfirmed = current.any { it.id == confirmed.id }
            val listWithoutTemp = current.filterNot { it.id == tempId }
            if (hasConfirmed) {
                listWithoutTemp
            } else {
                val index = current.indexOfFirst { it.id == tempId }
                if (index != -1) {
                    current.toMutableList().apply { set(index, confirmed) }
                } else {
                    current + confirmed
                }
            }
        }
    }

    private fun mergeIncomingMessage(incoming: ChatMessage) {
        _messagesList.update { current ->
            val index = current.indexOfFirst { it.id == incoming.id }
            if (index != -1) {
                return@update current.toMutableList().apply { set(index, incoming) }
            }

            val filtered = current.filterNot {
                it.id.startsWith("temp_") &&
                        it.senderId == incoming.senderId &&
                        (it.text == incoming.text ||
                                it.text == "Uploading attachment..." ||
                                it.messageType == incoming.messageType)
            }

            val insertIndex = filtered.indexOfFirst { it.timestamp > incoming.timestamp }
            if (insertIndex == -1) {
                filtered + incoming
            } else {
                filtered.toMutableList().apply { add(insertIndex, incoming) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        roomListenerJob?.cancel()
        reactionListenerJob?.cancel()
        presenceListenerJob?.cancel()
        roomsListenerJob?.cancel()
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
