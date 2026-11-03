package com.example.tourify_v2.screen

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.example.tourify_v2.ui.theme.TourifyInk

// Judul layar pakai ukuran dan warna yang sama.
@Composable
fun TourifyScreenTitle(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start
) {
    Text(
        text = text,
        modifier = modifier
            .semantics {
                heading()
            },
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = TourifyInk,
        textAlign = textAlign
    )
}
