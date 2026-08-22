package com.example.jobtown.ui.chat

import android.content.Context
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.jobtown.data.ChatMessage
import com.example.jobtown.ui.theme.DeepGreenDark
import com.example.jobtown.ui.theme.SageGreenMain
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Groups messages chronologically by their calendar date string.
 */
fun groupMessagesByDate(messages: List<ChatMessage>): Map<String, List<ChatMessage>> {
    val formatter = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
    return messages.groupBy { message ->
        formatter.format(Date(message.timestamp))
    }
}

/**
 * Initializes and starts the MediaRecorder to capture audio.
 */
fun startVoiceRecording(
    context: Context,
    onStart: (MediaRecorder, File) -> Unit
) {
    try {
        val outputDir = context.cacheDir
        val audioFile = File.createTempFile("voice_note_", ".3gp", outputDir)

        val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(audioFile.absolutePath)
            prepare()
            start()
        }

        onStart(recorder, audioFile)
    } catch (e: Exception) {
        Log.e("ChatUtils", "Failed to start voice recording", e)
    }
}

/**
 * Safely stops and releases the MediaRecorder, returning the recorded audio File.
 */
fun stopVoiceRecording(
    recorder: MediaRecorder?,
    audioFile: File?
): File? {
    return try {
        recorder?.apply {
            stop()
            release()
        }
        if (audioFile != null && audioFile.exists() && audioFile.length() > 0) {
            audioFile
        } else {
            Log.e("ChatUtils", "Audio file is missing or empty")
            null
        }
    } catch (e: Exception) {
        Log.e("ChatUtils", "Failed to stop voice recording", e)
        recorder?.release()
        null
    }
}

/**
 * Copies a content URI (e.g., from Gallery) to a local cache file to ensure
 * persistent read permissions and prevent security/permission crashes upon sending.
 */
fun getFileFromContentUri(context: Context, uri: Uri, fileExtension: String = ".jpg"): File? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val outputDir = context.cacheDir
        val outputFile = File.createTempFile("upload_", fileExtension, outputDir)

        outputFile.outputStream().use { outputStream ->
            inputStream.copyTo(outputStream)
        }
        inputStream.close()

        if (outputFile.exists() && outputFile.length() > 0) outputFile else null
    } catch (e: Exception) {
        Log.e("ChatUtils", "Error copying content URI to file", e)
        null
    }
}

/**
 * Dialog preview component for images before they are sent, with built-in URI validation and loading protection.
 */
@Composable
fun PhotoPreviewDialog(
    context: Context,
    imageUri: Uri,
    onDismiss: () -> Unit,
    onSend: (file: File, caption: String) -> Unit
) {
    var captionText by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = { if (!isSending) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = "Selected Photo Preview",
                    modifier = Modifier
                        .fillMaxSize()
                        .align(Alignment.Center),
                    contentScale = ContentScale.Fit
                )

                if (isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = SageGreenMain
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    enabled = !isSending,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Preview",
                        tint = Color.White
                    )
                }

                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .imePadding(),
                    color = Color.Black.copy(alpha = 0.7f)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = captionText,
                            onValueChange = { captionText = it },
                            placeholder = { Text("Add a caption...", color = Color.White.copy(alpha = 0.6f)) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(24.dp),
                            enabled = !isSending,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.DarkGray,
                                unfocusedContainerColor = Color.DarkGray,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            maxLines = 3
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                isSending = true
                                val preparedFile = getFileFromContentUri(context, imageUri)
                                if (preparedFile != null) {
                                    onSend(preparedFile, captionText)
                                } else {
                                    Log.e("PhotoPreview", "Failed to resolve local file from URI")
                                    isSending = false
                                }
                            },
                            enabled = !isSending,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(SageGreenMain)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send Photo",
                                tint = DeepGreenDark
                            )
                        }
                    }
                }
            }
        }
    }
}