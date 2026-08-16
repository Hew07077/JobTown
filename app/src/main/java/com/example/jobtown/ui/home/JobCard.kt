package com.example.jobtown.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jobtown.data.Job
import com.example.jobtown.ui.theme.*

/**
 * @param matchScore Optional 0-100 job-match percentage (see JobMatchUtils). When present, a
 * "Match" badge is shown and read aloud by screen readers as part of the card's label, so the
 * ranking signal isn't conveyed by color/position alone.
 */
@Composable
fun JobCard(
    job: Job,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    matchScore: Int? = null
) {
    val displayTitle = job.title.ifBlank { "Untitled Position" }
    val displayCompany = job.company.ifBlank { "Unknown Company" }
    val displayLocation = job.location.ifBlank { "Location Not Specified" }
    val displaySalary = job.salary.ifBlank { "Salary Undisclosed" }
    val displayType = job.type.ifBlank { "Full-time" }

    val matchColor = when {
        matchScore == null -> SageGreenDark
        matchScore >= 85 -> Color(0xFF1B7A3D)
        matchScore >= 65 -> DeepGreenDark
        matchScore >= 40 -> Color(0xFF9A7B1E)
        else -> Color(0xFF8A4A2C)
    }

    val cardDescription = buildString {
        append("$displayTitle at $displayCompany. ")
        append("$displayLocation. ")
        append("$displaySalary. $displayType. ")
        if (matchScore != null) append("$matchScore percent match to your profile.")
        append(" Double tap to view details.")
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .semantics {
                contentDescription = cardDescription
                role = Role.Button
            }
            .clickable(onClickLabel = "View job details") { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = SageGreenLight,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Business,
                            contentDescription = null,
                            tint = DeepGreenDark,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayTitle,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.semantics { heading() }
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = displayCompany,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextDark.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (matchScore != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = matchColor.copy(alpha = 0.12f),
                        modifier = Modifier.clearAndSetSemantics {}
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = matchColor,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "$matchScore%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = matchColor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = TextDark.copy(alpha = 0.5f),
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = displayLocation,
                    fontSize = 13.sp,
                    color = TextDark.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = displaySalary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepGreenDark
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SageGreenLight
                ) {
                    Text(
                        text = displayType,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        color = DeepGreenDark,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
