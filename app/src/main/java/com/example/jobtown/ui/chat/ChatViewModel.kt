package com.example.jobtown.ui.chat

import android.net.Uri
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
import java.io.File

// ==================== Sealed Classes & Data Classes ====================

sealed interface ChatUiEvent {
    data class ShowToast(val message: String, val isError: Boolean = false) : ChatUiEvent
    data class NavigateToChat(val roomId: String) : ChatUiEvent
    data class NavigateToUserProfile(val userId: String) : ChatUiEvent
    data class OpenAttachment(val url: String, val mimeType: String) : ChatUiEvent
    object ScrollToBottom : ChatUiEvent
    object ClearSearch : ChatUiEvent
}

data class MessageSearchResult(
    val message: ChatMessage,
    val matchCount: Int,
    val preview: String
)

data class ChatDraft(
    val text: String,
    val replyToId: String? = null,
    val attachments: List<Uri> = emptyList()
)

enum class MessageFilter {
    ALL,
    TEXT_ONLY,
    ATTACHMENTS_ONLY,
    WITH_REACTIONS,
    UNREAD_ONLY
}

enum class MessageSortOrder {
    NEWEST_FIRST,
    OLDEST_FIRST
}

// ==================== Main ViewModel ====================

class ChatViewModel(
    private val messageRepository: MessageRepository
) : ViewModel() {

    // ==================== Core State Flows ====================

    private val _chatRooms = MutableStateFlow<List<ChatRoom>>(emptyList())
    val chatRooms: StateFlow<List<ChatRoom>> = _chatRooms.asStateFlow()

    private val _messagesList = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messagesList: StateFlow<List<ChatMessage>> = _messagesList.asStateFlow()

    private val _filteredMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val filteredMessages: StateFlow<List<ChatMessage>> = _filteredMessages.asStateFlow()

    private val _reactionsList = MutableStateFlow<List<MessageReaction>>(emptyList())
    val reactionsList: StateFlow<List<MessageReaction>> = _reactionsList.asStateFlow()

    private val _roomPresence = MutableStateFlow(RoomPresence())
    val roomPresence: StateFlow<RoomPresence> = _roomPresence.asStateFlow()

    // ==================== UI State Flags ====================

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

    private val _isGroupChat = MutableStateFlow(false)
    val isGroupChat: StateFlow<Boolean> = _isGroupChat.asStateFlow()

    private val _isConnected = MutableStateFlow(true)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _typingUsers = MutableStateFlow<Set<String>>(emptySet())
    val typingUsers: StateFlow<Set<String>> = _typingUsers.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    // ==================== Draft & Search State ====================

    private val _draft = MutableStateFlow(ChatDraft(text = ""))
    val draft: StateFlow<ChatDraft> = _draft.asStateFlow()

    private val _messageSearchQuery = MutableStateFlow("")
    val messageSearchQuery: StateFlow<String> = _messageSearchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<MessageSearchResult>>(emptyList())
    val searchResults: StateFlow<List<MessageSearchResult>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    // ==================== Filter & Sort State ====================

    private val _messageFilter = MutableStateFlow(MessageFilter.ALL)
    val messageFilter: StateFlow<MessageFilter> = _messageFilter.asStateFlow()

    private val _sortOrder = MutableStateFlow(MessageSortOrder.NEWEST_FIRST)
    val sortOrder: StateFlow<MessageSortOrder> = _sortOrder.asStateFlow()

    // ==================== Pinned & Archived Rooms ====================

    private val _pinnedRoomIds = MutableStateFlow<Set<String>>(emptySet())
    val pinnedRoomIds: StateFlow<Set<String>> = _pinnedRoomIds.asStateFlow()

    private val _archivedRoomIds = MutableStateFlow<Set<String>>(emptySet())
    val archivedRoomIds: StateFlow<Set<String>> = _archivedRoomIds.asStateFlow()

    // ==================== Events ====================

    private val _eventFlow = MutableSharedFlow<ChatUiEvent>(extraBufferCapacity = 64)
    val eventFlow: SharedFlow<ChatUiEvent> = _eventFlow.asSharedFlow()

    // ==================== Private State ====================

    private var activeRoomId: String? = null
    private var roomListenerJob: Job? = null
    private var reactionListenerJob: Job? = null
    private var presenceListenerJob: Job? = null
    private var typingDebounceJob: Job? = null

    private val initialQuestionSentForRooms = mutableSetOf<String>()
    private val messageCache = mutableMapOf<String, List<ChatMessage>>()
    private val draftCache = mutableMapOf<String, String>()

    // ==================== Room Management ====================

    fun loadUserChatRooms(userId: String, includeArchived: Boolean = false) {
        if (userId.isBlank()) return
        viewModelScope.launch {
            _isLoadingRooms.value = true
            try {
                val rooms = messageRepository.getChatRoomsForUser(userId)
                _chatRooms.value = rooms
                updateUnreadCount()
            } catch (e: Exception) {
                Log.e(TAG, "Error loading chat rooms", e)
                _eventFlow.emit(ChatUiEvent.ShowToast("Failed to load chat conversations.", true))
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
        jobTitle: String,
        initialMessage: String? = null
    ): Result<String> {
        return try {
            val roomId = messageRepository.getOrCreateChatRoom(
                seekerId = seekerId,
                seekerName = seekerName,
                employerId = employerId,
                companyName = companyName,
                jobTitle = jobTitle
            )

            if (initialMessage != null && roomId.isNotEmpty()) {
                val messages = messageRepository.getMessagesForRoom(roomId, limit = 1)
                if (messages.isEmpty()) {
                    messageRepository.sendMessage(roomId, seekerId, initialMessage, MessageType.TEXT)
                }
            }

            Result.success(roomId)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating chat room", e)
            Result.failure(e)
        }
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

    fun loadMessages(
        roomId: String,
        currentUserId: String = "",
        loadFromCache: Boolean = true
    ) {
        if (roomId.isBlank()) return

        if (loadFromCache && messageCache.containsKey(roomId)) {
            _messagesList.value = messageCache[roomId] ?: emptyList()
        }

        activeRoomId = roomId
        roomListenerJob?.cancel()
        reactionListenerJob?.cancel()
        presenceListenerJob?.cancel()
        _hasMoreMessages.value = true
        _messageSearchQuery.value = ""

        setupMessageListener(roomId, currentUserId)
        setupReactionListener(roomId)
        setupPresenceListener(roomId, currentUserId)

        viewModelScope.launch {
            _isLoadingMessages.value = true
            try {
                val page = messageRepository.getMessagesForRoom(roomId)
                _messagesList.value = page
                messageCache[roomId] = page
                _reactionsList.value = messageRepository.getReactionsForRoom(roomId)
                _hasMoreMessages.value = page.size >= MessageRepository.DEFAULT_PAGE_SIZE
                _isGroupChat.value = page.any { it.chatRoomId.contains("group_") }

                if (currentUserId.isNotBlank()) {
                    messageRepository.markMessagesAsRead(roomId, currentUserId)
                    updateUnreadCount()
                }

                applyFilterAndSort()
            } catch (e: Exception) {
                Log.e(TAG, "Error loading initial messages", e)
                _eventFlow.emit(ChatUiEvent.ShowToast("Failed to load message history.", true))
            } finally {
                _isLoadingMessages.value = false
            }
        }
    }

    private fun setupMessageListener(roomId: String, currentUserId: String) {
        roomListenerJob = viewModelScope.launch {
            messageRepository.observeNewMessages(roomId)
                .catch { e -> Log.e(TAG, "Error in realtime message stream", e) }
                .collect { incoming ->
                    if (activeRoomId != roomId) return@collect
                    mergeIncomingMessage(incoming)
                    messageCache[roomId] = _messagesList.value

                    if (currentUserId.isNotBlank() && incoming.senderId != currentUserId) {
                        messageRepository.markMessagesAsRead(roomId, currentUserId)
                        updateUnreadCount()
                    }
                }
        }
    }

    private fun setupReactionListener(roomId: String) {
        reactionListenerJob = viewModelScope.launch {
            messageRepository.observeReactions(roomId)
                .catch { e -> Log.e(TAG, "Error in reactions stream", e) }
                .collect { reactions ->
                    if (activeRoomId == roomId) {
                        _reactionsList.value = reactions
                    }
                }
        }
    }

    private fun setupPresenceListener(roomId: String, currentUserId: String) {
        if (currentUserId.isBlank()) return
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
                    messageCache[roomId] = _messagesList.value
                    applyFilterAndSort()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading older messages", e)
                _eventFlow.emit(ChatUiEvent.ShowToast("Failed to load earlier messages.", true))
            } finally {
                _isLoadingOlderMessages.value = false
            }
        }
    }

    fun jumpToMessage(roomId: String, messageId: String) {
        viewModelScope.launch {
            try {
                val message = _messagesList.value.find { it.id == messageId }
                if (message != null) {
                    _eventFlow.emit(ChatUiEvent.ShowToast("Jumped to message"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error jumping to message", e)
            }
        }
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
            messageCache[roomId] = _messagesList.value

            try {
                val success = messageRepository.sendMessage(roomId, senderId, trimmed, type, replyToId)
                if (success) {
                    loadUserChatRooms(senderId)
                    clearDraft()
                } else {
                    _messagesList.update { list -> list.filterNot { it.id == tempMessage.id } }
                    _eventFlow.emit(ChatUiEvent.ShowToast("Failed to send message.", true))
                }
                onResult(success)
            } catch (e: Exception) {
                Log.e(TAG, "Exception while sending message", e)
                _messagesList.update { list -> list.filterNot { it.id == tempMessage.id } }
                _eventFlow.emit(ChatUiEvent.ShowToast("Error sending message. Check your connection.", true))
                onResult(false)
            } finally {
                _isSendingMessage.value = false
            }
        }
    }

    fun sendMessageBatch(
        roomId: String,
        senderId: String,
        messages: List<String>,
        onComplete: (Int) -> Unit = {}
    ) {
        viewModelScope.launch {
            var successCount = 0
            messages.forEach { content ->
                val trimmed = content.trim()
                if (trimmed.isNotEmpty()) {
                    val success = messageRepository.sendMessage(roomId, senderId, trimmed, MessageType.TEXT)
                    if (success) successCount++
                }
            }
            onComplete(successCount)
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

        typingDebounceJob?.cancel()
        typingDebounceJob = viewModelScope.launch {
            kotlinx.coroutines.delay(300)
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

            val tempMessage = ChatMessage(
                id = "temp_attachment_$now",
                chatRoomId = roomId,
                senderId = senderId,
                text = "Uploading attachment...",
                timestamp = now,
                isRead = false,
                messageType = type,
                replyToId = replyToId
            )
            _messagesList.update { it + tempMessage }
            messageCache[roomId] = _messagesList.value

            try {
                val url = messageRepository.uploadChatAttachment(
                    roomId = roomId,
                    fileName = fileName,
                    bytes = bytes,
                    mimeType = mimeType
                )

                if (url == null) {
                    _messagesList.update { list -> list.filterNot { it.id == tempMessage.id } }
                    _eventFlow.emit(ChatUiEvent.ShowToast("Failed to upload attachment.", true))
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
                    _messagesList.update { list -> list.filterNot { it.id == tempMessage.id } }
                    loadUserChatRooms(senderId)
                    _eventFlow.emit(ChatUiEvent.OpenAttachment(url, mimeType))
                } else {
                    _messagesList.update { list -> list.filterNot { it.id == tempMessage.id } }
                    _eventFlow.emit(ChatUiEvent.ShowToast("Failed to send attachment.", true))
                }
                onComplete(success)
            } catch (e: Exception) {
                Log.e(TAG, "Error sending attachment", e)
                _messagesList.update { list -> list.filterNot { it.id == tempMessage.id } }
                _eventFlow.emit(ChatUiEvent.ShowToast("Error sending attachment.", true))
                onComplete(false)
            } finally {
                _isUploadingAttachment.value = false
            }
        }
    }

    fun sendAttachmentFromFile(
        roomId: String,
        senderId: String,
        file: File,
        mimeType: String,
        type: MessageType,
        replyToId: String? = null,
        onComplete: (Boolean) -> Unit = {}
    ) {
        if (!file.exists() || file.length() == 0L) {
            viewModelScope.launch {
                _eventFlow.emit(ChatUiEvent.ShowToast("Attachment file is missing or empty.", true))
            }
            onComplete(false)
            return
        }

        try {
            val bytes = file.readBytes()
            sendAttachment(
                roomId = roomId,
                senderId = senderId,
                bytes = bytes,
                fileName = file.name,
                mimeType = mimeType,
                type = type,
                replyToId = replyToId,
                onComplete = onComplete
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read attachment file bytes", e)
            onComplete(false)
        }
    }

    fun sendVoiceMessage(
        roomId: String,
        senderId: String,
        audioFile: File,
        duration: Int,
        onComplete: (Boolean) -> Unit = {}
    ) {
        sendAttachmentFromFile(
            roomId = roomId,
            senderId = senderId,
            file = audioFile,
            mimeType = "audio/ogg",
            type = MessageType.FILE,
            onComplete = onComplete
        )
    }

    // ==================== Draft Management ====================

    fun updateDraft(text: String) {
        _draft.update { it.copy(text = text) }
        activeRoomId?.let { roomId ->
            draftCache[roomId] = text
        }
    }

    fun setReplyTarget(replyToId: String?) {
        _draft.update { it.copy(replyToId = replyToId) }
    }

    fun addDraftAttachment(uri: Uri) {
        _draft.update {
            it.copy(attachments = it.attachments + uri)
        }
    }

    fun removeDraftAttachment(uri: Uri) {
        _draft.update {
            it.copy(attachments = it.attachments.filterNot { it == uri })
        }
    }

    fun clearDraft() {
        _draft.value = ChatDraft(text = "")
        activeRoomId?.let { roomId ->
            draftCache[roomId] = ""
        }
    }

    // ==================== Load Draft ====================

    fun loadDraft(roomId: String) {
        val cachedDraft = draftCache[roomId]
        if (cachedDraft != null && cachedDraft.isNotBlank()) {
            _draft.update { it.copy(text = cachedDraft) }
        }
    }

    // ==================== Message Management ====================

    fun editMessage(
        roomId: String,
        messageId: String,
        newText: String,
        currentUserId: String = "",
        onResult: (Boolean) -> Unit = {}
    ) {
        val trimmed = newText.trim()
        if (roomId.isBlank() || messageId.isBlank() || trimmed.isBlank()) {
            onResult(false)
            return
        }

        viewModelScope.launch {
            _messagesList.update { current ->
                current.map {
                    if (it.id == messageId) {
                        it.copy(text = trimmed, isEdited = true)
                    } else it
                }
            }
            messageCache[roomId] = _messagesList.value

            try {
                messageRepository.editMessage(roomId, messageId, trimmed)
                if (currentUserId.isNotBlank()) loadUserChatRooms(currentUserId)
                onResult(true)
            } catch (e: Exception) {
                Log.e(TAG, "Error editing message", e)
                loadMessages(roomId, currentUserId)
                _eventFlow.emit(ChatUiEvent.ShowToast("Error updating message.", true))
                onResult(false)
            }
        }
    }

    fun deleteMessage(roomId: String, messageId: String, currentUserId: String = "") {
        if (roomId.isBlank() || messageId.isBlank()) return

        viewModelScope.launch {
            _messagesList.update { current ->
                current.map {
                    if (it.id == messageId) {
                        it.copy(
                            text = "This message was deleted",
                            isDeleted = true,
                            isEdited = false
                        )
                    } else it
                }
            }
            messageCache[roomId] = _messagesList.value

            try {
                messageRepository.deleteMessage(roomId, messageId)
                if (currentUserId.isNotBlank()) loadUserChatRooms(currentUserId)
                _eventFlow.emit(ChatUiEvent.ShowToast("Message deleted"))
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting message", e)
                loadMessages(roomId, currentUserId)
                _eventFlow.emit(ChatUiEvent.ShowToast("Error deleting message.", true))
            }
        }
    }

    fun toggleReaction(roomId: String, messageId: String, userId: String, emoji: String) {
        if (roomId.isBlank() || messageId.isBlank() || userId.isBlank()) return
        viewModelScope.launch {
            messageRepository.toggleReaction(roomId, messageId, userId, emoji)
        }
    }

    fun markMessageAsRead(roomId: String, messageId: String, userId: String) {
        viewModelScope.launch {
            _messagesList.update { current ->
                current.map {
                    if (it.id == messageId) it.copy(isRead = true) else it
                }
            }
            messageRepository.markMessagesAsRead(roomId, userId)
            updateUnreadCount()
        }
    }

    fun markAllMessagesAsRead(roomId: String, userId: String) {
        viewModelScope.launch {
            messageRepository.markMessagesAsRead(roomId, userId)
            _messagesList.update { current ->
                current.map { it.copy(isRead = true) }
            }
            updateUnreadCount()
        }
    }

    private suspend fun updateUnreadCount() {
        try {
            val count = _chatRooms.value.sumOf { room ->
                room.unreadCount ?: 0
            }
            _unreadCount.value = count
        } catch (e: Exception) {
            Log.e(TAG, "Error updating unread count", e)
        }
    }

    // ==================== Search & Filter ====================

    fun updateMessageSearchQuery(query: String) {
        _messageSearchQuery.value = query
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _isSearching.value = false
            applyFilterAndSort()
            return
        }
        performSearch(query)
    }

    private fun performSearch(query: String) {
        viewModelScope.launch {
            _isSearching.value = true
            try {
                val results = _messagesList.value
                    .filter { it.text.contains(query, ignoreCase = true) }
                    .map { message ->
                        MessageSearchResult(
                            message = message,
                            matchCount = message.text.split(query, ignoreCase = true).size - 1,
                            preview = message.text.take(100)
                        )
                    }
                _searchResults.value = results
                _filteredMessages.value = results.map { it.message }
            } catch (e: Exception) {
                Log.e(TAG, "Error searching messages", e)
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun setMessageFilter(filter: MessageFilter) {
        _messageFilter.value = filter
        applyFilterAndSort()
    }

    fun setSortOrder(order: MessageSortOrder) {
        _sortOrder.value = order
        applyFilterAndSort()
    }

    private fun applyFilterAndSort() {
        val filter = _messageFilter.value
        val order = _sortOrder.value

        var messages = _messagesList.value

        messages = when (filter) {
            MessageFilter.ALL -> messages
            MessageFilter.TEXT_ONLY -> messages.filter { it.messageType == MessageType.TEXT }
            MessageFilter.ATTACHMENTS_ONLY -> messages.filter {
                it.messageType == MessageType.IMAGE ||
                        it.messageType == MessageType.FILE
            }
            MessageFilter.WITH_REACTIONS -> messages.filter { message ->
                _reactionsList.value.any { it.messageId == message.id }
            }
            MessageFilter.UNREAD_ONLY -> messages.filter { !it.isRead }
        }

        messages = when (order) {
            MessageSortOrder.NEWEST_FIRST -> messages.sortedByDescending { it.timestamp }
            MessageSortOrder.OLDEST_FIRST -> messages.sortedBy { it.timestamp }
        }

        _filteredMessages.value = messages
    }

    fun clearSearch() {
        _messageSearchQuery.value = ""
        _searchResults.value = emptyList()
        _isSearching.value = false
        _filteredMessages.value = _messagesList.value
        _eventFlow.tryEmit(ChatUiEvent.ClearSearch)
    }

    // ==================== Message Forwarding ====================

    fun forwardMessage(messageId: String, targetRoomId: String, senderId: String) {
        viewModelScope.launch {
            try {
                val message = _messagesList.value.find { it.id == messageId }
                if (message != null) {
                    val forwardedText = "Forwarded: ${message.text}"
                    messageRepository.sendMessage(targetRoomId, senderId, forwardedText, message.messageType)
                    _eventFlow.emit(ChatUiEvent.ShowToast("Message forwarded"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error forwarding message", e)
                _eventFlow.emit(ChatUiEvent.ShowToast("Failed to forward message.", true))
            }
        }
    }

    // ==================== Mention Support ====================

    private val _mentionSuggestions = MutableStateFlow<List<String>>(emptyList())
    val mentionSuggestions: StateFlow<List<String>> = _mentionSuggestions.asStateFlow()

    fun updateMentionSuggestions(query: String) {
        viewModelScope.launch {
            val suggestions = _messagesList.value
                .map { it.senderId }
                .distinct()
                .filter { it.contains(query, ignoreCase = true) }
            _mentionSuggestions.value = suggestions
        }
    }

    fun insertMention(userId: String) {
        _draft.update { draft ->
            val lastMentionIndex = draft.text.lastIndexOf('@')
            if (lastMentionIndex != -1) {
                val before = draft.text.substring(0, lastMentionIndex)
                val afterIndex = draft.text.indexOf(' ', lastMentionIndex)
                val after = if (afterIndex != -1) draft.text.substring(afterIndex) else ""
                draft.copy(text = "$before@$userId $after")
            } else {
                draft
            }
        }
        _mentionSuggestions.value = emptyList()
    }

    // ==================== Report Message ====================

    fun reportMessage(roomId: String, messageId: String, reason: String) {
        viewModelScope.launch {
            try {
                // Call repository if available
                // messageRepository.reportMessage(roomId, messageId, reason)
                _eventFlow.emit(ChatUiEvent.ShowToast("Message reported"))
                Log.d(TAG, "Reported message: $messageId in room: $roomId with reason: $reason")
            } catch (e: Exception) {
                Log.e(TAG, "Error reporting message", e)
                _eventFlow.emit(ChatUiEvent.ShowToast("Failed to report message", true))
            }
        }
    }

    // ==================== Private Helpers ====================

    private fun mergeIncomingMessage(incoming: ChatMessage) {
        _messagesList.update { current ->
            val index = current.indexOfFirst { it.id == incoming.id }
            if (index != -1) {
                val existing = current[index]
                val updated = incoming.copy(
                    isEdited = incoming.isEdited || existing.isEdited,
                    isDeleted = incoming.isDeleted || existing.isDeleted
                )
                current.toMutableList().apply { set(index, updated) }
            } else {
                val filtered = current.filterNot {
                    it.id.startsWith("temp_") && it.senderId == incoming.senderId &&
                            it.text == incoming.text && it.timestamp > incoming.timestamp - 5000
                }
                val insertIndex = filtered.indexOfFirst { it.timestamp > incoming.timestamp }
                if (insertIndex == -1) {
                    filtered + incoming
                } else {
                    filtered.toMutableList().apply { add(insertIndex, incoming) }
                }
            }
        }.also {
            applyFilterAndSort()
        }
    }

    fun retryFailedMessages(roomId: String, userId: String) {
        viewModelScope.launch {
            val failedMessages = _messagesList.value.filter { it.id.startsWith("temp_") }
            failedMessages.forEach { message ->
                val success = messageRepository.sendMessage(
                    roomId = roomId,
                    senderId = userId,
                    content = message.text,
                    type = message.messageType,
                    replyToId = message.replyToId
                )
                if (success) {
                    _messagesList.update { list ->
                        list.filterNot { it.id == message.id }
                    }
                }
            }
        }
    }

    fun getMessageHistory(roomId: String, date: Long): List<ChatMessage> {
        return _messagesList.value.filter {
            it.timestamp in date..date + 24 * 60 * 60 * 1000
        }
    }

    fun getMessagesBySender(roomId: String, senderId: String): List<ChatMessage> {
        return _messagesList.value.filter { it.senderId == senderId }
    }

    fun getRecentMessages(roomId: String, count: Int): List<ChatMessage> {
        return _messagesList.value.takeLast(count)
    }

    fun getMessageById(messageId: String): ChatMessage? {
        return _messagesList.value.find { it.id == messageId }
    }

    fun clearCache() {
        messageCache.clear()
    }

    // ==================== Cleanup ====================

    override fun onCleared() {
        super.onCleared()
        roomListenerJob?.cancel()
        reactionListenerJob?.cancel()
        presenceListenerJob?.cancel()
        typingDebounceJob?.cancel()

        // Save draft if room is active
        activeRoomId?.let { roomId ->
            draftCache[roomId] = _draft.value.text
        }
    }

    companion object {
        private const val TAG = "ChatViewModel"
    }
}

// ==================== ViewModel Factory ====================

class ChatViewModelFactory(
    private val messageRepository: MessageRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            return ChatViewModel(messageRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}