package com.example.jobtown.ui.applied

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jobtown.data.model.JobApplication
import com.example.jobtown.ui.theme.DeepGreenDark
import com.example.jobtown.ui.theme.SageGreenDark
import com.example.jobtown.ui.theme.SageGreenLight
import com.example.jobtown.ui.theme.SageGreenMain
import com.example.jobtown.ui.theme.TextDark

internal val AppliedDividerColor = Color(0xFFE6EDE4)

internal fun applicationStatusBackground(status: String): Color {
    return when (status.lowercase()) {
        "shortlisted", "viewed" -> SageGreenMain.copy(alpha = 0.45f)
        "interview" -> SageGreenDark.copy(alpha = 0.2f)
        "rejected", "cancelled" -> Color(0xFFFFEBEE)
        "accepted" -> Color(0xFFE8F5E9)
        else -> SageGreenMain.copy(alpha = 0.35f)
    }
}
//
internal fun applicationStatusTextColor(status: String): Color {
    return when (status.lowercase()) {
        "rejected", "cancelled" -> Color(0xFFC62828)
        "accepted" -> Color(0xFF2E7D32)
        else -> DeepGreenDark
    }
}

internal fun JobApplication.isClosed(): Boolean {
    return status.equals("rejected", ignoreCase = true) ||
        status.equals("expired", ignoreCase = true) ||
        status.equals("cancelled", ignoreCase = true)
}

internal fun JobApplication.canCancel(): Boolean {
    val normalized = status.lowercase()
    return normalized in setOf("pending", "viewed", "shortlisted", "interview")
}

@Composable
internal fun ApplicationStatusBadge(status: String) {
    val text = status.ifBlank { "Pending" }
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = applicationStatusBackground(text)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = applicationStatusTextColor(text),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
internal fun AppliedDivider(verticalPadding: Dp = 14.dp) {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = verticalPadding),
        thickness = 1.dp,
        color = AppliedDividerColor
    )
}

@Composable
internal fun AppliedSectionTitle(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = DeepGreenDark
    )
}

@Composable
internal fun ApplicationDetailRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = SageGreenLight,
            modifier = Modifier.size(38.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = SageGreenDark,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = TextDark.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value.ifBlank { "Not provided" },
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextDark
            )
        }
    }
}
