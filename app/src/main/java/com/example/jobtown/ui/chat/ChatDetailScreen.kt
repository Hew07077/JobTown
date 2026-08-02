package com.example.jobtown.ui.chat

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
import androidx.compose.material.icons.filled.CalendarToday
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
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Safely unwrap messages list from state
    val messages = chatViewModel.messagesList.value

    // Load message history on room launch
    LaunchedEffect(roomId) {
        if (roomId.isNotBlank()) {
            chatViewModel.loadMessages(roomId)
            if (initialQuestion.isNotBlank()) {
                chatViewModel.sendMessage(roomId, currentUserId, initialQuestion)
            }
        }
    }

    // Auto-scroll to bottom when new messages arrive
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
                            text = companyName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        if (chatTitle.isNotBlank()) {
                            Text(
                                text = chatTitle,
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
                    IconButton(onClick = onNavigateToSchedule) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Schedule",
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                                messageText = ""

                                chatViewModel.sendMessage(roomId, currentUserId, textToSend) { success: Boolean ->
                                    if (success) {
                                        coroutineScope.launch {
                                            val currentMessages = chatViewModel.messagesList.value
                                            if (currentMessages.isNotEmpty()) {
                                                listState.animateScrollToItem(currentMessages.size - 1)
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
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
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = messages,
                            key = { msg: ChatMessage -> msg.id.ifBlank { msg.timestamp.toString() + msg.text } }
                        ) { msg ->
                            MessageBubble(
                                message = msg,
                                isMe = msg.senderId == currentUserId
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: ChatMessage,
    isMe: Boolean
) {
    val alignment = if (isMe) Alignment.End else Alignment.Start
    val bubbleColor = if (isMe) SageGreenMain else Color.White

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isMe) 16.dp else 0.dp,
                bottomEnd = if (isMe) 0.dp else 16.dp
            ),
            color = bubbleColor,
            shadowElevation = 1.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = message.text,
                    fontSize = 14.sp,
                    color = TextDark
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(message.timestamp)),
                    fontSize = 10.sp,
                    color = TextDark.copy(alpha = 0.5f),
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}