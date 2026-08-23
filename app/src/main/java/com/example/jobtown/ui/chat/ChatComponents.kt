package com.example.jobtown.ui.chat

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.jobtown.data.ChatMessage
import com.example.jobtown.data.MessageType
import com.example.jobtown.data.ReactionGroup
import com.example.jobtown.ui.theme.DeepGreenDark
import com.example.jobtown.ui.theme.SageGreenLight
import com.example.jobtown.ui.theme.SageGreenMain
import com.example.jobtown.ui.theme.TextDark
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Cached outside Composables to prevent unnecessary object allocations on recomposition
private val chatTimeFormatter by lazy { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

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
fun MessageBubble(
    message: ChatMessage,
    isMe: Boolean,
    replySourceMessage: ChatMessage?,
    onReply: (ChatMessage) -> Unit,
    onEdit: (ChatMessage) -> Unit,
    onDelete: (ChatMessage) -> Unit,
    onReactionSelected: (String) -> Unit = {},
    onReplyPreviewClick: (messageId: String) -> Unit = {},
    reactions: List<ReactionGroup> = emptyList()
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
                            AttachmentRow(
                                icon = Icons.AutoMirrored.Filled.InsertDriveFile,
                                title = displayFileName(message.text),
                                subtitle = "Tap to view document",
                                onClick = { openAttachmentUrl(context, message.text) }
                            )
                        }
                        message.messageType == MessageType.VOICE -> {
                            AttachmentRow(
                                icon = Icons.Default.Mic,
                                title = "Voice message",
                                subtitle = "Tap to play",
                                onClick = { openAttachmentUrl(context, message.text) }
                            )
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
                            text = chatTimeFormatter.format(Date(message.timestamp)),
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
                            clipboardManager.setText(AnnotatedString(message.text))
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

        ReactionsRow(
            reactions = reactions,
            onReactionClick = { emoji -> onReactionSelected(emoji) }
        )
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
        ImageViewerDialog(
            imageUrl = message.text,
            onDismiss = { showFullScreenImage = false }
        )
    }
}

@Composable
fun ImageViewerDialog(imageUrl: String, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "Full Screen Image",
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center),
                contentScale = ContentScale.Fit
            )

            IconButton(
                onClick = onDismiss,
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

@Composable
private fun ReplyPreviewChip(
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
                    sourceMessage.messageType == MessageType.VOICE -> "🎤 Voice message"
                    else -> sourceMessage.text
                },
                fontSize = 11.sp,
                color = TextDark.copy(alpha = 0.7f),
                maxLines = 1
            )
        }
    }
}

private fun displayFileName(url: String): String {
    val rawName = url.substringAfterLast("/")
    val uuidPrefixPattern = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}_")
    return rawName.replaceFirst(uuidPrefixPattern, "").ifBlank { rawName }
}

private fun openAttachmentUrl(context: android.content.Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        Log.e("MessageBubble", "Error opening attachment URI", e)
    }
}

@Composable
private fun AttachmentRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .widthIn(max = 240.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
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
                    imageVector = icon,
                    contentDescription = title,
                    tint = DeepGreenDark,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextDark,
                maxLines = 1
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = TextDark.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun ReactionsRow(
    reactions: List<ReactionGroup>,
    onReactionClick: (String) -> Unit
) {
    if (reactions.isEmpty()) return

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(top = 3.dp)
    ) {
        reactions.forEach { group ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (group.reactedByMe) SageGreenMain.copy(alpha = 0.4f) else Color.White,
                shadowElevation = 0.5.dp,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onReactionClick(group.emoji) }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(text = group.emoji, fontSize = 12.sp)
                    if (group.count > 1) {
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = group.count.toString(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextDark.copy(alpha = 0.75f)
                        )
                    }
                }
            }
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
fun OnlineStatusDot(isOnline: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(9.dp)
            .clip(CircleShape)
            .background(if (isOnline) Color(0xFF34C759) else Color.LightGray)
    )
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
                        replyTarget.messageType == MessageType.VOICE -> "🎤 Voice message"
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