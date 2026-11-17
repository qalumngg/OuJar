package com.qalumn.ourjar.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qalumn.ourjar.R

/** Everything the app paints with. Material's colour scheme is derived from these. */
@Immutable
data class OurColors(
    val bg: Color,
    val card: Color,
    val cardAlt: Color,
    val line: Color,
    val ink: Color,
    val inkSoft: Color,
    val accent: Color,
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val partnerTint: Color,
    val dateNight: Color,
    val birthday: Color,
    val important: Color,
    val isDark: Boolean,
)

val LightOurColors = OurColors(
    bg = Color(0xFFFFF7EE),
    card = Color(0xFFFFFCF7),
    cardAlt = Color(0xFFFDE9ED),
    line = Color(0xFFEFDDD0),
    ink = Color(0xFF4A2D37),
    inkSoft = Color(0xFF7D5C67),
    accent = Color(0xFFB5476A),
    primary = Color(0xFFF2A9BB),
    onPrimary = Color(0xFF4A2631),
    primaryContainer = Color(0xFFFBDCE3),
    onPrimaryContainer = Color(0xFF5A2B3A),
    partnerTint = Color(0xFFEBE4F7),
    dateNight = Color(0xFFE88BA3),
    birthday = Color(0xFFF0B93A),
    important = Color(0xFF9A8AD6),
    isDark = false,
)

val DarkOurColors = OurColors(
    bg = Color(0xFF1E1519),
    card = Color(0xFF2A1E24),
    cardAlt = Color(0xFF3A2630),
    line = Color(0xFF402F38),
    ink = Color(0xFFF8EAEE),
    inkSoft = Color(0xFFC9ADB7),
    accent = Color(0xFFF2A3BA),
    primary = Color(0xFFEFB0C1),
    onPrimary = Color(0xFF3A1824),
    primaryContainer = Color(0xFF4A2A36),
    onPrimaryContainer = Color(0xFFFBDCE3),
    partnerTint = Color(0xFF3A3050),
    dateNight = Color(0xFFF2A3BA),
    birthday = Color(0xFFF2C65A),
    important = Color(0xFFB5A8EA),
    isDark = true,
)

val LocalOurColors = staticCompositionLocalOf { LightOurColors }

object Ours {
    val colors: OurColors
        @Composable
        @ReadOnlyComposable
        get() = LocalOurColors.current
}

val Fraunces = FontFamily(Font(R.font.fraunces_semibold, FontWeight.SemiBold))
val DmSans = FontFamily(
    Font(R.font.dmsans_regular, FontWeight.Normal),
    Font(R.font.dmsans_medium, FontWeight.Medium),
    Font(R.font.dmsans_bold, FontWeight.Bold),
)
val Hand = FontFamily(Font(R.font.caveat_semibold, FontWeight.SemiBold))

private fun display(size: Int, line: Int) = TextStyle(
    fontFamily = Fraunces, fontWeight = FontWeight.SemiBold, fontSize = size.sp, lineHeight = line.sp,
)

private fun sans(size: Int, line: Int, weight: FontWeight = FontWeight.Normal, tracking: Double = 0.0) = TextStyle(
    fontFamily = DmSans, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp, letterSpacing = tracking.sp,
)

val OurTypography = Typography(
    displayLarge = display(44, 50),
    displayMedium = display(38, 44),
    displaySmall = display(32, 38),
    headlineLarge = display(32, 38),
    headlineMedium = display(26, 32),
    headlineSmall = display(22, 28),
    titleLarge = display(20, 26),
    titleMedium = sans(16, 22, FontWeight.Medium),
    titleSmall = sans(14, 20, FontWeight.Medium),
    bodyLarge = sans(16, 24),
    bodyMedium = sans(14, 21),
    bodySmall = sans(12, 17),
    labelLarge = sans(15, 20, FontWeight.Medium),
    labelMedium = sans(12, 16, FontWeight.Medium, 0.2),
    labelSmall = sans(11, 16, FontWeight.Medium, 0.8),
)

val OurShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun OurTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val c = if (dark) DarkOurColors else LightOurColors
    val scheme = if (dark) {
        darkColorScheme(
            primary = c.primary, onPrimary = c.onPrimary,
            primaryContainer = c.primaryContainer, onPrimaryContainer = c.onPrimaryContainer,
            secondary = c.accent, onSecondary = c.onPrimary,
            secondaryContainer = c.primaryContainer, onSecondaryContainer = c.onPrimaryContainer,
            background = c.bg, onBackground = c.ink,
            surface = c.card, onSurface = c.ink, surfaceVariant = c.cardAlt, onSurfaceVariant = c.inkSoft,
            surfaceContainerLowest = c.bg, surfaceContainerLow = c.card, surfaceContainer = c.card,
            surfaceContainerHigh = c.card, surfaceContainerHighest = c.cardAlt,
            outline = c.inkSoft, outlineVariant = c.line,
        )
    } else {
        lightColorScheme(
            primary = c.primary, onPrimary = c.onPrimary,
            primaryContainer = c.primaryContainer, onPrimaryContainer = c.onPrimaryContainer,
            secondary = c.accent, onSecondary = Color.White,
            secondaryContainer = c.primaryContainer, onSecondaryContainer = c.onPrimaryContainer,
            background = c.bg, onBackground = c.ink,
            surface = c.card, onSurface = c.ink, surfaceVariant = c.cardAlt, onSurfaceVariant = c.inkSoft,
            surfaceContainerLowest = c.bg, surfaceContainerLow = c.card, surfaceContainer = c.card,
            surfaceContainerHigh = c.card, surfaceContainerHighest = c.cardAlt,
            outline = c.inkSoft, outlineVariant = c.line,
        )
    }
    CompositionLocalProvider(LocalOurColors provides c) {
        MaterialTheme(colorScheme = scheme, typography = OurTypography, shapes = OurShapes, content = content)
    }
}
