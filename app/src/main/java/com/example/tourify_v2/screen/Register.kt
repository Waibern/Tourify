package com.example.tourify_v2.screen

import com.example.tourify_v2.ui.theme.*

import com.example.tourify_v2.viewmodel.AuthViewModel

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview

// Isi form daftar ikut fade di panel akun.
@Composable
internal fun RegisterForm(
    authViewModel: AuthViewModel,
    onRegisterSuccess: () -> Unit,
    onBackToLogin: () -> Unit,
    enabled: Boolean,
    modifier: Modifier
) {
    // Simpan isian form dan pesan validasi.
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
        unfocusedContainerColor = TourifySoftSurface,
        focusedContainerColor = TourifySoftSurface,
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        cursorColor = MaterialTheme.colorScheme.primary,
        errorBorderColor = MaterialTheme.colorScheme.error
    )

    AuthFormPanel(
        enabled = enabled,
        modifier = modifier,
        footer = {
            // Taruh tautan masuk di bawah panel, seperti tautan daftar di Login.
            TextButton(
                onClick = onBackToLogin,
                enabled = enabled
            ) {
                Text(text = "Sudah punya akun? Masuk di sini")
            }
        }
    ) {
        // Judul form membedakan pendaftaran dari layar masuk.
        Text(
            text = "Daftar akun",
            color = TourifyInk,
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Buat akun untuk mulai memesan tiket wahana.",
            color = TourifyMuted,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(22.dp))

        // Label di atas dan petunjuk di dalam kolom nama pengguna.
        AuthFieldLabel(
            text = "Nama pengguna"
        )
        OutlinedTextField(
            value = username,
            onValueChange = {
                username = it
                errorMessage = ""
                authViewModel.clearError()
            },
            enabled = enabled,
            placeholder = {
                Text(
                    text = "Masukkan nama pengguna",
                    color = TourifyMuted
                )
            },
            // Enter pindah ke kolom password.
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(
                onNext = {
                    focusManager.moveFocus(FocusDirection.Down)
                }
            ),
            colors = fieldColors,
            shape = TourifyShapes.input,
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = "Nama pengguna"
                }
        )
        Spacer(modifier = Modifier.height(14.dp))

        // Label di atas kolom kata sandi dan tombol lihat/sembunyikan.
        AuthFieldLabel(
            text = "Kata sandi"
        )
        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                errorMessage = ""
                authViewModel.clearError()
            },
            enabled = enabled,
            placeholder = {
                Text(
                    text = "Masukkan kata sandi",
                    color = TourifyMuted
                )
            },
            visualTransformation = if (passwordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            // Enter di password pindah ke konfirmasi password.
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
                    },
                    enabled = enabled
                ) {
                    Icon(
                        painter = painterResource(
                            id = if (passwordVisible) {
                                android.R.drawable.ic_menu_close_clear_cancel
                            } else {
                                android.R.drawable.ic_menu_view
                            }
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
            shape = TourifyShapes.input,
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = "Kata sandi"
                }
        )
        Spacer(modifier = Modifier.height(14.dp))

        // Label di atas konfirmasi kata sandi dengan ikon mata yang sama.
        AuthFieldLabel(
            text = "Konfirmasi kata sandi"
        )
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = {
                confirmPassword = it
                errorMessage = ""
            },
            enabled = enabled,
            placeholder = {
                Text(
                    text = "Ulangi kata sandi",
                    color = TourifyMuted
                )
            },
            visualTransformation = if (confirmVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            // Tombol Done menutup keyboard setelah kolom terakhir.
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
                    },
                    enabled = enabled
                ) {
                    Icon(
                        painter = painterResource(
                            id = if (confirmVisible) {
                                android.R.drawable.ic_menu_close_clear_cancel
                            } else {
                                android.R.drawable.ic_menu_view
                            }
                        ),
                        contentDescription = if (confirmVisible) {
                            "Sembunyikan konfirmasi kata sandi"
                        } else {
                            "Tampilkan konfirmasi kata sandi"
                        }
                    )
                }
            },
            colors = fieldColors,
            shape = TourifyShapes.input,
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = "Konfirmasi kata sandi"
                }
        )
        // Tampilkan kesalahan dekat isian agar mudah ditemukan dan dibacakan.
        val formError = errorMessage.ifEmpty {
            authViewModel.errorMessage
        }
        if (formError.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = formError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.semantics {
                    liveRegion = LiveRegionMode.Polite
                }
            )
        }
        Spacer(modifier = Modifier.height(20.dp))

        // Cek isian sesuai aturan API sebelum membuat akun.
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
            enabled = enabled && !authViewModel.isLoading,
            // Biarkan tombol bertambah tinggi kalau ukuran teks diperbesar.
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 53.dp),
            shape = TourifyShapes.input,
            colors = ButtonDefaults.buttonColors(containerColor = TourifyForest)
        ) {
            Text(
                if (authViewModel.isLoading) "Membuat akun..." else "Daftar akun"
            )
        }

    }
}

// Preview form daftar akun.
@Preview(showBackground = true)
@Composable
fun RegisterScreenPreview() {
    AuthScreenLayout { minimumPanelHeight ->
        RegisterForm(
            authViewModel = AuthViewModel(),
            onRegisterSuccess = {},
            onBackToLogin = {},
            enabled = true,
            modifier = Modifier.heightIn(min = minimumPanelHeight)
        )
    }
}
