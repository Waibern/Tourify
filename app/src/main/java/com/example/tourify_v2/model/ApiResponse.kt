package com.example.tourify_v2.model

import com.google.gson.annotations.SerializedName

data class ApiResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("message") val message: String = "",
    @SerializedName("user") val user: ApiUser? = null
)

data class ApiUser(
    @SerializedName("id") val id: Long,
    @SerializedName("username") val username: String
)

data class TicketResultResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("message") val message: String = "",
    @SerializedName("ticket") val ticket: Ticket? = null
)
