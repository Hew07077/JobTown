package com.example.jobtown.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.jobtown.data.model.ChatRoom
import com.example.jobtown.data.model.User
import com.example.jobtown.data.repository.UserRepository
import com.example.jobtown.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
private val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    currentUser: User?,
    chatRooms: List<ChatRoom>,
    isLoading: Boolean,
    onChatRoomClick: (roomId: String, otherName: String, position: String) -> Unit,
    onProfileClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {

    var searchQuery by remember { mutableStateOf("") }
    var showUnreadOnly by remember { mutableStateOf(false) }

    val currentUserId = currentUser?.id.orEmpty()

    // Total unread count across all rooms
    val totalUnreadCount by remember(chatRooms) {
        derivedStateOf {
            chatRooms.sumOf { room -> room.unreadCount }
        }
    }

    // Dynamic filtering and sorting based on latest message time
    val filteredRooms by remember(chatRooms, searchQuery, showUnreadOnly, currentUserId) {
        derivedStateOf {
            var rooms = chatRooms

            if (showUnreadOnly) {
                rooms = rooms.filter { it.unreadCount > 0 }
            }

            if (searchQuery.isNotBlank()) {
                val query = searchQuery.trim()
                rooms = rooms.filter { room ->
                    val isSeeker = room.seekerId == currentUserId
                    val otherName = if (isSeeker) room.companyName else room.seekerName
                    val positionName = room.jobTitle

                    otherName.contains(query, ignoreCase = true) ||
                            positionName.contains(query, ignoreCase = true) ||
                            room.lastMessage.contains(query, ignoreCase = true)
                }
            }
            // Keep room with the most recent message timestamp at top
            rooms.sortedByDescending { it.lastMessageTime }
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = BackgroundWhite,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Messages",
                            fontWeight = FontWeight.Bold,
                            color = TextDark,
                            fontSize = 20.sp
                        )
                        if (totalUnreadCount > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = CircleShape,
                                color = DeepGreenDark
                            ) {
                                Text(
                                    text = if (totalUnreadCount > 99) "99+" else totalUnreadCount.toString(),
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onProfileClick,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(SageGreenLight)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = DeepGreenDark
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
            // Search and Filter Bar Row
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                text = "Search messages or companies...",
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
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                FilterChip(
                    selected = showUnreadOnly,
                    onClick = { showUnreadOnly = !showUnreadOnly },
                    label = { Text("Unread only", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = DeepGreenDark,
                        selectedLabelColor = Color.White,
                        selectedLeadingIconColor = Color.White,
                        containerColor = Color.White,
                        labelColor = TextDark
                    )
                )
            }

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
                        EmptyChatState(modifier = Modifier.align(Alignment.Center))
                    }
                    filteredRooms.isEmpty() -> {
                        Text(
                            text = if (showUnreadOnly) "No unread messages" else "No results found for \"$searchQuery\"",
                            color = TextDark.copy(alpha = 0.5f),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    else -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(
                                items = filteredRooms,
                                key = { it.id }
                            ) { room ->
                                val isSeeker = room.seekerId == currentUserId
                                val otherName = if (isSeeker) room.companyName else room.seekerName
                                val positionName = room.jobTitle.ifBlank { "Position" }

                                ChatRoomItem(
                                    room = room,
                                    isSeeker = isSeeker,
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
    isSeeker: Boolean,
    otherName: String,
    positionName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var fetchedAvatarUrl by remember(room.id) { mutableStateOf<String?>(null) }

    // Safely fetch target user's avatar from repository if not present directly in ChatRoom
    LaunchedEffect(room.id) {
        val targetUserId = if (isSeeker) room.employerId else room.seekerId
        if (!targetUserId.isNullOrBlank()) {
            val user = UserRepository.fetchUserById(targetUserId)
            fetchedAvatarUrl = user?.avatarUrl
        }
    }

    // Resolves avatar URL safely matching standard ChatRoom / User schema variations
    val activeAvatarUrl = fetchedAvatarUrl?.takeIf { it.isNotBlank() }

    val formattedTime = remember(room.lastMessageTime) {
        if (room.lastMessageTime > 0L) formatRelativeTime(room.lastMessageTime) else ""
    }

    val displayLastMessage = remember(room.lastMessage) {
        formatLastMessagePreview(room.lastMessage)
    }

    val unreadCount = room.unreadCount
    val hasUnread = unreadCount > 0

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // AVATAR CONTAINER
            Surface(
                modifier = Modifier.size(50.dp),
                shape = CircleShape,
                color = SageGreenLight
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (!activeAvatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = activeAvatarUrl,
                            contentDescription = "$otherName avatar",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        InitialsAvatar(name = otherName)
                    }
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
                        text = displayLastMessage,
                        fontSize = 13.sp,
                        fontWeight = if (hasUnread) FontWeight.SemiBold else FontWeight.Normal,
                        color = when {
                            hasUnread -> TextDark
                            room.lastMessage.isNotBlank() -> TextDark.copy(alpha = 0.7f)
                            else -> TextDark.copy(alpha = 0.4f)
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (hasUnread) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = CircleShape,
                            color = DeepGreenDark
                        ) {
                            Text(
                                text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InitialsAvatar(
    name: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxSize()
            .clip(CircleShape),
        color = SageGreenLight
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = name.take(1).uppercase().ifBlank { "?" },
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = DeepGreenDark
            )
        }
    }
}

@Composable
private fun EmptyChatState(modifier: Modifier = Modifier) {
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
            text = "No Conversations Yet",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "When you contact an employer or applicant, your chats will appear here.",
            fontSize = 13.sp,
            color = TextDark.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
    }
}

private fun formatLastMessagePreview(message: String): String {
    if (message.isBlank()) return "Tap to start chatting..."
    return when {
        message.startsWith("http") && (message.contains(".jpg") || message.contains(".png") || message.contains(".jpeg")) -> "📷 Photo"
        message.startsWith("http") || message.contains("attachment") -> "📄 Document"
        else -> message
    }
}

private fun formatRelativeTime(timestamp: Long): String {
    val now = Calendar.getInstance()
    val time = Calendar.getInstance().apply { timeInMillis = timestamp }

    val isSameYear = now.get(Calendar.YEAR) == time.get(Calendar.YEAR)
    val dayOfYearNow = now.get(Calendar.DAY_OF_YEAR)
    val dayOfYearTime = time.get(Calendar.DAY_OF_YEAR)

    return when {
        isSameYear && dayOfYearNow == dayOfYearTime -> timeFormat.format(Date(timestamp))
        isSameYear && (dayOfYearNow - dayOfYearTime == 1) -> "Yesterday"
        else -> dateFormat.format(Date(timestamp))
    }
}
