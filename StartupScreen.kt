package com.example.jobtown.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jobtown.R
import com.example.jobtown.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun StartupScreen(
    onNavigateToLogin: () -> Unit
) {

    LaunchedEffect(Unit) {
        delay(500)
        onNavigateToLogin()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SageGreenMain)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // "J" Logo Container
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(BackgroundWhite)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_jobtown_logo),
                    contentDescription = "JobTown Logo",
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // App Name
            Text(
                text = "JobTown",
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                color = DeepGreenDark,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Motto / Tagline
            Text(
                text = "Exploring Opportunities,\nBuilding Your Future.",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = DarkTextPurple,
                textAlign = TextAlign.Center,
                lineHeight = 26.sp
            )
        }
    }
}