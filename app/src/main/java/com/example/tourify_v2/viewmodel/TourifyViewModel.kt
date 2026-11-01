package com.example.tourify_v2.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tourify_v2.model.ChatHistoryItem
import com.example.tourify_v2.model.ChatRequest
import com.example.tourify_v2.model.ChatResponse
import com.example.tourify_v2.model.Ticket
import com.example.tourify_v2.model.TicketBookingRequest
import com.example.tourify_v2.model.Wahana
import com.example.tourify_v2.network.ApiClient
import com.example.tourify_v2.network.ApiService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

// ambil data destinasi dan urus tiket
class TourifyViewModel(
    // Service yang sama bisa diganti saat menguji koneksi tiket yang gagal.
    private val apiService: ApiService = ApiClient.service
) : ViewModel() {
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
    // Waktu ini hanya maju kalau status tiket benar-benar berhasil diambil.
    var ticketsUpdatedAtMillis by mutableStateOf<Long?>(null)
        private set
    var ticketsOwner by mutableStateOf("")
        private set
    private var ticketsRequest: Job? = null
    var bookingMessage by mutableStateOf("")
        private set
    var isBooking by mutableStateOf(false)
        private set
    var isSendingChat by mutableStateOf(false)
        private set

    fun clearBookingMessage() {
        bookingMessage = ""
    }

    // Kirim pertanyaan dan akun aktif agar backend bisa menjawab dengan data tiketnya.
    fun sendChatMessage(
        message: String,
        username: String,
        history: List<ChatHistoryItem>,
        onComplete: (ChatResponse) -> Unit
    ) {
        if (isSendingChat) return

        viewModelScope.launch {
            isSendingChat = true
            try {
                val response = apiService.sendChatMessage(
                    ChatRequest(
                        message = message,
                        history = history,
                        username = username
                    )
                )
                val result = response.body()
                val chatResult = if (response.isSuccessful && result?.success == true) {
                    if (result.answer.isNullOrBlank() && result.bookingProposal == null) {
                        ChatResponse(message = "Asisten belum bisa membuat jawaban. Coba lagi nanti.")
                    } else {
                        result
                    }
                } else {
                    ChatResponse(message = result?.message ?: "Asisten gagal menjawab. Coba lagi nanti.")
                }
                onComplete(chatResult)
            } catch (_: Exception) {
                onComplete(
                    ChatResponse(
                        message = "Tidak bisa menghubungi asisten. Pastikan server Tourify aktif lalu coba lagi."
                    )
                )
            } finally {
                isSendingChat = false
            }
        }
    }

    fun loadWahana() {
        viewModelScope.launch {
            isLoadingWahana = true
            wahanaError = ""
            try {
                val response = apiService.getWahana()
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

    fun loadTickets(username: String, forceRefresh: Boolean = false) {
        // Jangan tumpuk refresh, dan jangan pakai tiket dari akun sebelumnya.
        val alreadyRefreshing = ticketsOwner == username && ticketsRequest?.isActive == true
        if (username.isBlank() || (!forceRefresh && alreadyRefreshing)) {
            return
        }
        ticketsRequest?.cancel()
        if (ticketsOwner != username) {
            tickets = emptyList()
            ticketsUpdatedAtMillis = null
        }
        ticketsOwner = username
        ticketsRequest = viewModelScope.launch {
            isLoadingTickets = true
            ticketsError = ""
            try {
                val response = apiService.getTickets(username)
                val result = response.body()
                if (response.isSuccessful && result?.success == true) {
                    tickets = result.tickets
                    ticketsUpdatedAtMillis = System.currentTimeMillis()
                } else {
                    ticketsError = result?.message?.takeIf { it.isNotBlank() }
                        ?: "Status tiket belum bisa diperbarui. Coba lagi."
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Tiket yang sudah ada tetap disimpan saat koneksi terputus.
                ticketsError = "Status tiket belum bisa diperbarui. Periksa koneksi lalu coba lagi."
            } finally {
                if (isActive) {
                    isLoadingTickets = false
                }
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
                val response = apiService.bookTicket(request)
                val result = response.body()
                if (response.isSuccessful && result?.success == true) {
                    bookingMessage = result.message
                    // Pesanan baru harus mengambil hasil baru meski refresh sebelumnya belum selesai.
                    loadTickets(username, forceRefresh = true)
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
