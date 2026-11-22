package com.antigravity.audioflow.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

val LocalAudioFlowColors = staticCompositionLocalOf { LightColors }

object AudioFlowTheme {
    val colors: AudioFlowColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAudioFlowColors.current
}

@Composable
fun AudioFlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors

    val materialColors = if (darkTheme) {
        darkColorScheme(
            primary = colors.yellow,
            background = colors.canvas,
            surface = colors.surface,
            onBackground = colors.textPrimary,
            onSurface = colors.textPrimary
        )
    } else {
        lightColorScheme(
            primary = colors.yellow,
            background = colors.canvas,
            surface = colors.surface,
            onBackground = colors.textPrimary,
            onSurface = colors.textPrimary
        )
    }

    CompositionLocalProvider(LocalAudioFlowColors provides colors) {
        MaterialTheme(
            colorScheme = materialColors,
            typography = Typography,
            content = content
        )
    }
}
