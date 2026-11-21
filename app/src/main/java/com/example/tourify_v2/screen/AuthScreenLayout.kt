package com.example.tourify_v2.screen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tourify_v2.R
import com.example.tourify_v2.ui.theme.TourifySurface
import kotlinx.coroutines.delay

// Lima foto wahana berganti seperti carousel di login dashboard.
private val authHeroPhotos = listOf(
    R.drawable.samudra_biru to "Samudra Biru",
    R.drawable.pantai_mentari to "Pantai Mentari",
    R.drawable.pulau_air to "Pulau Air",
    R.drawable.taman_satwa_pesisir to "Taman Satwa Pesisir",
    R.drawable.dunia_petualang to "Dunia Petualang"
)

// Samakan foto, panel form, dan posisi tautan di kedua layar akun.
@Composable
internal fun AuthScreenLayout(
    animateHeroPhotos: Boolean = true,
    isHeroCopyVisible: Boolean = true,
    content: @Composable (Dp) -> Unit
) {
    var currentPhotoIndex by remember {
        mutableIntStateOf(0)
    }
    var previousPhotoIndex by remember {
        mutableIntStateOf(0)
    }
    val photoAlpha = remember {
        Animatable(1f)
    }
    val isPhotoFading by remember {
        derivedStateOf {
            photoAlpha.value < 1f
        }
    }
    val screenHeight = LocalConfiguration.current.screenHeightDp.toFloat()
    // Batasi tinggi foto supaya form tetap nyaman di layar pendek dan tinggi.
    val heroHeight = (screenHeight * 0.4f).coerceIn(270f, 360f)
    // Jeda carousel pada frame yang sama saat form berganti, lalu lanjutkan fade.
    LaunchedEffect(animateHeroPhotos) {
        if (animateHeroPhotos) {
            while (true) {
                if (photoAlpha.value < 1f) {
                    photoAlpha.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 700)
                    )
                }
                delay(7000)
                previousPhotoIndex = currentPhotoIndex
                photoAlpha.snapTo(0f)
                currentPhotoIndex = (currentPhotoIndex + 1) % authHeroPhotos.size
            }
        }
    }
    // Pakai tinggi yang tersedia setelah inset supaya tautan tidak terpotong.
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            // Sisakan area keyboard supaya konten login tetap bisa digulir ke atas.
            .imePadding()
            .background(TourifySurface)
    ) {
        val minimumPanelHeight = (maxHeight - heroHeight.dp + 30.dp).coerceAtLeast(0.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            // Foto wahana memenuhi bagian atas, dengan lapisan gelap agar teks terbaca.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(heroHeight.dp)
            ) {
                // Foto lama tetap di belakang sampai foto baru selesai muncul.
                if (isPhotoFading) {
                    Image(
                        painter = painterResource(
                            id = authHeroPhotos[previousPhotoIndex].first
                        ),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Image(
                    painter = painterResource(id = authHeroPhotos[currentPhotoIndex].first),
                    contentDescription = "Foto wahana ${authHeroPhotos[currentPhotoIndex].second}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = photoAlpha.value
                        }
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0x4400140D),
                                    Color(0xCC00140D)
                                )
                            )
                        )
                )
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Logo berada di kiri atas foto.
                    Image(
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = "Tourify",
                        contentScale = ContentScale.Fit,
                        colorFilter = ColorFilter.tint(Color.White),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(
                                start = 24.dp,
                                top = 28.dp
                            )
                            .height(50.dp)
                            .authCopyReveal(
                                visible = isHeroCopyVisible,
                                delayMillis = 80,
                                label = "heroLogo"
                            )
                    )
                    // Tagline dan nama wahana berada di atas panel form.
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val heroTextModifier = Modifier
                            .fillMaxWidth()
                            .offset(y = (heroHeight * 0.18f).dp)
                        // Tampilkan tagline dengan gerakan pembuka yang sama.
                        Text(
                            text = "Satu Aplikasi untuk\nKebutuhan Liburan mu",
                            color = Color.White,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontSize = 26.sp,
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = 30.sp
                            ),
                            modifier = heroTextModifier.authCopyReveal(
                                visible = isHeroCopyVisible,
                                label = "heroTagline"
                            )
                        )
                        Text(
                            text = authHeroPhotos[currentPhotoIndex].second,
                            color = Color.White.copy(alpha = 0.88f),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = heroTextModifier.authCopyReveal(
                                visible = isHeroCopyVisible,
                                delayMillis = 360,
                                label = "heroLocation"
                            )
                        )
                    }
                }
            }
            // Latar panel tetap di tempatnya ketika isi form pudar.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = (heroHeight - 30f).dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 36.dp,
                            topEnd = 36.dp
                        )
                    )
                    .background(TourifySurface)
            ) {
                content(minimumPanelHeight)
            }
        }
    }
}

// Label tampil di atas kolom, sementara nama aksesibel dibacakan dari field.
@Composable
internal fun AuthFieldLabel(
    text: String
) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier
            .padding(bottom = 8.dp)
            .semantics {
                hideFromAccessibility()
            }
    )
}

// Susun isian dan tautan sebagai satu kelompok supaya keduanya ikut transisi.
@Composable
internal fun AuthFormPanel(
    enabled: Boolean,
    modifier: Modifier,
    footer: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                if (!enabled) {
                    hideFromAccessibility()
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 24.dp,
                    top = 30.dp,
                    end = 24.dp,
                    bottom = 16.dp
                )
        ) {
            content()
        }
        // Tautan ikut turun saat form atau ukuran teks bertambah tinggi.
        Spacer(modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(
                    start = 24.dp,
                    end = 24.dp,
                    bottom = 16.dp
                )
        ) {
            footer()
        }
    }
}

// Efek copy masuk dengan pudar, naik sedikit, dan blur tipis seperti login web.
@Composable
private fun Modifier.authCopyReveal(
    visible: Boolean,
    delayMillis: Int = 0,
    label: String
): Modifier {
    val revealEasing = CubicBezierEasing(
        0.16f,
        1f,
        0.3f,
        1f
    )
    val revealDelay = if (visible) delayMillis else 0
    val copyAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(
            durationMillis = 650,
            delayMillis = revealDelay,
            easing = revealEasing
        ),
        label = "$label-alpha"
    )
    val copyOffset by animateDpAsState(
        targetValue = if (visible) 0.dp else 14.dp,
        animationSpec = tween(
            durationMillis = 650,
            delayMillis = revealDelay,
            easing = revealEasing
        ),
        label = "$label-offset"
    )
    val copyBlur by animateDpAsState(
        targetValue = if (visible) 0.dp else 3.dp,
        animationSpec = tween(
            durationMillis = 650,
            delayMillis = revealDelay,
            easing = revealEasing
        ),
        label = "$label-blur"
    )

    return this
        .blur(copyBlur)
        .alpha(copyAlpha)
        .offset(y = copyOffset)
}
