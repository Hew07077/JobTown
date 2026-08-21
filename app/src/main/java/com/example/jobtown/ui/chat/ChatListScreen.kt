package com.example.jobtown.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
private val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())

enum class ChatFilterTab {
    ALL, UNREAD
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    currentUser: User?,
    chatRooms: List<ChatRoom>,
    isLoading: Boolean,
    onChatRoomClick: (roomId: String, otherName: String, position: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(ChatFilterTab.ALL) }

    // Filter chat rooms based on search query and selected tab
    val filteredRooms = remember(chatRooms, searchQuery, currentUser?.id, selectedTab) {
        val baseList = if (selectedTab == ChatFilterTab.UNREAD) {
            chatRooms.filter { it.hasUnreadMessages() }
        } else {
            chatRooms
        }

        if (searchQuery.isBlank()) {
            baseList
        } else {
            val query = searchQuery.trim()
            baseList.filter { room ->
                val isSeeker = room.seekerId == currentUser?.id
                val otherName = if (isSeeker) room.companyName else room.seekerName
                val positionName = room.jobTitle

                otherName.contains(query, ignoreCase = true) ||
                        positionName.contains(query, ignoreCase = true) ||
                        room.lastMessage.contains(query, ignoreCase = true)
            }
        }
    }

    val unreadTotal = remember(chatRooms) { chatRooms.count { it.hasUnreadMessages() } }

    Scaffold(
        modifier = modifier,
        containerColor = BackgroundWhite,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Messages",
                            fontWeight = FontWeight.Bold,
                            color = TextDark,
                            fontSize = 20.sp
                        )
                        if (unreadTotal > 0) {
                            Text(
                                text = "$unreadTotal unread conversation${if (unreadTotal > 1) "s" else ""}",
                                fontSize = 11.sp,
                                color = DeepGreenDark.copy(alpha = 0.8f),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { /* Optional: Filter or settings action */ }) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filter Options",
                            tint = TextDark
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SageGreenMain)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Bar Component
            if (chatRooms.isNotEmpty() || searchQuery.isNotEmpty()) {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Search messages, companies, or positions...",
                            fontSize = 13.sp,
                            color = TextDark.copy(alpha = 0.5f)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = TextDark.copy(alpha = 0.5f)
                        )
                    },
                    trailingIcon = {
                        AnimatedVisibility(
                            visible = searchQuery.isNotEmpty(),
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search",
                                    tint = TextDark.copy(alpha = 0.5f)
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true
                )

                // Filter Tabs (All / Unread)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedTab == ChatFilterTab.ALL,
                        onClick = { selectedTab = ChatFilterTab.ALL },
                        label = { Text("All (${chatRooms.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DeepGreenDark,
                            selectedLabelColor = Color.White,
                            containerColor = Color.White,
                            labelColor = TextDark
                        )
                    )
                    FilterChip(
                        selected = selectedTab == ChatFilterTab.UNREAD,
                        onClick = { selectedTab = ChatFilterTab.UNREAD },
                        label = { Text("Unread ($unreadTotal)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DeepGreenDark,
                            selectedLabelColor = Color.White,
                            containerColor = Color.White,
                            labelColor = TextDark
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                when {
                    isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = DeepGreenDark
                        )
                    }
                    chatRooms.isEmpty() -> {
                        EmptyChatState(
                            title = "No Conversations Yet",
                            subtitle = "When you contact an employer or applicant, your chats will appear here.",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    filteredRooms.isEmpty() -> {
                        EmptyChatState(
                            title = "No matches found",
                            subtitle = if (selectedTab == ChatFilterTab.UNREAD) {
                                "You have no unread messages matching your filters."
                            } else {
                                "No results found for \"$searchQuery\"."
                            },
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    else -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(
                                items = filteredRooms,
                                key = { it.id }
                            ) { room ->
                                val isSeeker = room.seekerId == currentUser?.id
                                val otherName = if (isSeeker) room.companyName else room.seekerName
                                val positionName = room.jobTitle.ifBlank { "Position" }

                                ChatRoomItem(
                                    room = room,
                                    otherName = otherName,
                                    positionName = positionName,
                                    onClick = { onChatRoomClick(room.id, otherName, positionName) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatRoomItem(
    room: ChatRoom,
    otherName: String,
    positionName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formattedTime = remember(room.lastMessageTime) {
        if (room.lastMessageTime > 0L) formatRelativeTime(room.lastMessageTime) else ""
    }
    val hasUnread = remember(room) { room.hasUnreadMessages() }

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = if (hasUnread) 2.dp else 0.5.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile Initials Avatar with Online/Unread Accent Ring if needed
            Surface(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape),
                color = if (hasUnread) SageGreenMain else SageGreenLight
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
                        text = otherName.ifBlank { "Unknown User" },
                        fontSize = 15.sp,
                        fontWeight = if (hasUnread) FontWeight.ExtraBold else FontWeight.Bold,
                        color = TextDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (formattedTime.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = formattedTime,
                            fontSize = 11.sp,
                            fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.Normal,
                            color = if (hasUnread) DeepGreenDark else TextDark.copy(alpha = 0.5f)
                        )
                    }
                }

                if (positionName.isNotBlank() && positionName != "Position") {
                    Text(
                        text = positionName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = DeepGreenDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = room.lastMessage.ifBlank { "Tap to start chatting..." },
                        fontSize = 13.sp,
                        fontWeight = if (hasUnread) FontWeight.SemiBold else FontWeight.Normal,
                        color = when {
                            hasUnread -> TextDark
                            room.lastMessage.isNotBlank() -> TextDark.copy(alpha = 0.7f)
                            else -> SageGreenDark
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (hasUnread) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = CircleShape,
                            color = DeepGreenDark,
                            modifier = Modifier.defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = room.unreadCount.toString(),
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyChatState(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
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
                imageVector = Icons.AutoMirrored.Filled.Chat,
                contentDescription = null,
                tint = DeepGreenDark,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            fontSize = 13.sp,
            color = TextDark.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
    }
}

private fun ChatRoom.hasUnreadMessages(): Boolean = this.unreadCount > 0

private fun formatRelativeTime(timestamp: Long): String {
    val now = Calendar.getInstance()
    val time = Calendar.getInstance().apply { timeInMillis = timestamp }

    val isSameYear = now.get(Calendar.YEAR) == time.get(Calendar.YEAR)
    val dayOfYearDiff = now.get(Calendar.DAY_OF_YEAR) - time.get(Calendar.DAY_OF_YEAR)

    return when {
        isSameYear && dayOfYearDiff == 0 -> timeFormat.format(Date(timestamp))
        isSameYear && dayOfYearDiff == 1 -> "Yesterday"
        else -> dateFormat.format(Date(timestamp))
    }
}