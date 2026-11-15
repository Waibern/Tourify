package com.example.tourify_v2.screen

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.example.tourify_v2.model.Ticket
import com.example.tourify_v2.model.TicketsResponse
import com.example.tourify_v2.network.ApiService
import com.example.tourify_v2.ui.theme.Tourify_v2Theme
import com.example.tourify_v2.viewmodel.TourifyViewModel
import java.io.File
import java.io.IOException
import java.lang.reflect.Proxy
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import retrofit2.Response

class TicketCardTest {
    @get:Rule
    val compose = createComposeRule()

    // Pakai tanggal mendatang supaya tiket tetap aktif kapan pun tes dijalankan.
    private val ticket = Ticket(
        id = "TRF-10248",
        wahanaName = "Taman Safari Indonesia",
        bookingDate = "2099-10-16",
        visitDate = "2099-11-12",
        quantity = 2,
        unitPrice = 150000,
        totalPrice = 300000,
        paymentMethod = "QRIS",
        username = "Pengunjung contoh"
    )

    // Ukuran ini meniru layar ponsel tanpa mengubah setelan emulator.
    @Composable
    private fun TestSurface(
        fontScale: Float = 1f,
        width: Dp = 360.dp,
        height: Dp = 760.dp,
        content: @Composable () -> Unit
    ) {
        CompositionLocalProvider(
            LocalDensity provides Density(LocalDensity.current.density, fontScale)
        ) {
            Tourify_v2Theme {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.White)
                        .safeDrawingPadding()
                ) {
                    Box(Modifier.width(width).height(height)) {
                        content()
                    }
                }
            }
        }
    }

    // Gambar ini diambil dari tampilan Android yang sedang diuji.
    private fun capture(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val file = File(instrumentation.targetContext.getExternalFilesDir(null), "ticket-fixes-$name.png")
        file.outputStream().use {
            instrumentation.uiAutomation.takeScreenshot().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    private fun assertWholeQrVisible() {
        // Gulir dulu karena foto tiket mendorong QR ke bawah.
        val qr = compose.onNodeWithContentDescription("QR code tiket ${ticket.id}")
            .performScrollTo()
            .assertIsDisplayed()
            .fetchSemanticsNode()
        // Cek seluruh QR supaya tes nggak lolos saat yang terlihat cuma bagian atas.
        assertEquals(qr.layoutInfo.height.toFloat(), qr.boundsInRoot.height, 1f)
    }

    @Test
    fun compactSummaryOpensQrAndReturnsToList() {
        compose.setContent {
            TestSurface {
                TicketsContent(tickets = listOf(ticket), updatedAtMillis = 1_793_488_000_000L)
            }
        }
        // Ringkasan menampilkan wahana, tanggal, dan kode sebelum tiket dibuka.
        compose.onNodeWithText(ticket.wahanaName).assertIsDisplayed()
        compose.onNodeWithText(formatTicketDate(ticket.visitDate)).assertIsDisplayed()
        compose.onNodeWithText(ticket.id).assertIsDisplayed()
        capture("list")
        compose.onNodeWithText("Lihat tiket").performClick()
        compose.onNodeWithContentDescription("QR code tiket ${ticket.id}")
            .performScrollTo()
            .assertIsDisplayed()
        capture("qr")
        compose.onNodeWithContentDescription("Kembali ke daftar tiket").performClick()
        compose.onNodeWithText("Lihat tiket").assertIsDisplayed()
        compose.onNodeWithContentDescription("QR code tiket ${ticket.id}").assertDoesNotExist()
    }

    @Test
    fun searchFiltersTicketsKeepsQueryAfterQrAndClearsEmptyResults() {
        val beach = ticket.copy(
            id = "TRF-PANTAI",
            wahanaName = "Pantai Mentari",
            visitDate = "2099-11-06"
        )
        compose.setContent {
            TestSurface {
                TicketsContent(tickets = listOf(ticket, beach))
            }
        }
        // Cari kode tiket dan pastikan pencariannya tetap ada setelah kembali dari QR.
        compose.onNode(hasSetTextAction()).performTextInput("  trf-pantai  ")
        compose.onNode(hasSetTextAction()).performImeAction()
        compose.onNodeWithText("Pantai Mentari").assertIsDisplayed()
        compose.onNodeWithText(ticket.wahanaName).assertDoesNotExist()
        capture("search")
        compose.onNodeWithText("Lihat tiket").performClick()
        compose.onNodeWithContentDescription("QR code tiket ${beach.id}").assertIsDisplayed()
        compose.onNodeWithContentDescription("Kembali ke daftar tiket").performClick()
        compose.onNodeWithText(ticket.wahanaName).assertDoesNotExist()
        compose.onNode(hasSetTextAction()).performTextClearance()
        compose.onNode(hasSetTextAction()).performTextInput("tidak-ada")
        compose.onNode(hasSetTextAction()).performImeAction()
        compose.onNodeWithText("Tiket tidak ditemukan").assertIsDisplayed()
        // Tombol silang kembali ke daftar tanpa menghapus pencarian.
        compose.onNodeWithContentDescription("Hapus pencarian").performClick()
        compose.onNodeWithText("Pantai Mentari").assertIsDisplayed()
        compose.onNodeWithText(ticket.wahanaName).assertExists()
    }

    @Test
    fun largeTextKeepsQrWholeAndReceiptReadable() {
        compose.setContent {
            TestSurface(fontScale = 1.5f, width = 320.dp, height = 640.dp) {
                TicketDetailScreen(ticket = ticket, onDismiss = {})
            }
        }
        // Tiket lama menaruh QR setelah detail, jadi gulir dulu untuk melihatnya.
        compose.onNodeWithContentDescription("QR code tiket ${ticket.id}").performScrollTo()
        assertWholeQrVisible()
        capture("qr-large-text")
        compose.onNodeWithContentDescription("QR code tiket ${ticket.id}").performClick()
        compose.onNodeWithContentDescription("Kembali ke tiket").assertIsDisplayed()
        capture("qr-fullscreen")
        compose.onNodeWithContentDescription("Kembali ke tiket").performClick()
        compose.onNodeWithText("TOTAL").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Rp\u00A0300.000").performScrollTo().assertIsDisplayed()
        capture("receipt-large-text")
        // Tombol unduh tetap di kanan atas saat detail digulir.
        compose.onNodeWithContentDescription("Unduh tiket PDF").assertIsDisplayed()
    }

    @Test
    fun largestTextStillShowsTheWholeQrAfterScrolling() {
        compose.setContent {
            TestSurface(fontScale = 2f, width = 320.dp, height = 640.dp) {
                TicketDetailScreen(ticket = ticket, onDismiss = {})
            }
        }
        // QR tetap utuh saat ukuran teks paling besar dipakai.
        compose.onNodeWithContentDescription("QR code tiket ${ticket.id}").performScrollTo()
        assertWholeQrVisible()
        capture("qr-largest-text")
    }

    @Test
    fun restoresOpenQrAndHistoryFilterAfterConfigurationChange() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            TestSurface {
                TicketsContent(tickets = listOf(ticket, ticket.copy(id = "expired", status = "Expired")))
            }
        }
        // QR dan pilihan filter pulih setelah layar dibuat ulang.
        compose.onNodeWithText("Lihat tiket").performClick()
        restoration.emulateSavedInstanceStateRestore()
        assertWholeQrVisible()
        compose.onNodeWithContentDescription("Kembali ke daftar tiket").performClick()
        compose.onNodeWithText("Aktif").performClick()
        compose.onNodeWithText("Riwayat").performClick()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Riwayat").assertIsDisplayed()
        // Setelah layar dibuat ulang, riwayat tetap menampilkan tiket kedaluwarsa.
        compose.onNodeWithText("expired").assertIsDisplayed()
        compose.onNodeWithText(ticket.id).assertDoesNotExist()
    }

    @Test
    fun expiredAndUsedDetailsNeverOfferAnEntryQr() {
        val current = mutableStateOf(ticket.copy(status = "Expire"))
        compose.setContent {
            TestSurface {
                TicketDetailScreen(ticket = current.value, onDismiss = {})
            }
        }
        // Tiket riwayat tidak boleh menampilkan instruksi masuk.
        compose.onNodeWithText("Tidak dapat digunakan untuk masuk.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("QR code tiket ${ticket.id}").assertDoesNotExist()
        capture("expired")
        compose.runOnIdle {
            current.value = ticket.copy(checkedInQuantity = ticket.quantity)
        }
        compose.onNodeWithText("Semua tiket pada pesanan ini sudah dipakai.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("QR code tiket ${ticket.id}").assertDoesNotExist()
        capture("used")
    }

    @Test
    fun failedRefreshKeepsTicketsAccessibleAndRetryUpdatesAdmissions() {
        val fail = AtomicBoolean(false)
        val checkedIn = AtomicInteger(0)
        // Retrofit palsu memberi data tes tanpa menghubungi akun atau server asli.
        val service = Proxy.newProxyInstance(
            ApiService::class.java.classLoader,
            arrayOf(ApiService::class.java)
        ) { _, method, _ ->
            check(method.name == "getTickets")
            if (fail.get()) {
                throw IOException("koneksi tes terputus")
            }
            Response.success(
                TicketsResponse(
                    success = true,
                    tickets = listOf(ticket.copy(checkedInQuantity = checkedIn.get()))
                )
            )
        } as ApiService
        val viewModel = TourifyViewModel(service)
        compose.setContent {
            TestSurface {
                TicketsScreen(
                    viewModel = viewModel,
                    currentUser = "pengunjung-tes",
                    accessToken = "test-access-token"
                )
            }
        }
        compose.waitUntil(5_000) { viewModel.ticketsUpdatedAtMillis != null }
        val verifiedAt = viewModel.ticketsUpdatedAtMillis
        compose.onNodeWithText("Lihat tiket").performClick()
        fail.set(true)
        compose.onNodeWithContentDescription("Perbarui tiket").performClick()
        compose.waitUntil(5_000) { viewModel.ticketsError.isNotBlank() }
        assertWholeQrVisible()
        compose.runOnIdle {
            assertEquals(verifiedAt, viewModel.ticketsUpdatedAtMillis)
            assertEquals(listOf(ticket), viewModel.tickets)
        }
        capture("refresh-failed")
        // Tiket yang sudah ada tetap bisa dibuka setelah koneksi gagal.
        compose.onNodeWithContentDescription("Kembali ke daftar tiket").performClick()
        compose.onNodeWithText("Lihat tiket").performClick()
        assertWholeQrVisible()
        fail.set(false)
        checkedIn.set(1)
        compose.onNodeWithContentDescription("Perbarui tiket").performClick()
        compose.waitUntil(5_000) {
            viewModel.ticketsError.isEmpty() && viewModel.tickets.firstOrNull()?.checkedInQuantity == 1
        }
        compose.onNodeWithText("1/2 masuk").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("QR code tiket ${ticket.id}").performScrollTo()
        assertWholeQrVisible()
        assertTrue(viewModel.ticketsUpdatedAtMillis!! >= verifiedAt!!)
    }
}
