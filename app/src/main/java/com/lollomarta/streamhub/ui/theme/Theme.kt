package com.lollomarta.streamhub.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object SH {
    val Background = Color(0xFF070B16)
    val Surface = Color(0xFF10182A)
    val SurfaceElevated = Color(0xFF17223A)
    val Primary = Color(0xFF00D9FF)
    val Secondary = Color(0xFF7957FF)
    val TextPrimary = Color(0xFFF5F7FF)
    val TextSecondary = Color(0xFF9CA9C2)
    val Error = Color(0xFFFF5263)
    val Success = Color(0xFF59E3A5)
    val Rental = Color(0xFFFF9F1C)
}

/** true se l'utente ha scelto "Animazioni ridotte". */
val LocalReduceMotion = staticCompositionLocalOf { false }

private val TvTypography = Typography(
    displaySmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 38.sp, lineHeight = 44.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 36.sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp),
)

@Composable
fun StreamHubTheme(reduceMotion: Boolean = false, content: @Composable () -> Unit) {
    val scheme = darkColorScheme(
        primary = SH.Primary, onPrimary = SH.Background,
        secondary = SH.Secondary, onSecondary = SH.TextPrimary,
        background = SH.Background, onBackground = SH.TextPrimary,
        surface = SH.Surface, onSurface = SH.TextPrimary,
        surfaceVariant = SH.SurfaceElevated, onSurfaceVariant = SH.TextSecondary,
        error = SH.Error,
    )
    CompositionLocalProvider(LocalReduceMotion provides reduceMotion) {
        MaterialTheme(colorScheme = scheme, typography = TvTypography, content = content)
    }
}
