package com.example.jobtown.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
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
import com.example.jobtown.utils.JobMatchResult
import com.example.jobtown.ui.theme.*

@Composable
fun JobCard(
    job: Job,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    avatarUrl: String? = null,
    matchResult: JobMatchResult? = null,
    showMatchInsights: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }

    // 1. Resolve image URL with fallback generation if empty
    val rawImageUrl = avatarUrl?.takeIf { it.isNotBlank() } ?: job.companyImageUrl

    // GUARANTEE LOGO APPEARANCE: If the URL is blank, generate an automatic initials avatar badge using the company name!
    val targetImageUrl = rawImageUrl?.takeIf { it.isNotBlank() } ?: run {
        val companySafeName = job.companyName.ifBlank { "Company" }.trim()
        "https://ui-avatars.com/api/?name=${java.net.URLEncoder.encode(companySafeName, "UTF-8")}&background=E8F5E9&color=1B5E20&bold=true"
    }

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (job.isFeatured == true) {
                SageGreenMain.copy(alpha = 0.25f)
            } else {
                Color.White
            }
        ),
        border = BorderStroke(
            width = if (job.isFeatured == true) 1.dp else 0.5.dp,
            color = if (job.isFeatured == true) {
                DeepGreenDark
            } else {
                Color(0xFFE0E0E0)
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {

                // =========================================================
                // COMPANY LOGO CONTAINER
                // =========================================================
                Surface(
                    modifier = Modifier.size(42.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = SageGreenMain.copy(alpha = 0.3f),
                    border = BorderStroke(
                        0.5.dp,
                        DeepGreenDark.copy(alpha = 0.2f)
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = targetImageUrl,
                            contentDescription = "Employer Logo",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(6.dp)),
                            contentScale = ContentScale.Crop,
                            onError = {
                                // Logs out if image loading fails for debugging
                                val error = it.result.throwable
                                android.util.Log.e("JobCardLogo", "Failed to load image from: $targetImageUrl", error)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // =========================================================
                // JOB DETAILS
                // =========================================================
                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    // -----------------------------------------------------
                    // TITLE + BADGES (Match Score & Featured)
                    // -----------------------------------------------------
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = job.title.ifBlank { "Job Title Placeholder" },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark,
                            modifier = Modifier.weight(1f)
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Match Score Badge
                            if (matchResult != null) {
                                val matchColor = when {
                                    matchResult.score >= 85 -> DeepGreenDark
                                    matchResult.score >= 65 -> Color(0xFF2E7D32)
                                    matchResult.score >= 40 -> Color(0xFFEF6C00)
                                    else -> Color(0xFF757575)
                                }

                                Surface(
                                    color = matchColor.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(3.dp),
                                    border = BorderStroke(0.5.dp, matchColor.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = "${matchResult.score}% • ${matchResult.label}",
                                        fontSize = 7.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = matchColor,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }

                            // Featured Badge
                            if (job.isFeatured == true) {
                                Surface(
                                    color = DeepGreenDark,
                                    shape = RoundedCornerShape(3.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = Color.Yellow,
                                            modifier = Modifier.size(8.dp)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = "FEATURED",
                                            fontSize = 7.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // -----------------------------------------------------
                    // COMPANY + LOCATION
                    // -----------------------------------------------------
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Business,
                            contentDescription = null,
                            tint = DeepGreenDark,
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = job.companyName.ifBlank { "Company" },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextDark.copy(alpha = 0.8f),
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = DeepGreenDark,
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = job.location.ifBlank { "Location" },
                            fontSize = 10.sp,
                            color = TextDark.copy(alpha = 0.6f),
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // -----------------------------------------------------
                    // JOB TYPE & SALARY CHIPS
                    // -----------------------------------------------------
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (job.jobType.isNotBlank()) {
                            Surface(
                                color = SageGreenMain.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(3.dp)
                            ) {
                                Text(
                                    text = job.jobType,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DeepGreenDark,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }

                        if (job.salary.isNotBlank()) {
                            Surface(
                                color = DeepGreenDark.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(3.dp)
                            ) {
                                Text(
                                    text = job.salary,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepGreenDark,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    // -----------------------------------------------------
                    // DESCRIPTION
                    // -----------------------------------------------------
                    if (job.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = job.description,
                            fontSize = 10.sp,
                            color = TextDark.copy(alpha = 0.7f),
                            maxLines = 2
                        )
                    }
                }
            }

            // =========================================================
            // MATCH INSIGHTS EXPANDABLE FOOTER
            // =========================================================
            if (showMatchInsights && matchResult != null && matchResult.reasons.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 0.5.dp)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .clickable { expanded = !expanded },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (expanded) "Hide match insights" else "Why you match (${matchResult.reasons.size} reasons)",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DeepGreenDark
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = DeepGreenDark,
                        modifier = Modifier.size(12.dp)
                    )
                }

                AnimatedVisibility(visible = expanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        matchResult.reasons.forEach { reason ->
                            Text(
                                text = "• $reason",
                                fontSize = 8.sp,
                                color = TextDark.copy(alpha = 0.8f)
                            )
                        }

                        if (matchResult.missingSkills.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Skills to learn: ${matchResult.missingSkills.take(3).joinToString(", ")}",
                                fontSize = 8.sp,
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