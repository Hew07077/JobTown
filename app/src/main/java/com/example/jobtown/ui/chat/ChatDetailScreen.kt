// File-level annotation to suppress experimental API warnings
@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
package com.example.jobtown.ui.chat

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.jobtown.data.ChatMessage
import com.example.jobtown.data.MessageType
import com.example.jobtown.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun ChatDetailScreen(
    navController: NavController,
    roomId: String,
    companyName: String,
    chatTitle: String,
    initialQuestion: String = "",
    currentUserId: String = "1",
    chatViewModel: ChatViewModel,
    onNavigateToSchedule: () -> Unit = {},
    onNavigateToUserProfile: (String) -> Unit = {}
) {
    var messageText by remember { mutableStateOf("") }
    var editingMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var replyingToMessage by remember { mutableStateOf<ChatMessage?>(null) }

    var showAttachmentSheet by remember { mutableStateOf(false) }
    var showInterviewDialog by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }
    var inChatSearchQuery by remember { mutableStateOf("") }
    var hasScrolledToBottomInitially by remember { mutableStateOf(false) }
    var showReactionDetail by remember { mutableStateOf(false) }
    var selectedReactionMessageId by remember { mutableStateOf<String?>(null) }
    var showReportDialog by remember { mutableStateOf(false) }
    var reportMessageId by remember { mutableStateOf<String?>(null) }
    var reportReason by remember { mutableStateOf("") }
    var showFilterMenu by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var isOffline by remember { mutableStateOf(false) }

    // State for photo preview & voice recording
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isRecordingVoice by remember { mutableStateOf(false) }
    var mediaRecorder: MediaRecorder? by remember { mutableStateOf(null) }
    var audioFile: File? by remember { mutableStateOf(null) }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Collect ViewModel state
    val messages by chatViewModel.messagesList.collectAsStateWithLifecycle()
    val filteredMessages by chatViewModel.filteredMessages.collectAsStateWithLifecycle()
    val reactions by chatViewModel.reactionsList.collectAsStateWithLifecycle()
    val roomPresence by chatViewModel.roomPresence.collectAsStateWithLifecycle()
    val draft by chatViewModel.draft.collectAsStateWithLifecycle()

    val isMessagesLoading by chatViewModel.isLoadingMessages.collectAsStateWithLifecycle()
    val isSendingMessage by chatViewModel.isSendingMessage.collectAsStateWithLifecycle()
    val isUploadingAttachment by chatViewModel.isUploadingAttachment.collectAsStateWithLifecycle()
    val isLoadingOlderMessages by chatViewModel.isLoadingOlderMessages.collectAsStateWithLifecycle()
    val hasMoreMessages by chatViewModel.hasMoreMessages.collectAsStateWithLifecycle()
    val isConnected by chatViewModel.isConnected.collectAsStateWithLifecycle()
    val isGroupChat by chatViewModel.isGroupChat.collectAsStateWithLifecycle()
    val typingUsers by chatViewModel.typingUsers.collectAsStateWithLifecycle()
    val unreadCount by chatViewModel.unreadCount.collectAsStateWithLifecycle()
    val messageFilter by chatViewModel.messageFilter.collectAsStateWithLifecycle()
    val sortOrder by chatViewModel.sortOrder.collectAsStateWithLifecycle()
    val searchResults by chatViewModel.searchResults.collectAsStateWithLifecycle()
    val mentionSuggestions by chatViewModel.mentionSuggestions.collectAsStateWithLifecycle()

    val displayCompanyName = companyName.ifBlank { "Company Name" }
    val displayPosition = chatTitle.ifBlank { "Position" }

    // Use filtered messages when search is active
    val displayedMessages = remember(messages, filteredMessages, inChatSearchQuery, messageFilter) {
        when {
            inChatSearchQuery.isNotBlank() -> searchResults.map { it.message }
            messageFilter != MessageFilter.ALL -> filteredMessages
            else -> messages
        }
    }

    val isNearBottom by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            val totalItems = listState.layoutInfo.totalItemsCount
            totalItems == 0 || lastVisible >= totalItems - 2
        }
    }

    val groupedMessages = remember(displayedMessages) { groupMessagesByDate(displayedMessages) }
    val (messageIndexMap, totalLazyItems) = remember(groupedMessages) {
        val map = mutableMapOf<String, Int>()
        var currentIndex = 0
        groupedMessages.forEach { (_, list) ->
            currentIndex++
            list.forEach { msg ->
                map[msg.id] = currentIndex
                currentIndex++
            }
        }
        Pair(map, currentIndex)
    }

    // Update offline status
    LaunchedEffect(isConnected) {
        isOffline = !isConnected
    }

    // Voice Recorder Starter Implementation
    fun startRecordingAudio() {
        try {
            val outputDir = context.cacheDir
            val file = File.createTempFile("voice_note_", ".3gp", outputDir)
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            mediaRecorder = recorder
            audioFile = file
            isRecordingVoice = true
            Toast.makeText(context, "Recording voice note...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            android.util.Log.e("ChatDetailScreen", "Failed to start recording", e)
            Toast.makeText(context, "Unable to start voice recording", Toast.LENGTH_SHORT).show()
            isRecordingVoice = false
        }
    }

    // Permission launcher for audio recording
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startRecordingAudio()
        } else {
            Toast.makeText(context, "Microphone permission is required to send voice notes.", Toast.LENGTH_LONG).show()
        }
    }

    // Typing status
    LaunchedEffect(messageText) {
        if (roomId.isNotBlank() && currentUserId.isNotBlank()) {
            chatViewModel.sendTypingStatus(roomId, currentUserId, messageText.isNotBlank())
        }
    }

    // Load draft on startup
    LaunchedEffect(roomId) {
        chatViewModel.loadDraft(roomId)
        if (draft.text.isNotBlank()) {
            messageText = draft.text
        }
    }

    // Universal bytes & attachment sender helper
    fun sendBytesAsAttachment(bytes: ByteArray, fileName: String, mimeType: String, type: MessageType) {
        coroutineScope.launch {
            chatViewModel.sendAttachment(
                roomId = roomId,
                senderId = currentUserId,
                bytes = bytes,
                fileName = fileName,
                mimeType = mimeType,
                type = type,
                replyToId = replyingToMessage?.id
            ) { success ->
                if (success) {
                    replyingToMessage = null
                    chatViewModel.loadUserChatRooms(currentUserId)
                }
            }
        }
    }

    // File pickers
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { selectedImageUri = it }
    }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { resolvedUri ->
            coroutineScope.launch {
                try {
                    val resolver = context.contentResolver
                    val bytes = resolver.openInputStream(resolvedUri)?.use { it.readBytes() } ?: return@launch
                    val mimeType = resolver.getType(resolvedUri) ?: "application/octet-stream"
                    val fileName = resolvedUri.lastPathSegment?.substringAfterLast("/") ?: "document"
                    sendBytesAsAttachment(bytes, fileName, mimeType, MessageType.FILE)
                } catch (e: Exception) {
                    android.util.Log.e("ChatDetailScreen", "Error reading document", e)
                }
            }
        }
    }

    // Load messages
    LaunchedEffect(roomId) {
        if (roomId.isNotBlank()) {
            chatViewModel.loadMessages(roomId, currentUserId)
            chatViewModel.sendInitialQuestionOnce(roomId, currentUserId, initialQuestion)
        }
    }

    // Auto-scroll
    LaunchedEffect(messages) {
        if (messages.isNotEmpty() && totalLazyItems > 0 && !isSearchActive) {
            if (!hasScrolledToBottomInitially) {
                listState.scrollToItem(totalLazyItems - 1)
                hasScrolledToBottomInitially = true
            } else if (isNearBottom) {
                listState.animateScrollToItem(totalLazyItems - 1)
            }
        }
    }

    Scaffold(
        topBar = {
            ChatDetailTopBar(
                companyName = displayCompanyName,
                position = displayPosition,
                isSearchActive = isSearchActive,
                inChatSearchQuery = inChatSearchQuery,
                onSearchQueryChange = { inChatSearchQuery = it },
                isTyping = roomPresence.typingUserIds.isNotEmpty(),
                isConnected = isConnected,
                isGroupChat = isGroupChat,
                presenceCount = roomPresence.onlineUserIds.size,
                unreadCount = unreadCount,
                onBackPressed = { navController.popBackStack() },
                onSearchToggle = {
                    isSearchActive = !isSearchActive
                    if (!isSearchActive) inChatSearchQuery = ""
                },
                onFilterClick = { showFilterMenu = true },
                onSortClick = { showSortMenu = true },
                onInterviewClick = { showInterviewDialog = true },
                onProfileClick = { onNavigateToUserProfile(currentUserId) }
            )
        },
        bottomBar = {
            Surface(
                shadowElevation = 8.dp,
                color = Color.White,
                modifier = Modifier.imePadding()
            ) {
                Column {
                    // Offline banner
                    if (isOffline) {
                        OfflineBanner()
                    }

                    // Edit banner
                    editingMessage?.let { editMsg ->
                        EditBanner(
                            message = editMsg,
                            onCancel = {
                                editingMessage = null
                                messageText = ""
                                chatViewModel.clearDraft()
                            }
                        )
                    }

                    // Reply banner
                    replyingToMessage?.let { replyMsg ->
                        ReplyComposerBanner(
                            replyTarget = replyMsg,
                            onCancel = {
                                replyingToMessage = null
                                chatViewModel.setReplyTarget(null)
                            }
                        )
                    }

                    // Voice recording indicator
                    if (isRecordingVoice) {
                        VoiceRecordingIndicator(
                            onCancel = {
                                try {
                                    mediaRecorder?.stop()
                                    mediaRecorder?.release()
                                    mediaRecorder = null
                                } catch (_: Exception) {}
                                isRecordingVoice = false
                                audioFile?.delete()
                                audioFile = null
                            }
                        )
                    }

                    // Message composer
                    MessageComposer(
                        inputText = messageText,
                        onTextChange = {
                            messageText = it
                            chatViewModel.updateDraft(it)
                        },
                        onSend = {
                            if (editingMessage != null) {
                                chatViewModel.editMessage(
                                    roomId = roomId,
                                    messageId = editingMessage!!.id,
                                    newText = messageText,
                                    currentUserId = currentUserId
                                ) { success ->
                                    if (success) {
                                        editingMessage = null
                                        messageText = ""
                                        chatViewModel.clearDraft()
                                    }
                                }
                            } else {
                                chatViewModel.sendMessage(
                                    roomId = roomId,
                                    senderId = currentUserId,
                                    content = messageText,
                                    replyToId = replyingToMessage?.id
                                ) { success ->
                                    if (success) {
                                        replyingToMessage = null
                                        messageText = ""
                                        chatViewModel.clearDraft()
                                        coroutineScope.launch {
                                            if (totalLazyItems > 0) {
                                                listState.animateScrollToItem(totalLazyItems - 1)
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        onTyping = { isTyping ->
                            chatViewModel.sendTypingStatus(roomId, currentUserId, isTyping)
                        },
                        onAttachmentClick = { showAttachmentSheet = true },
                        isSending = isSendingMessage,
                        isUploading = isUploadingAttachment,
                        isRecordingVoice = isRecordingVoice,
                        onVoiceRecordToggle = {
                            if (isRecordingVoice) {
                                // Stop recording
                                try {
                                    mediaRecorder?.stop()
                                    mediaRecorder?.release()
                                    mediaRecorder = null
                                } catch (_: Exception) {}
                                isRecordingVoice = false

                                audioFile?.let { file ->
                                    if (file.exists() && file.length() > 0) {
                                        val bytes = file.readBytes()
                                        sendBytesAsAttachment(
                                            bytes = bytes,
                                            fileName = "voice_note_${System.currentTimeMillis()}.3gp",
                                            mimeType = "audio/3gpp",
                                            type = MessageType.FILE
                                        )
                                        file.delete()
                                    }
                                }
                                audioFile = null
                            } else {
                                val permission = Manifest.permission.RECORD_AUDIO
                                if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                                    startRecordingAudio()
                                } else {
                                    permissionLauncher.launch(permission)
                                }
                            }
                        },
                        onDraftMention = { query ->
                            chatViewModel.updateMentionSuggestions(query)
                        },
                        mentionSuggestions = mentionSuggestions,
                        onMentionSelected = { userId ->
                            chatViewModel.insertMention(userId)
                            messageText = chatViewModel.draft.value.text
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(4.dp, shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                            .background(Color.White)
                    )
                }
            }
        },
        containerColor = BackgroundWhite
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                isMessagesLoading && messages.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(40.dp),
                                color = DeepGreenDark
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Loading messages...",
                                color = TextDark.copy(alpha = 0.6f),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
                displayedMessages.isEmpty() -> {
                    EmptyChatState(
                        isGroupChat = isGroupChat,
                        onStartChat = {
                            chatViewModel.sendInitialQuestionOnce(
                                roomId = roomId,
                                userId = currentUserId,
                                initialQuestion = "Hello! I'm interested in this opportunity."
                            )
                        }
                    )
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Load older messages trigger
                        if (hasMoreMessages && inChatSearchQuery.isBlank() && !isLoadingOlderMessages) {
                            item(key = "load_more") {
                                LoadMoreTrigger(
                                    isLoading = isLoadingOlderMessages,
                                    onClick = { chatViewModel.loadOlderMessages(roomId) }
                                )
                            }
                        }

                        // Loading older messages indicator
                        if (isLoadingOlderMessages && inChatSearchQuery.isBlank()) {
                            item(key = "loading_older") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = DeepGreenDark
                                    )
                                }
                            }
                        }

                        // Messages grouped by date
                        groupedMessages.forEach { (dateHeader: String, messageList: List<ChatMessage>) ->
                            item(key = "header_$dateHeader") {
                                DateHeader(dateString = dateHeader)
                            }
                            items(
                                items = messageList,
                                key = { msg: ChatMessage -> msg.id.ifBlank { "${msg.timestamp}_${msg.text.hashCode()}" } }
                            ) { msg: ChatMessage ->
                                val replySource = msg.replyToId?.let { replyId ->
                                    messages.firstOrNull { it.id == replyId }
                                }
                                val messageReactions = reactions.filter { it.messageId == msg.id }

                                MessageBubble(
                                    message = msg,
                                    isMe = msg.senderId == currentUserId,
                                    replySourceMessage = replySource,
                                    reactions = messageReactions,
                                    onReply = { selectedMsg ->
                                        replyingToMessage = selectedMsg
                                        chatViewModel.setReplyTarget(selectedMsg.id)
                                    },
                                    onEdit = { selectedMsg ->
                                        editingMessage = selectedMsg
                                        messageText = selectedMsg.text
                                        chatViewModel.updateDraft(selectedMsg.text)
                                    },
                                    onDelete = { selectedMsg ->
                                        chatViewModel.deleteMessage(roomId, selectedMsg.id, currentUserId)
                                    },
                                    onReactionSelected = { emoji ->
                                        chatViewModel.toggleReaction(roomId, msg.id, currentUserId, emoji)
                                    },
                                    onReactionLongPress = { messageId ->
                                        selectedReactionMessageId = messageId
                                        showReactionDetail = true
                                    },
                                    onReport = { selectedMsg ->
                                        reportMessageId = selectedMsg.id
                                        showReportDialog = true
                                    },
                                    onForward = { selectedMsg ->
                                        chatViewModel.forwardMessage(
                                            messageId = selectedMsg.id,
                                            targetRoomId = roomId,
                                            senderId = currentUserId
                                        )
                                    },
                                    onReplyPreviewClick = { targetId ->
                                        messageIndexMap[targetId]?.let { targetIndex ->
                                            coroutineScope.launch {
                                                listState.animateScrollToItem(targetIndex)
                                            }
                                        }
                                    },
                                    onCopyText = {
                                        // Handled internally
                                    },
                                    onOpenAttachment = { url, mimeType ->
                                        try {
                                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            android.util.Log.e("ChatDetailScreen", "Error opening attachment", e)
                                            Toast.makeText(context, "Cannot open attachment", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            }
                        }

                        // Typing indicator
                        if (typingUsers.isNotEmpty() && !isMessagesLoading) {
                            item(key = "typing_indicator") {
                                TypingIndicatorBubble(
                                    label = "${typingUsers.joinToString(", ")} ${if (typingUsers.size > 1) "are" else "is"} typing..."
                                )
                            }
                        }
                    }

                    // Scroll to bottom button
                    AnimatedVisibility(
                        visible = !isNearBottom && inChatSearchQuery.isBlank() && displayedMessages.isNotEmpty(),
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut() + scaleOut(),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                    ) {
                        FloatingActionButton(
                            onClick = {
                                coroutineScope.launch {
                                    if (totalLazyItems > 0) {
                                        listState.animateScrollToItem(totalLazyItems - 1)
                                    }
                                }
                            },
                            containerColor = SageGreenMain,
                            contentColor = DeepGreenDark,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Scroll to latest message"
                            )
                        }
                    }
                }
            }
        }
    }

    // ==================== Dialogs ====================

    // Attachment sheet
    if (showAttachmentSheet) {
        AttachmentBottomSheet(
            onDismiss = { showAttachmentSheet = false },
            onOptionSelected = { option ->
                when (option) {
                    "PHOTO" -> imagePickerLauncher.launch("image/*")
                    "DOCUMENT" -> documentPickerLauncher.launch("application/*")
                }
            }
        )
    }

    // Photo preview
    selectedImageUri?.let { uri ->
        PhotoPreviewDialog(
            context = context,
            imageUri = uri,
            onDismiss = { selectedImageUri = null },
            onSend = { file, caption ->
                selectedImageUri = null
                coroutineScope.launch {
                    try {
                        val bytes = file.readBytes()
                        val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                        val fileName = if (caption.isNotBlank()) caption else "photo_${System.currentTimeMillis()}.jpg"

                        sendBytesAsAttachment(bytes, fileName, mimeType, MessageType.IMAGE)
                    } catch (e: Exception) {
                        android.util.Log.e("ChatDetailScreen", "Error sending image attachment", e)
                        Toast.makeText(context, "Failed to send image", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    // Interview dialog
    if (showInterviewDialog) {
        InterviewDetailDialog(
            companyName = displayCompanyName,
            chatTitle = displayPosition,
            onDismiss = { showInterviewDialog = false },
            onNavigateToSchedule = onNavigateToSchedule
        )
    }

    // Filter menu
    if (showFilterMenu) {
        FilterMenuDialog(
            currentFilter = messageFilter,
            onFilterSelected = {
                chatViewModel.setMessageFilter(it)
                showFilterMenu = false
            },
            onDismiss = { showFilterMenu = false }
        )
    }

    // Sort menu
    if (showSortMenu) {
        SortMenuDialog(
            currentSortOrder = sortOrder,
            onSortSelected = {
                chatViewModel.setSortOrder(it)
                showSortMenu = false
            },
            onDismiss = { showSortMenu = false }
        )
    }

    // Reaction detail dialog
    if (showReactionDetail && selectedReactionMessageId != null) {
        ReactionDetailDialog(
            messageId = selectedReactionMessageId!!,
            reactions = reactions.filter { it.messageId == selectedReactionMessageId },
            onDismiss = {
                showReactionDetail = false
                selectedReactionMessageId = null
            }
        )
    }

    // Report dialog
    if (showReportDialog && reportMessageId != null) {
        ReportMessageDialog(
            reason = reportReason,
            onReasonChange = { reportReason = it },
            onSubmit = {
                chatViewModel.reportMessage(
                    roomId = roomId,
                    messageId = reportMessageId!!,
                    reason = reportReason
                )
                showReportDialog = false
                reportMessageId = null
                reportReason = ""
                Toast.makeText(context, "Message reported", Toast.LENGTH_SHORT).show()
            },
            onDismiss = {
                showReportDialog = false
                reportMessageId = null
                reportReason = ""
            }
        )
    }
}

@Composable
fun ChatDetailTopBar(
    companyName: String,
    position: String,
    isSearchActive: Boolean,
    inChatSearchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    isTyping: Boolean,
    isConnected: Boolean,
    isGroupChat: Boolean,
    presenceCount: Int,
    unreadCount: Int,
    onBackPressed: () -> Unit,
    onSearchToggle: () -> Unit,
    onFilterClick: () -> Unit,
    onSortClick: () -> Unit,
    onInterviewClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    TopAppBar(
        title = {
            if (isSearchActive) {
                TextField(
                    value = inChatSearchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Search in chat...", fontSize = 14.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
            } else {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = companyName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        if (isGroupChat) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SageGreenMain.copy(alpha = 0.3f)
                            ) {
                                Text(
                                    text = "$presenceCount online",
                                    fontSize = 9.sp,
                                    color = DeepGreenDark,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isConnected) Color(0xFF34C759) else Color.Red)
                        )
                        Text(
                            text = if (isTyping) "typing..." else position,
                            fontSize = 11.sp,
                            color = if (isTyping) DeepGreenDark else TextDark.copy(alpha = 0.6f),
                            fontWeight = if (isTyping) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBackPressed) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextDark
                )
            }
        },
        actions = {
            // Unread badge
            if (unreadCount > 0 && !isSearchActive) {
                Badge(
                    containerColor = Color.Red,
                    modifier = Modifier.offset(x = (-4).dp, y = 4.dp)
                ) {
                    Text(text = if (unreadCount > 99) "99+" else unreadCount.toString())
                }
            }

            IconButton(onClick = onSearchToggle) {
                Icon(
                    imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                    contentDescription = "Toggle Search",
                    tint = TextDark
                )
            }

            IconButton(onClick = onFilterClick) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "Filter",
                    tint = TextDark
                )
            }

            IconButton(onClick = onSortClick) {
                Icon(
                    imageVector = Icons.Default.Sort,
                    contentDescription = "Sort",
                    tint = TextDark
                )
            }

            IconButton(onClick = onInterviewClick) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = "View Interview Details",
                    tint = DeepGreenDark
                )
            }

            IconButton(onClick = onProfileClick) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile",
                    tint = TextDark
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = SageGreenMain)
    )
}