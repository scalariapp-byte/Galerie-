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

// Palette signature : nuit violette + accents lumineux
private val Dark = darkColorScheme(
    primary = Color(0xFFB69CFF), onPrimary = Color(0xFF1E0A4F),
    primaryContainer = Color(0xFF4B2A9E), onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFFCBC0EA), onSecondary = Color(0xFF221A3A),
    secondaryContainer = Color(0xFF2A1F45), onSecondaryContainer = Color(0xFFE8DEFF),
    tertiary = Color(0xFFFFB2C9), tertiaryContainer = Color(0xFF5A2440), onTertiaryContainer = Color(0xFFFFD9E3),
    background = Color(0xFF07050E), onBackground = Color(0xFFF0ECFA),
    surface = Color(0xFF07050E), onSurface = Color(0xFFF0ECFA),
    surfaceVariant = Color(0xFF1D1730), onSurfaceVariant = Color(0xFFADA4C4),
    surfaceContainerLowest = Color(0xFF07050E), surfaceContainerLow = Color(0xFF0D0918),
    surfaceContainer = Color(0xFF130E22), surfaceContainerHigh = Color(0xFF1B142E),
    surfaceContainerHighest = Color(0xFF251C3D), outline = Color(0xFF4A3B73), outlineVariant = Color(0xFF2E2448),
)

private val Light = lightColorScheme(
    primary = Color(0xFF6D3FE0), onPrimary = Color.White,
    primaryContainer = Color(0xFFE9DDFF), onPrimaryContainer = Color(0xFF22005D),
    secondaryContainer = Color(0xFFE8DEF8), onSecondaryContainer = Color(0xFF1E192B),
    tertiaryContainer = Color(0xFFFFD9E3), onTertiaryContainer = Color(0xFF3E0021),
    background = Color(0xFFFBF8FF), surface = Color(0xFFFBF8FF),
    surfaceContainerLow = Color(0xFFF5F0FD), surfaceContainer = Color(0xFFEFE9FA),
    surfaceContainerHigh = Color(0xFFE9E2F6), surfaceContainerHighest = Color(0xFFE3DBF2),
    outline = Color(0xFFB9A8E6), outlineVariant = Color(0xFFDCD0F5),
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
    if (theme == ThemeMode.AMOLED) {
        scheme = scheme.copy(background = Color.Black, surface = Color.Black, surfaceContainerLowest = Color.Black)
    }
    MaterialTheme(colorScheme = scheme, typography = AppTypography, content = content)
}

/** Taille de texte indépendante du réglage « taille de police » : pour les zones très denses. */
@Composable
@ReadOnlyComposable
fun fixedSp(value: Float): TextUnit = with(LocalDensity.current) {
    if (value >= 0f) Dp(value).toSp() else (-Dp(-value).toSp().value).sp
}
