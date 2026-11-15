package com.example.tourify_v2.network

import com.example.tourify_v2.model.ApiResponse
import com.example.tourify_v2.model.ChatRequest
import com.example.tourify_v2.model.ChatResponse
import com.example.tourify_v2.model.ProfileResponse
import com.example.tourify_v2.model.TicketBookingRequest
import com.example.tourify_v2.model.TicketResultResponse
import com.example.tourify_v2.model.TicketsResponse
import com.example.tourify_v2.model.WahanaResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

interface ApiService {
    @POST("register.php")
    suspend fun register(@Body credentials: CredentialsRequest): Response<ApiResponse>

    @POST("login.php")
    suspend fun login(@Body credentials: CredentialsRequest): Response<ApiResponse>

    // Kirim info pelanggan yang sedang aktif.
    @POST("customer_presence.php")
    suspend fun updateCustomerPresence(@Header("Authorization") authorization: String): Response<ApiResponse>

    // Ambil dan simpan foto profil milik akun yang login.
    @GET("profile.php")
    suspend fun getProfile(@Header("Authorization") authorization: String): Response<ProfileResponse>

    @Multipart
    @POST("profile.php")
    suspend fun uploadProfileImage(
        @Header("Authorization") authorization: String,
        @Part image: MultipartBody.Part
    ): Response<ProfileResponse>

    @GET("wahana.php")
    suspend fun getWahana(): Response<WahanaResponse>

    // Kirim chat ke PHP supaya API key OpenAI tetap di server.
    @POST("chat.php")
    suspend fun sendChatMessage(
        @Header("Authorization") authorization: String,
        @Body request: ChatRequest
    ): Response<ChatResponse>

    // Ambil tiket milik akun yang login.
    @GET("tickets.php")
    suspend fun getTickets(@Header("Authorization") authorization: String): Response<TicketsResponse>

    @POST("tickets.php")
    suspend fun bookTicket(
        @Header("Authorization") authorization: String,
        @Body request: TicketBookingRequest
    ): Response<TicketResultResponse>
}

data class CredentialsRequest(
    val username: String,
    val password: String
)
