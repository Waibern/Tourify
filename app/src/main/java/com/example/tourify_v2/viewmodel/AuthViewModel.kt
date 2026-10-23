package com.example.tourify_v2.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tourify_v2.network.ApiClient
import com.example.tourify_v2.network.CredentialsRequest
import kotlinx.coroutines.launch

// urusan login, daftar akun, dan logout
class AuthViewModel : ViewModel() {
    var isLoading by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf("")
        private set
    var currentUsername by mutableStateOf("")
        private set

    fun clearError() {
        errorMessage = ""
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
            try {
                val response = ApiClient.service.login(CredentialsRequest(username, password))
                val result = response.body()
                if (response.isSuccessful && result?.success == true && result.user != null) {
                    currentUsername = result.user.username
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

    fun logout(onComplete: () -> Unit) {
        currentUsername = ""
        errorMessage = ""
        onComplete()
    }
}
