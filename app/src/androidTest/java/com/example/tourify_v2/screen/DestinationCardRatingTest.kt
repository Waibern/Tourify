package com.example.tourify_v2.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.dp
import com.example.tourify_v2.model.Wahana
import com.example.tourify_v2.ui.theme.Tourify_v2Theme
import org.junit.Rule
import org.junit.Test

class DestinationCardRatingTest {
    @get:Rule
    val compose = createComposeRule()

    @Composable
    private fun Card(wahana: Wahana) {
        Tourify_v2Theme {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                DestinationCard(
                    wahana = wahana,
                    modifier = Modifier
                        .width(320.dp)
                        .height(200.dp),
                    onClick = {}
                )
            }
        }
    }

    // Nilai rata-rata harus dibulatkan satu desimal dan punya label aksesibilitas.
    @Test
    fun averageRatingDisplaysOneDecimalAndStar() {
        compose.setContent {
            Card(
                Wahana(
                    id = 1,
                    name = "Dunia Petualang",
                    description = "Destinasi contoh",
                    ticketPrice = 150000,
                    averageRating = 4.66
                )
            )
        }

        compose.onNodeWithContentDescription("Rating wahana 4.7 dari 5")
            .assertIsDisplayed()
    }

    // Kartu tanpa nilai rata-rata tidak menampilkan badge rating.
    @Test
    fun averageRatingBadgeIsHiddenUntilFirstRating() {
        compose.setContent {
            Card(
                Wahana(
                    id = 1,
                    name = "Dunia Petualang",
                    description = "Destinasi contoh",
                    ticketPrice = 150000
                )
            )
        }

        compose.onNodeWithContentDescription("Rating wahana 0.0 dari 5")
            .assertDoesNotExist()
    }
}
