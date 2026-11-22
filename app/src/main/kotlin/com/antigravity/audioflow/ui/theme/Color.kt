package com.antigravity.audioflow.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Neo-Brutalist 2.0 Design Tokens for AudioFlow.
 * High-contrast, tactile, and engineered for Samsung Galaxy S9+ AMOLED display.
 */
@Immutable
data class AudioFlowColors(
    val canvas: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val border: Color,
    val borderSubtle: Color,
    val shadow: Color,
    val yellow: Color,
    val green: Color,
    val blue: Color,
    val pink: Color,
    val isDark: Boolean
)

val LightColors = AudioFlowColors(
    canvas = Color(0xFFF8F6F0),
    surface = Color(0xFFFFFFFF),
    surfaceElevated = Color(0xFFF0EEE7),
    textPrimary = Color(0xFF121212),
    textSecondary = Color(0xFF383838),
    textMuted = Color(0xFF6E6E77),
    border = Color(0xFF121212),
    borderSubtle = Color(0xFFDCDAD2),
    shadow = Color(0xFF121212),
    yellow = Color(0xFFFFDE59),
    green = Color(0xFF55E6A5),
    blue = Color(0xFF70B4FF),
    pink = Color(0xFFFF6B8B),
    isDark = false
)

val DarkColors = AudioFlowColors(
    canvas = Color(0xFF0C0D11),
    surface = Color(0xFF171922),
    surfaceElevated = Color(0xFF222533),
    textPrimary = Color(0xFFF5F5F7),
    textSecondary = Color(0xFFC7C7D1),
    textMuted = Color(0xFF7E8092),
    border = Color(0xFFFFFFFF),
    borderSubtle = Color(0xFF2E3244),
    shadow = Color(0xFF000000),
    yellow = Color(0xFFFFDE59),
    green = Color(0xFF00FF88),
    blue = Color(0xFF00F0FF),
    pink = Color(0xFFFF2A85),
    isDark = true
)
