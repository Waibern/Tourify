package com.example.tourify_v2.model

import com.google.gson.annotations.SerializedName

data class ApiResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("message") val message: String = "",
    @SerializedName("user") val user: ApiUser? = null,
    // Token hasil login dipakai khusus untuk endpoint foto profil.
    @SerializedName("access_token") val accessToken: String? = null
)

data class ApiUser(
    @SerializedName("id") val id: Long,
    @SerializedName("username") val username: String,
    @SerializedName("profile_image") val profileImage: String? = null
)

data class ProfileResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("message") val message: String = "",
    @SerializedName("profile") val profile: ApiProfile? = null
)

// Path gambar yang sama dipakai oleh aplikasi dan dashboard.
data class ApiProfile(
    @SerializedName("username") val username: String,
    @SerializedName("profile_image") val profileImage: String? = null
)

data class TicketResultResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("message") val message: String = "",
    @SerializedName("ticket") val ticket: Ticket? = null
)
