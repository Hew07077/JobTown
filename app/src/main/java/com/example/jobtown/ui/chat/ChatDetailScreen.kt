package com.example.jobtown.ui.chat

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import androidx.navigation.NavController
import com.example.jobtown.data.ChatMessage
import com.example.jobtown.data.MessageType
import com.example.jobtown.data.toReactionGroups
import com.example.jobtown.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
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

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val messages by chatViewModel.messagesList.collectAsState(initial = emptyList())
    val rawReactions by chatViewModel.reactionsList.collectAsState(initial = emptyList())
    val roomPresence by chatViewModel.roomPresence.collectAsState()

    val isMessagesLoading by chatViewModel.isLoadingMessages.collectAsState(initial = false)
    val isSendingMessage by chatViewModel.isSendingMessage.collectAsState(initial = false)
    val isUploadingAttachment by chatViewModel.isUploadingAttachment.collectAsState(initial = false)
    val isLoadingOlderMessages by chatViewModel.isLoadingOlderMessages.collectAsState(initial = false)

    val displayCompanyName = companyName.ifBlank { "Company Name" }
    val displayPosition = chatTitle.ifBlank { "Position" }

    val isNearBottom by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            val totalItems = listState.layoutInfo.totalItemsCount
            totalItems == 0 || lastVisible >= totalItems - 2
        }
    }

    // Typing status observer helper
    LaunchedEffect(messageText) {
        if (roomId.isNotBlank() && currentUserId.isNotBlank()) {
            chatViewModel.sendTypingStatus(roomId, currentUserId, messageText.isNotBlank())
        }
    }

    fun readBytesAndSend(uri: android.net.Uri, type: MessageType) {
        coroutineScope.launch {
            try {
                val resolver = context.contentResolver
                val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes == null) {
                    android.util.Log.e("ChatDetailScreen", "Could not read attachment bytes for $uri")
                    return@launch
                }
                val mimeType = resolver.getType(uri)
                    ?: MimeTypeMap.getFileExtensionFromUrl(uri.toString())
                        ?.let { MimeTypeMap.getSingleton().getMimeTypeFromExtension(it) }
                    ?: "application/octet-stream"
                val fileName = uri.lastPathSegment?.substringAfterLast("/") ?: "attachment"

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
            } catch (e: Exception) {
                android.util.Log.e("ChatDetailScreen", "Error reading attachment", e)
            }
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { readBytesAndSend(it, MessageType.IMAGE) }
    }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { readBytesAndSend(it, MessageType.FILE) }
    }

    LaunchedEffect(roomId) {
        if (roomId.isNotBlank()) {
            chatViewModel.loadMessages(roomId, currentUserId)
            chatViewModel.sendInitialQuestionOnce(roomId, currentUserId, initialQuestion)
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty() && isNearBottom) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    LaunchedEffect(listState, roomId) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { firstVisible ->
                if (firstVisible <= 2 && chatViewModel.messagesList.value.isNotEmpty()) {
                    chatViewModel.loadOlderMessages(roomId)
                }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
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
                color = Color.White
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

                        val canSend = messageText.isNotBlank() && !isSendingMessage

                        IconButton(
                            onClick = {
                                if (canSend) {
                                    val textToSend = messageText
                                    val currentEdit = editingMessage
                                    val currentReplyId = replyingToMessage?.id
                                    messageText = ""

                                    if (currentEdit != null) {
                                        chatViewModel.editMessage(roomId, currentEdit.id, textToSend)
                                        editingMessage = null
                                        chatViewModel.loadUserChatRooms(currentUserId)
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
                                                    val currentMessages = chatViewModel.messagesList.value
                                                    if (currentMessages.isNotEmpty()) {
                                                        listState.animateScrollToItem(currentMessages.size - 1)
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
                                    contentDescription = "Send",
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
                messages.isEmpty() -> {
                    Text(
                        text = "No messages yet. Send a message to start!",
                        color = TextDark.copy(alpha = 0.5f),
                        fontSize = 14.sp,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    val groupedMessages = remember(messages) { groupMessagesByDate(messages) }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (isLoadingOlderMessages) {
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
                                val messageReactions = rawReactions
                                    .filter { it.messageId == msg.id }
                                    .toReactionGroups(currentUserId)

                                MessageBubble(
                                    message = msg,
                                    isMe = msg.senderId == currentUserId,
                                    replySourceMessage = replySource,
                                    reactionGroups = messageReactions,
                                    onReply = { selectedMsg ->
                                        replyingToMessage = selectedMsg
                                    },
                                    onToggleReaction = { emoji ->
                                        chatViewModel.toggleReaction(roomId, msg.id, currentUserId, emoji)
                                    },
                                    onEdit = { selectedMsg ->
                                        editingMessage = selectedMsg
                                        messageText = selectedMsg.text
                                    },
                                    onDelete = { selectedMsg ->
                                        chatViewModel.deleteMessage(roomId, selectedMsg.id)
                                        chatViewModel.loadUserChatRooms(currentUserId)
                                    },
                                    onReplyPreviewClick = { targetId ->
                                        val index = messages.indexOfFirst { it.id == targetId }
                                        if (index != -1) {
                                            coroutineScope.launch {
                                                listState.animateScrollToItem(index)
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = !isNearBottom,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                    ) {
                        SmallFloatingActionButton(
                            onClick = {
                                coroutineScope.launch {
                                    if (messages.isNotEmpty()) {
                                        listState.animateScrollToItem(messages.size - 1)
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
            onOptionSelected = { option ->
                when (option) {
                    "PHOTO" -> imagePickerLauncher.launch("image/*")
                    "DOCUMENT" -> documentPickerLauncher.launch("application/*")
                }
            }
        )
    }

    if (showInterviewDialog) {
        InterviewDetailDialog(
            companyName = displayCompanyName,
            chatTitle = displayPosition,
            onDismiss = { showInterviewDialog = false },
            onNavigateToSchedule = onNavigateToSchedule
        )
    }
}

private fun groupMessagesByDate(messages: List<ChatMessage>): Map<String, List<ChatMessage>> {
    val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return messages.groupBy { msg ->
        formatter.format(Date(msg.timestamp))
    }
}