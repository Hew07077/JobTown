@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.jobtown.ui.chat

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.AddReaction
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

private val chatTimeFormatter by lazy { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

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

    val isPending = message.id.startsWith("temp_")
    val alignment = if (isMe) Alignment.End else Alignment.Start
    val bubbleColor = if (isMe) SageGreenMain else Color.White
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val interactionSource = remember { MutableInteractionSource() }

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
                shadowElevation = 1.dp,
                modifier = Modifier.combinedClickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    onClick = {},
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
                        Spacer(modifier = Modifier.height(6.dp))
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
                        message.messageType == MessageType.IMAGE && isPending -> {
                            UploadingAttachmentPlaceholder(fileName = "Uploading image…")
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
                        message.messageType == MessageType.FILE && isPending -> {
                            UploadingAttachmentPlaceholder(fileName = displayFileName(message.text))
                        }
                        message.messageType == MessageType.FILE -> {
                            AttachmentRow(
                                icon = Icons.AutoMirrored.Filled.InsertDriveFile,
                                title = displayFileName(message.text),
                                subtitle = "Tap to view document",
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

                    Spacer(modifier = Modifier.height(4.dp))

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
                                isPending = isPending
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
fun DateHeader(dateString: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = SageGreenLight.copy(alpha = 0.6f)
        ) {
            Text(
                text = dateString,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextDark.copy(alpha = 0.8f),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
            )
        }
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
    val backgroundColor = (if (isMe) Color.White else SageGreenLight).copy(alpha = 0.6f)

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

private fun displayFileName(url: String): String {
    val rawName = url.substringAfterLast("/")
    val uuidPrefixPattern = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}_")
    return rawName.replaceFirst(uuidPrefixPattern, "").ifBlank { "Document" }
}

private fun openAttachmentUrl(context: Context, url: String) {
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
            .background(Color.Black.copy(alpha = 0.05f))
            .padding(8.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = DeepGreenDark.copy(alpha = 0.12f),
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
                color = if (group.reactedByMe) SageGreenMain.copy(alpha = 0.8f) else Color.White,
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
private fun UploadingAttachmentPlaceholder(fileName: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .widthIn(max = 240.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.05f))
            .padding(8.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            strokeWidth = 2.dp,
            color = DeepGreenDark
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = "Uploading attachment…",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextDark
            )
            Text(
                text = fileName,
                fontSize = 10.sp,
                color = TextDark.copy(alpha = 0.5f),
                maxLines = 1
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
                tint = Color(0xFF2E7D32)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachmentBottomSheet(
    onDismiss: () -> Unit,
    onPickImage: () -> Unit,
    onPickDocument: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Attach File",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Surface(
                onClick = {
                    onPickImage()
                    onDismiss()
                },
                shape = RoundedCornerShape(12.dp),
                color = SageGreenLight.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = "Photo",
                        tint = DeepGreenDark,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Photo or Image",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = TextDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                onClick = {
                    onPickDocument()
                    onDismiss()
                },
                shape = RoundedCornerShape(12.dp),
                color = SageGreenLight.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = "Document",
                        tint = DeepGreenDark,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Document / File",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = TextDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

