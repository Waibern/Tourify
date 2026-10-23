package com.example.tourify_v2.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.tourify_v2.R

val TourifyFontFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold)
)

private val defaultTypography = Typography()

val Typography = Typography(
    displayLarge = defaultTypography.displayLarge.copy(fontFamily = TourifyFontFamily),
    displayMedium = defaultTypography.displayMedium.copy(fontFamily = TourifyFontFamily),
    displaySmall = defaultTypography.displaySmall.copy(fontFamily = TourifyFontFamily),
    headlineLarge = defaultTypography.headlineLarge.copy(fontFamily = TourifyFontFamily),
    headlineMedium = defaultTypography.headlineMedium.copy(fontFamily = TourifyFontFamily),
    headlineSmall = defaultTypography.headlineSmall.copy(fontFamily = TourifyFontFamily),
    titleLarge = defaultTypography.titleLarge.copy(fontFamily = TourifyFontFamily),
    titleMedium = defaultTypography.titleMedium.copy(fontFamily = TourifyFontFamily),
    titleSmall = defaultTypography.titleSmall.copy(fontFamily = TourifyFontFamily),
    bodyLarge = defaultTypography.bodyLarge.copy(fontFamily = TourifyFontFamily),
    bodyMedium = defaultTypography.bodyMedium.copy(fontFamily = TourifyFontFamily),
    bodySmall = defaultTypography.bodySmall.copy(fontFamily = TourifyFontFamily),
    labelLarge = defaultTypography.labelLarge.copy(fontFamily = TourifyFontFamily),
    labelMedium = defaultTypography.labelMedium.copy(fontFamily = TourifyFontFamily),
    labelSmall = defaultTypography.labelSmall.copy(fontFamily = TourifyFontFamily)
)
