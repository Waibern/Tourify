package com.example.tourify_v2.screen

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.test.platform.app.InstrumentationRegistry
import com.example.tourify_v2.model.Ticket
import com.example.tourify_v2.ui.theme.Tourify_v2Theme
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class UpcomingVisitTest {
    @get:Rule
    val compose = createComposeRule()

    // Data contoh tidak membutuhkan login atau perubahan pesanan server.
    private val ticket = Ticket(
        id = "upcoming-demo",
        wahanaName = "Samudra Biru",
        bookingDate = "2099-11-01",
        visitDate = "2099-11-19",
        quantity = 2,
        unitPrice = 100000,
        totalPrice = 200000,
        paymentMethod = "Bank"
    )

    // Simpan bukti tampilan dari emulator, bukan gambar mockup.
    private fun capture(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val file = File(instrumentation.targetContext.getExternalFilesDir(null), "upcoming-$name.png")
        file.outputStream().use {
            instrumentation.uiAutomation.takeScreenshot().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test
    fun compactCardOpensTheRequestedTicketAndReturnsToList() {
        val selectedId = mutableStateOf<String?>(null)
        val consumed = mutableStateOf(false)
        compose.setContent {
            Tourify_v2Theme {
                if (selectedId.value == null) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .safeDrawingPadding()
                    ) {
                        UpcomingVisitSection(
                            ticket = ticket,
                            refreshError = "",
                            isRefreshing = false,
                            onOpenTickets = {},
                            onOpenTicket = {
                                selectedId.value = ticket.id
                            },
                            onRetry = {}
                        )
                    }
                } else {
                    TicketsContent(
                        tickets = listOf(ticket),
                        requestedTicketId = if (consumed.value) null else selectedId.value,
                        onTicketRequestConsumed = {
                            consumed.value = true
                        }
                    )
                }
            }
        }
        compose.onNodeWithText("Samudra Biru").assertIsDisplayed()
        compose.onNodeWithText("19 Nov 2099 · 2 tiket").assertIsDisplayed()
        capture("normal")
        compose.onNodeWithText("Lihat QR").performClick()
        compose.onNodeWithText("Ketuk QR untuk memperbesar").performScrollTo().assertIsDisplayed()
        capture("detail")
        // QR bisa diperbesar lalu ditutup tanpa kehilangan tiket yang sedang dibuka.
        compose.onNodeWithContentDescription("QR code tiket ${ticket.id}").performClick()
        compose.onNodeWithText("QR masuk").assertIsDisplayed()
        capture("expanded-qr")
        compose.onNodeWithContentDescription("Kembali ke tiket").performClick()
        compose.onNodeWithContentDescription("Kembali ke daftar tiket").performClick()
        compose.onNodeWithText("Lihat QR").assertIsDisplayed()
        assertEquals(true, consumed.value)
    }

    @Test
    fun largeTextKeepsLongNameAndRetryAccessible() {
        var retries = 0
        var listOpens = 0
        compose.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(LocalDensity.current.density, 2f)
            ) {
                Tourify_v2Theme {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .safeDrawingPadding()
                            .verticalScroll(rememberScrollState())
                    ) {
                        UpcomingVisitSection(
                            ticket = ticket.copy(wahanaName = "Taman Satwa Pesisir dan Petualangan Keluarga"),
                            refreshError = "offline",
                            isRefreshing = false,
                            onOpenTickets = {
                                listOpens++
                            },
                            onOpenTicket = {},
                            onRetry = {
                                retries++
                            }
                        )
                    }
                }
            }
        }
        compose.onNodeWithText("Lihat QR").performScrollTo().assertIsDisplayed()
        capture("large-text")
        compose.onNodeWithText("Coba lagi").performScrollTo().performClick()
        compose.onNodeWithText("Semua tiket ›").performScrollTo().performClick()
        assertEquals(1, retries)
        assertEquals(1, listOpens)
    }
    // Semua pintasan menjalankan aksi yang sesuai, dan panduan bisa ditutup.
    @Test
    fun departureShortcutsOpenGuideVisitAndAssistant() {
        var visits = 0
        var help = 0
        compose.setContent {
            Tourify_v2Theme {
                BeforeDepartureSection(
                    onOpenVisit = {
                        visits++
                    },
                    onOpenAssistant = {
                        help++
                    }
                )
            }
        }
        compose.onNodeWithText("Cara masuk wahana").performClick()
        compose.onNodeWithText("Buka tiket").assertIsDisplayed().performClick()
        assertEquals(1, visits)
        compose.onNodeWithText("Cara masuk wahana").performClick()
        compose.onNodeWithText("Tutup").performClick()
        compose.onNodeWithText("Cek detail kunjungan").performClick()
        compose.onNodeWithText("Butuh bantuan?").performClick()
        assertEquals(2, visits)
        assertEquals(1, help)
        capture("departure-shortcuts")
    }

}
