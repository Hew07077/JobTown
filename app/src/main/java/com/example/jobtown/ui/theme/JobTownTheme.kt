package com.example.jobtown.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = SageGreenDark,
    surface = SageGreenLight,
    background = BackgroundWhite,
    onPrimary = BackgroundWhite,
    onSurface = TextDark
)

@Composable
fun JobTownTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}