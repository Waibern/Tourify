package com.example.tourify_v2.screen

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.test.platform.app.InstrumentationRegistry
import com.example.tourify_v2.model.Ticket
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TicketPdfTest {
    @Test
    fun exportedPdfContainsAnEntryQrOnlyForAnAvailableTicket() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val active = Ticket(
            id = "TRF-PDF-10248",
            wahanaName = "Taman Safari Indonesia",
            bookingDate = "2099-10-16",
            visitDate = "2099-11-12",
            quantity = 2,
            unitPrice = 150000,
            totalPrice = 300000,
            paymentMethod = "QRIS",
            username = "Pengunjung contoh"
        )
        // Buat PDF sungguhan supaya isi QR dan aturan masuk ikut dicek.
        val cases = mapOf(
            "active" to active,
            "expired" to active.copy(status = "Expire"),
            "used" to active.copy(checkedInQuantity = active.quantity),
            "past-visit" to active.copy(visitDate = "2000-01-01"),
            "overwrite" to active.copy(status = "Expire")
        )
        cases.forEach { (name, ticket) ->
            val file = File(context.getExternalFilesDir(null), "ticket-fixes-$name.pdf")
            if (name == "overwrite") {
                // PDF riwayat harus mengganti seluruh isi PDF lama di lokasi yang sama.
                writeTicketPdf(context, Uri.fromFile(file), active)
            }
            writeTicketPdf(context, Uri.fromFile(file), ticket)
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
                PdfRenderer(descriptor).use { renderer ->
                    assertEquals(1, renderer.pageCount)
                    renderer.openPage(0).use { page ->
                        val bitmap = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                        bitmap.eraseColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        File(context.getExternalFilesDir(null), "ticket-fixes-pdf-$name.png").outputStream().use {
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                        }
                        val decoded = decodeQr(bitmap)
                        if (name == "active") {
                            assertEquals(ticketQrPayload(ticket), decoded)
                        } else {
                            assertNull("PDF $name tidak boleh menawarkan QR masuk", decoded)
                        }
                        bitmap.recycle()
                    }
                }
            }
        }
    }

    // Baca QR dari hasil render, bukan cuma cek file PDF-nya ada.
    private fun decodeQr(bitmap: Bitmap): String? {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val luminance = RGBLuminanceSource(bitmap.width, bitmap.height, pixels)
        return runCatching {
            QRCodeReader().decode(
                BinaryBitmap(HybridBinarizer(luminance)),
                mapOf(DecodeHintType.TRY_HARDER to true)
            ).text
        }.getOrNull()
    }
}
