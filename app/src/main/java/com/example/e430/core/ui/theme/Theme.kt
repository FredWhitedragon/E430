package com.example.e430.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = E430GoldMuted,
    onPrimary = Color(0xFF3E2E00),
    secondary = E430Gold,
    background = DarkBackground,
    surface = Color(0xFF142331),
    surfaceVariant = Color(0xFF203647),
    onSurface = Color(0xFFE8EEF3),
)

private val LightColorScheme = lightColorScheme(
    primary = E430Blue,
    onPrimary = Color.White,
    secondary = E430Gold,
    onSecondary = Color(0xFF302400),
    background = LightBackground,
    surface = Color.White,
    surfaceVariant = Color(0xFFE1E8ED),
    onSurface = Color(0xFF17232C),
)

@Composable
fun E430Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = E430Typography,
        content = content,
    )
}
