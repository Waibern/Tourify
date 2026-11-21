package com.example.tourify_v2.screen

import com.example.tourify_v2.ui.theme.*

import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.tourify_v2.viewmodel.TourifyViewModel

@Composable
fun DestinationsScreen(
    viewModel: TourifyViewModel,
    onDestinationClick: (Int) -> Unit
) {
    // Simpan pencarian dan kategori yang dipilih.
    val destinations = viewModel.wahana
    var searchQuery by remember {
        mutableStateOf("")
    }
    var selectedCategory by remember {
        mutableStateOf(DestinationCategory.ALL)
    }

    LaunchedEffect(viewModel) {
        if (
            destinations.isEmpty() &&
                !viewModel.isLoadingWahana &&
                viewModel.wahanaError.isEmpty()
        ) {
            viewModel.loadWahana()
        }
    }

    // Cari nama atau deskripsi, lalu cocokkan kategorinya.
    val filteredDestinations = destinations.filter { destination ->
        val query = searchQuery.trim()
        val matchesSearch = destination.name.contains(
            query,
            ignoreCase = true
        ) ||
            destination.description.contains(
                query,
                ignoreCase = true
            )
        val matchesCategory = selectedCategory == DestinationCategory.ALL ||
            selectedCategory.keywords.any { keyword ->
                destination.name.contains(
                    keyword,
                    ignoreCase = true
                ) ||
                    destination.description.contains(
                        keyword,
                        ignoreCase = true
                    )
            }

        matchesSearch && matchesCategory
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TourifyCanvas)
    ) {
        // Header halaman menyatu dengan latar utama.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(
                    start = TourifySpacing.screen,
                    top = TourifySpacing.screenHeaderTop,
                    end = TourifySpacing.screen,
                    bottom = 16.dp
                )
        ) {
            // TalkBack membaca judul ini sebagai heading.
            Text(
                text = "Destinasi",
                modifier = Modifier.semantics {
                    heading()
                },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TourifyInk
            )
        }

        Spacer(modifier = Modifier.height(TourifySpacing.control))

        LazyColumn(
            modifier = Modifier.weight(1f),
            // Tambahkan ruang bawah supaya navbar nggak menutupi isi terakhir.
            contentPadding = PaddingValues(
                start = TourifySpacing.screen,
                end = TourifySpacing.screen,
                bottom = 104.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            ),
            verticalArrangement = Arrangement.spacedBy(TourifySpacing.control)
        ) {
            item {
                DestinationFilters(
                    searchQuery = searchQuery,
                    onSearchQueryChange = {
                        searchQuery = it
                    },
                    selectedCategory = selectedCategory,
                    onCategorySelected = {
                        selectedCategory = it
                    }
                )
            }

            if (viewModel.isLoadingWahana && destinations.isEmpty()) {
                item {
                    DestinationListMessage {
                        CircularProgressIndicator(
                            color = TourifyForest
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Memuat destinasi...",
                            color = TourifyMuted
                        )
                    }
                }
            } else if (viewModel.wahanaError.isNotEmpty() && destinations.isEmpty()) {
                item {
                    DestinationListMessage {
                        Text(
                            text = viewModel.wahanaError,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                viewModel.loadWahana()
                            }
                        ) {
                            Text(
                                "Coba lagi"
                            )
                        }
                    }
                }
            } else if (filteredDestinations.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = TourifyShapes.card,
                        color = TourifySoftSurface
                    ) {
                        Text(
                            text = if (destinations.isEmpty()) {
                                "Belum ada destinasi tersedia."
                            } else {
                                "Tidak ada destinasi yang cocok. Coba kata kunci atau kategori lain."
                            },
                            color = TourifyMuted,
                            modifier = Modifier.padding(18.dp)
                        )
                    }
                }
            } else {
                items(
                    filteredDestinations,
                    key = { destination ->
                        destination.id
                    }
                ) { destination ->
                    // Kartu ikut bergerak saat daftar kategori berubah.
                    DestinationCard(
                        wahana = destination,
                        modifier = Modifier
                            .animateItem(
                                fadeInSpec = tween(TourifyMotion.standardMillis),
                                placementSpec = tween(TourifyMotion.standardMillis),
                                fadeOutSpec = tween(TourifyMotion.standardMillis)
                            )
                            .fillMaxWidth()
                            .aspectRatio(1.65f),
                        onClick = {
                            onDestinationClick(destination.id)
                        }
                    )
                }
            }
        }
    }
}

// Tampilkan status daftar di tengah layar.
@Composable
private fun DestinationListMessage(content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            content = content
        )
    }
}
