package com.example.tourify_v2.screen

import android.content.Context
import android.graphics.Paint as PdfPaint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.example.tourify_v2.model.Ticket
import java.util.Locale

// PDF satu halaman memakai payload QR dan detail tiket dari layar yang sama.
internal fun writeTicketPdf(
    context: Context,
    uri: Uri,
    ticket: Ticket
) {
    val document = PdfDocument()
    try {
        // Aturan masuk sama dengan layar, termasuk tiket lama yang tanggalnya sudah lewat.
        val available = canShowTicketQr(ticket)
        val statusLabel = ticketStatusLabel(ticket).uppercase(Locale.forLanguageTag("id-ID"))
        // Siapkan ukuran halaman, warna, dan kuas untuk menggambar tiket.
        val pageInfo = PdfDocument.PageInfo.Builder(
            595,
            842,
            1
        ).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        val forest = android.graphics.Color.rgb(
            20,
            92,
            75
        )
        val ink = android.graphics.Color.rgb(
            27,
            40,
            36
        )
        val muted = android.graphics.Color.rgb(
            104,
            117,
            111
        )
        val border = android.graphics.Color.rgb(
            221,
            233,
            230
        )
        val white = android.graphics.Color.WHITE
        val labelPaint = pdfTextPaint(
            muted,
            11f
        )
        val ticketIdPaint = pdfTextPaint(
            ink,
            18f,
            bold = true
        )
        val titlePaint = pdfTextPaint(
            white,
            19f,
            bold = true
        )
        val wahanaPaint = pdfTextPaint(
            ink,
            23f,
            bold = true
        )
        val headerPaint = PdfPaint(PdfPaint.ANTI_ALIAS_FLAG).apply {
            color = forest
            style = PdfPaint.Style.FILL
        }

        // Gambar identitas tiket sebelum rincian pesanan dan QR.
        canvas.drawColor(white)
        canvas.drawRoundRect(
            RectF(
                32f,
                32f,
                563f,
                132f
            ),
            24f,
            24f,
            headerPaint
        )
        canvas.drawText(
            "TOURIFY",
            54f,
            73f,
            titlePaint
        )
        canvas.drawText(
            "TIKET DIGITAL",
            54f,
            104f,
            pdfTextPaint(
                white,
                12f
            )
        )
        canvas.drawText(
            statusLabel,
            541f,
            104f,
            pdfTextPaint(
                white,
                12f,
                bold = true,
                align = PdfPaint.Align.RIGHT
            )
        )

        canvas.drawText(
            fitPdfText(
                ticket.wahanaName,
                wahanaPaint,
                499f
            ),
            48f,
            181f,
            wahanaPaint
        )
        canvas.drawText(
            "KODE TIKET",
            48f,
            211f,
            labelPaint
        )
        canvas.drawText(
            fitPdfText(
                ticket.id,
                ticketIdPaint,
                499f
            ),
            48f,
            237f,
            ticketIdPaint
        )
        drawPdfDivider(
            canvas,
            border,
            48f,
            258f,
            499f
        )

        drawPdfField(
            canvas,
            "TANGGAL KUNJUNGAN",
            formatTicketDate(ticket.visitDate),
            48f,
            288f,
            240f
        )
        drawPdfField(
            canvas,
            "JUMLAH PENGUNJUNG",
            ticketVisitorSummary(ticket),
            330f,
            288f,
            217f
        )
        drawPdfField(
            canvas,
            "PEMBAYARAN",
            ticket.paymentMethod,
            48f,
            352f,
            240f
        )
        drawPdfField(
            canvas,
            "TOTAL PEMBAYARAN",
            rupiah(ticket.totalPrice),
            330f,
            352f,
            217f
        )
        drawPdfField(
            canvas,
            "DIPESAN PADA",
            formatTicketDate(ticket.bookingDate),
            48f,
            416f,
            499f
        )

        // Riwayat tetap bisa disimpan, tetapi tidak memberi QR atau instruksi masuk.
        if (available) {
            val qrBitmap = generateQrCode(ticketQrPayload(ticket))
            val qrPaint = PdfPaint(PdfPaint.ANTI_ALIAS_FLAG or PdfPaint.FILTER_BITMAP_FLAG)
            canvas.drawBitmap(
                qrBitmap,
                null,
                RectF(
                    197f,
                    464f,
                    398f,
                    665f
                ),
                qrPaint
            )
        } else {
            canvas.drawText(
                "RIWAYAT TIKET",
                297.5f,
                565f,
                pdfTextPaint(
                    muted,
                    18f,
                    bold = true,
                    align = PdfPaint.Align.CENTER
                )
            )
        }
        canvas.drawText(
            ticketEntryMessage(ticket),
            297.5f,
            691f,
            pdfTextPaint(
                muted,
                13f,
                align = PdfPaint.Align.CENTER
            )
        )
        canvas.drawRoundRect(
            RectF(
                222f,
                724f,
                373f,
                762f
            ),
            19f,
            19f,
            PdfPaint(PdfPaint.ANTI_ALIAS_FLAG).apply {
                color = if (available) {
                    android.graphics.Color.rgb(
                        231,
                        241,
                        236
                    )
                } else {
                    android.graphics.Color.rgb(
                        241,
                        245,
                        242
                    )
                }
                style = PdfPaint.Style.FILL
            }
        )
        canvas.drawText(
            statusLabel,
            297.5f,
            749f,
            pdfTextPaint(
                if (available) forest else ink,
                13f,
                bold = true,
                align = PdfPaint.Align.CENTER
            )
        )
        canvas.drawText(
            if (available) {
                "Simpan tiket ini dan tunjukkan QR saat kunjungan."
            } else {
                "Dokumen ini hanya sebagai riwayat pemesanan."
            },
            297.5f,
            800f,
            pdfTextPaint(
                muted,
                11f,
                align = PdfPaint.Align.CENTER
            )
        )
        document.finishPage(page)

        // Timpa seluruh file agar ekspor riwayat tidak menyisakan QR dari PDF sebelumnya.
        val output = requireNotNull(
            context.contentResolver.openOutputStream(
                uri,
                "wt"
            )
        ) {
            "Lokasi penyimpanan tidak dapat dibuka"
        }
        output.use {
            document.writeTo(it)
        }
    } finally {
        document.close()
    }
}

// Tulis label dan nilainya supaya detail gampang dibaca di PDF.
private fun drawPdfField(
    canvas: android.graphics.Canvas,
    label: String,
    value: String,
    left: Float,
    top: Float,
    width: Float
) {
    val valuePaint = pdfTextPaint(
        android.graphics.Color.rgb(
            27,
            40,
            36
        ),
        16f,
        bold = true
    )
    canvas.drawText(
        label,
        left,
        top,
        pdfTextPaint(
            android.graphics.Color.rgb(
                104,
                117,
                111
            ),
            11f
        )
    )
    canvas.drawText(
        fitPdfText(
            value,
            valuePaint,
            width
        ),
        left,
        top + 25f,
        valuePaint
    )
}

// Garis tipis misahin identitas tiket dari detail kunjungan.
private fun drawPdfDivider(
    canvas: android.graphics.Canvas,
    color: Int,
    left: Float,
    top: Float,
    width: Float
) {
    val paint = PdfPaint(PdfPaint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        strokeWidth = 1f
    }
    canvas.drawLine(
        left,
        top,
        left + width,
        top,
        paint
    )
}

// Atur warna, ukuran, dan ketebalan teks PDF di satu tempat.
private fun pdfTextPaint(
    color: Int,
    textSize: Float,
    bold: Boolean = false,
    align: PdfPaint.Align = PdfPaint.Align.LEFT
): PdfPaint {
    return PdfPaint(PdfPaint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        this.textSize = textSize
        this.textAlign = align
        typeface = Typeface.create(
            Typeface.DEFAULT,
            if (bold) Typeface.BOLD else Typeface.NORMAL
        )
    }
}

// Potong teks panjang supaya tetap muat di halaman PDF.
private fun fitPdfText(
    value: String,
    paint: PdfPaint,
    maxWidth: Float
): String {
    if (paint.measureText(value) <= maxWidth) return value

    val ellipsis = "…"
    var end = value.length
    while (end > 0 && paint.measureText(
        value.substring(
            0,
            end
        ) + ellipsis
    ) > maxWidth) {
        end--
    }
    return value.take(end).trimEnd() + ellipsis
}
