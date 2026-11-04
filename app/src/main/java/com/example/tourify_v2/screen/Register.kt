package com.example.tourify_v2.screen

import com.example.tourify_v2.ui.theme.*

import com.example.tourify_v2.viewmodel.AuthViewModel

import com.example.tourify_v2.R
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview

// form daftar akun
@Composable
fun RegisterScreen(
    authViewModel: AuthViewModel,
    onRegisterSuccess: () -> Unit,
    onBackToLogin: () -> Unit
) {
    // Simpan isian form dan pesan validasi sebelum membuat akun.
    var username by remember {
        mutableStateOf("")
    }
    var password by remember {
        mutableStateOf("")
    }
    var confirmPassword by remember {
        mutableStateOf("")
    }
    var passwordVisible by remember {
        mutableStateOf(false)
    }
    var confirmVisible by remember {
        mutableStateOf(false)
    }
    var errorMessage by remember {
        mutableStateOf("")
    }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val fieldColors = OutlinedTextFieldDefaults.colors(
        unfocusedBorderColor = Color.LightGray,
        focusedBorderColor = Color.Gray
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // logo di atas form
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "Tourify Logo",
            contentScale = ContentScale.Fit,
            modifier = Modifier.height(150.dp)
        )
        Spacer(modifier = Modifier.height(30.dp))

        // Label input di form daftar.
        OutlinedTextField(
            value = username,
            onValueChange = {
                username = it
                errorMessage = ""
                authViewModel.clearError()
            },
            label = {
                Text(
                    "Nama pengguna"
                )
            },
            // Enter pindah ke kolom password berikutnya.
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(
                onNext = {
                    focusManager.moveFocus(FocusDirection.Down)
                }
            ),
            colors = fieldColors,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        // password dan tombol lihat/sembunyikan
        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                errorMessage = ""
                authViewModel.clearError()
            },
            label = {
                Text(
                    "Kata sandi"
                )
            },
            visualTransformation = if (passwordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            // Enter di password pindah ke kolom konfirmasi.
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = {
                    focusManager.moveFocus(FocusDirection.Down)
                }
            ),
            trailingIcon = {
                IconButton(
                    onClick = {
                        passwordVisible = !passwordVisible
                    }
                ) {
                    Icon(
                        painter = painterResource(
                            if (passwordVisible) android.R.drawable.ic_menu_close_clear_cancel
                            else android.R.drawable.ic_menu_view
                        ),
                        contentDescription = if (passwordVisible) {
                            "Sembunyikan kata sandi"
                        } else {
                            "Tampilkan kata sandi"
                        }
                    )
                }
            },
            colors = fieldColors,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        // ulangi password buat konfirmasi
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = {
                confirmPassword = it
                errorMessage = ""
            },
            label = {
                Text(
                    "Konfirmasi kata sandi"
                )
            },
            visualTransformation = if (confirmVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            // Tombol Done menutup keyboard setelah semua kolom selesai.
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    keyboardController?.hide()
                }
            ),
            trailingIcon = {
                IconButton(
                    onClick = {
                        confirmVisible = !confirmVisible
                    }
                ) {
                    Icon(
                        painter = painterResource(
                            if (confirmVisible) android.R.drawable.ic_menu_close_clear_cancel
                            else android.R.drawable.ic_menu_view
                        ),
                        contentDescription = if (confirmVisible) {
                            "Sembunyikan kata sandi"
                        } else {
                            "Tampilkan kata sandi"
                        }
                    )
                }
            },
            colors = fieldColors,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(20.dp))

        // cek input sesuai aturan API lalu bikin akun
        Button(
            onClick = {
                if (username.isEmpty() || password.isEmpty()) {
                    errorMessage = "Semua kolom wajib diisi."
                } else if (password != confirmPassword) {
                    errorMessage = "Konfirmasi kata sandi tidak cocok."
                } else if (password.length < 8) {
                    errorMessage = "Kata sandi minimal 8 karakter."
                } else {
                    errorMessage = ""
                    authViewModel.clearError()
                    authViewModel.register(
                        username.trim(),
                        password,
                        onRegisterSuccess
                    )
                }
            },
            enabled = !authViewModel.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(53.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TourifyForest)
        ) {
            Text(
                if (authViewModel.isLoading) "Membuat akun..." else "Daftar akun"
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Kembali ke layar masuk.
        TextButton(
            onClick = onBackToLogin
        ) {
            Text(
                "Sudah punya akun? Masuk di sini"
            )
        }

        // pesan kalau daftar akun gagal
        if (errorMessage.isNotEmpty() || authViewModel.errorMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = errorMessage.ifEmpty {
                    authViewModel.errorMessage
                },
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

// Contoh form daftar akun untuk preview Compose.
@Preview(showBackground = true)
@Composable
fun RegisterScreenPreview() {
    RegisterScreen(
        authViewModel = AuthViewModel(),
        onRegisterSuccess = {},
        onBackToLogin = {}
    )
}
