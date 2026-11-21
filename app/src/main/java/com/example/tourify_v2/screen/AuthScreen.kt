package com.example.tourify_v2.screen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import com.example.tourify_v2.viewmodel.AuthViewModel
import kotlinx.coroutines.delay

// Login dan Daftar berbagi satu header supaya perpindahan cukup terjadi di bawah foto.
@Composable
internal fun AuthScreen(
    authViewModel: AuthViewModel,
    onLoginSuccess: (String) -> Unit,
    isActive: Boolean,
    playIntro: Boolean,
    formFadeSpec: FiniteAnimationSpec<Float>
) {
    var showRegister by rememberSaveable {
        mutableStateOf(false)
    }
    var introFinished by rememberSaveable {
        mutableStateOf(!playIntro)
    }
    var showHeroCopy by remember {
        mutableStateOf(introFinished)
    }
    val focusManager = LocalFocusManager.current
    // Intro hanya berjalan saat pertama masuk ke layar akun.
    LaunchedEffect(isActive) {
        if (isActive && !introFinished) {
            delay(32)
            showHeroCopy = true
            delay(1010)
            introFinished = true
        }
    }
    val introAlpha by animateFloatAsState(
        targetValue = if (introFinished) 1f else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "authFormIntro"
    )
    val returnToLogin: () -> Unit = {
        focusManager.clearFocus()
        showRegister = false
    }
    // Tombol kembali sistem tetap menutup form daftar terlebih dahulu.
    BackHandler(
        enabled = isActive && showRegister,
        onBack = returnToLogin
    )

    val formTransition = updateTransition(
        targetState = showRegister,
        label = "authForm"
    )
    val isSwitchingForm = formTransition.currentState != showRegister || formTransition.isRunning
    AuthScreenLayout(
        animateHeroPhotos = isActive && !isSwitchingForm,
        isHeroCopyVisible = showHeroCopy
    ) { minimumPanelHeight ->
        // Fade hanya isi panel putih. Selama fade, form lama berhenti menerima input.
        formTransition.AnimatedContent(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = introAlpha
                },
            transitionSpec = {
                (
                    fadeIn(animationSpec = formFadeSpec) togetherWith
                        fadeOut(animationSpec = formFadeSpec)
                    ).using(null)
            }
        ) { showingRegister ->
            val formEnabled = isActive && introFinished && showingRegister == showRegister
            val formModifier = Modifier.heightIn(min = minimumPanelHeight)
            if (showingRegister) {
                RegisterForm(
                    authViewModel = authViewModel,
                    onRegisterSuccess = returnToLogin,
                    onBackToLogin = returnToLogin,
                    enabled = formEnabled,
                    modifier = formModifier
                )
            } else {
                LoginForm(
                    authViewModel = authViewModel,
                    onLoginSuccess = onLoginSuccess,
                    onNavigateToRegister = {
                        focusManager.clearFocus()
                        showRegister = true
                    },
                    enabled = formEnabled,
                    modifier = formModifier
                )
            }
        }
    }
}
