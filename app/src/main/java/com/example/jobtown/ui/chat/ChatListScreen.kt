package com.example.jobtown.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jobtown.data.ChatRoom
import com.example.jobtown.data.User
import com.example.jobtown.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    currentUser: User?,
    chatRooms: List<ChatRoom>,
    isLoading: Boolean,
    onChatRoomClick: (String, String) -> Unit
) {
    Scaffold(
        containerColor = BackgroundWhite,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Messages",
                        fontWeight = FontWeight.Bold,
                        color = TextDark,
                        fontSize = 18.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SageGreenMain)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = DeepGreenDark)
                    }
                }

                chatRooms.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(SageGreenLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Chat,
                                contentDescription = null,
                                tint = DeepGreenDark,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Conversations Yet",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "When you contact an employer or applicant, your chats will appear here.",
                            fontSize = 13.sp,
                            color = TextDark.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(chatRooms) { room ->
                            val isSeeker = room.seekerId == currentUser?.id
                            val otherName = if (isSeeker) room.companyName else room.seekerName

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onChatRoomClick(room.id, otherName) },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape),
                                        color = SageGreenLight
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = otherName.take(1).uppercase(),
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = DeepGreenDark
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = otherName,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextDark,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )

                                            if (room.lastMessageTime > 0L) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = formatRelativeTime(room.lastMessageTime),
                                                    fontSize = 11.sp,
                                                    color = TextDark.copy(alpha = 0.5f)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            text = if (room.lastMessage.isNotBlank()) room.lastMessage else "Tap to start chatting...",
                                            fontSize = 13.sp,
                                            color = if (room.lastMessage.isNotBlank()) TextDark.copy(alpha = 0.7f) else SageGreenDark,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
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
}

private fun formatRelativeTime(timestamp: Long): String {
    val now = Calendar.getInstance()
    val time = Calendar.getInstance().apply { timeInMillis = timestamp }

    return when {
        now.get(Calendar.YEAR) == time.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == time.get(Calendar.DAY_OF_YEAR) -> {
            SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(timestamp))
        }
        now.get(Calendar.YEAR) == time.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) - time.get(Calendar.DAY_OF_YEAR) == 1 -> {
            "Yesterday"
        }
        else -> {
            SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestamp))
        }
    }
}