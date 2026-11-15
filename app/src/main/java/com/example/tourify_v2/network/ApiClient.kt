package com.example.tourify_v2.network

import com.example.tourify_v2.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private val baseUrl = BuildConfig.TOURIFY_API_BASE_URL

    // Ubah path foto jadi URL yang mengikuti alamat API di perangkat.
    fun profileImageUrl(path: String?): String? {
        if (path.isNullOrBlank()) return null
        if (path.startsWith("https://") || path.startsWith("http://")) return path
        return "${baseUrl.trimEnd('/')}/${path.trimStart('/')}"
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        // Chat boleh menunggu lebih lama daripada daftar wahana atau tiket.
        .readTimeout(55, TimeUnit.SECONDS)
        .build()

    val service: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
