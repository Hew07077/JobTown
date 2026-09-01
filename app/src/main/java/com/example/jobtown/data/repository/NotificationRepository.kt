package com.example.jobtown.data.repository

import android.util.Log
import com.example.jobtown.data.SupabaseClient
import com.example.jobtown.data.model.AppNotification
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

object NotificationRepository {

    private const val TAG = "NotificationRepository"

    @Serializable
    private data class NewNotificationPayload(
        @SerialName("user_id") val userId: String,
        @SerialName("title") val title: String,
        @SerialName("body") val body: String,
        @SerialName("type") val type: String,
        @SerialName("related_id") val relatedId: String = "",
        @SerialName("is_read") val isRead: Boolean = false
    )

    suspend fun getNotifications(userId: String): List<AppNotification> = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext emptyList()
        try {
            SupabaseClient.client.from("notifications")
                .select {
                    filter { eq("user_id", userId) }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<AppNotification>()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching notifications: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun notifyUser(
        userId: String,
        title: String,
        body: String,
        type: String,
        relatedId: String = ""
    ): Boolean = withContext(Dispatchers.IO) {
        if (userId.isBlank() || title.isBlank()) return@withContext false
        try {
            SupabaseClient.client.from("notifications").insert(
                NewNotificationPayload(
                    userId = userId,
                    title = title,
                    body = body,
                    type = type,
                    relatedId = relatedId
                )
            )
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting notification: ${e.message}", e)
            false
        }
    }

    suspend fun markAsRead(notificationId: String): Boolean = withContext(Dispatchers.IO) {
        if (notificationId.isBlank()) return@withContext false
        try {
            SupabaseClient.client.from("notifications").update(
                mapOf("is_read" to true)
            ) {
                filter { eq("id", notificationId) }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error marking notification read: ${e.message}", e)
            false
        }
    }

    suspend fun markAllAsRead(userId: String): Boolean = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext false
        try {
            SupabaseClient.client.from("notifications").update(
                mapOf("is_read" to true)
            ) {
                filter { eq("user_id", userId); eq("is_read", false) }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error marking all notifications read: ${e.message}", e)
            false
        }
    }
}
