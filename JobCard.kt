package com.example.jobtown.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.jobtown.data.Job
import com.example.jobtown.ui.theme.*

@Composable
fun JobCard(
    job: Job,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    avatarUrl: String? = null
) {
    /*
     * Logo priority:
     * 1. avatarUrl passed into JobCard
     * 2. job.companyImageUrl
     * 3. Business icon
     */
    val targetImageUrl = avatarUrl
        ?.takeIf { it.isNotBlank() }
        ?: job.companyImageUrl?.takeIf { it.isNotBlank() }

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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.Top
        ) {

            // =========================================================
            // COMPANY LOGO
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

                if (!targetImageUrl.isNullOrBlank()) {

                    AsyncImage(
                        model = targetImageUrl,
                        contentDescription = "Employer Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(1.dp),
                        onError = {
                            // If the URL cannot be loaded,
                            // AsyncImage simply shows nothing.
                            // The surrounding Surface remains visible.
                        }
                    )

                } else {

                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {

                        Icon(
                            imageVector = Icons.Default.Business,
                            contentDescription = "Company",
                            tint = DeepGreenDark,
                            modifier = Modifier.size(22.dp)
                        )
                    }
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
                // TITLE + FEATURED
                // -----------------------------------------------------

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Text(
                        text = job.title.ifBlank {
                            "Job Title Placeholder"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark,
                        modifier = Modifier.weight(1f)
                    )

                    if (job.isFeatured == true) {

                        Spacer(
                            modifier = Modifier.width(4.dp)
                        )

                        Surface(
                            color = DeepGreenDark,
                            shape = RoundedCornerShape(3.dp)
                        ) {

                            Row(
                                modifier = Modifier.padding(
                                    horizontal = 4.dp,
                                    vertical = 1.dp
                                ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color.Yellow,
                                    modifier = Modifier.size(8.dp)
                                )

                                Spacer(
                                    modifier = Modifier.width(2.dp)
                                )

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

                Spacer(
                    modifier = Modifier.height(2.dp)
                )

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

                    Spacer(
                        modifier = Modifier.width(3.dp)
                    )

                    Text(
                        text = job.companyName.ifBlank {
                            "Company"
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextDark.copy(alpha = 0.8f),
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(
                        modifier = Modifier.width(6.dp)
                    )

                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = DeepGreenDark,
                        modifier = Modifier.size(10.dp)
                    )

                    Spacer(
                        modifier = Modifier.width(3.dp)
                    )

                    Text(
                        text = job.location.ifBlank {
                            "Location"
                        },
                        fontSize = 10.sp,
                        color = TextDark.copy(alpha = 0.6f),
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                // -----------------------------------------------------
                // JOB TYPE
                // -----------------------------------------------------

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
                            modifier = Modifier.padding(
                                horizontal = 4.dp,
                                vertical = 1.dp
                            )
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                // -----------------------------------------------------
                // SALARY
                // -----------------------------------------------------

                if (job.salary.isNotBlank()) {

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Surface(
                            color = DeepGreenDark.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(3.dp)
                        ) {

                            Text(
                                text = job.salary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepGreenDark,
                                modifier = Modifier.padding(
                                    horizontal = 4.dp,
                                    vertical = 1.dp
                                )
                            )
                        }
                    }
                }

                // -----------------------------------------------------
                // DESCRIPTION
                // -----------------------------------------------------

                if (job.description.isNotBlank()) {

                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )

                    Text(
                        text = job.description,
                        fontSize = 10.sp,
                        color = TextDark.copy(alpha = 0.7f),
                        maxLines = 2
                    )
                }
            }
        }
    }
}