package com.example.tourify_v2.model

import com.google.gson.annotations.SerializedName

data class Ticket(
    @SerializedName("id") val id: String,
    @SerializedName("wahana_name") val wahanaName: String,
    @SerializedName("booking_date") val bookingDate: String,
    @SerializedName("visit_date") val visitDate: String,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("unit_price") val unitPrice: Int,
    @SerializedName("total_price") val totalPrice: Int,
    @SerializedName("payment_method") val paymentMethod: String,
    @SerializedName("username") val username: String = ""
)

data class TicketBookingRequest(
    @SerializedName("username") val username: String,
    @SerializedName("wahana_id") val wahanaId: Int,
    @SerializedName("visit_date") val visitDate: String,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("payment_method") val paymentMethod: String
)

data class TicketsResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("message") val message: String = "",
    @SerializedName("tickets") val tickets: List<Ticket> = emptyList()
)
