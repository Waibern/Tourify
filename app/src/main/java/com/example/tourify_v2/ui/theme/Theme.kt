package com.example.tourify_v2.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = TourifyForest,
    secondary = TourifySage,
    tertiary = TourifyAccent,
    background = TourifyCanvas,
    surface = TourifySurface,
    onPrimary = TourifySurface,
    onSecondary = TourifySurface,
    onTertiary = TourifyInk,
    onBackground = TourifyInk,
    onSurface = TourifyInk,
    error = TourifyError
)

@Composable
fun Tourify_v2Theme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography
    ) {
        CompositionLocalProvider(LocalTextStyle provides LocalTextStyle.current.copy(fontFamily = TourifyFontFamily)) {
            content()
        }
    }
}
