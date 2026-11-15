package com.example.tourify_v2.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = TourifyForest,
    onPrimary = TourifySurface,
    primaryContainer = TourifyMint,
    onPrimaryContainer = TourifyForestDeep,
    secondary = TourifySage,
    onSecondary = TourifySurface,
    secondaryContainer = TourifySecondaryContainer,
    onSecondaryContainer = TourifyForestDeep,
    tertiary = TourifyAccent,
    onTertiary = TourifyOnAccent,
    tertiaryContainer = TourifyTertiaryContainer,
    onTertiaryContainer = TourifyOnAccent,
    background = TourifyCanvas,
    onBackground = TourifyInk,
    surface = TourifySurface,
    onSurface = TourifyInk,
    surfaceVariant = TourifySoftSurface,
    onSurfaceVariant = TourifyMuted,
    surfaceTint = TourifyForest,
    inverseSurface = TourifyInk,
    inverseOnSurface = TourifySurface,
    inversePrimary = TourifyMint,
    error = TourifyError,
    onError = TourifySurface,
    errorContainer = TourifyErrorContainer,
    onErrorContainer = TourifyOnErrorContainer,
    outline = TourifyOutline,
    outlineVariant = TourifyBorder,
    scrim = TourifyScrim
)

@Composable
fun Tourify_v2Theme(
    content: @Composable () -> Unit
) {
    // Hormati ukuran teks dari setelan perangkat.
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
