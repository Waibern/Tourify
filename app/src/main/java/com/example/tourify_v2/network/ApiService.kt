package com.example.tourify_v2.network

import com.example.tourify_v2.model.ApiResponse
import com.example.tourify_v2.model.TicketBookingRequest
import com.example.tourify_v2.model.TicketResultResponse
import com.example.tourify_v2.model.TicketsResponse
import com.example.tourify_v2.model.WahanaResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {
    @POST("register.php")
    suspend fun register(@Body credentials: CredentialsRequest): Response<ApiResponse>

    @POST("login.php")
    suspend fun login(@Body credentials: CredentialsRequest): Response<ApiResponse>

    @GET("wahana.php")
    suspend fun getWahana(): Response<WahanaResponse>

    @GET("tickets.php")
    suspend fun getTickets(@Query("username") username: String): Response<TicketsResponse>

    @POST("tickets.php")
    suspend fun bookTicket(
        @Body request: TicketBookingRequest
    ): Response<TicketResultResponse>
}

data class CredentialsRequest(
    val username: String,
    val password: String
)
