package dev.zionixx.pcmstudyos.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

// Palette mirrors the web app so both surfaces feel like one product.
val Ink = Color(0xFFE8EEF5)
val Muted = Color(0xFF8B98A8)
val Bg = Color(0xFF0B0F14)
val SurfaceDark = Color(0xFF11171F)
val RaisedDark = Color(0xFF17212B)
val LineDark = Color(0xFF26313F)
val AccentDefault = Color(0xFF4F8CFF)

val Success = Color(0xFF39C6A4)
val Warning = Color(0xFFF2A03D)
val Danger = Color(0xFFF87171)

private fun parseHex(hex: String, fallback: Color = AccentDefault): Color = runCatching {
    val cleaned = hex.removePrefix("#")
    val value = cleaned.toLong(16)
    when (cleaned.length) {
        6 -> Color(0xFF000000 or value)
        8 -> Color(value)
        else -> fallback
    }
}.getOrDefault(fallback)

fun accentOf(hex: String): Color = parseHex(hex)

private fun darkScheme(accent: Color) = darkColorScheme(
    primary = accent,
    onPrimary = Color.White,
    primaryContainer = accent.copy(alpha = 0.18f),
    onPrimaryContainer = accent,
    secondary = Success,
    onSecondary = Color(0xFF04120E),
    tertiary = Warning,
    background = Bg,
    onBackground = Ink,
    surface = SurfaceDark,
    onSurface = Ink,
    surfaceVariant = RaisedDark,
    onSurfaceVariant = Muted,
    surfaceContainer = SurfaceDark,
    surfaceContainerHigh = RaisedDark,
    surfaceContainerHighest = RaisedDark,
    outline = LineDark,
    outlineVariant = LineDark,
    error = Danger,
    onError = Color(0xFF2B0B0B),
)

private fun lightScheme(accent: Color) = lightColorScheme(
    primary = accent,
    onPrimary = Color.White,
    primaryContainer = accent.copy(alpha = 0.16f),
    onPrimaryContainer = accent,
    secondary = Color(0xFF1B9C82),
    background = Color(0xFFF6F8FB),
    onBackground = Color(0xFF111821),
    surface = Color.White,
    onSurface = Color(0xFF111821),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF64748B),
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFF1F5F9),
    surfaceContainerHighest = Color(0xFFE9EEF5),
    outline = Color(0xFFDCE3EB),
    outlineVariant = Color(0xFFE4EAF1),
    error = Color(0xFFD93B3B),
)

private val PcmTypography = Typography(
    headlineSmall = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp),
    titleLarge = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium),
    titleSmall = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.4.sp),
    bodyLarge = TextStyle(fontSize = 15.sp),
    bodyMedium = TextStyle(fontSize = 13.5.sp, lineHeight = 19.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.3.sp),
)

@Composable
fun PcmTheme(
    theme: String = "dark",
    accentHex: String = "#4F8CFF",
    fontScale: String = "base",
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val dark = when (theme) {
        "light" -> false
        "system" -> systemDark
        else -> true
    }
    val accent = accentOf(accentHex)
    val colorScheme = if (dark) darkScheme(accent) else lightScheme(accent)
    val scale = when (fontScale) {
        "sm" -> 0.92f
        "lg" -> 1.1f
        else -> 1f
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        val context = LocalContext.current
        SideEffect {
            (context as? Activity)?.let { activity ->
                WindowCompat.getInsetsController(activity.window, view).isAppearanceLightStatusBars = !dark
                WindowCompat.getInsetsController(activity.window, view).isAppearanceLightNavigationBars = !dark
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PcmTypography,
        shapes = androidx.compose.material3.Shapes(
            extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
            small = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
            medium = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
            large = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
            extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        ),
    ) {
        // A single text-size scale knob, applied at the theme boundary.
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.platform.LocalDensity.current.let { density ->
                androidx.compose.ui.unit.Density(density.density, density.fontScale * scale)
            },
        ) {
            content()
        }
    }
}
