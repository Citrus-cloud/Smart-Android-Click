package com.clickflow.android.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Ink = Color(0xFF101114)
private val Paper = Color(0xFFFAFAF7)
private val Card = Color(0xFFFFFFFF)
private val Muted = Color(0xFF6F737A)
private val Line = Color(0xFFE6E3DD)
private val Accent = Color(0xFF111111)
private val AccentSoft = Color(0xFFEDEBE6)

private val LightColors = lightColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    primaryContainer = AccentSoft,
    onPrimaryContainer = Ink,
    secondary = Color(0xFF6B6257),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF1EAE2),
    onSecondaryContainer = Ink,
    background = Paper,
    onBackground = Ink,
    surface = Card,
    onSurface = Ink,
    surfaceVariant = Color(0xFFF1F0EC),
    onSurfaceVariant = Muted,
    outline = Line,
    error = Color(0xFFD34848),
    onError = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF2B2C30),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFFE0D7C8),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF33302B),
    onSecondaryContainer = Color.White,
    background = Color(0xFF090A0C),
    onBackground = Color(0xFFF5F3EF),
    surface = Color(0xFF141519),
    onSurface = Color(0xFFF5F3EF),
    surfaceVariant = Color(0xFF202126),
    onSurfaceVariant = Color(0xFFB9B7B1),
    outline = Color(0xFF303137),
    error = Color(0xFFFF9B9B),
    onError = Color.Black,
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
