package com.example.tourify_v2.ui.theme

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

// Jarak memakai kelipatan 4 atau 8 dp supaya layar terasa lega.
object TourifySpacing {
    val compact = 8.dp
    val control = 12.dp
    val card = 16.dp
    val screen = 20.dp
    // Jarak bersama dari status bar ke judul halaman.
    val screenHeaderTop = 24.dp
    val section = 24.dp
}

// Bentuk dasar untuk input, kartu, panel, dan tombol kapsul.
object TourifyShapes {
    val input = RoundedCornerShape(14.dp)
    val card = RoundedCornerShape(20.dp)
    val sheet = RoundedCornerShape(28.dp)
    val pill = RoundedCornerShape(50)
}

// Bayangan kartu umum dibuat tipis agar panel tetap terasa ringan.
object TourifyElevation {
    val card = 2.dp
    // Kartu foto destinasi perlu lebih menonjol dari latar terang.
    val destinationCard = 5.dp
    val floating = 8.dp
}

// Animasi singkat memberi respons cepat saat pindah layar atau menekan tombol.
object TourifyMotion {
    const val standardMillis = 220
    internal val navigationEasing = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)
}

// Detail masuk dari kanan dan kembali lewat jalur yang sama.
internal fun detailEnterTransition(): EnterTransition =
    fadeIn(animationSpec = tween(TourifyMotion.standardMillis, easing = TourifyMotion.navigationEasing)) +
        slideInHorizontally(
            animationSpec = tween(TourifyMotion.standardMillis, easing = TourifyMotion.navigationEasing),
            initialOffsetX = { width -> width / 16 }
        )

internal fun detailExitTransition(): ExitTransition =
    fadeOut(animationSpec = tween(TourifyMotion.standardMillis, easing = TourifyMotion.navigationEasing)) +
        slideOutHorizontally(
            animationSpec = tween(TourifyMotion.standardMillis, easing = TourifyMotion.navigationEasing),
            targetOffsetX = { width -> width / 16 }
        )

// Kontrol nonaktif tetap terlihat, tapi nggak lebih menonjol dari tombol aktif.
object TourifyInteraction {
    const val disabledContentAlpha = 0.38f
}
