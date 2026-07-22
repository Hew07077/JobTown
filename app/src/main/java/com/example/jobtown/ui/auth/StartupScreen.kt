package com.example.jobtown.ui.startup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.jobtown.Screen
import com.example.jobtown.ui.theme.DarkTextPurple
import kotlinx.coroutines.delay

@Composable
fun StartupScreen(navController: NavController) {
    // Automatically navigates to Login after 2.5 seconds
    LaunchedEffect(Unit) {
        delay(2500)
        navController.navigate(Screen.Login.route) {
            popUpTo(Screen.Startup.route) { inclusive = true }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Matching the green background color from your Figma layout
            .background(Color(0xFFA1C695)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            // "J" Logo Container Box
            Surface(
                modifier = Modifier.size(90.dp),
                shape = RoundedCornerShape(18.dp),
                color = Color.White,
                shadowElevation = 4.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "J",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFA1C695)
                    )
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Motto Text directly underneath
            Text(
                text = "Exploring\nOpportunities,\nBuilding Your\nFuture",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = DarkTextPurple,
                textAlign = TextAlign.Center,
                lineHeight = 32.sp
            )
        }
    }
}