package com.example.tourify_v2.screen

import com.example.tourify_v2.model.Ticket
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TicketDateTest {
    @Test fun formatsStoredDatesAndPreservesUnknownValues() {
        assertEquals("24 Okt 2026", formatTicketDate("2026-10-24"))
        assertEquals("24 Okt 2026", formatTicketDate("24/10/2026"))
        assertEquals("24 Okt 2026", formatTicketDate("2026-10-24 09:30:00"))
        assertEquals("Belum ditentukan", formatTicketDate("Belum ditentukan"))
    }

    @Test fun movesOnlyPastVisitDatesToExpiredTickets() {
        // Hari ini masih berlaku; yang lewat cuma tanggal sebelumnya.
        val today = LocalDate.of(2026, 10, 25)
        assertTrue(isTicketExpired("2026-10-24", today))
        assertFalse(isTicketExpired("2026-10-25", today))
        assertFalse(isTicketExpired("26/10/2026", today))
        assertFalse(isTicketExpired("Belum ditentukan", today))
    }

    // Status lama nggak bisa mengaktifkan lagi tiket yang tanggalnya sudah lewat.
    @Test fun expiresOldActiveTicketsButKeepsTheVisitDayValid() {
        val today = LocalDate.of(2026, 11, 1)
        assertTrue(isTicketExpired("Active", "2026-10-31", today))
        assertFalse(isTicketExpired("Active", "2026-11-01", today))
        assertTrue(isTicketExpired("Expired", "2026-11-12", today))
        assertFalse(isTicketExpired("Used", "2026-10-31", today))
    }

    // QR tetap ada selama masih ada pengunjung yang bisa masuk.
    @Test fun sharesEntryRulesBetweenActiveExpiredAndUsedTickets() {
        val active = ticket("active", "2099-11-12")
        val expired = active.copy(status = "Expire")
        val used = active.copy(checkedInQuantity = active.quantity)

        assertTrue(canShowTicketQr(active))
        assertFalse(canShowTicketQr(expired))
        assertFalse(canShowTicketQr(used))
        assertEquals("Kedaluwarsa", ticketStatusLabel(expired))
        assertEquals("Tidak dapat digunakan untuk masuk.", ticketEntryMessage(expired))
        assertEquals("Terpakai", ticketStatusLabel(used))
    }

    // Tiket rombongan tetap aktif sampai semua pengunjung check-in.
    @Test fun keepsPartialAdmissionsAvailableAndShowsRemainingVisitors() {
        val partial = ticket("partial", "2099-11-12").copy(checkedInQuantity = 1)
        assertTrue(canShowTicketQr(partial))
        assertEquals("Sisa 2 dari 3 orang", ticketVisitorSummary(partial))
        assertEquals("3 orang sudah masuk", ticketVisitorSummary(partial.copy(checkedInQuantity = 3)))
    }

    // Urutkan dari tanggal kunjungan, bukan urutan respons atau teks tanggal.
    @Test fun ordersUpcomingVisitsAndPlacesUnknownDatesLast() {
        val later = ticket("later", "2099-11-12")
        val sooner = ticket("sooner", "06/11/2099")
        val unknown = ticket("unknown", "Belum ditentukan")
        val expired = ticket("expired", "2000-01-01")

        assertEquals(
            listOf("sooner", "later", "unknown"),
            visibleTickets(listOf(later, unknown, expired, sooner), false).map { it.id }
        )
    }

    // Tiket riwayat terbaru tetap mudah ditemukan, meski kuotanya sudah habis.
    @Test fun ordersHistoryByMostRecentVisit() {
        val older = ticket("older", "2000-01-01")
        val newer = ticket("newer", "2001-01-01")
        val used = ticket("used", "2099-11-12").copy(status = "Used")
        assertEquals(
            listOf("used", "newer", "older"),
            visibleTickets(listOf(older, used, newer), true).map { it.id }
        )
    }

    // Cari di tab yang dipilih pakai nama destinasi atau kode tiket.
    @Test fun searchesByDestinationOrCodeWithinTheSelectedTab() {
        val safari = ticket("TRF-SAFARI", "2099-11-12")
        val beach = ticket("TRF-PANTAI", "2099-11-06").copy(wahanaName = "Pantai Mentari")
        val used = beach.copy(id = "TRF-TERPAKAI", status = "Used")
        val tickets = listOf(safari, used, beach)

        assertEquals(listOf(beach), visibleTickets(tickets, false, "  pAnTaI  "))
        assertEquals(listOf(safari), visibleTickets(tickets, false, "trf-safari"))
        assertEquals(listOf(used), visibleTickets(tickets, true, "pantai"))
        assertTrue(visibleTickets(tickets, false, "tidak ditemukan").isEmpty())
        assertEquals(listOf(beach, safari), visibleTickets(tickets, false, "   "))
    }

    // Perubahan tampilan nggak mengubah kode yang dibaca pemindai.
    @Test fun preservesTheScannerPayload() {
        val ticket = ticket("TRF-10248", "2099-11-12")
        assertEquals(
            "TOURIFY|TRF-10248|pengunjung|Taman Safari Indonesia|2099-11-12|3",
            ticketQrPayload(ticket)
        )
    }

    // Data tes ini tidak memakai akun atau nomor tiket asli.
    private fun ticket(id: String, visitDate: String): Ticket {
        return Ticket(
            id = id,
            wahanaName = "Taman Safari Indonesia",
            bookingDate = "2099-10-16",
            visitDate = visitDate,
            quantity = 3,
            unitPrice = 150000,
            totalPrice = 450000,
            paymentMethod = "QRIS",
            username = "pengunjung"
        )
    }
}
