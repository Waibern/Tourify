package com.example.tourify_v2.screen

import android.graphics.Bitmap
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.tourify_v2.model.Ticket
import com.example.tourify_v2.ui.theme.Tourify_v2Theme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TicketHistoryDeleteTest {
    @get:Rule
    val compose = createComposeRule()

    // Tiket selesai jadi contoh riwayat tanpa memakai data akun asli.
    private val ticket = Ticket(
        id = "55",
        wahanaName = "Pantai Mentari",
        bookingDate = "2020-11-15",
        visitDate = "2020-11-15",
        quantity = 8,
        unitPrice = 40000,
        totalPrice = 320000,
        paymentMethod = "Bank",
        status = "Expire"
    )

    @Test
    fun topDeleteRequiresConfirmationAndClosesAfterSuccess() {
        var deletedId = ""
        var dismissCount = 0
        compose.setContent {
            Tourify_v2Theme {
                TicketDetailScreen(
                    ticket = ticket,
                    onDismiss = {
                        dismissCount++
                    },
                    onHideHistoryTicket = { id, complete ->
                        deletedId = id
                        complete(
                            true,
                            "Tiket berhasil dihapus dari riwayat."
                        )
                    }
                )
            }
        }
        // Simpan tampilan hasil render untuk mengecek posisi tombol kanan atas.
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val screenshot = File(
            instrumentation.targetContext.getExternalFilesDir(null),
            "ticket-history-delete.png"
        )
        screenshot.outputStream().use {
            instrumentation.uiAutomation.takeScreenshot().compress(
                Bitmap.CompressFormat.PNG,
                100,
                it
            )
        }
        // Riwayat bisa diunduh, dan batal menghapus tidak mengirim permintaan.
        compose.onNodeWithContentDescription("Unduh tiket PDF").assertIsDisplayed()
        compose.onNodeWithContentDescription("Hapus riwayat tiket").performClick()
        compose.onNodeWithText("Hapus riwayat tiket?").assertIsDisplayed()
        compose.onNodeWithText("Batal").performClick()
        compose.runOnIdle {
            assertEquals("", deletedId)
        }
        compose.onNodeWithContentDescription("Hapus riwayat tiket").performClick()
        compose.onNodeWithText("Hapus").performClick()
        compose.runOnIdle {
            assertEquals(ticket.id, deletedId)
            assertEquals(1, dismissCount)
        }
    }

    @Test
    fun failedDeleteKeepsDetailAndShowsRetryMessage() {
        var dismissCount = 0
        compose.setContent {
            Tourify_v2Theme {
                TicketDetailScreen(
                    ticket = ticket,
                    onDismiss = {
                        dismissCount++
                    },
                    onHideHistoryTicket = { _, complete ->
                        complete(
                            false,
                            "Koneksi gagal. Coba lagi."
                        )
                    }
                )
            }
        }
        // Saat gagal, dialog tetap terbuka supaya pengguna bisa mencoba lagi.
        compose.onNodeWithContentDescription("Hapus riwayat tiket").performClick()
        compose.onNodeWithText("Hapus").performClick()
        compose.onNodeWithText("Koneksi gagal. Coba lagi.").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(0, dismissCount)
        }
    }

    @Test
    fun activeTicketCannotBeDeletedFromHistory() {
        compose.setContent {
            Tourify_v2Theme {
                TicketDetailScreen(
                    ticket = ticket.copy(
                        visitDate = "2099-11-15",
                        status = "Active"
                    ),
                    onDismiss = {}
                )
            }
        }
        // Tiket yang masih bisa dipakai tetap terlindungi dari penghapusan riwayat.
        compose.onNodeWithContentDescription("Hapus riwayat tiket").assertIsNotEnabled()
    }
}
