package com.clickflow.android.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Calm, minimal palette (Step 59). Soft indigo/teal accents on near-neutral surfaces — no acid
 * colors, gentle contrast in both light and dark.
 */
private val LightColors = lightColorScheme(
    primary = Color(0xFF4A6CD4),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDDE4FB),
    onPrimaryContainer = Color(0xFF12224D),
    secondary = Color(0xFF52606E),
    secondaryContainer = Color(0xFFE2E8EF),
    error = Color(0xFFC04A4A),
    onError = Color(0xFFFFFFFF),
    background = Color(0xFFF7F8FB),
    onBackground = Color(0xFF1A1C1E),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFEDEFF3),
    onSurfaceVariant = Color(0xFF45474A),
    outline = Color(0xFFC3C7CE),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFAEC2FF),
    onPrimary = Color(0xFF112048),
    primaryContainer = Color(0xFF2C3E6B),
    onPrimaryContainer = Color(0xFFDDE4FB),
    secondary = Color(0xFFBCC7D6),
    secondaryContainer = Color(0xFF3A434E),
    error = Color(0xFFE49A9A),
    onError = Color(0xFF3A1212),
    background = Color(0xFF121316),
    onBackground = Color(0xFFE3E2E6),
    surface = Color(0xFF1A1C1F),
    onSurface = Color(0xFFE3E2E6),
    surfaceVariant = Color(0xFF42474E),
    onSurfaceVariant = Color(0xFFC3C7CE),
    outline = Color(0xFF8C9199),
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
