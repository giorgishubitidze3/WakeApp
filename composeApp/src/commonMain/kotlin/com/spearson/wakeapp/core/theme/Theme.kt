package com.spearson.wakeapp.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val WakeColorScheme = darkColorScheme(
    primary = Color(0xFF3B82F6),
    onPrimary = Color(0xFF002640),
    secondary = Color(0xFFFFB968),
    onSecondary = Color(0xFF3E2300),
    tertiary = Color(0xFF8BE0C2),
    onTertiary = Color(0xFF003828),
    background = Color(0xFF0A111A),
    onBackground = Color(0xFFE5EDF8),
    surface = Color(0xFF0F1723),
    onSurface = Color(0xFFDDE6F3),
    surfaceVariant = Color(0xFF1E2838),
    onSurfaceVariant = Color(0xFFB9C5D9),
    outline = Color(0xFF7A879A),
    error = Color(0xFFFF8E94),
    onError = Color(0xFF5E1318),
)

@Composable
fun WakeAppTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = WakeColorScheme,
        typography = WakeTypography,
        content = content,
    )
}
