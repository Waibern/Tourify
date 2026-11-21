package com.example.tourify_v2.model

import com.google.gson.annotations.SerializedName

data class Wahana(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String,
    @SerializedName("ticket_price") val ticketPrice: Int,
    @SerializedName("average_rating") val averageRating: Double? = null,
    // Admin menentukan unggulan lewat dashboard.
    @SerializedName("is_featured") val isFeatured: Boolean = false,
    // Foto unggahan dari dashboard bisa kosong pada wahana lama.
    @SerializedName("image_path") val imagePath: String? = null
)

data class WahanaResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("wahana") val wahana: List<Wahana> = emptyList()
)
