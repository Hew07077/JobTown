package com.example.jobtown.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.jobtown.data.JobApplication
import com.example.jobtown.data.User
import com.example.jobtown.ui.theme.DarkTextPurple
import com.example.jobtown.ui.theme.DeepGreenDark
import com.example.jobtown.ui.theme.SageGreenLight
import com.example.jobtown.ui.theme.SageGreenMain

data class ChatMessage(
    val senderId: String,
    val senderName: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    navController: NavController,
    appId: String,
    currentUser: User?,
    applications: List<JobApplication>
) {
    // Find the application related to this chat room
    val application = applications.find { it.id == appId }

    var messageInput by remember { mutableStateOf("") }
    // Simulated message list for this specific channel
    val messages = remember { mutableStateListOf<ChatMessage>() }

    val currentUserId = currentUser?.id ?: ""
    val currentUserName = currentUser?.name ?: "User"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = application?.jobTitle ?: "Chat Room",
                            fontWeight = FontWeight.Bold,
                            color = DeepGreenDark,
                            fontSize = 16.sp
                        )
                        Text(
                            text = if (currentUser?.role.equals("company", ignoreCase = true))
                                "Applicant: ${application?.applicantName ?: "Unknown"}"
                            else
                                "Company: ${application?.companyName ?: "Unknown"}",
                            fontSize = 12.sp,
                            color = DarkTextPurple
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = DeepGreenDark)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SageGreenMain)
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                color = Color.White
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = messageInput,
                        onValueChange = { messageInput = it },
                        placeholder = { Text("Type a message...") },
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        shape = RoundedCornerShape(24.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DeepGreenDark,
                            unfocusedBorderColor = SageGreenMain
                        )
                    )

                    IconButton(
                        onClick = {
                            if (messageInput.isNotBlank()) {
                                messages.add(
                                    ChatMessage(
                                        senderId = currentUserId,
                                        senderName = currentUserName,
                                        message = messageInput.trim()
                                    )
                                )
                                messageInput = ""
                            }
                        },
                        modifier = Modifier
                            .background(SageGreenMain, shape = RoundedCornerShape(50.dp))
                            .size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send Message",
                            tint = DeepGreenDark
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFFAFAFA))
        ) {
            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No messages yet. Start the conversation below!",
                        color = DarkTextPurple.copy(alpha = 0.6f),
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    items(messages) { chat ->
                        val isMe = chat.senderId == currentUserId

                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
                        ) {
                            Surface(
                                shape = RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = if (isMe) 16.dp else 4.dp,
                                    bottomEnd = if (isMe) 4.dp else 16.dp
                                ),
                                color = if (isMe) SageGreenMain else SageGreenLight,
                                modifier = Modifier.widthIn(max = 280.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    if (!isMe) {
                                        Text(
                                            text = chat.senderName,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = DeepGreenDark
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                    }
                                    Text(
                                        text = chat.message,
                                        fontSize = 14.sp,
                                        color = DeepGreenDark
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}