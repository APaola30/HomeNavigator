package com.homenavigator.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val DeepNavy      = Color(0xFF0A1628)
val NavyMid       = Color(0xFF1A2E4A)
val AccentTeal    = Color(0xFF00C9B1)
val AccentTealDim = Color(0xFF009B88)
val AccentAmber   = Color(0xFFFFB830)
val SoftWhite     = Color(0xFFF0F4F8)
val CardSurface   = Color(0xFF162035)
val ErrorRed      = Color(0xFFE05757)

private val DarkColors = darkColorScheme(
    primary = AccentTeal, onPrimary = DeepNavy,
    secondary = AccentAmber, onSecondary = DeepNavy,
    background = DeepNavy, onBackground = SoftWhite,
    surface = CardSurface, onSurface = SoftWhite,
    surfaceVariant = Color(0xFF243650), onSurfaceVariant = Color(0xFFB0C4D8),
    error = ErrorRed, outline = Color(0xFF3A5068)
)

private val LightColors = lightColorScheme(
    primary = AccentTealDim, onPrimary = Color.White,
    secondary = AccentAmber, onSecondary = DeepNavy,
    background = SoftWhite, onBackground = DeepNavy,
    surface = Color.White, onSurface = DeepNavy,
    surfaceVariant = Color(0xFFE8EDF2), onSurfaceVariant = NavyMid,
    error = ErrorRed, outline = Color(0xFFB0BEC5)
)

@Composable
fun HomeNavigatorTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, content = content)
}