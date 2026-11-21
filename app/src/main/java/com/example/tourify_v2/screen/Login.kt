package com.example.tourify_v2.screen

import com.example.tourify_v2.ui.theme.*
import com.example.tourify_v2.viewmodel.AuthViewModel
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

// Isi form masuk ikut fade di panel akun.
@Composable
internal fun LoginForm(
    authViewModel: AuthViewModel,
    onLoginSuccess: (String) -> Unit,
    onNavigateToRegister: () -> Unit,
    enabled: Boolean,
    modifier: Modifier
) {
    // Simpan isian login dan status tampilan kata sandi.
    var username by remember {
        mutableStateOf("")
    }
    var password by remember {
        mutableStateOf("")
    }
    var passwordVisible by remember {
        mutableStateOf(false)
    }
    var errorMessage by remember {
        mutableStateOf("")
    }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val passwordBringIntoViewRequester = remember {
        BringIntoViewRequester()
    }
    var isPasswordFocused by remember {
        mutableStateOf(false)
    }
    // Gulirkan kolom password setelah keyboard mulai membuka ruang di layar.
    LaunchedEffect(isPasswordFocused) {
        if (isPasswordFocused) {
            delay(350)
            passwordBringIntoViewRequester.bringIntoView()
        }
    }
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
            // Tautan daftar akun ikut fade bersama form masuk.
            TextButton(
                onClick = onNavigateToRegister,
                enabled = enabled
            ) {
                Text(text = "Belum punya akun? Daftar di sini")
            }
        }
    ) {
        // Sambutan menjelaskan fungsi form login.
        Text(
            text = "Log in",
            color = TourifyInk,
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Masuk untuk lanjut mencari wahana favoritmu.",
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
            // Enter di username langsung pindah ke password.
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
            // Tombol Done di keyboard langsung menjalankan login.
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    keyboardController?.hide()
                    if (username.isBlank() || password.isBlank()) {
                        errorMessage = "Nama pengguna dan kata sandi wajib diisi."
                    } else {
                        errorMessage = ""
                        authViewModel.login(
                            username.trim(),
                            password,
                            onLoginSuccess
                        )
                    }
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
            modifier = Modifier
                .fillMaxWidth()
                .bringIntoViewRequester(passwordBringIntoViewRequester)
                .onFocusChanged {
                    isPasswordFocused = it.isFocused
                }
                .semantics {
                    contentDescription = "Kata sandi"
                },
            shape = RoundedCornerShape(12.dp)
        )
        // Pesan kalau login gagal.
        if (
            errorMessage.isNotEmpty() ||
            authViewModel.errorMessage.isNotEmpty()
        ) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = errorMessage.ifEmpty {
                    authViewModel.errorMessage
                },
                color = MaterialTheme.colorScheme.error
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        // Tombol masuk ke akun.
        Button(
            onClick = {
                if (username.isBlank() || password.isBlank()) {
                    errorMessage = "Nama pengguna dan kata sandi wajib diisi."
                } else {
                    errorMessage = ""
                    authViewModel.login(
                        username.trim(),
                        password,
                        onLoginSuccess
                    )
                }
            },
            enabled = enabled && !authViewModel.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(53.dp),
            shape = TourifyShapes.input,
            colors = ButtonDefaults.buttonColors(containerColor = TourifyForest)
        ) {
            Text(
                text = if (authViewModel.isLoading) "Memproses..." else "Masuk"
            )
        }
    }
}

// Preview form login.
@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    AuthScreenLayout { minimumPanelHeight ->
        LoginForm(
            authViewModel = AuthViewModel(),
            onLoginSuccess = {},
            onNavigateToRegister = {},
            enabled = true,
            modifier = Modifier.heightIn(min = minimumPanelHeight)
        )
    }
}
