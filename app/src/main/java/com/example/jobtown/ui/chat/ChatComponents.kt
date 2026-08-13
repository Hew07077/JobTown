package com.example.jobtown.ui.chat

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.jobtown.data.ChatMessage
import com.example.jobtown.data.MessageType
import com.example.jobtown.ui.theme.DeepGreenDark
import com.example.jobtown.ui.theme.SageGreenMain
import com.example.jobtown.ui.theme.TextDark
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
            color = Color.LightGray.copy(alpha = 0.3f)
        ) {
            Text(
                text = dateString,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = TextDark.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: ChatMessage,
    isMe: Boolean,
    onEdit: (ChatMessage) -> Unit,
    onDelete: (ChatMessage) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val alignment = if (isMe) Alignment.End else Alignment.Start
    val bubbleColor = if (isMe) SageGreenMain else Color.White

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Box {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isMe) 16.dp else 2.dp,
                    bottomEnd = if (isMe) 2.dp else 16.dp
                ),
                color = bubbleColor,
                shadowElevation = 1.dp,
                modifier = Modifier.combinedClickable(
                    onClick = { },
                    onLongClick = {
                        if (isMe && !message.isDeleted) {
                            showMenu = true
                        }
                    }
                )
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    when {
                        message.isDeleted -> {
                            Text(
                                text = "This message was deleted",
                                fontSize = 14.sp,
                                fontStyle = FontStyle.Italic,
                                color = TextDark.copy(alpha = 0.5f)
                            )
                        }
                        message.messageType == MessageType.IMAGE -> {
                            AsyncImage(
                                model = message.text,
                                contentDescription = "Attached Image",
                                modifier = Modifier
                                    .widthIn(max = 220.dp)
                                    .heightIn(max = 200.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                        message.messageType == MessageType.FILE -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .widthIn(max = 220.dp)
                                    .padding(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                                    contentDescription = "Document",
                                    tint = DeepGreenDark,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = message.text.substringAfterLast("/"),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextDark,
                                    maxLines = 1
                                )
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

                    Spacer(modifier = Modifier.height(2.dp))

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
                            text = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(message.timestamp)),
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
                DropdownMenuItem(
                    text = { Text("Edit") },
                    onClick = {
                        showMenu = false
                        onEdit(message)
                    },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = "Edit") }
                )
                DropdownMenuItem(
                    text = { Text("Delete", color = Color.Red) },
                    onClick = {
                        showMenu = false
                        onDelete(message)
                    },
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red) }
                )
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
                modifier = Modifier.size(14.dp),
                tint = TextDark.copy(alpha = 0.3f)
            )
        }
        isRead -> {
            Icon(
                imageVector = Icons.Default.DoneAll,
                contentDescription = "Read",
                modifier = Modifier.size(14.dp),
                tint = Color(0xFF007AFF)
            )
        }
        else -> {
            Icon(
                imageVector = Icons.Default.DoneAll,
                contentDescription = "Delivered",
                modifier = Modifier.size(14.dp),
                tint = TextDark.copy(alpha = 0.5f)
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
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp, start = 16.dp, end = 16.dp)
        ) {
            Text(
                text = "Share Content",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                AttachmentOptionItem(
                    icon = Icons.Default.Image,
                    label = "Photo",
                    color = Color(0xFF4CAF50),
                    onClick = {
                        onOptionSelected("PHOTO")
                        onDismiss()
                    }
                )
                AttachmentOptionItem(
                    icon = Icons.AutoMirrored.Filled.InsertDriveFile,
                    label = "Document",
                    color = Color(0xFF2196F3),
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
            color = color.copy(alpha = 0.15f),
            modifier = Modifier.size(56.dp)
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
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = label, fontSize = 12.sp, color = TextDark)
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