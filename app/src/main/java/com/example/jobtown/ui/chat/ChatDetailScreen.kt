package com.example.jobtown.ui.chat

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.jobtown.data.ChatMessage
import com.example.jobtown.data.MessageType
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

    var showAttachmentSheet by remember { mutableStateOf(false) }
    var showInterviewDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val messages: List<ChatMessage> = chatViewModel.messagesList.value

    val displayCompanyName = companyName.ifBlank { "Company Name" }
    val displayPosition = chatTitle.ifBlank { "Position" }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            chatViewModel.sendMessage(roomId, currentUserId, it.toString(), MessageType.IMAGE) {
                chatViewModel.loadUserChatRooms(currentUserId)
            }
        }
    }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            chatViewModel.sendMessage(roomId, currentUserId, it.toString(), MessageType.FILE) {
                chatViewModel.loadUserChatRooms(currentUserId)
            }
        }
    }

    LaunchedEffect(roomId) {
        if (roomId.isNotBlank()) {
            chatViewModel.loadMessages(roomId, currentUserId)
            if (initialQuestion.isNotBlank()) {
                chatViewModel.sendMessage(roomId, currentUserId, initialQuestion) {
                    chatViewModel.loadUserChatRooms(currentUserId)
                }
            }
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
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
                        Text(
                            text = displayPosition,
                            fontSize = 12.sp,
                            color = TextDark.copy(alpha = 0.6f)
                        )
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
                    editingMessage?.let { editMsg: ChatMessage ->
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

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { showAttachmentSheet = true },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Attach File",
                                tint = TextDark.copy(alpha = 0.7f)
                            )
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

                        IconButton(
                            onClick = {
                                if (messageText.isNotBlank()) {
                                    val textToSend = messageText
                                    val currentEdit = editingMessage

                                    if (currentEdit != null) {
                                        chatViewModel.editMessage(roomId, currentEdit.id, textToSend)
                                        editingMessage = null
                                        messageText = ""
                                        chatViewModel.loadUserChatRooms(currentUserId)
                                    } else {
                                        messageText = ""
                                        chatViewModel.sendMessage(roomId, currentUserId, textToSend) { success ->
                                            if (success) {
                                                chatViewModel.loadUserChatRooms(currentUserId)
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
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(SageGreenMain)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = DeepGreenDark
                            )
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
                chatViewModel.isLoadingMessages && messages.isEmpty() -> {
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
                        groupedMessages.forEach { (dateHeader: String, messageList: List<ChatMessage>) ->
                            item(key = "header_$dateHeader") {
                                DateHeader(dateString = dateHeader)
                            }
                            items(
                                items = messageList,
                                key = { msg: ChatMessage -> msg.id.ifBlank { "${msg.timestamp}_${msg.text.hashCode()}" } }
                            ) { msg: ChatMessage ->
                                MessageBubble(
                                    message = msg,
                                    isMe = msg.senderId == currentUserId,
                                    onEdit = { selectedMsg: ChatMessage ->
                                        editingMessage = selectedMsg
                                        messageText = selectedMsg.text
                                    },
                                    onDelete = { selectedMsg: ChatMessage ->
                                        chatViewModel.deleteMessage(roomId, selectedMsg.id)
                                        // Refreshes the chat rooms list outside after deletion
                                        chatViewModel.loadUserChatRooms(currentUserId)
                                    }
                                )
                            }
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