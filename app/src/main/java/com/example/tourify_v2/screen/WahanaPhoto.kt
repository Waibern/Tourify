package com.example.tourify_v2.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.tourify_v2.R
import com.example.tourify_v2.model.Wahana
import com.example.tourify_v2.network.ApiClient

@Composable
internal fun WahanaPhoto(
    wahana: Wahana,
    contentDescription: String?,
    contentScale: ContentScale,
    modifier: Modifier = Modifier
) {
    // Wahana lama punya foto bawaan, sementara wahana baru bisa belum punya foto.
    val drawable = when (wahana.id) {
        1 -> R.drawable.dunia_petualang
        2 -> R.drawable.samudra_biru
        3 -> R.drawable.pulau_air
        4 -> R.drawable.pantai_mentari
        5 -> R.drawable.taman_satwa_pesisir
        else -> null
    }
    // Pakai jalur async yang sama dengan foto wahana di tiket.
    AsyncWahanaImage(
        imagePath = wahana.imagePath,
        drawable = drawable,
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier
    )
}

private val wahanaPlaceholder = ColorPainter(Color(0xFF52605B))

@Composable
internal fun AsyncWahanaImage(
    imagePath: String?,
    drawable: Int?,
    contentDescription: String?,
    contentScale: ContentScale,
    modifier: Modifier = Modifier,
    colorFilter: ColorFilter? = null
) {
    val remoteUrl = ApiClient.profileImageUrl(imagePath)
    var remoteFailed by remember(remoteUrl) {
        mutableStateOf(false)
    }
    val useRemote = remoteUrl != null && !remoteFailed

    // Coil membaca foto sesuai ukuran tampil. Foto bawaan baru dibaca kalau dibutuhkan.
    AsyncImage(
        model = if (useRemote) remoteUrl else drawable,
        contentDescription = contentDescription,
        placeholder = wahanaPlaceholder,
        error = wahanaPlaceholder,
        fallback = wahanaPlaceholder,
        onError = {
            if (useRemote) {
                remoteFailed = true
            }
        },
        contentScale = contentScale,
        colorFilter = colorFilter,
        modifier = modifier
    )
}
