package com.example.tourify_v2.model

import com.google.gson.annotations.SerializedName

// Payload mengikuti kontrak endpoint PHP chat.php.
data class ChatRequest(
    val message: String,
    val history: List<ChatHistoryItem>
)

data class ChatHistoryItem(
    val role: String,
    val text: String
)

data class ChatResponse(
    val success: Boolean = false,
    val answer: String? = null,
    val message: String? = null,
    @SerializedName("booking_proposal") val bookingProposal: ChatBookingProposal? = null
)

// Ringkasan ini hanya draft; pesanan baru dibuat setelah pengguna konfirmasi.
data class ChatBookingProposal(
    @SerializedName("wahana_id") val wahanaId: Int,
    @SerializedName("wahana_name") val wahanaName: String,
    @SerializedName("visit_date") val visitDate: String,
    val quantity: Int,
    @SerializedName("payment_method") val paymentMethod: String,
    @SerializedName("unit_price") val unitPrice: Int,
    @SerializedName("total_price") val totalPrice: Int
)
