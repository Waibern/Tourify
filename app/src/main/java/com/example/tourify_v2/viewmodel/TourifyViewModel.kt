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

// Muat destinasi dan kelola tiket.
class TourifyViewModel(
    // Service bisa diganti saat tes koneksi tiket gagal.
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
    // Perbarui waktu sinkron hanya kalau tiket berhasil dimuat.
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

    // Token login mengikat chat ke akun yang sedang dipakai.
    fun sendChatMessage(
        accessToken: String,
        message: String,
        history: List<ChatHistoryItem>,
        onComplete: (ChatResponse) -> Unit
    ) {
        if (isSendingChat) return

        viewModelScope.launch {
            isSendingChat = true
            try {
                val response = apiService.sendChatMessage(
                    authorization = "Bearer $accessToken",
                    request = ChatRequest(
                        message = message,
                        history = history
                    )
                )
                val serverChatResponse = response.body()
                // Siapkan jawaban untuk ditampilkan di layar.
                val displayChatResponse = if (
                    response.isSuccessful &&
                    serverChatResponse?.success == true
                ) {
                    if (
                        serverChatResponse.answer.isNullOrBlank() &&
                        serverChatResponse.bookingProposal == null
                    ) {
                        ChatResponse(message = "Asisten belum bisa membuat jawaban. Coba lagi nanti.")
                    } else {
                        serverChatResponse
                    }
                } else {
                    ChatResponse(
                        message = serverChatResponse?.message
                            ?: "Asisten gagal menjawab. Coba lagi nanti."
                    )
                }
                onComplete(displayChatResponse)
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
                val wahanaResult = response.body()
                // Ganti daftar wahana hanya setelah server berhasil mengirim data.
                if (
                    response.isSuccessful &&
                    wahanaResult?.success == true
                ) {
                    wahana = wahanaResult.wahana
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

    // Token menentukan akun pemilik tiket.
    fun loadTickets(
        username: String,
        accessToken: String,
        forceRefresh: Boolean = false
    ) {
        // Cegah refresh bertumpuk dan jangan pakai tiket dari akun lama.
        val alreadyRefreshing = ticketsOwner == username && ticketsRequest?.isActive == true
        if (username.isBlank() || accessToken.isBlank() || (!forceRefresh && alreadyRefreshing)) {
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
                val response = apiService.getTickets("Bearer $accessToken")
                val ticketsResult = response.body()
                // Perbarui daftar tiket dan waktu sinkron setelah permintaan berhasil.
                if (
                    response.isSuccessful &&
                    ticketsResult?.success == true
                ) {
                    tickets = ticketsResult.tickets
                    ticketsUpdatedAtMillis = System.currentTimeMillis()
                } else {
                    ticketsError = ticketsResult?.message?.takeIf { it.isNotBlank() }
                        ?: "Status tiket belum bisa diperbarui. Coba lagi."
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Kalau koneksi gagal, pertahankan tiket yang sudah dimuat.
                ticketsError = "Status tiket belum bisa diperbarui. Periksa koneksi lalu coba lagi."
            } finally {
                if (isActive) {
                    isLoadingTickets = false
                }
            }
        }
    }

    // Hapus tiket dari daftar hanya setelah server menyimpan status riwayatnya.
    fun hideTicketFromHistory(
        username: String,
        accessToken: String,
        ticketId: String,
        onComplete: (Boolean, String) -> Unit
    ) {
        if (username.isBlank() || accessToken.isBlank() || ticketId.isBlank()) {
            onComplete(
                false,
                "Sesi login tidak valid. Masuk kembali lalu coba lagi."
            )
            return
        }

        viewModelScope.launch {
            try {
                val response = apiService.hideTicketFromHistory(
                    authorization = "Bearer $accessToken",
                    ticketId = ticketId
                )
                val result = response.body()
                // Hilangkan kartu lokal setelah API mengonfirmasi penghapusan riwayat.
                if (
                    response.isSuccessful &&
                    result?.success == true
                ) {
                    if (ticketsOwner == username) {
                        tickets = tickets.filterNot {
                            it.id == ticketId
                        }
                        ticketsUpdatedAtMillis = System.currentTimeMillis()
                    }
                    onComplete(
                        true,
                        result.message.ifBlank {
                            "Tiket berhasil dihapus dari riwayat."
                        }
                    )
                } else {
                    onComplete(
                        false,
                        result?.message?.takeIf { it.isNotBlank() }
                            ?: "Riwayat tiket gagal dihapus. Coba lagi."
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                onComplete(
                    false,
                    "Riwayat tiket gagal dihapus. Periksa koneksi lalu coba lagi."
                )
            }
        }
    }

    fun bookTicket(
        username: String,
        accessToken: String,
        request: TicketBookingRequest,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            isBooking = true
            bookingMessage = ""
            try {
                val response = apiService.bookTicket("Bearer $accessToken", request)
                val bookingResult = response.body()
                // Tampilkan pesan dari server.
                if (
                    response.isSuccessful &&
                    bookingResult?.success == true
                ) {
                    bookingMessage = bookingResult.message
                    // Muat ulang tiket setelah pesan berhasil, walau refresh sebelumnya belum selesai.
                    loadTickets(
                        username = username,
                        accessToken = accessToken,
                        forceRefresh = true
                    )
                    onComplete(true)
                } else {
                    bookingMessage = bookingResult?.message ?: "Pemesanan tiket gagal."
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
