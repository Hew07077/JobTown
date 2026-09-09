package com.example.jobtown.ui.applied

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jobtown.data.model.JobApplication
import com.example.jobtown.ui.theme.DeepGreenDark
import com.example.jobtown.ui.theme.SageGreenDark
import com.example.jobtown.ui.theme.SageGreenLight
import com.example.jobtown.ui.theme.SageGreenMain
import com.example.jobtown.ui.theme.TextDark

internal val AppliedDividerColor = Color(0xFFE6EDE4)

internal fun formatCertificatesForDisplay(certificates: String): Pair<String, String?> {
    val urlRegex = Regex("https?://\\S+")
    val firstUrl = urlRegex.find(certificates)?.value
    val displayText = certificates.lines().joinToString("\n") { line ->
        val url = urlRegex.find(line)?.value
        if (url == null) {
            line
        } else {
            val withoutUrl = line.replace(url, "").trim().trim('-', '—', '·', ' ')
            if (withoutUrl.isBlank()) "Certificate (PDF)" else "$withoutUrl · Certificate (PDF)"
        }
    }
    return displayText to firstUrl
}

// In ApplicationUi_8.kt

internal fun applicationStatusBackground(status: String): Color {
    return when (status.lowercase()) {
        "scheduled", "interview scheduled", "viewed" -> SageGreenMain.copy(alpha = 0.45f)
        "considered", "interview" -> Color(0xFFE3F2FD)
        "rejected", "cancelled", "deletedbyemployer", "deletedbyseeker", "deleted" -> Color(0xFFFFEBEE)
        "offered", "accepted" -> Color(0xFFE8F5E9)
        else -> SageGreenMain.copy(alpha = 0.35f)
    }
}

internal fun applicationStatusTextColor(status: String): Color {
    return when (status.lowercase()) {
        "rejected", "cancelled", "deletedbyemployer", "deletedbyseeker", "deleted" -> Color(0xFFC62828)
        "offered", "accepted" -> Color(0xFF2E7D32)
        "considered", "interview" -> Color(0xFF1565C0)
        else -> DeepGreenDark
    }
}

internal fun JobApplication.isClosed(): Boolean {
    return status.equals("rejected", ignoreCase = true) ||
            status.equals("expired", ignoreCase = true) ||
            status.equals("cancelled", ignoreCase = true)
}

internal fun JobApplication.applicationTab(): ApplicationTab {
    return when (status.trim().lowercase()) {
        "", "pending", "applied", "submitted" -> ApplicationTab.PENDING
        "viewed" -> ApplicationTab.VIEWED
        "scheduled", "interview scheduled" -> ApplicationTab.SCHEDULED
        "considered", "interview" -> ApplicationTab.CONSIDERED
        "offered", "accepted" -> ApplicationTab.OFFERED
        "rejected" -> ApplicationTab.REJECTED
        "cancelled", "expired" -> ApplicationTab.CANCELLED
        else -> ApplicationTab.PENDING
    }
}


internal fun JobApplication.canCancel(): Boolean {
    val normalized = status.trim().lowercase()
    if (normalized.isBlank()) return true
    return normalized in setOf(
        "pending",
        "applied",
        "submitted",
        "viewed",
        "shortlisted",
        "considered",
        "interview",
        "offered"
    )
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
    value: String,
    modifier: Modifier = Modifier,
    valueFontSize: TextUnit = 15.sp,
    valueLineHeight: TextUnit = TextUnit.Unspecified,
    valueColor: Color = TextDark,
    onClick: (() -> Unit)? = null
) {
    val clickableModifier = if (onClick != null) {
        modifier
            .fillMaxWidth()
            .clickable { onClick() }
    } else {
        modifier.fillMaxWidth()
    }

    Row(
        modifier = clickableModifier,
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
                fontSize = valueFontSize,
                lineHeight = valueLineHeight,
                fontWeight = FontWeight.SemiBold,
                color = valueColor
            )
        }
    }
}