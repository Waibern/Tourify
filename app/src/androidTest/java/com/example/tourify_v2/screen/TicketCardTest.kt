package com.example.tourify_v2.screen

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.example.tourify_v2.model.Ticket
import java.io.File
import org.junit.Rule
import org.junit.Test

class TicketCardTest {
    @get:Rule val compose = createComposeRule()

    private fun showCard(fontScale: Float = 1f) {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale)) {
                MaterialTheme {
                    Column(
                        Modifier.width(320.dp).background(Color(0xFFF7F8F4))
                            .verticalScroll(rememberScrollState()).padding(16.dp)
                    ) {
                        TicketCard(Ticket(
                            "TRF-10248", "Taman Safari Indonesia", "2026-10-16", "2026-10-24",
                            2, 150000, 300000, "QRIS"
                        ))
                    }
                }
            }
        }
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val file = File(instrumentation.targetContext.getExternalFilesDir(null), "$name.png")
        file.outputStream().use {
            instrumentation.uiAutomation.takeScreenshot().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test fun qrOpensAndClosesWithoutLosingTicketSummary() {
        showCard()
        compose.onNodeWithText("24 Okt 2026").assertIsDisplayed()
        compose.onNodeWithText("Rp 300.000").assertIsDisplayed()
        compose.onNodeWithContentDescription("QR code tiket TRF-10248").assertDoesNotExist()
        capture("ticket-collapsed")
        compose.onNodeWithText("Lihat QR tiket").performClick()
        compose.onNodeWithContentDescription("QR code tiket TRF-10248").performScrollTo().assertIsDisplayed()
        capture("ticket-expanded")
        compose.onNodeWithText("QRIS").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Tutup QR tiket").performScrollTo().performClick()
        compose.onNodeWithContentDescription("QR code tiket TRF-10248").assertDoesNotExist()
        compose.onNodeWithText("Taman Safari Indonesia").assertIsDisplayed()
    }

    @Test fun narrowCardSupportsLargerText() {
        showCard(fontScale = 1.3f)
        compose.onNodeWithText("Lihat QR tiket").performScrollTo().assertIsDisplayed()
        capture("ticket-large-text")
        compose.onNodeWithText("Lihat QR tiket").performClick()
        compose.onNodeWithContentDescription("QR code tiket TRF-10248").performScrollTo().assertIsDisplayed()
    }
}
