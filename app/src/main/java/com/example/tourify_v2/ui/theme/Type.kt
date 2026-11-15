package com.example.tourify_v2.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.tourify_v2.R

val TourifyFontFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold)
)

private val defaultTypography = Typography()

// Font Inter menjaga teks dan detail tiket tetap jelas.
val Typography = Typography(
    displayLarge = defaultTypography.displayLarge.copy(
        fontFamily = TourifyFontFamily,
        fontWeight = FontWeight.SemiBold
    ),
    displayMedium = defaultTypography.displayMedium.copy(
        fontFamily = TourifyFontFamily,
        fontWeight = FontWeight.SemiBold
    ),
    displaySmall = defaultTypography.displaySmall.copy(
        fontFamily = TourifyFontFamily,
        fontWeight = FontWeight.SemiBold
    ),
    headlineLarge = defaultTypography.headlineLarge.copy(
        fontFamily = TourifyFontFamily,
        fontWeight = FontWeight.SemiBold
    ),
    headlineMedium = defaultTypography.headlineMedium.copy(
        fontFamily = TourifyFontFamily,
        fontWeight = FontWeight.SemiBold
    ),
    headlineSmall = defaultTypography.headlineSmall.copy(
        fontFamily = TourifyFontFamily,
        fontWeight = FontWeight.SemiBold
    ),
    titleLarge = defaultTypography.titleLarge.copy(
        fontFamily = TourifyFontFamily,
        fontWeight = FontWeight.SemiBold
    ),
    titleMedium = defaultTypography.titleMedium.copy(
        fontFamily = TourifyFontFamily,
        fontWeight = FontWeight.Medium
    ),
    titleSmall = defaultTypography.titleSmall.copy(
        fontFamily = TourifyFontFamily,
        fontWeight = FontWeight.Medium
    ),
    bodyLarge = defaultTypography.bodyLarge.copy(
        fontFamily = TourifyFontFamily,
        lineHeight = 24.sp
    ),
    bodyMedium = defaultTypography.bodyMedium.copy(
        fontFamily = TourifyFontFamily,
        lineHeight = 21.sp
    ),
    bodySmall = defaultTypography.bodySmall.copy(
        fontFamily = TourifyFontFamily,
        lineHeight = 18.sp
    ),
    labelLarge = defaultTypography.labelLarge.copy(
        fontFamily = TourifyFontFamily,
        fontWeight = FontWeight.SemiBold
    ),
    labelMedium = defaultTypography.labelMedium.copy(
        fontFamily = TourifyFontFamily,
        fontWeight = FontWeight.Medium
    ),
    labelSmall = defaultTypography.labelSmall.copy(
        fontFamily = TourifyFontFamily,
        fontWeight = FontWeight.Medium
    )
)
