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

// Atur login, sesi akun, dan foto profil.
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

    // Kalau presensi gagal, login tetap lanjut.
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
                val registrationResult = response.body()
                // Buat akun hanya setelah server menyatakan pendaftaran berhasil.
                if (
                    response.isSuccessful &&
                    registrationResult?.success == true
                ) {
                    onSuccess()
                } else {
                    errorMessage = registrationResult?.message
                        ?: "Pendaftaran gagal. Periksa koneksi server."
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
                val loginResult = response.body()
                // Simpan sesi setelah server mengembalikan akun yang valid.
                if (
                    response.isSuccessful &&
                    loginResult?.success == true &&
                    loginResult.user != null
                ) {
                    currentUsername = loginResult.user.username
                    currentAccessToken = loginResult.accessToken.orEmpty()
                    profileImageUrl = ApiClient.profileImageUrl(loginResult.user.profileImage)
                    onSuccess(currentUsername)
                } else {
                    errorMessage = loginResult?.message ?: "Login gagal. Periksa username dan password."
                }
            } catch (exception: Exception) {
                errorMessage = "Server atau database sedang offline. Periksa koneksi lalu coba lagi."
            } finally {
                isLoading = false
            }
        }
    }

    // Ambil path terbaru supaya foto dari dashboard ikut tampil di aplikasi.
    fun loadProfileImage() {
        val token = currentAccessToken
        if (token.isBlank()) {
            profileImageError = "Sesi foto belum tersedia. Perbarui server API lalu login kembali."
            return
        }

        viewModelScope.launch {
            try {
                val response = ApiClient.service.getProfile("Bearer $token")
                val profileResult = response.body()
                // Ganti foto hanya setelah profil terbaru berhasil dimuat.
                if (
                    response.isSuccessful &&
                    profileResult?.success == true
                ) {
                    profileImageUrl = ApiClient.profileImageUrl(profileResult.profile?.profileImage)
                    profileImageError = ""
                } else {
                    profileImageError = if (response.code() == 401) {
                        "Sesi login habis. Keluar lalu login kembali untuk memuat foto."
                    } else {
                        profileResult?.message ?: "Foto profil gagal dimuat."
                    }
                }
            } catch (exception: Exception) {
                profileImageError = "Foto profil gagal dimuat. Periksa koneksi server."
            }
        }
    }

    // Pakai token login supaya foto akun lain nggak bisa diganti lewat username.
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
                val uploadResult: ProfileResponse? = response.body()
                // Pakai path foto baru setelah server menyatakan unggahan berhasil.
                if (
                    response.isSuccessful &&
                    uploadResult?.success == true &&
                    uploadResult.profile != null
                ) {
                    profileImageUrl = ApiClient.profileImageUrl(uploadResult.profile.profileImage)
                } else {
                    profileImageError = if (response.code() == 401) {
                        "Sesi login habis. Keluar lalu login kembali untuk mengunggah foto."
                    } else {
                        uploadResult?.message ?: "Foto profil gagal disimpan."
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
