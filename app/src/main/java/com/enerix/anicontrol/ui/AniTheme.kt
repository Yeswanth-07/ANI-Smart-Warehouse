package com.enerix.anicontrol.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp

/** Warehouse-floor palette: concrete greys, black tape, safety-yellow route. */
object AniColors {
    val Safety = Color(0xFFF2B705)
    val Ink = Color(0xFF15171A)
    val Ok = Color(0xFF2E9D5E)
    val Warn = Color(0xFFE08A00)
    val Crit = Color(0xFFD63B33)
    val Term = Color(0xFF0E1012)
}

private val Light = lightColorScheme(
    primary = Color(0xFF1E2124),
    onPrimary = Color.White,
    secondary = AniColors.Safety,
    onSecondary = AniColors.Ink,
    background = Color(0xFFEDEEEA),
    surface = Color(0xFFF6F7F3),
    surfaceVariant = Color(0xFFE4E6E0),
    onSurface = Color(0xFF1B1D1F),
    onSurfaceVariant = Color(0xFF5C6166),
    outline = Color(0xFF8A9096),
    outlineVariant = Color(0xFFCDD0C9),
)

private val Dark = darkColorScheme(
    primary = AniColors.Safety,
    onPrimary = AniColors.Ink,
    secondary = AniColors.Safety,
    onSecondary = AniColors.Ink,
    background = Color(0xFF101214),
    surface = Color(0xFF17191C),
    surfaceVariant = Color(0xFF262A2F),
    onSurface = Color(0xFFE7E8E4),
    onSurfaceVariant = Color(0xFF9BA1A7),
    outline = Color(0xFF7D848B),
    outlineVariant = Color(0xFF363B41),
)

val Mono = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.5.sp)

@Composable
fun AniTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, content = content)
}
