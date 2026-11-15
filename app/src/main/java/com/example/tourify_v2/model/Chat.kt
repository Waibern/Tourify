package com.example.tourify_v2.model

import com.google.gson.annotations.SerializedName

// Bentuk data chat mengikuti yang diminta chat.php.
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

// Ini baru draf; pesanan dibuat setelah pengguna mengonfirmasi.
data class ChatBookingProposal(
    @SerializedName("wahana_id") val wahanaId: Int,
    @SerializedName("wahana_name") val wahanaName: String,
    @SerializedName("visit_date") val visitDate: String,
    val quantity: Int,
    @SerializedName("payment_method") val paymentMethod: String,
    @SerializedName("unit_price") val unitPrice: Int,
    @SerializedName("total_price") val totalPrice: Int
)
