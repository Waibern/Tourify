package com.example.tourify_v2.screen

import com.example.tourify_v2.viewmodel.AuthViewModel

import com.example.tourify_v2.R
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight


// form login dan animasi logo
@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onLoginSuccess: (String) -> Unit,
    onNavigateToRegister: () -> Unit,
    isActive: Boolean = true
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val screenHeight = LocalConfiguration.current.screenHeightDp.toFloat()
    val logoOffsetY = remember(screenHeight) { Animatable(-screenHeight) }
    LaunchedEffect(isActive, screenHeight) {
        if (isActive) {
            logoOffsetY.snapTo(-screenHeight)
            logoOffsetY.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing)
            )
        } else {
            logoOffsetY.snapTo(-screenHeight)
        }
    }
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
            modifier = Modifier
                .height(150.dp)
                .offset(y = logoOffsetY.value.dp)
        )
        Spacer(modifier = Modifier.height(25.dp))

        Text(
            text = "Username",
            color = Color.Gray,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )


        // input username
        OutlinedTextField(
            value = username,
            onValueChange = { username = it; errorMessage = ""; authViewModel.clearError() },
            placeholder = { Text("Username") },
            colors = fieldColors,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Password",
            color = Color.Gray,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )

        // input password dan tombol lihat/sembunyikan
        OutlinedTextField(
            value = password,
            onValueChange = { password = it; errorMessage = ""; authViewModel.clearError() },
            placeholder = { Text("Password") },
            visualTransformation = if (passwordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        painter = painterResource(
                            if (passwordVisible) android.R.drawable.ic_menu_close_clear_cancel
                            else android.R.drawable.ic_menu_view
                        ),
                        contentDescription = if (passwordVisible) {
                            "Sembunyikan password"
                        } else {
                            "Tampilkan password"
                        }
                    )
                }
            },
            colors = fieldColors,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        // pesan kalau login gagal
        if (errorMessage.isNotEmpty() || authViewModel.errorMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = errorMessage.ifEmpty { authViewModel.errorMessage },
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(150.dp))

        // masuk ke akun
        Button(
            onClick = {
                if (username.isBlank() || password.isBlank()) {
                    errorMessage = "Username dan password wajib diisi."
                } else {
                    errorMessage = ""
                    authViewModel.login(username.trim(), password, onLoginSuccess)
                }
            },
            enabled = !authViewModel.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(53.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF259780))
        ) {
            Text(
                text = if (authViewModel.isLoading) "Memproses..." else "Login",
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // pindah ke form daftar
        Button(
            onClick = {
                onNavigateToRegister()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(53.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFFFFF)),
            border = BorderStroke(2.dp, Color(0xFFE5E5E5))

        ) {
            Text(
                text = "Create Account",
                color = Color.Black,
                fontWeight = FontWeight.Bold
            )

        }


    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    LoginScreen(authViewModel = AuthViewModel(), onLoginSuccess = {}, onNavigateToRegister = {})
}
