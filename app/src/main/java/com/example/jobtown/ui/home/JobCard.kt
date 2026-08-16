package com.example.jobtown.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jobtown.data.Job
import com.example.jobtown.ui.theme.*

/**
 * Defines layout configurations for [JobCard].
 */
enum class JobCardVariant {
    COMPACT,
    STANDARD,
    DETAILED
}

/**
 * Defines elevation levels for [JobCard].
 */
enum class CardElevation {
    NONE,
    LOW,
    MEDIUM,
    HIGH
}

/**
 * Enhanced JobCard with multiple display variants, animations, and rich interactions
 */
@Composable
fun JobCard(
    job: Job,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: JobCardVariant = JobCardVariant.STANDARD,
    showSaveButton: Boolean = true,
    isSaved: Boolean = false,
    onSaveClick: (() -> Unit)? = null,
    showShareButton: Boolean = false,
    onShareClick: (() -> Unit)? = null,
    showApplyButton: Boolean = false,
    onApplyClick: (() -> Unit)? = null,
    isAnimated: Boolean = true,
    elevation: CardElevation = CardElevation.MEDIUM,
    showFeaturedBadge: Boolean = true,
    companyImageUrl: String? = null
) {
    val displayTitle = job.title.ifBlank { "Untitled Position" }
    val displayCompany = job.company.ifBlank { "Unknown Company" }
    val displayLocation = job.location.ifBlank { "Location Not Specified" }
    val displaySalary = job.salary.ifBlank { "Salary Undisclosed" }
    val displayType = job.type.ifBlank { "Full-time" }
    val displayDescription = job.description.ifBlank { "No description available" }

    // Animation states
    var isVisible by remember { mutableStateOf(!isAnimated) }
    LaunchedEffect(isAnimated) {
        if (isAnimated) {
            isVisible = true
        }
    }

    // Company initial for fallback avatar
    val companyInitial = displayCompany.take(1).uppercase()

    // Card description for accessibility
    val cardDescription = buildString {
        append("$displayTitle at $displayCompany. ")
        append("$displayLocation. ")
        append("$displaySalary. $displayType. ")
        if (job.isFeatured == true) append(" Featured job.")
        append(" Double tap to view details.")
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = when (elevation) {
                    CardElevation.NONE -> 0.dp
                    CardElevation.LOW -> 2.dp
                    CardElevation.MEDIUM -> 4.dp
                    CardElevation.HIGH -> 8.dp
                },
                shape = RoundedCornerShape(16.dp),
                clip = false
            )
            .clip(RoundedCornerShape(16.dp))
            .semantics {
                contentDescription = cardDescription
                role = Role.Button
            }
            .clickable(onClickLabel = "View job details") { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White,
            disabledContainerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = when (elevation) {
                CardElevation.NONE -> 0.dp
                CardElevation.LOW -> 1.dp
                CardElevation.MEDIUM -> 2.dp
                CardElevation.HIGH -> 4.dp
            }
        )
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(animationSpec = tween(300)) +
                    slideInVertically(
                        initialOffsetY = { it / 2 },
                        animationSpec = tween(300)
                    ),
            exit = fadeOut() + slideOutVertically()
        ) {
            when (variant) {
                JobCardVariant.COMPACT -> CompactJobCardContent(
                    displayTitle = displayTitle,
                    displayCompany = displayCompany,
                    displayLocation = displayLocation,
                    displaySalary = displaySalary,
                    displayType = displayType,
                    companyInitial = companyInitial,
                    companyImageUrl = companyImageUrl,
                    showSaveButton = showSaveButton,
                    isSaved = isSaved,
                    onSaveClick = onSaveClick,
                    showFeaturedBadge = showFeaturedBadge && job.isFeatured == true
                )
                JobCardVariant.STANDARD -> StandardJobCardContent(
                    displayTitle = displayTitle,
                    displayCompany = displayCompany,
                    displayLocation = displayLocation,
                    displaySalary = displaySalary,
                    displayType = displayType,
                    displayDescription = displayDescription,
                    companyInitial = companyInitial,
                    companyImageUrl = companyImageUrl,
                    showSaveButton = showSaveButton,
                    isSaved = isSaved,
                    onSaveClick = onSaveClick,
                    showShareButton = showShareButton,
                    onShareClick = onShareClick,
                    showApplyButton = showApplyButton,
                    onApplyClick = onApplyClick,
                    showFeaturedBadge = showFeaturedBadge && job.isFeatured == true,
                    skills = job.skills ?: emptyList()
                )
                JobCardVariant.DETAILED -> DetailedJobCardContent(
                    displayTitle = displayTitle,
                    displayCompany = displayCompany,
                    displayLocation = displayLocation,
                    displaySalary = displaySalary,
                    displayType = displayType,
                    displayDescription = displayDescription,
                    companyInitial = companyInitial,
                    companyImageUrl = companyImageUrl,
                    showSaveButton = showSaveButton,
                    isSaved = isSaved,
                    onSaveClick = onSaveClick,
                    showShareButton = showShareButton,
                    onShareClick = onShareClick,
                    showApplyButton = showApplyButton,
                    onApplyClick = onApplyClick,
                    showFeaturedBadge = showFeaturedBadge && job.isFeatured == true,
                    skills = job.skills ?: emptyList(),
                    requirements = job.requirements ?: emptyList()
                )
            }
        }
    }
}

// ==================== Content Composables ====================

@Composable
private fun CompactJobCardContent(
    displayTitle: String,
    displayCompany: String,
    displayLocation: String,
    displaySalary: String,
    displayType: String,
    companyInitial: String,
    companyImageUrl: String?,
    showSaveButton: Boolean,
    isSaved: Boolean,
    onSaveClick: (() -> Unit)?,
    showFeaturedBadge: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CompanyAvatar(
            initial = companyInitial,
            imageUrl = companyImageUrl,
            isFeatured = showFeaturedBadge,
            size = 40.dp
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayTitle,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() }
            )

            Text(
                text = displayCompany,
                fontSize = 12.sp,
                color = TextDark.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InfoChip(
                    icon = Icons.Outlined.LocationOn,
                    text = displayLocation.take(15),
                    size = "small"
                )
                InfoChip(
                    icon = Icons.Outlined.AttachMoney,
                    text = displaySalary.take(12),
                    size = "small"
                )
                JobTypeChip(type = displayType, size = "small")
            }
        }

        if (showSaveButton && onSaveClick != null) {
            IconButton(
                onClick = onSaveClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.Bookmark,
                    contentDescription = if (isSaved) "Remove from saved" else "Save job",
                    tint = if (isSaved) DeepGreenDark else TextDark.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun StandardJobCardContent(
    displayTitle: String,
    displayCompany: String,
    displayLocation: String,
    displaySalary: String,
    displayType: String,
    displayDescription: String,
    companyInitial: String,
    companyImageUrl: String?,
    showSaveButton: Boolean,
    isSaved: Boolean,
    onSaveClick: (() -> Unit)?,
    showShareButton: Boolean,
    onShareClick: (() -> Unit)?,
    showApplyButton: Boolean,
    onApplyClick: (() -> Unit)?,
    showFeaturedBadge: Boolean,
    skills: List<String>
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
            CompanyAvatar(
                initial = companyInitial,
                imageUrl = companyImageUrl,
                isFeatured = showFeaturedBadge,
                size = 48.dp
            )

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
                Text(
                    text = displayCompany,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextDark.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (displayDescription.isNotBlank() && displayDescription != "No description available") {
            Text(
                text = displayDescription,
                fontSize = 13.sp,
                color = TextDark.copy(alpha = 0.7f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        if (skills.isNotEmpty()) {
            SkillsRow(skills = skills.take(3))
            Spacer(modifier = Modifier.height(8.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                InfoChip(
                    icon = Icons.Outlined.LocationOn,
                    text = displayLocation,
                    size = "medium"
                )
                InfoChip(
                    icon = Icons.Outlined.AttachMoney,
                    text = displaySalary,
                    size = "medium"
                )
                JobTypeChip(type = displayType, size = "medium")
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (showShareButton && onShareClick != null) {
                    IconButton(
                        onClick = onShareClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = "Share job",
                            tint = TextDark.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (showSaveButton && onSaveClick != null) {
                    IconButton(
                        onClick = onSaveClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.Bookmark,
                            contentDescription = if (isSaved) "Remove from saved" else "Save job",
                            tint = if (isSaved) DeepGreenDark else TextDark.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        if (showApplyButton && onApplyClick != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onApplyClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SageGreenMain,
                    contentColor = DeepGreenDark
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Apply Now",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun DetailedJobCardContent(
    displayTitle: String,
    displayCompany: String,
    displayLocation: String,
    displaySalary: String,
    displayType: String,
    displayDescription: String,
    companyInitial: String,
    companyImageUrl: String?,
    showSaveButton: Boolean,
    isSaved: Boolean,
    onSaveClick: (() -> Unit)?,
    showShareButton: Boolean,
    onShareClick: (() -> Unit)?,
    showApplyButton: Boolean,
    onApplyClick: (() -> Unit)?,
    showFeaturedBadge: Boolean,
    skills: List<String>,
    requirements: List<String>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            CompanyAvatar(
                initial = companyInitial,
                imageUrl = companyImageUrl,
                isFeatured = showFeaturedBadge,
                size = 56.dp
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayTitle,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    text = displayCompany,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextDark.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (showShareButton && onShareClick != null) {
                    IconButton(
                        onClick = onShareClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = "Share job",
                            tint = TextDark.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                if (showSaveButton && onSaveClick != null) {
                    IconButton(
                        onClick = onSaveClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.Bookmark,
                            contentDescription = if (isSaved) "Remove from saved" else "Save job",
                            tint = if (isSaved) DeepGreenDark else TextDark.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        InfoGrid(
            location = displayLocation,
            salary = displaySalary,
            type = displayType
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (displayDescription.isNotBlank()) {
            Text(
                text = "About this position:",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextDark
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = displayDescription,
                fontSize = 13.sp,
                color = TextDark.copy(alpha = 0.7f),
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (skills.isNotEmpty()) {
            Text(
                text = "Skills:",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextDark
            )
            Spacer(modifier = Modifier.height(4.dp))
            SkillsRow(skills = skills)
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (requirements.isNotEmpty()) {
            Text(
                text = "Requirements:",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextDark
            )
            Spacer(modifier = Modifier.height(4.dp))
            RequirementsList(requirements = requirements.take(3))
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (showApplyButton && onApplyClick != null) {
            Button(
                onClick = onApplyClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SageGreenMain,
                    contentColor = DeepGreenDark
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Apply Now",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

// ==================== Sub-Components ====================

@Composable
private fun CompanyAvatar(
    initial: String,
    imageUrl: String?,
    isFeatured: Boolean,
    size: Dp
) {
    Box {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = SageGreenLight,
            modifier = Modifier.size(size)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = initial,
                    fontSize = (size.value * 0.4f).sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepGreenDark
                )
            }
        }
        if (isFeatured) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size((size.value * 0.35f).dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF6B35))
                    .border(1.5.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = "Featured",
                    tint = Color.White,
                    modifier = Modifier.size((size.value * 0.18f).dp)
                )
            }
        }
    }
}

@Composable
private fun InfoChip(
    icon: ImageVector,
    text: String,
    size: String = "medium"
) {
    val fontSize = if (size == "small") 11.sp else 12.sp
    val iconSize = if (size == "small") 12.dp else 14.dp

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TextDark.copy(alpha = 0.4f),
            modifier = Modifier.size(iconSize)
        )
        Text(
            text = text,
            fontSize = fontSize,
            color = TextDark.copy(alpha = 0.6f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun JobTypeChip(type: String, size: String = "medium") {
    val fontSize = if (size == "small") 10.sp else 11.sp
    val padding = if (size == "small") 4.dp else 6.dp

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = SageGreenLight.copy(alpha = 0.5f)
    ) {
        Text(
            text = type,
            modifier = Modifier.padding(horizontal = padding, vertical = padding / 2),
            fontSize = fontSize,
            color = DeepGreenDark,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun InfoGrid(
    location: String,
    salary: String,
    type: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Outlined.LocationOn,
                contentDescription = null,
                tint = TextDark.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = location,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = TextDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Location",
                fontSize = 10.sp,
                color = TextDark.copy(alpha = 0.4f)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Outlined.AttachMoney,
                contentDescription = null,
                tint = TextDark.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = salary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = TextDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Salary",
                fontSize = 10.sp,
                color = TextDark.copy(alpha = 0.4f)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Outlined.Business,
                contentDescription = null,
                tint = TextDark.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = type,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = TextDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Type",
                fontSize = 10.sp,
                color = TextDark.copy(alpha = 0.4f)
            )
        }
    }
}

@Composable
private fun SkillsRow(skills: List<String>) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        skills.take(3).forEach { skill ->
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = SageGreenLight.copy(alpha = 0.3f),
                border = androidx.compose.foundation.BorderStroke(
                    0.5.dp,
                    SageGreenMain.copy(alpha = 0.3f)
                )
            ) {
                Text(
                    text = skill,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    fontSize = 11.sp,
                    color = DeepGreenDark
                )
            }
        }
        if (skills.size > 3) {
            Text(
                text = "+${skills.size - 3}",
                fontSize = 11.sp,
                color = TextDark.copy(alpha = 0.5f),
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .align(Alignment.CenterVertically)
            )
        }
    }
}

@Composable
private fun RequirementsList(requirements: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        requirements.forEach { requirement ->
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "•",
                    fontSize = 14.sp,
                    color = DeepGreenDark
                )
                Text(
                    text = requirement,
                    fontSize = 12.sp,
                    color = TextDark.copy(alpha = 0.7f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (requirements.size > 3) {
            Text(
                text = "+${requirements.size - 3} more requirements",
                fontSize = 12.sp,
                color = TextDark.copy(alpha = 0.5f)
            )
        }
    }
}