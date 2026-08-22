package com.example.jobtown.ui.chat

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.jobtown.data.ChatMessage
import com.example.jobtown.data.MessageReaction
import com.example.jobtown.data.MessageType
import com.example.jobtown.ui.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// ==================== Extension Functions ====================

fun Long.toChatTime(): String {
    val format = SimpleDateFormat("hh:mm a", Locale.getDefault())
    return format.format(Date(this))
}

fun Long.toChatDate(): String {
    val format = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
    return format.format(Date(this))
}


fun displayFileName(url: String): String {
    val rawName = url.substringAfterLast("/")
    val uuidPrefixPattern = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}_")
    return rawName.replaceFirst(uuidPrefixPattern, "").ifBlank { rawName }
}

// ==================== Component Composables ====================

@Composable
fun DateHeader(dateString: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.LightGray.copy(alpha = 0.25f)
        ) {
            Text(
                text = dateString,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextDark.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
            )
        }
    }
}

@Composable
fun MessageStatusTicks(isRead: Boolean, isPending: Boolean) {
    when {
        isPending -> {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Sending",
                modifier = Modifier.size(13.dp),
                tint = TextDark.copy(alpha = 0.3f)
            )
        }
        isRead -> {
            Icon(
                imageVector = Icons.Default.DoneAll,
                contentDescription = "Read",
                modifier = Modifier.size(14.dp),
                tint = Color(0xFF34C759)
            )
        }
        else -> {
            Icon(
                imageVector = Icons.Default.DoneAll,
                contentDescription = "Delivered",
                modifier = Modifier.size(14.dp),
                tint = TextDark.copy(alpha = 0.4f)
            )
        }
    }
}

@Composable
private fun TypingDots() {
    val transition = rememberInfiniteTransition(label = "typing")
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(3) { index ->
            val delayMillis = index * 150
            val alpha by transition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 600, delayMillis = delayMillis, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot_$index"
            )
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(TextDark.copy(alpha = alpha))
            )
        }
    }
}

@Composable
fun TypingIndicatorBubble(label: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp),
            color = Color.White,
            shadowElevation = 1.dp
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                TypingDots()
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontStyle = FontStyle.Italic,
            color = TextDark.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun ReplyPreviewChip(
    sourceMessage: ChatMessage?,
    isMe: Boolean,
    onClick: () -> Unit
) {
    val barColor = if (isMe) DeepGreenDark else SageGreenMain
    val backgroundColor = (if (isMe) Color.White else SageGreenLight).copy(alpha = 0.5f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(vertical = 4.dp, horizontal = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .heightIn(min = 26.dp)
                .background(barColor, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(
                text = when {
                    sourceMessage == null -> "Original message"
                    sourceMessage.isDeleted -> "Deleted message"
                    else -> "Replying to"
                },
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = barColor
            )
            Text(
                text = when {
                    sourceMessage == null -> "Message unavailable"
                    sourceMessage.isDeleted -> "This message was deleted"
                    sourceMessage.messageType == MessageType.IMAGE -> "📷 Photo"
                    sourceMessage.messageType == MessageType.FILE -> "📄 ${displayFileName(sourceMessage.text)}"
                    else -> sourceMessage.text
                },
                fontSize = 11.sp,
                color = TextDark.copy(alpha = 0.7f),
                maxLines = 1
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: ChatMessage,
    isMe: Boolean,
    replySourceMessage: ChatMessage?,
    reactions: List<MessageReaction> = emptyList(),
    onReply: (ChatMessage) -> Unit,
    onEdit: (ChatMessage) -> Unit,
    onDelete: (ChatMessage) -> Unit,
    onReactionSelected: (String) -> Unit = {},
    onReactionLongPress: (String) -> Unit = {},
    onReport: (ChatMessage) -> Unit = {},
    onForward: (ChatMessage) -> Unit = {},
    onReplyPreviewClick: (String) -> Unit = {},
    onCopyText: () -> Unit = {},
    onOpenAttachment: (String, String) -> Unit = { _, _ -> }
) {
    var showMenu by remember { mutableStateOf(false) }
    var showFullScreenImage by remember { mutableStateOf(false) }
    var showReactionPicker by remember { mutableStateOf(false) }

    val alignment = if (isMe) Alignment.End else Alignment.Start
    val bubbleColor = if (isMe) SageGreenMain else Color.White
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        if (reactions.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .padding(bottom = 2.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.8f))
                    .clickable { onReactionLongPress(message.id) }
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                reactions.groupBy { it.emoji }.forEach { (emoji, list) ->
                    Text(
                        text = "$emoji ${list.size}",
                        fontSize = 11.sp
                    )
                }
            }
        }

        Box {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isMe) 16.dp else 4.dp,
                    bottomEnd = if (isMe) 4.dp else 16.dp
                ),
                color = bubbleColor,
                shadowElevation = 1.5.dp,
                modifier = Modifier.combinedClickable(
                    onClick = { },
                    onLongClick = {
                        if (!message.isDeleted) {
                            showMenu = true
                        }
                    }
                )
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    if (!message.isDeleted && !message.replyToId.isNullOrBlank()) {
                        ReplyPreviewChip(
                            sourceMessage = replySourceMessage,
                            isMe = isMe,
                            onClick = {
                                message.replyToId?.let { onReplyPreviewClick(it) }
                            }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    when {
                        message.isDeleted -> {
                            Text(
                                text = "This message was deleted",
                                fontSize = 13.sp,
                                fontStyle = FontStyle.Italic,
                                color = TextDark.copy(alpha = 0.5f)
                            )
                        }
                        message.messageType == MessageType.IMAGE -> {
                            AsyncImage(
                                model = message.text,
                                contentDescription = "Attached Image",
                                modifier = Modifier
                                    .widthIn(max = 240.dp)
                                    .heightIn(max = 220.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { showFullScreenImage = true },
                                contentScale = ContentScale.Crop
                            )
                        }
                        message.messageType == MessageType.FILE -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .widthIn(max = 240.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        onOpenAttachment(message.text, "application/pdf")
                                    }
                                    .background(Color.Black.copy(alpha = 0.04f))
                                    .padding(8.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = DeepGreenDark.copy(alpha = 0.1f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                                            contentDescription = "Document",
                                            tint = DeepGreenDark,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = displayFileName(message.text),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextDark,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "Tap to view document",
                                        fontSize = 10.sp,
                                        color = TextDark.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                        else -> {
                            Text(
                                text = message.text,
                                fontSize = 14.sp,
                                color = TextDark
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Row(
                        modifier = Modifier.align(Alignment.End),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (message.isEdited && !message.isDeleted) {
                            Text(
                                text = "edited",
                                fontSize = 10.sp,
                                fontStyle = FontStyle.Italic,
                                color = TextDark.copy(alpha = 0.4f)
                            )
                        }

                        Text(
                            text = message.timestamp.toChatTime(),
                            fontSize = 10.sp,
                            color = TextDark.copy(alpha = 0.5f)
                        )

                        if (isMe && !message.isDeleted) {
                            MessageStatusTicks(
                                isRead = message.isRead,
                                isPending = message.id.startsWith("temp_")
                            )
                        }
                    }
                }
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                if (!message.isDeleted && message.messageType == MessageType.TEXT) {
                    DropdownMenuItem(
                        text = { Text("Copy text") },
                        onClick = {
                            showMenu = false
                            onCopyText()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                        }
                    )
                }

                if (!message.isDeleted) {
                    DropdownMenuItem(
                        text = { Text("Reply") },
                        onClick = {
                            showMenu = false
                            onReply(message)
                        },
                        leadingIcon = {
                            Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = "Reply")
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("React") },
                        onClick = {
                            showMenu = false
                            showReactionPicker = true
                        },
                        leadingIcon = {
                            Icon(Icons.Default.AddReaction, contentDescription = "React")
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Forward") },
                        onClick = {
                            showMenu = false
                            onForward(message)
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Forward, contentDescription = "Forward")
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Report", color = Color(0xFFFF5722)) },
                        onClick = {
                            showMenu = false
                            onReport(message)
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Report,
                                contentDescription = "Report",
                                tint = Color(0xFFFF5722)
                            )
                        }
                    )
                }

                if (isMe && !message.isDeleted) {
                    if (message.messageType == MessageType.TEXT) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            onClick = {
                                showMenu = false
                                onEdit(message)
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Edit, contentDescription = "Edit")
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Delete", color = Color.Red) },
                        onClick = {
                            showMenu = false
                            onDelete(message)
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                        }
                    )
                }
            }
        }
    }

    if (showReactionPicker) {
        Dialog(onDismissRequest = { showReactionPicker = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("❤️", "👍", "🔥", "😂", "👏").forEach { emoji ->
                        Text(
                            text = emoji,
                            fontSize = 24.sp,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable {
                                    onReactionSelected(emoji)
                                    showReactionPicker = false
                                }
                                .padding(8.dp)
                        )
                    }
                }
            }
        }
    }

    if (showFullScreenImage) {
        Dialog(
            onDismissRequest = { showFullScreenImage = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                AsyncImage(
                    model = message.text,
                    contentDescription = "Full Screen Image",
                    modifier = Modifier
                        .fillMaxSize()
                        .align(Alignment.Center),
                    contentScale = ContentScale.Fit
                )

                IconButton(
                    onClick = { showFullScreenImage = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close preview",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun ReplyComposerBanner(
    replyTarget: ChatMessage,
    onCancel: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SageGreenMain.copy(alpha = 0.25f))
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .heightIn(min = 28.dp)
                    .background(DeepGreenDark, RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Replying to message",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepGreenDark
                )
                Text(
                    text = when {
                        replyTarget.messageType == MessageType.IMAGE -> "📷 Photo"
                        replyTarget.messageType == MessageType.FILE -> "📄 ${displayFileName(replyTarget.text)}"
                        else -> replyTarget.text
                    },
                    fontSize = 12.sp,
                    color = TextDark.copy(alpha = 0.7f),
                    maxLines = 1
                )
            }
        }
        IconButton(onClick = onCancel, modifier = Modifier.size(24.dp)) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Cancel reply",
                tint = TextDark
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachmentBottomSheet(
    onDismiss: () -> Unit,
    onOptionSelected: (String) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp, start = 24.dp, end = 24.dp, top = 8.dp)
        ) {
            Text(
                text = "Share Content",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                AttachmentOptionItem(
                    icon = Icons.Default.Image,
                    label = "Photo",
                    color = Color(0xFF34C759),
                    onClick = {
                        onOptionSelected("PHOTO")
                        onDismiss()
                    }
                )
                AttachmentOptionItem(
                    icon = Icons.AutoMirrored.Filled.InsertDriveFile,
                    label = "Document",
                    color = Color(0xFF007AFF),
                    onClick = {
                        onOptionSelected("DOCUMENT")
                        onDismiss()
                    }
                )
            }
        }
    }
}

@Composable
fun AttachmentOptionItem(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Surface(
            shape = CircleShape,
            color = color.copy(alpha = 0.12f),
            modifier = Modifier.size(60.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = color,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextDark)
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
        confirmButton = {
            Button(
                onClick = {
                    onDismiss()
                    onNavigateToSchedule()
                },
                colors = ButtonDefaults.buttonColors(containerColor = SageGreenMain)
            ) {
                Text("Manage Schedule", color = DeepGreenDark, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = TextDark)
            }
        },
        title = {
            Text(text = "Interview Details", fontWeight = FontWeight.Bold, color = TextDark)
        },
        text = {
            Column {
                Text(
                    text = if (chatTitle.isNotBlank()) "$companyName • $chatTitle" else companyName,
                    fontSize = 13.sp,
                    color = TextDark.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "• Time: Scheduled / Pending", fontSize = 13.sp, color = TextDark)
                Text(text = "• Format: Video Call", fontSize = 13.sp, color = TextDark)
            }
        }
    )
}

@Composable
fun EditBanner(
    message: ChatMessage,
    onCancel: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFFF3E0))
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .heightIn(min = 28.dp)
                    .background(Color(0xFFFF9800), RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Editing message",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF9800)
                )
                Text(
                    text = message.text,
                    fontSize = 12.sp,
                    color = TextDark.copy(alpha = 0.7f),
                    maxLines = 1
                )
            }
        }
        IconButton(onClick = onCancel, modifier = Modifier.size(24.dp)) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Cancel edit",
                tint = TextDark
            )
        }
    }
}

@Composable
fun OfflineBanner() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFFFF5722),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.WifiOff,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "You're offline. Messages will be sent when reconnected.",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun EmptyChatState(
    isGroupChat: Boolean,
    onStartChat: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = SageGreenMain.copy(alpha = 0.1f),
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Chat,
                        contentDescription = null,
                        tint = SageGreenMain,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (isGroupChat) "Start the conversation" else "No messages yet",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isGroupChat) {
                    "Say something to the group"
                } else {
                    "Send your first message to get started"
                },
                fontSize = 13.sp,
                color = TextDark.copy(alpha = 0.6f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onStartChat,
                colors = ButtonDefaults.buttonColors(containerColor = SageGreenMain),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text(
                    text = "Start Chat",
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterMenuDialog(
    currentFilter: MessageFilter,
    onFilterSelected: (MessageFilter) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter Messages") },
        text = {
            Column {
                MessageFilter.values().forEach { filter ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onFilterSelected(filter) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentFilter == filter,
                            onClick = { onFilterSelected(filter) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = SageGreenMain
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (filter) {
                                MessageFilter.ALL -> "All Messages"
                                MessageFilter.TEXT_ONLY -> "Text Only"
                                MessageFilter.ATTACHMENTS_ONLY -> "With Attachments"
                                MessageFilter.WITH_REACTIONS -> "With Reactions"
                                MessageFilter.UNREAD_ONLY -> "Unread Only"
                            },
                            color = TextDark,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = SageGreenMain)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortMenuDialog(
    currentSortOrder: MessageSortOrder,
    onSortSelected: (MessageSortOrder) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sort Messages") },
        text = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSortSelected(MessageSortOrder.NEWEST_FIRST) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = currentSortOrder == MessageSortOrder.NEWEST_FIRST,
                        onClick = { onSortSelected(MessageSortOrder.NEWEST_FIRST) },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = SageGreenMain
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Newest First", color = TextDark, fontSize = 14.sp)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSortSelected(MessageSortOrder.OLDEST_FIRST) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = currentSortOrder == MessageSortOrder.OLDEST_FIRST,
                        onClick = { onSortSelected(MessageSortOrder.OLDEST_FIRST) },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = SageGreenMain
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Oldest First", color = TextDark, fontSize = 14.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = SageGreenMain)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReactionDetailDialog(
    messageId: String,
    reactions: List<MessageReaction>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reactions") },
        text = {
            Column {
                if (reactions.isEmpty()) {
                    Text("No reactions yet", color = TextDark.copy(alpha = 0.5f))
                } else {
                    reactions.groupBy { it.emoji }.forEach { (emoji, users) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$emoji ${users.size}",
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = users.joinToString { it.userId },
                                fontSize = 12.sp,
                                color = TextDark.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = SageGreenMain)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportMessageDialog(
    reason: String,
    onReasonChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report Message") },
        text = {
            Column {
                Text(
                    text = "Why are you reporting this message?",
                    fontSize = 13.sp,
                    color = TextDark.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = onReasonChange,
                    placeholder = { Text("Enter reason...") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SageGreenMain,
                        unfocusedBorderColor = SageGreenLight
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onSubmit,
                enabled = reason.isNotBlank()
            ) {
                Text("Submit", color = if (reason.isNotBlank()) Color.Red else Color.Gray)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextDark)
            }
        }
    )
}

@Composable
fun MessageComposer(
    inputText: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onTyping: (Boolean) -> Unit,
    onAttachmentClick: () -> Unit,
    isSending: Boolean,
    isUploading: Boolean,
    isRecordingVoice: Boolean = false,
    onVoiceRecordToggle: () -> Unit = {},
    onDraftMention: (String) -> Unit = {},
    mentionSuggestions: List<String> = emptyList(),
    onMentionSelected: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isTyping by remember { mutableStateOf(false) }
    var showMentionSuggestions by remember { mutableStateOf(false) }

    LaunchedEffect(inputText) {
        val lastWord = inputText.split(" ").lastOrNull() ?: ""
        if (lastWord.startsWith("@") && lastWord.length > 1) {
            showMentionSuggestions = true
            onDraftMention(lastWord.substring(1))
        } else {
            showMentionSuggestions = false
        }
    }

    Column(modifier = modifier) {
        AnimatedVisibility(
            visible = showMentionSuggestions && mentionSuggestions.isNotEmpty(),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 4.dp
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 150.dp)
                ) {
                    items(mentionSuggestions) { suggestion ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onMentionSelected(suggestion)
                                    showMentionSuggestions = false
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = SageGreenMain.copy(alpha = 0.2f),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        tint = SageGreenMain,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = suggestion,
                                fontSize = 13.sp,
                                color = TextDark,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = onAttachmentClick,
                enabled = !isUploading && !isRecordingVoice,
                modifier = Modifier.size(40.dp)
            ) {
                if (isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = DeepGreenDark
                    )
                } else {
                    Icon(
                        Icons.Default.AttachFile,
                        contentDescription = "Attach file",
                        tint = SageGreenMain
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(SageGreenLight.copy(alpha = 0.3f))
                    .padding(horizontal = 12.dp)
            ) {
                BasicTextField(
                    value = inputText,
                    onValueChange = {
                        onTextChange(it)
                        if (it.isNotBlank() && !isTyping) {
                            isTyping = true
                            onTyping(true)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextDark),
                    cursorBrush = SolidColor(SageGreenMain),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Send
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (inputText.isNotBlank()) {
                                onSend()
                                onTyping(false)
                                isTyping = false
                            }
                        }
                    ),
                    decorationBox = { innerTextField ->
                        if (inputText.isEmpty()) {
                            Text(
                                text = if (isRecordingVoice) "Recording..." else "Type a message...",
                                color = TextDark.copy(alpha = 0.5f),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        innerTextField()
                    }
                )
            }

            if (isSending) {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    color = SageGreenMain,
                    strokeWidth = 2.dp
                )
            } else {
                FloatingActionButton(
                    onClick = {
                        if (inputText.isBlank()) {
                            onVoiceRecordToggle()
                        } else {
                            onSend()
                            onTyping(false)
                            isTyping = false
                        }
                    },
                    containerColor = if (isRecordingVoice || inputText.isNotBlank()) SageGreenMain else Color.LightGray,
                    contentColor = Color.White,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (inputText.isBlank()) {
                            if (isRecordingVoice) Icons.Default.Check else Icons.Default.Mic
                        } else {
                            Icons.AutoMirrored.Filled.Send
                        },
                        contentDescription = if (inputText.isBlank()) "Voice" else "Send",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun VoiceRecordingIndicator(
    onCancel: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Red.copy(alpha = 0.1f))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Color.Red)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Recording voice note...",
                color = Color.Red,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
        IconButton(onClick = onCancel, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Default.Cancel,
                contentDescription = "Cancel Voice Note",
                tint = Color.Red
            )
        }
    }
}

@Composable
fun LoadMoreTrigger(
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { if (!isLoading) onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (isLoading) "Loading..." else "Load earlier messages",
            fontSize = 12.sp,
            color = TextDark.copy(alpha = 0.5f),
            modifier = Modifier.padding(8.dp)
        )
    }
}