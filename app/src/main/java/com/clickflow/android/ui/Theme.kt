package com.clickflow.android.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF151515),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE1DDD4),
    onPrimaryContainer = Color(0xFF171717),
    secondary = Color(0xFF625B51),
    onSecondary = Color.White,
    background = Color(0xFFE9E4DA),
    onBackground = Color(0xFF151515),
    surface = Color(0xFFF3EFE7),
    onSurface = Color(0xFF151515),
    surfaceVariant = Color(0xFFD8D1C5),
    onSurfaceVariant = Color(0xFF4F4B45),
    outline = Color(0xFFC3BAAC),
    error = Color(0xFFC44535),
    onError = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFF1EEE8),
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF302C26),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFFD6CDBE),
    background = Color(0xFF11100E),
    onBackground = Color(0xFFF1EEE8),
    surface = Color(0xFF1A1815),
    onSurface = Color(0xFFF1EEE8),
    surfaceVariant = Color(0xFF2A2722),
    onSurfaceVariant = Color(0xFFC8C0B4),
    outline = Color(0xFF4D463E),
    error = Color(0xFFFF8D7D),
)

@Composable
fun ClickFlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
