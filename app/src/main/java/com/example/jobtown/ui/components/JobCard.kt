package com.example.jobtown.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.jobtown.data.Job
import com.example.jobtown.data.repository.UserRepository
import com.example.jobtown.utils.JobMatchResult
import com.example.jobtown.ui.theme.*

@Composable
fun JobCard(
    job: Job,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSaved: Boolean = false,
    onToggleSave: (() -> Unit)? = null,
    avatarUrl: String? = null,
    matchResult: JobMatchResult? = null,
    showMatchInsights: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    var employerAvatarUrl by remember { mutableStateOf<String?>(null) }

    // Fetch the employer's user profile photo from Supabase using job.employerId
    LaunchedEffect(job.employerId) {
        if (!job.employerId.isNullOrBlank() && employerAvatarUrl == null) {
            val employerUser = UserRepository.fetchUserById(job.employerId)
            employerAvatarUrl = employerUser?.avatarUrl
        }
    }

    // Resolution priority: explicit avatarUrl parameter -> job.companyImageUrl -> employer's avatarUrl from database
    val activePhotoUrl = avatarUrl?.takeIf { it.isNotBlank() }
        ?: job.companyImageUrl?.takeIf { it.isNotBlank() }
        ?: employerAvatarUrl?.takeIf { it.isNotBlank() }

    // Dynamic color coding based on JobMatchResult score ranges
    val matchColor = remember(matchResult?.score) {
        when {
            (matchResult?.score ?: 0) >= 85 -> DeepGreenDark
            (matchResult?.score ?: 0) >= 65 -> Color(0xFF2E7D32)
            (matchResult?.score ?: 0) >= 40 -> Color(0xFFEF6C00)
            else -> Color(0xFF757575)
        }
    }

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (job.isFeatured == true) {
                SageGreenMain.copy(alpha = 0.12f)
            } else {
                Color.White
            }
        ),
        border = BorderStroke(
            width = if (job.isFeatured == true) 1.5.dp else 1.dp,
            color = if (job.isFeatured == true) {
                DeepGreenDark.copy(alpha = 0.8f)
            } else {
                Color(0xFFE2E8F0)
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (job.isFeatured == true) 4.dp else 1.5.dp,
            pressedElevation = 6.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // TOP SECTION: Logo, Title, and Right-hand Badges/Bookmark Column
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.Top
                ) {
                    // LOGO / AVATAR CONTAINER
                    Surface(
                        modifier = Modifier.size(52.dp),
                        shape = CircleShape,
                        color = Color.White,
                        shadowElevation = 3.dp,
                        border = BorderStroke(0.5.dp, SageGreenDark.copy(alpha = 0.25f))
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!activePhotoUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = activePhotoUrl,
                                    contentDescription = "Company Logo",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Business,
                                    contentDescription = "Company Icon",
                                    tint = DeepGreenDark,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // JOB TITLE, COMPANY & LOCATION
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = job.title.ifBlank { "Job Title Placeholder" },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark,
                            maxLines = 1
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = job.companyName.ifBlank { "Company Name" },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextDark.copy(alpha = 0.8f),
                                maxLines = 1,
                                modifier = Modifier.weight(1f, fill = false)
                            )

                            Text(
                                text = " • ",
                                fontSize = 12.sp,
                                color = TextDark.copy(alpha = 0.4f)
                            )

                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = DeepGreenDark,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = job.location.ifBlank { "Location" },
                                fontSize = 12.sp,
                                color = TextDark.copy(alpha = 0.6f),
                                maxLines = 1,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // RIGHT COLUMN: Match/Featured Badges stacked above the Saved/Bookmark Button & Label
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (matchResult != null) {
                        Surface(
                            color = matchColor.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(0.5.dp, matchColor.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoGraph,
                                    contentDescription = null,
                                    tint = matchColor,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = "${matchResult.score}% • ${matchResult.label}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = matchColor
                                )
                            }
                        }
                    }

                    if (job.isFeatured == true) {
                        Surface(
                            color = DeepGreenDark,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color.Yellow,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "FEATURED",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // Saved / Bookmark Button with label positioned directly below match score
                    if (onToggleSave != null) {
                        Surface(
                            color = if (isSaved) DeepGreenDark.copy(alpha = 0.1f) else Color.Transparent,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(
                                0.5.dp,
                                if (isSaved) DeepGreenDark.copy(alpha = 0.4f) else Color(0xFFCBD5E1)
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(onClick = onToggleSave)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    contentDescription = if (isSaved) "Unsave job" else "Save job",
                                    tint = if (isSaved) DeepGreenDark else TextDark.copy(alpha = 0.5f),
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = if (isSaved) "Saved" else "Save",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSaved) DeepGreenDark else TextDark.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // TAGS ROW: Job Type and Salary Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (job.jobType.isNotBlank()) {
                    Surface(
                        color = SageGreenMain.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = job.jobType,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DeepGreenDark,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                if (job.salary.isNotBlank()) {
                    Surface(
                        color = DeepGreenDark.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = job.salary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepGreenDark,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // DESCRIPTION SNIPPET
            if (job.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = job.description,
                    fontSize = 12.sp,
                    color = TextDark.copy(alpha = 0.75f),
                    maxLines = 2,
                    lineHeight = 16.sp
                )
            }

            // MATCH INSIGHTS DROPDOWN SECTION
            if (showMatchInsights && matchResult != null && matchResult.reasons.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .clickable { expanded = !expanded },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = DeepGreenDark,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = if (expanded) "Hide match insights" else "Why you match (${matchResult.reasons.size} highlights)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DeepGreenDark
                        )
                    }
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = DeepGreenDark,
                        modifier = Modifier.size(18.dp)
                    )
                }

                AnimatedVisibility(
                    visible = expanded,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF8FAFC))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Display match reasons provided by JobMatchUtils
                        matchResult.reasons.forEach { reason ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "• ",
                                    fontSize = 11.sp,
                                    color = DeepGreenDark,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = reason,
                                    fontSize = 11.sp,
                                    color = TextDark.copy(alpha = 0.85f)
                                )
                            }
                        }

                        // Showcase Matched Skills vs Missing Skills efficiently
                        if (matchResult.matchedSkills.isNotEmpty()) {
                            Text(
                                text = "Matched Skills: ${matchResult.matchedSkills.joinToString(", ")}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF2E7D32),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        if (matchResult.missingSkills.isNotEmpty()) {
                            Text(
                                text = "Skills to learn: ${matchResult.missingSkills.take(3).joinToString(", ")}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFEF6C00)
                            )
                        }
                    }
                }
            }
        }
    }
}