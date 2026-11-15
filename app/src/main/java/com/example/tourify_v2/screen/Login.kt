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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview

// Form login dan animasi logo.
@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onLoginSuccess: (String) -> Unit,
    onNavigateToRegister: () -> Unit,
    isActive: Boolean = true
) {
    // Simpan isian login dan status password.
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
    var isLoginFormVisible by remember {
        mutableStateOf(false)
    }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val screenHeight = LocalConfiguration.current.screenHeightDp.toFloat()
    // Logo turun dari atas setiap kali layar login dibuka lagi.
    val logoOffsetY = remember(screenHeight) {
        Animatable(-screenHeight)
    }
    LaunchedEffect(
        isActive,
        screenHeight
    ) {
        if (isActive) {
            isLoginFormVisible = false
            logoOffsetY.snapTo(-screenHeight)
            logoOffsetY.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = 900,
                    easing = FastOutSlowInEasing
                )
            )
            isLoginFormVisible = true
        } else {
            isLoginFormVisible = false
            logoOffsetY.snapTo(-screenHeight)
        }
    }
    // Tampilkan form setelah logo selesai turun.
    val loginFormAlpha by animateFloatAsState(
        targetValue = if (isLoginFormVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = 220
        ),
        label = "loginFormReveal"
    )
    val fieldColors = OutlinedTextFieldDefaults.colors(
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        cursorColor = MaterialTheme.colorScheme.primary,
        errorBorderColor = MaterialTheme.colorScheme.error
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TourifyCanvas)
            .verticalScroll(rememberScrollState())
            .padding(TourifySpacing.screen),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo di atas form.
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "Tourify Logo",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .height(150.dp)
                .offset(y = logoOffsetY.value.dp)
        )
        Spacer(modifier = Modifier.height(30.dp))

        // Form baru bisa disentuh setelah animasi logo selesai.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(loginFormAlpha)
                .semantics {
                    if (!isLoginFormVisible) {
                        hideFromAccessibility()
                    }
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Label input sama seperti yang dipakai di form daftar.
            OutlinedTextField(
                value = username,
                onValueChange = {
                    username = it
                    errorMessage = ""
                    authViewModel.clearError()
                },
                enabled = isLoginFormVisible,
                label = {
                    Text(
                        "Nama pengguna"
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
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Input password dan tombol lihat/sembunyikan.
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    errorMessage = ""
                    authViewModel.clearError()
                },
                enabled = isLoginFormVisible,
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
                        enabled = isLoginFormVisible
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
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Pesan kalau login gagal.
            if (errorMessage.isNotEmpty() || authViewModel.errorMessage.isNotEmpty()) {
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
                enabled = isLoginFormVisible && !authViewModel.isLoading,
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

            Spacer(modifier = Modifier.height(10.dp))

            // Tampilkan tautan buat pengguna yang belum punya akun.
            TextButton(
                onClick = {
                    onNavigateToRegister()
                },
                enabled = isLoginFormVisible
            ) {
                Text(
                    text = "Belum punya akun? Daftar di sini"
                )
            }
        }

    }
}

// Preview form login.
@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    LoginScreen(
        authViewModel = AuthViewModel(),
        onLoginSuccess = {},
        onNavigateToRegister = {}
    )
}
