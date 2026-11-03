package com.example.tourify_v2.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tourify_v2.model.ProfileResponse
import com.example.tourify_v2.network.ApiClient
import com.example.tourify_v2.network.CredentialsRequest
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

// Login, sesi akun, dan foto profil.
class AuthViewModel : ViewModel() {
    var isLoading by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf("")
        private set
    var currentUsername by mutableStateOf("")
        private set
    var currentAccessToken by mutableStateOf("")
        private set
    var profileImageUrl by mutableStateOf<String?>(null)
        private set
    var isUploadingProfileImage by mutableStateOf(false)
        private set
    var profileImageError by mutableStateOf("")
        private set

    fun clearError() {
        errorMessage = ""
    }

    // Gagal kirim presensi jangan sampai mengganggu login.
    suspend fun updateCustomerPresence() {
        val token = currentAccessToken
        if (token.isBlank()) return

        try {
            ApiClient.service.updateCustomerPresence("Bearer $token")
        } catch (_: Exception) {
        }
    }

    fun register(username: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = ""
            try {
                val response = ApiClient.service.register(CredentialsRequest(username, password))
                val result = response.body()
                if (response.isSuccessful && result?.success == true) {
                    onSuccess()
                } else {
                    errorMessage = result?.message ?: "Pendaftaran gagal. Periksa koneksi server."
                }
            } catch (exception: Exception) {
                errorMessage = "Server atau database sedang offline. Periksa koneksi lalu coba lagi."
            } finally {
                isLoading = false
            }
        }
    }

    fun login(username: String, password: String, onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = ""
            profileImageError = ""
            try {
                val response = ApiClient.service.login(CredentialsRequest(username, password))
                val result = response.body()
                if (response.isSuccessful && result?.success == true && result.user != null) {
                    currentUsername = result.user.username
                    currentAccessToken = result.accessToken.orEmpty()
                    profileImageUrl = ApiClient.profileImageUrl(result.user.profileImage)
                    onSuccess(currentUsername)
                } else {
                    errorMessage = result?.message ?: "Login gagal. Periksa username dan password."
                }
            } catch (exception: Exception) {
                errorMessage = "Server atau database sedang offline. Periksa koneksi lalu coba lagi."
            } finally {
                isLoading = false
            }
        }
    }

    // Ambil path terbaru supaya foto yang diganti dari dashboard ikut tampil di aplikasi.
    fun loadProfileImage() {
        val token = currentAccessToken
        if (token.isBlank()) {
            profileImageError = "Sesi foto belum tersedia. Perbarui server API lalu login kembali."
            return
        }

        viewModelScope.launch {
            try {
                val response = ApiClient.service.getProfile("Bearer $token")
                val result = response.body()
                if (response.isSuccessful && result?.success == true) {
                    profileImageUrl = ApiClient.profileImageUrl(result.profile?.profileImage)
                    profileImageError = ""
                } else {
                    profileImageError = if (response.code() == 401) {
                        "Sesi login habis. Keluar lalu login kembali untuk memuat foto."
                    } else {
                        result?.message ?: "Foto profil gagal dimuat."
                    }
                }
            } catch (exception: Exception) {
                profileImageError = "Foto profil gagal dimuat. Periksa koneksi server."
            }
        }
    }

    // Upload memakai token hasil login agar akun lain tidak bisa ditimpa lewat username.
    fun uploadProfileImage(imageBytes: ByteArray, mimeType: String) {
        val token = currentAccessToken
        if (token.isBlank()) {
            profileImageError = "Sesi foto belum tersedia. Login kembali untuk mengunggah foto."
            return
        }

        val extension = when (mimeType) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> {
                profileImageError = "Pilih foto JPG, PNG, atau WebP."
                return
            }
        }

        viewModelScope.launch {
            isUploadingProfileImage = true
            profileImageError = ""
            try {
                val requestBody = imageBytes.toRequestBody(mimeType.toMediaType())
                val imagePart = MultipartBody.Part.createFormData(
                    "image",
                    "profile.$extension",
                    requestBody
                )
                val response = ApiClient.service.uploadProfileImage(
                    authorization = "Bearer $token",
                    image = imagePart
                )
                val result: ProfileResponse? = response.body()
                if (response.isSuccessful && result?.success == true && result.profile != null) {
                    profileImageUrl = ApiClient.profileImageUrl(result.profile.profileImage)
                } else {
                    profileImageError = if (response.code() == 401) {
                        "Sesi login habis. Keluar lalu login kembali untuk mengunggah foto."
                    } else {
                        result?.message ?: "Foto profil gagal disimpan."
                    }
                }
            } catch (exception: Exception) {
                profileImageError = "Foto profil gagal disimpan. Periksa koneksi server."
            } finally {
                isUploadingProfileImage = false
            }
        }
    }

    fun logout(onComplete: () -> Unit) {
        currentUsername = ""
        currentAccessToken = ""
        profileImageUrl = null
        profileImageError = ""
        errorMessage = ""
        onComplete()
    }
}
