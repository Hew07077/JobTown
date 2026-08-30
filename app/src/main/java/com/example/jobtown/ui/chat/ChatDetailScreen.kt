@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.jobtown.ui.chat

import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.jobtown.data.model.ChatMessage
import com.example.jobtown.data.model.MessageType
import com.example.jobtown.data.model.toReactionGroups
import com.example.jobtown.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun ChatDetailScreen(
    navController: NavController,
    roomId: String,
    companyName: String,
    chatTitle: String,
    initialQuestion: String = "",
    currentUserId: String = "1",
    chatViewModel: ChatViewModel,
    onNavigateToSchedule: () -> Unit = {}
) {

    var messageText by remember { mutableStateOf("") }
    var editingMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var replyingToMessage by remember { mutableStateOf<ChatMessage?>(null) }

    var showAttachmentSheet by remember { mutableStateOf(false) }
    var showInterviewDialog by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }
    var inChatSearchQuery by remember { mutableStateOf("") }
    var hasScrolledToBottomInitially by remember { mutableStateOf(false) }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val messages by chatViewModel.messagesList.collectAsStateWithLifecycle()
    val reactions by chatViewModel.reactionsList.collectAsStateWithLifecycle()
    val roomPresence by chatViewModel.roomPresence.collectAsStateWithLifecycle()

    val isMessagesLoading by chatViewModel.isLoadingMessages.collectAsStateWithLifecycle()
    val isSendingMessage by chatViewModel.isSendingMessage.collectAsStateWithLifecycle()
    val isUploadingAttachment by chatViewModel.isUploadingAttachment.collectAsStateWithLifecycle()
    val isLoadingOlderMessages by chatViewModel.isLoadingOlderMessages.collectAsStateWithLifecycle()
    val hasMoreMessages by chatViewModel.hasMoreMessages.collectAsStateWithLifecycle()

    val displayCompanyName = companyName.ifBlank { "Company Name" }
    val displayPosition = chatTitle.ifBlank { "Position" }

    val displayedMessages = remember(messages, inChatSearchQuery) {
        if (inChatSearchQuery.isBlank()) {
            messages
        } else {
            messages.filter { it.text.contains(inChatSearchQuery, ignoreCase = true) }
        }
    }

    val isNearBottom by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            val totalItems = listState.layoutInfo.totalItemsCount
            totalItems == 0 || lastVisible >= totalItems - 2
        }
    }

    val reactionsByMessage = remember(reactions, currentUserId) {
        reactions.groupBy { it.messageId }
            .mapValues { (_, messageReactions) -> messageReactions.toReactionGroups(currentUserId) }
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

    // Pagination: fetch older history once the user scrolls near the top of what's loaded.
    LaunchedEffect(roomId) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { firstVisible ->
                if (firstVisible <= 3 &&
                    hasMoreMessages &&
                    !isLoadingOlderMessages &&
                    !isMessagesLoading &&
                    inChatSearchQuery.isBlank()
                ) {
                    chatViewModel.loadOlderMessages(roomId)
                }
            }
    }

    // Typing indicator: debounce keystrokes so we don't broadcast on every character
    LaunchedEffect(roomId, currentUserId) {
        if (roomId.isBlank() || currentUserId.isBlank()) return@LaunchedEffect
        snapshotFlow { messageText }
            .collectLatest { text ->
                if (text.isBlank()) {
                    chatViewModel.sendTypingStatus(roomId, currentUserId, false)
                } else {
                    chatViewModel.sendTypingStatus(roomId, currentUserId, true)
                    delay(3000)
                    chatViewModel.sendTypingStatus(roomId, currentUserId, false)
                }
            }
    }

    // Clear typing status on screen dispose
    DisposableEffect(roomId, currentUserId) {
        onDispose {
            if (roomId.isNotBlank() && currentUserId.isNotBlank()) {
                chatViewModel.sendTypingStatus(roomId, currentUserId, false)
            }
        }
    }

    fun readBytesAndSendWithCaption(uri: Uri, type: MessageType, caption: String) {
        coroutineScope.launch {
            try {
                val resolver = context.contentResolver
                val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes == null) return@launch
                val mimeType = resolver.getType(uri)
                    ?: MimeTypeMap.getFileExtensionFromUrl(uri.toString())
                        ?.let { MimeTypeMap.getSingleton().getMimeTypeFromExtension(it) }
                    ?: "application/octet-stream"
                val rawFileName = uri.lastPathSegment?.substringAfterLast("/") ?: "attachment"
                val finalFileName = if (caption.isNotBlank()) caption else rawFileName

                chatViewModel.sendAttachment(
                    roomId = roomId,
                    senderId = currentUserId,
                    bytes = bytes,
                    fileName = finalFileName,
                    mimeType = mimeType,
                    type = type,
                    replyToId = replyingToMessage?.id
                ) { success ->
                    if (success) {
                        replyingToMessage = null
                        chatViewModel.loadUserChatRooms(currentUserId)
                    }
                }
            } catch (e: Exception) {
                Log.e("ChatDetailScreen", "Error reading attachment", e)
            }
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { selectedImageUri = it }
    }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { readBytesAndSendWithCaption(it, MessageType.FILE, "") }
    }

    LaunchedEffect(roomId) {
        if (roomId.isNotBlank()) {
            chatViewModel.loadMessages(roomId, currentUserId)
            chatViewModel.sendInitialQuestionOnce(roomId, currentUserId, initialQuestion)
        }
    }

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
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        TextField(
                            value = inChatSearchQuery,
                            onValueChange = { inChatSearchQuery = it },
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
                            Text(
                                text = displayCompanyName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                            val otherTyping = roomPresence.typingUserIds.isNotEmpty()
                            if (otherTyping) {
                                Text(
                                    text = "typing...",
                                    fontSize = 12.sp,
                                    color = DeepGreenDark,
                                    fontWeight = FontWeight.SemiBold
                                )
                            } else {
                                Text(
                                    text = displayPosition,
                                    fontSize = 12.sp,
                                    color = TextDark.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextDark
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        isSearchActive = !isSearchActive
                        if (!isSearchActive) inChatSearchQuery = ""
                    }) {
                        Icon(
                            imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "Toggle Search",
                            tint = TextDark
                        )
                    }
                    IconButton(onClick = { showInterviewDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "View Interview Details",
                            tint = DeepGreenDark
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SageGreenMain)
            )
        },
        bottomBar = {
            Surface(
                shadowElevation = 8.dp,
                color = Color.White,
                modifier = Modifier.imePadding()
            ) {
                Column {
                    editingMessage?.let { editMsg ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SageGreenMain.copy(alpha = 0.2f))
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Editing Message",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepGreenDark
                                )
                                Text(
                                    text = editMsg.text,
                                    fontSize = 12.sp,
                                    color = TextDark.copy(alpha = 0.7f),
                                    maxLines = 1
                                )
                            }
                            IconButton(
                                onClick = {
                                    editingMessage = null
                                    messageText = ""
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancel Edit",
                                    tint = TextDark
                                )
                            }
                        }
                    }

                    replyingToMessage?.let { replyMsg ->
                        ReplyComposerBanner(
                            replyTarget = replyMsg,
                            onCancel = { replyingToMessage = null }
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { showAttachmentSheet = true },
                            enabled = !isUploadingAttachment,
                            modifier = Modifier.size(40.dp)
                        ) {
                            if (isUploadingAttachment) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = DeepGreenDark
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Attach File",
                                    tint = TextDark.copy(alpha = 0.7f)
                                )
                            }
                        }

                        TextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            placeholder = { Text("Type a message...") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(24.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = BackgroundWhite,
                                unfocusedContainerColor = BackgroundWhite,
                                disabledContainerColor = BackgroundWhite,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            maxLines = 4
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        val canSend = messageText.isNotBlank()

                        IconButton(
                            onClick = {
                                if (canSend) {
                                    val textToSend = messageText
                                    val currentEdit = editingMessage
                                    val currentReplyId = replyingToMessage?.id

                                    messageText = ""

                                    if (currentEdit != null) {
                                        chatViewModel.editMessage(roomId, currentEdit.id, textToSend, currentUserId)
                                        editingMessage = null
                                    } else {
                                        chatViewModel.sendMessage(
                                            roomId = roomId,
                                            senderId = currentUserId,
                                            content = textToSend,
                                            replyToId = currentReplyId
                                        ) { success ->
                                            if (success) {
                                                chatViewModel.loadUserChatRooms(currentUserId)
                                                replyingToMessage = null
                                                coroutineScope.launch {
                                                    if (totalLazyItems > 0) {
                                                        listState.animateScrollToItem(totalLazyItems - 1)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            enabled = canSend,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (canSend) SageGreenMain else SageGreenMain.copy(alpha = 0.4f))
                        ) {
                            if (isSendingMessage && editingMessage == null) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = DeepGreenDark
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send Message",
                                    tint = DeepGreenDark
                                )
                            }
                        }
                    }
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
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = DeepGreenDark
                    )
                }
                displayedMessages.isEmpty() -> {
                    Text(
                        text = if (inChatSearchQuery.isNotBlank()) "No messages found matching \"$inChatSearchQuery\"" else "No messages yet. Send a message to start!",
                        color = TextDark.copy(alpha = 0.5f),
                        fontSize = 14.sp,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
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

                                MessageBubble(
                                    message = msg,
                                    isMe = msg.senderId == currentUserId,
                                    replySourceMessage = replySource,
                                    onReply = { selectedMsg ->
                                        replyingToMessage = selectedMsg
                                    },
                                    onEdit = { selectedMsg ->
                                        editingMessage = selectedMsg
                                        messageText = selectedMsg.text
                                    },
                                    onDelete = { selectedMsg ->
                                        chatViewModel.deleteMessage(roomId, selectedMsg.id, currentUserId)
                                    },
                                    onReplyPreviewClick = { targetId ->
                                        messageIndexMap[targetId]?.let { targetIndex ->
                                            coroutineScope.launch {
                                                listState.animateScrollToItem(targetIndex)
                                            }
                                        }
                                    },
                                    onReactionSelected = { emoji ->
                                        chatViewModel.toggleReaction(roomId, msg.id, currentUserId, emoji)
                                    },
                                    reactions = reactionsByMessage[msg.id] ?: emptyList()
                                )
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = !isNearBottom && inChatSearchQuery.isBlank(),
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                    ) {
                        SmallFloatingActionButton(
                            onClick = {
                                coroutineScope.launch {
                                    if (totalLazyItems > 0) {
                                        listState.animateScrollToItem(totalLazyItems - 1)
                                    }
                                }
                            },
                            containerColor = SageGreenMain,
                            contentColor = DeepGreenDark
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

    if (showAttachmentSheet) {
        AttachmentBottomSheet(
            onDismiss = { showAttachmentSheet = false },
            onPickImage = { imagePickerLauncher.launch("image/*") },
            onPickDocument = { documentPickerLauncher.launch("application/*") }
        )
    }

    selectedImageUri?.let { uri ->
        PhotoPreviewDialog(
            context = context,
            imageUri = uri,
            onDismiss = { selectedImageUri = null },
            onSend = { file, caption ->
                selectedImageUri = null
                val imageUri = Uri.fromFile(file)
                readBytesAndSendWithCaption(imageUri, MessageType.IMAGE, caption)
            }
        )
    }

    if (showInterviewDialog) {
        InterviewDetailDialog(
            companyName = displayCompanyName,
            chatTitle = displayPosition,
            onDismiss = { showInterviewDialog = false },
            onNavigateToSchedule = {
                showInterviewDialog = false
                onNavigateToSchedule()
            }
        )
    }
}

// --- Supporting Components ---

@Composable
fun ReplyComposerBanner(
    replyTarget: ChatMessage,
    onCancel: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SageGreenMain.copy(alpha = 0.15f))
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Replying to message",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = DeepGreenDark
            )
            Text(
                text = replyTarget.text,
                fontSize = 12.sp,
                color = TextDark.copy(alpha = 0.7f),
                maxLines = 1
            )
        }
        IconButton(onClick = onCancel, modifier = Modifier.size(24.dp)) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Cancel Reply",
                tint = TextDark
            )
        }
    }
}

@Composable
fun InterviewDetailDialog(
    companyName: String,
    chatTitle: String,
    onDismiss: () -> Unit,
    onNavigateToSchedule: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Interview Details") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "Company: $companyName", fontWeight = FontWeight.Bold)
                Text(text = "Position: $chatTitle")
                Text(text = "Manage your interview scheduling or view booked calendar slots below.")
            }
        },
        confirmButton = {
            TextButton(onClick = onNavigateToSchedule) {
                Text("View Schedule")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}