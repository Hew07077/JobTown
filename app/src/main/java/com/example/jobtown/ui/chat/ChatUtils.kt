package com.example.jobtown.ui.chat

import android.content.Context
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Groups messages chronologically by human-readable calendar dates ("Today", "Yesterday", or "MMMM dd, yyyy").
 */
fun groupMessagesByDate(messages: List<ChatMessage>): Map<String, List<ChatMessage>> {
    val fullFormatter = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
    val todayCal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    val msgCal = Calendar.getInstance()

    return messages.groupBy { message ->
        msgCal.timeInMillis = message.timestamp
        msgCal.set(Calendar.HOUR_OF_DAY, 0)
        msgCal.set(Calendar.MINUTE, 0)
        msgCal.set(Calendar.SECOND, 0)
        msgCal.set(Calendar.MILLISECOND, 0)

        val diffInMillis = todayCal.timeInMillis - msgCal.timeInMillis
        val diffInDays = diffInMillis / (24 * 60 * 60 * 1000)

        when (diffInDays) {
            0L -> "Today"
            1L -> "Yesterday"
            else -> fullFormatter.format(Date(message.timestamp))
        }
    }
}

/**
 * Initializes and starts the MediaRecorder to capture audio using AAC formatting for universal playback compatibility.
 */
fun startVoiceRecording(
    context: Context,
    onStart: (MediaRecorder, File) -> Unit
) {
    var audioFile: File? = null
    var recorder: MediaRecorder? = null
    try {
        val outputDir = context.cacheDir
        audioFile = File.createTempFile("voice_note_", ".m4a", outputDir)

        recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(128000)
            setAudioSamplingRate(44100)
            setOutputFile(audioFile.absolutePath)
            prepare()
            start()
        }

        onStart(recorder, audioFile)
    } catch (e: Exception) {
        Log.e("ChatUtils", "Failed to start voice recording", e)
        recorder?.release()
        if (audioFile?.exists() == true) {
            audioFile.delete()
        }
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
            audioFile?.delete()
            null
        }
    } catch (e: Exception) {
        Log.e("ChatUtils", "Failed to stop voice recording cleanly", e)
        try {
            recorder?.release()
        } catch (releaseEx: Exception) {
            Log.e("ChatUtils", "Error releasing recorder", releaseEx)
        }
        audioFile?.delete()
        null
    }
}

/**
 * Copies a content URI (e.g., from Gallery or File Picker) to a local cache file securely,
 * automatically matching the original MIME type extension.
 */
fun getFileFromContentUri(context: Context, uri: Uri, defaultExtension: String = ".jpg"): File? {
    return try {
        val contentResolver = context.contentResolver
        val mimeType = contentResolver.getType(uri)
        val extension = if (mimeType != null) {
            MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)?.let { ".$it" } ?: defaultExtension
        } else {
            defaultExtension
        }

        val inputStream = contentResolver.openInputStream(uri) ?: return null
        val outputDir = context.cacheDir
        val outputFile = File.createTempFile("upload_", extension, outputDir)

        inputStream.use { input ->
            outputFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        if (outputFile.exists() && outputFile.length() > 0) {
            outputFile
        } else {
            Log.e("ChatUtils", "Copied file is empty or missing")
            outputFile.delete()
            null
        }
    } catch (e: Exception) {
        Log.e("ChatUtils", "Error copying content URI to local file", e)
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

    // System Back Press Handler
    BackHandler(enabled = !isSending) {
        onDismiss()
    }

    Dialog(
        onDismissRequest = { if (!isSending) onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !isSending,
            dismissOnClickOutside = false
        )
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

                // Top Bar with Back Action
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopStart)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        enabled = !isSending,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                }

                // Bottom Input & Send Row
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .imePadding(),
                    color = Color.Black.copy(alpha = 0.75f)
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
                                if (!isSending) {
                                    isSending = true
                                    val preparedFile = getFileFromContentUri(context, imageUri)
                                    if (preparedFile != null) {
                                        onSend(preparedFile, captionText)
                                    } else {
                                        Log.e("PhotoPreview", "Failed to resolve local file from URI")
                                        isSending = false
                                    }
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