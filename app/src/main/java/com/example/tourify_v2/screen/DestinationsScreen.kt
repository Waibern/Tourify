package com.example.tourify_v2.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.tourify_v2.model.Wahana
import com.example.tourify_v2.viewmodel.TourifyViewModel

private val DestinationListGreen = Color(0xFF145C4B)
private val DestinationListMuted = Color(0xFF68756F)

@Composable
fun DestinationsScreen(
    viewModel: TourifyViewModel,
    onDestinationClick: (Int) -> Unit
) {
    val destinations = viewModel.wahana

    LaunchedEffect(viewModel) {
        if (
            viewModel.wahana.isEmpty() &&
            !viewModel.isLoadingWahana &&
            viewModel.wahanaError.isEmpty()
        ) {
            viewModel.loadWahana()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Text(
            text = "Destination",
            style = MaterialTheme.typography.headlineSmall,
            color = DestinationListGreen,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Semua destinasi yang tersedia",
            style = MaterialTheme.typography.bodyMedium,
            color = DestinationListMuted
        )
        Spacer(modifier = Modifier.height(18.dp))

        when {
            destinations.isNotEmpty() -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(destinations, key = { it.id }) { destination ->
                        DestinationListItem(
                            destination = destination,
                            onClick = { onDestinationClick(destination.id) }
                        )
                    }
                }
            }

            viewModel.isLoadingWahana -> {
                DestinationListMessage(modifier = Modifier.weight(1f)) {
                    CircularProgressIndicator(color = DestinationListGreen)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Memuat destinasi...",
                        color = DestinationListMuted
                    )
                }
            }

            viewModel.wahanaError.isNotEmpty() -> {
                DestinationListMessage(modifier = Modifier.weight(1f)) {
                    Text(
                        text = viewModel.wahanaError,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.loadWahana() }
                    ) {
                        Text("Coba lagi")
                    }
                }
            }

            else -> {
                DestinationListMessage(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Belum ada destinasi tersedia.",
                        color = DestinationListMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun DestinationListItem(
    destination: Wahana,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE5E9E6))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Image(
                painter = painterResource(destinationImage(destination.id)),
                contentDescription = "Foto ${destination.name}",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = 96.dp, height = 104.dp)
                    .clip(RoundedCornerShape(10.dp))
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = destination.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF1B2824),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = destination.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = DestinationListMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tiket ${rupiah(destination.ticketPrice)} / orang",
                    style = MaterialTheme.typography.labelLarge,
                    color = DestinationListGreen,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun DestinationListMessage(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            content = content
        )
    }
}
