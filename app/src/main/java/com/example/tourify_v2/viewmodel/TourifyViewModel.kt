package com.example.tourify_v2.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tourify_v2.model.Ticket
import com.example.tourify_v2.model.TicketBookingRequest
import com.example.tourify_v2.model.Wahana
import com.example.tourify_v2.network.ApiClient
import kotlinx.coroutines.launch

// ambil data destinasi dan urus tiket
class TourifyViewModel : ViewModel() {
    var wahana by mutableStateOf<List<Wahana>>(emptyList())
        private set
    var tickets by mutableStateOf<List<Ticket>>(emptyList())
        private set
    var isLoadingWahana by mutableStateOf(false)
        private set
    var wahanaError by mutableStateOf("")
        private set
    var isLoadingTickets by mutableStateOf(false)
        private set
    var ticketsError by mutableStateOf("")
        private set
    var bookingMessage by mutableStateOf("")
        private set
    var isBooking by mutableStateOf(false)
        private set

    fun clearBookingMessage() {
        bookingMessage = ""
    }

    fun loadWahana() {
        viewModelScope.launch {
            isLoadingWahana = true
            wahanaError = ""
            try {
                val response = ApiClient.service.getWahana()
                val result = response.body()
                if (response.isSuccessful && result?.success == true) {
                    wahana = result.wahana
                } else {
                    wahanaError = "Daftar wahana gagal dimuat dari server."
                }
            } catch (_: Exception) {
                wahanaError = "Server atau database sedang offline. Periksa koneksi lalu coba lagi."
            } finally {
                isLoadingWahana = false
            }
        }
    }

    fun loadTickets(username: String) {
        viewModelScope.launch {
            isLoadingTickets = true
            ticketsError = ""
            try {
                val response = ApiClient.service.getTickets(username)
                val result = response.body()
                if (response.isSuccessful && result?.success == true) {
                    tickets = result.tickets
                } else {
                    ticketsError = result?.message ?: "Riwayat tiket gagal dimuat."
                }
            } catch (_: Exception) {
                ticketsError = "Server atau database sedang offline. Periksa koneksi lalu coba lagi."
            } finally {
                isLoadingTickets = false
            }
        }
    }

    fun bookTicket(
        username: String,
        request: TicketBookingRequest,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            isBooking = true
            bookingMessage = ""
            try {
                val response = ApiClient.service.bookTicket(request)
                val result = response.body()
                if (response.isSuccessful && result?.success == true) {
                    bookingMessage = result.message
                    loadTickets(username)
                    onComplete(true)
                } else {
                    bookingMessage = result?.message ?: "Pemesanan tiket gagal."
                    onComplete(false)
                }
            } catch (_: Exception) {
                bookingMessage = "Server atau database sedang offline. Periksa koneksi lalu coba lagi."
                onComplete(false)
            } finally {
                isBooking = false
            }
        }
    }
}
