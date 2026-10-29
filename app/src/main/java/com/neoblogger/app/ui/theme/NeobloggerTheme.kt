package com.neoblogger.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = DarkGreenPrimary,
    secondary = DarkGreenSecondary,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkCardSurface,
    onBackground = OnDarkSurface,
    onSurface = OnDarkSurface,
    error = DarkError,
)

@Composable
fun NeobloggerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content,
    )
}