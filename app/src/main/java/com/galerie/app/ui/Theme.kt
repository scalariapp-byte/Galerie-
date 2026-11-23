package com.galerie.app.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

// Palette signature : noir profond + bleu-lavande électrique
private val Dark = darkColorScheme(
    primary = Color(0xFFAAB6FF), onPrimary = Color(0xFF0A1350),
    primaryContainer = Color(0xFF2B378A), onPrimaryContainer = Color(0xFFE0E4FF),
    secondary = Color(0xFFC3C6E0), onSecondary = Color(0xFF1C2033),
    secondaryContainer = Color(0xFF23273A), onSecondaryContainer = Color(0xFFDDE1FA),
    tertiary = Color(0xFFFFB2C9), tertiaryContainer = Color(0xFF5A2440), onTertiaryContainer = Color(0xFFFFD9E3),
    background = Color.Black, onBackground = Color(0xFFEDEDF4),
    surface = Color.Black, onSurface = Color(0xFFEDEDF4),
    surfaceVariant = Color(0xFF1E1F26), onSurfaceVariant = Color(0xFFA3A5B4),
    surfaceContainerLowest = Color.Black, surfaceContainerLow = Color(0xFF0C0C10),
    surfaceContainer = Color(0xFF131318), surfaceContainerHigh = Color(0xFF1B1B22),
    surfaceContainerHighest = Color(0xFF25252E), outline = Color(0xFF3A3B47), outlineVariant = Color(0xFF2A2B33),
)

private val Light = lightColorScheme(
    primary = Color(0xFF3B4CCA), onPrimary = Color.White,
    primaryContainer = Color(0xFFDFE2FF), onPrimaryContainer = Color(0xFF0A1350),
    secondaryContainer = Color(0xFFE3E4F2), onSecondaryContainer = Color(0xFF1C2033),
    tertiaryContainer = Color(0xFFFFD9E3), onTertiaryContainer = Color(0xFF3E0021),
    background = Color(0xFFFBFBFE), surface = Color(0xFFFBFBFE),
    surfaceContainerLow = Color(0xFFF4F4F9), surfaceContainer = Color(0xFFEEEEF4),
    surfaceContainerHigh = Color(0xFFE8E8F0), surfaceContainerHighest = Color(0xFFE1E1EA),
)

private val AppTypography = Typography().let { t ->
    t.copy(
        headlineSmall = t.headlineSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
        titleLarge = t.titleLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp),
        titleMedium = t.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    )
}

@Composable
fun GalerieTheme(theme: ThemeMode, dynamicColor: Boolean = false, content: @Composable () -> Unit) {
    val ctx = LocalContext.current
    val dark = when (theme) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.AMOLED -> true
    }
    val dynamic = dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    var scheme = when {
        dynamic && dark -> dynamicDarkColorScheme(ctx).copy(
            background = Color.Black, surface = Color.Black, surfaceContainerLowest = Color.Black,
            surfaceContainerLow = Color(0xFF0C0C10), surfaceContainer = Color(0xFF131318),
            surfaceContainerHigh = Color(0xFF1B1B22), surfaceContainerHighest = Color(0xFF25252E),
        )
        dynamic -> dynamicLightColorScheme(ctx)
        dark -> Dark
        else -> Light
    }
    if (theme == ThemeMode.DARK) {
        scheme = scheme.copy(background = Color(0xFF111116), surface = Color(0xFF111116), surfaceContainerLowest = Color(0xFF111116))
    }
    MaterialTheme(colorScheme = scheme, typography = AppTypography, content = content)
}

/** Taille de texte indépendante du réglage « taille de police » : pour les zones très denses. */
@Composable
@ReadOnlyComposable
fun fixedSp(value: Float): TextUnit = with(LocalDensity.current) { Dp(value).toSp() }
