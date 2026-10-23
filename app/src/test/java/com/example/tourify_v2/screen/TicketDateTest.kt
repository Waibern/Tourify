package com.example.tourify_v2.screen

import org.junit.Assert.assertEquals
import org.junit.Test

class TicketDateTest {
    @Test fun formatsStoredDatesAndPreservesUnknownValues() {
        assertEquals("24 Okt 2026", formatTicketDate("2026-10-24"))
        assertEquals("24 Okt 2026", formatTicketDate("24/10/2026"))
        assertEquals("24 Okt 2026", formatTicketDate("2026-10-24 09:30:00"))
        assertEquals("Belum ditentukan", formatTicketDate("Belum ditentukan"))
    }
}
