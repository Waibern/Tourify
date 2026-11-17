package com.example.tourify_v2.screen

import com.example.tourify_v2.ui.theme.*

import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.tourify_v2.model.Ticket
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.awaitCancellation
import com.example.tourify_v2.model.Wahana
import com.example.tourify_v2.R
import com.example.tourify_v2.viewmodel.TourifyViewModel
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.launch

private val Forest = TourifyForest
// Tombol cari dan kategori aktif dibuat sedikit lebih terang.
private val SelectedGreen = TourifyForestBright
private val Mint = TourifyMint
private val Canvas = TourifyCanvas
private val Ink = TourifyInk
private val Muted = TourifyMuted

// Setiap chip kategori menampilkan foto destinasi.
internal enum class DestinationCategory(
    val label: String,
    val imageRes: Int,
    val keywords: List<String>

    // Daftar kategori.
) {
    ALL(
        "Semua",
        R.drawable.card_opening,
        emptyList()
    ),
    BEACH(
        "Pantai",
        R.drawable.pantai_mentari,
        listOf(
            "pantai",
            "pesisir"
        )
    ),
    WATER(
        "Air",
        R.drawable.pulau_air,
        listOf(
            "air",
            "samudra",
            "laut"
        )
    ),
    ANIMALS(
        "Satwa",
        R.drawable.taman_satwa_pesisir,
        listOf(
            "satwa",
            "hewan"
        )
    ),
    ADVENTURE(
        "Petualangan",
        R.drawable.dunia_petualang,
        listOf(
            "petualang",
            "adrenalin"
        )
    )
}

internal fun rupiah(amount: Int): String =
    "Rp ${NumberFormat.getNumberInstance(Locale.forLanguageTag("id-ID")).format(amount)}"

@Composable
internal fun DestinationFilters(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedCategory: DestinationCategory,
    onCategorySelected: (DestinationCategory) -> Unit
) {
    // Kolom cari dan pilihan kategori ada di bagian Destinasi.
    DestinationSearchBar(
        searchQuery = searchQuery,
        onSearchQueryChange = onSearchQueryChange
    )

    Spacer(modifier = Modifier.height(TourifySpacing.control))

    DestinationCategoryFilters(
        selectedCategory = selectedCategory,
        onCategorySelected = onCategorySelected
    )
}

// Kolom cari ini juga dipakai di atas foto header Beranda.
@Composable
internal fun DestinationSearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var inputQuery by remember(searchQuery) {
        mutableStateOf(searchQuery)
    }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    // Tombol cari dan Enter di keyboard menjalankan pencarian yang sama.
    val submitSearch: () -> Unit = {
        onSearchQueryChange(inputQuery.trim())
        focusManager.clearFocus()
        keyboardController?.hide()
        Unit
    }

    OutlinedTextField(
        value = inputQuery,
        onValueChange = {
            inputQuery = it
        },
        placeholder = {
            Text(
                "Cari destinasi",
                color = Muted
            )
        },
        leadingIcon = {
            // Ikon hitam di kiri menjalankan pencarian tanpa alas berwarna.
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clickable(
                        role = Role.Button,
                        onClickLabel = "Cari destinasi",
                        onClick = submitSearch
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = Ink,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Search
        ),
        keyboardActions = KeyboardActions(
            onSearch = {
                submitSearch()
            }
        ),
        trailingIcon = {
            // Tombol pencarian memakai hijau yang sedikit lebih terang.
            Row(
                modifier = Modifier.padding(end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (inputQuery.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            inputQuery = ""
                            onSearchQueryChange("")
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Hapus pencarian"
                        )
                    }
                }
                FilledIconButton(
                    onClick = submitSearch,
                    modifier = Modifier
                        .width(64.dp)
                        .height(48.dp),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = TourifyForestBright,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Cari destinasi",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        },
        singleLine = true,
        shape = CircleShape,
        // Sembunyikan garis tepi supaya kolom tampak lebih rapi.
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = TourifySurface,
            focusedContainerColor = TourifySurface,
            unfocusedBorderColor = Color.Transparent,
            focusedBorderColor = Color.Transparent,
            cursorColor = TourifyForest
        ),
        // Sediakan tinggi minimum supaya teks dan ikon tidak terpotong.
        // Modifier dari layar menempatkan kolom di pertemuan foto dan panel.
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .shadow(
                2.dp,
                CircleShape
            )
    )
}

// Tampilkan foto dan chip pilihan kategori.
@Composable
internal fun DestinationCategoryFilters(
    selectedCategory: DestinationCategory,
    onCategorySelected: (DestinationCategory) -> Unit
) {
    // Tampilkan chip tanpa alas di belakangnya.
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .selectableGroup()
            .padding(
                horizontal = 4.dp,
                vertical = 4.dp
            ),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            DestinationCategory.entries,
            key = {
                it.name
            }
        ) { category ->
            val isSelected = category == selectedCategory
            // Kategori terpilih pakai hijau yang sama dengan tombol cari.
            val chipShape = CircleShape
            // Warna dan label chip berubah saat kategori dipilih.
            val chipColor by animateColorAsState(
                targetValue = if (isSelected) SelectedGreen else TourifySurface,
                animationSpec = tween(TourifyMotion.standardMillis),
                label = "categoryChipColor"
            )
            val chipBorderColor by animateColorAsState(
                targetValue = if (isSelected) SelectedGreen else TourifyBorder,
                animationSpec = tween(TourifyMotion.standardMillis),
                label = "categoryChipBorder"
            )
            val chipTextColor by animateColorAsState(
                targetValue = if (isSelected) Color.White else Ink,
                animationSpec = tween(TourifyMotion.standardMillis),
                label = "categoryChipText"
            )

            Surface(
                modifier = Modifier
                    .height(48.dp)
                    .clip(chipShape)
                    .selectable(
                        selected = isSelected,
                        role = Role.RadioButton,
                        onClick = {
                            onCategorySelected(category)
                        }
                    ),
                shape = chipShape,
                color = chipColor,
                shadowElevation = 1.dp,
                border = BorderStroke(
                    1.dp,
                    chipBorderColor
                )
            ) {
                Row(
                    modifier = Modifier.padding(start = 4.dp, end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(category.imageRes),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = category.label,
                        color = chipTextColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun HomeScreen(
    viewModel: TourifyViewModel,
    currentUser: String,
    profileImageUrl: String?,
    onDestinationClick: (Int) -> Unit,
    onExploreDestinations: () -> Unit,
    welcomeTitleIntroPending: Boolean,
    playWelcomeTitleIntro: Boolean,
    // Navigasi tiket dan token tetap disediakan pemilik sesi.
    accessToken: String = "",
    onOpenTickets: () -> Unit = {},
    onOpenTicket: (String) -> Unit = {},
    // Bantuan memakai panel asisten yang sudah dikelola navigasi utama.
    onOpenAssistant: () -> Unit = {}
) {
    // Ukuran kartu tetap lega saat pengguna membesarkan teks.
    val largeTextEnabled = LocalDensity.current.fontScale >= 1.3f
    val featuredCardMinHeight = if (largeTextEnabled) 240.dp else 220.dp
    val featuredCardAspectRatio = if (largeTextEnabled) 1.35f else 1.5f
    val destinationCardAspectRatio = if (largeTextEnabled) 1.45f else 1.6f

    // Ingat status judul supaya animasinya nggak berulang saat pindah tab.
    var hasPlayedWelcomeTitleIntro by rememberSaveable {
        mutableStateOf(false)
    }
    val titleIntroStartProgress = if (
        (welcomeTitleIntroPending || playWelcomeTitleIntro) &&
        !hasPlayedWelcomeTitleIntro
    ) {
        0f
    } else {
        1f
    }
    val firstTitleProgress = remember {
        Animatable(titleIntroStartProgress)
    }
    val secondTitleProgress = remember {
        Animatable(titleIntroStartProgress)
    }
    // Setelah login, dua baris judul naik satu per satu.
    LaunchedEffect(playWelcomeTitleIntro) {
        if (playWelcomeTitleIntro && !hasPlayedWelcomeTitleIntro) {
            hasPlayedWelcomeTitleIntro = true
            launch {
                firstTitleProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = 280,
                        easing = LinearOutSlowInEasing
                    )
                )
            }
            launch {
                secondTitleProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = 280,
                        delayMillis = 170,
                        easing = LinearOutSlowInEasing
                    )
                )
            }
        }
    }

    // Segarkan kunjungan saat Beranda aktif, termasuk setelah kembali dari aplikasi lain.
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(currentUser, accessToken, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.loadTickets(
                username = currentUser,
                accessToken = accessToken
            )
            awaitCancellation()
        }
    }
    // Pakai aturan urutan dan status yang sama dengan daftar tiket.
    val upcomingTicket = if (viewModel.ticketsOwner == currentUser) {
        visibleTickets(
            tickets = viewModel.tickets,
            showHistory = false
        ).firstOrNull { canShowTicketQr(it) }
    } else {
        null
    }
    val wahanaList = viewModel.wahana
    LaunchedEffect(Unit) {
        viewModel.loadWahana()
    }
    // Ingat pencarian dan kategori selama Beranda terbuka.
    var searchQuery by remember {
        mutableStateOf("")
    }
    var selectedCategory by remember {
        mutableStateOf(DestinationCategory.ALL)
    }
    val filteredWahana = wahanaList.filter { wahana ->
        val matchesSearch = wahana.name.contains(
            searchQuery.trim(),
            ignoreCase = true
        ) ||
            wahana.description.contains(
                searchQuery.trim(),
                ignoreCase = true
            )
        val matchesCategory = selectedCategory == DestinationCategory.ALL ||
            selectedCategory.keywords.any { keyword ->
                wahana.name.contains(
                    keyword,
                    ignoreCase = true
                ) ||
                    wahana.description.contains(
                        keyword,
                        ignoreCase = true
                    )
            }

        matchesSearch && matchesCategory
    }
    val featured = wahanaList.firstOrNull {
        it.name.trim().equals(
            "Pantai Mentari",
            ignoreCase = true
        )
    } ?: wahanaList.firstOrNull()
    // Jarak ini menyisakan 60 dp bar dan 8 dp di atas/bawah, lalu inset gesture sistem.
    val bottomNavClearance = 76.dp +
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(TourifySurface)
            .padding(bottom = bottomNavClearance),
        // Sisakan napas setelah konten terakhir di area yang sudah aman dari navbar.
        contentPadding = PaddingValues(
            bottom = TourifySpacing.card
        ),
        // Rapatkan jarak antarbagian utama Beranda.
        verticalArrangement = Arrangement.spacedBy(TourifySpacing.control)
    ) {
        // Foto tanpa fade bertemu panel putih membulat di bawahnya.
        item {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        // Sisakan ruang gambar agar lengkung panel terlihat jelas.
                        .heightIn(min = 232.dp)
                ) {
                    Image(
                        // Pakai ilustrasi hijau sebagai latar header Beranda.
                        painter = painterResource(R.drawable.home_background),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        // Fokus crop agak ke area atas latar.
                        alignment = androidx.compose.ui.BiasAlignment(0f, -0.45f),
                        modifier = Modifier.matchParentSize()
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = 20.dp,
                                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 24.dp,
                                end = 20.dp,
                                bottom = 16.dp
                            ),
                        // Header yang lebih ringkas memberi ruang lebih banyak untuk destinasi.
                        verticalArrangement = Arrangement.spacedBy(TourifySpacing.compact)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                val displayName = currentUser
                                    .trim()
                                    .ifBlank {
                                        "petualang"
                                    }
                                    .replaceFirstChar {
                                        if (it.isLowerCase()) {
                                            it.titlecase(Locale.getDefault())
                                        } else {
                                            it.toString()
                                        }
                                    }
                                // Judul tetap menonjol, tapi tidak mendorong konten terlalu jauh.
                                val photoHeadingStyle = MaterialTheme.typography.headlineLarge.copy(
                                    fontSize = 32.sp,
                                    lineHeight = 38.sp,
                                    fontWeight = FontWeight.Bold,
                                    shadow = Shadow(
                                        color = Color.Black.copy(alpha = 0.32f),
                                        offset = Offset(0f, 2f),
                                        blurRadius = 4f
                                    )
                                )
                                Text(
                                    text = "Halo, $displayName",
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        shadow = photoHeadingStyle.shadow
                                    )
                                )
                                // Dua baris ini jadi fokus utama di header Beranda.
                                Text(
                                    text = "Mau ke mana",
                                    color = Color.White,
                                    modifier = Modifier.graphicsLayer {
                                        alpha = firstTitleProgress.value
                                        translationY = (1f - firstTitleProgress.value) * 18.dp.toPx()
                                    },
                                    style = photoHeadingStyle.copy(
                                        textMotion = TextMotion.Animated
                                    )
                                )
                                Text(
                                    text = "hari ini?",
                                    color = Color.White,
                                    modifier = Modifier.graphicsLayer {
                                        alpha = secondTitleProgress.value
                                        translationY = (1f - secondTitleProgress.value) * 12.dp.toPx()
                                    },
                                    style = photoHeadingStyle.copy(
                                        textMotion = TextMotion.Animated
                                    )
                                )
                            }
                            // Tetap pakai foto profil yang sama dengan layar akun.
                            Surface(
                                shape = CircleShape,
                                color = Mint,
                                modifier = Modifier
                                    .size(48.dp)
                                    .shadow(
                                        8.dp,
                                        CircleShape
                                    )
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = currentUser.firstOrNull()?.uppercase() ?: "?",
                                        color = Forest,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (!profileImageUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = profileImageUrl,
                                            contentDescription = "Foto profil",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                // Naikkan panel ke atas foto sambil menjaga tinggi item tetap rapat.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .layout { measurable, constraints ->
                            val panel = measurable.measure(constraints)
                            // Sisakan gambar di bawah supaya transisi melengkung tetap tampak.
                            val overlap = 24.dp.roundToPx()
                            layout(
                                width = panel.width,
                                height = (panel.height - overlap).coerceAtLeast(0)
                            ) {
                                panel.placeRelative(
                                    x = 0,
                                    y = -overlap
                                )
                            }
                        }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(
                                RoundedCornerShape(
                                    topStart = 40.dp,
                                    topEnd = 40.dp
                                )
                            )
                            .background(TourifySurface)
                            // Sisakan jarak tipis sebelum judul fitur.
                            .padding(bottom = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                // Tempatkan kategori rapat di bawah searchbar.
                                .padding(top = 40.dp)
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 20.dp)
                            ) {
                                DestinationCategoryFilters(
                                    selectedCategory = selectedCategory,
                                    onCategorySelected = {
                                        selectedCategory = it
                                    }
                                )
                            }
                        }
                    }
                    // Pusatkan searchbar di batas foto dan panel putih.
                    DestinationSearchBar(
                        searchQuery = searchQuery,
                        onSearchQueryChange = {
                            searchQuery = it
                        },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = (-32).dp)
                            .padding(horizontal = 20.dp)
                    )
                }
            }

        }

        // Judul dan kartu unggulan jadi satu bagian di daftar.
        item {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Judul pendek menjaga nada tetap akrab dan cepat dipindai.
                Text(
                    text = "Rekomendasi untukmu",
                    color = Ink,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))

                if (viewModel.isLoadingWahana && featured == null) {
                    Box(
                        modifier = Modifier.padding(horizontal = 20.dp)
                    ) {
                        FeaturedDestinationSkeleton()
                    }
                } else if (featured != null) {
                    Card(
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .fillMaxWidth()
                            // Kartu diringkas, dengan tinggi penuh untuk ukuran teks besar.
                            .heightIn(min = featuredCardMinHeight)
                            .aspectRatio(featuredCardAspectRatio),
                        shape = TourifyShapes.card,
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = TourifyElevation.destinationCard,
                            pressedElevation = TourifyElevation.floating
                        ),
                        onClick = {
                            onDestinationClick(featured.id)
                        }
                    ) {
                        Box(
                            Modifier.fillMaxSize()
                        ) {
                            val featuredPhoto = painterResource(destinationImage(featured.id))
                            Image(
                                painter = featuredPhoto,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        compositingStrategy = CompositingStrategy.Offscreen
                                    }

                                    // Gelapkan bagian bawah foto supaya teks tetap terbaca.
                                    .drawWithContent {
                                        drawContent()
                                        drawRect(
                                            brush = Brush.verticalGradient(
                                                0f to Color.Transparent,
                                                0.52f to Color.Transparent,
                                                0.76f to Color.Black,
                                                1f to Color.Black
                                            ),
                                            blendMode = BlendMode.DstIn
                                        )
                                    }
                            ) {

                                Image(
                                    painter = featuredPhoto,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .blur(18.dp)
                                )
                            }
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(
                                        // Gradasi gelap bikin teks terbaca di atas foto.
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                TourifyForestDeep.copy(alpha = 0.85f),
                                                TourifyForestDeep.copy(alpha = 0.6f),
                                                TourifyForestDeep.copy(alpha = 0.13f),
                                                Color.Transparent
                                            )
                                        )
                                    )
                            )
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(72.dp)
                                    .align(Alignment.BottomCenter)
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                TourifyForestDeep.copy(alpha = 0.34f)
                                            )
                                        )
                                    )
                            )
                            // Nama destinasi tampil dulu, lalu ringkasannya di bawah.
                            Column(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .fillMaxWidth()
                                    .padding(
                                        horizontal = 18.dp,
                                        vertical = 20.dp
                                    ),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = featured.name,
                                    color = Color.White,
                                    fontSize = 21.sp,
                                    lineHeight = 26.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = featured.description,
                                    color = Color.White.copy(alpha = 0.88f),
                                    fontSize = 14.sp,
                                    lineHeight = 19.sp,
                                    // Batasi ringkasan supaya foto dan harga tetap jadi fokus kartu.
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            // Harga dan tombol pesan tetap satu baris di bawah.
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .fillMaxWidth()
                                    .padding(
                                        horizontal = 18.dp,
                                        vertical = 16.dp
                                    ),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "Mulai dari",
                                        color = Color.White.copy(alpha = 0.78f),
                                        fontSize = 14.sp,
                                        lineHeight = 16.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = rupiah(featured.ticketPrice),
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        lineHeight = 19.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Row(
                                    modifier = Modifier.widthIn(max = 190.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Jelajahi sekarang",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        lineHeight = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Icon(
                                        imageVector = Icons.Filled.ArrowOutward,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

        }
        // Judul daftar destinasi.
        item {
            val canOpenDestinations = searchQuery.isBlank() &&
                selectedCategory == DestinationCategory.ALL
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        enabled = canOpenDestinations,
                        role = Role.Button,
                        onClick = onExploreDestinations
                    )
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Ketuk baris untuk membuka daftar destinasi.
                Text(
                    text = when {
                        searchQuery.isNotBlank() -> "Hasil pencarian"
                        selectedCategory != DestinationCategory.ALL ->
                            "Destinasi ${selectedCategory.label.lowercase(Locale.getDefault())}"
                        else -> "Jelajahi destinasi"
                    },
                    color = Ink,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                if (canOpenDestinations) {
                    // Panah berada di ujung kanan baris.
                    Icon(
                        imageVector = Icons.Filled.ArrowOutward,
                        contentDescription = "Buka semua destinasi",
                        tint = Ink,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Tampilkan status memuat, gagal, atau daftar kosong.
        if (viewModel.isLoadingWahana && wahanaList.isEmpty()) {
            item {
                // Samakan batas carousel sementara dengan kartu destinasi.
                LazyRow(
                    modifier = Modifier.padding(start = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        DestinationCardSkeleton(
                            Modifier.width(300.dp)
                        )
                    }
                }
            }
        }
        if (viewModel.wahanaError.isNotEmpty()) {
            item {
                Surface(
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .fillMaxWidth(),
                    color = TourifyErrorContainer,
                    shape = TourifyShapes.input
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            viewModel.wahanaError,
                            color = MaterialTheme.colorScheme.error
                        )
                        TextButton(
                            onClick = {
                                viewModel.loadWahana()
                            }
                        ) {
                            Text(
                                "Coba lagi",
                                color = Forest
                            )
                        }
                    }
                }
            }
        }
        if (wahanaList.isEmpty() && !viewModel.isLoadingWahana && viewModel.wahanaError.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .fillMaxWidth(),
                    color = TourifySurface,
                    shape = TourifyShapes.input
                ) {
                    Text(
                        "Destinasi belum tersedia. Coba lagi nanti.",
                        color = Muted,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    )
                }
            }
        }
        if (filteredWahana.isEmpty() && wahanaList.isNotEmpty()) {
            item {
                Surface(
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .fillMaxWidth(),
                    color = TourifySurface,
                    shape = TourifyShapes.input
                ) {
                    Text(
                        "Tidak ada destinasi yang cocok dengan pilihanmu.",
                        color = Muted,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    )
                }
            }
        }
        // Tampilkan carousel pada halaman awal Beranda.
        if (searchQuery.isBlank() && selectedCategory == DestinationCategory.ALL) {
            item {
                // Biarkan carousel melebar sampai tepi layar supaya kartu berikutnya terlihat utuh.
                LazyRow(
                    modifier = Modifier.padding(start = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(
                        filteredWahana,
                        key = {
                            it.id
                        }
                    ) { wahana ->
                        // Kartu ikut bergeser saat hasil kategori berubah.
                        DestinationCard(
                            wahana = wahana,
                            modifier = Modifier
                                .animateItem(
                                    fadeInSpec = tween(TourifyMotion.standardMillis),
                                    placementSpec = tween(TourifyMotion.standardMillis),
                                    fadeOutSpec = tween(TourifyMotion.standardMillis)
                                )
                                .width(300.dp)
                                // Kartu lebih pendek agar carousel terbaca sebelum navbar.
                                .aspectRatio(destinationCardAspectRatio),
                            onClick = {
                                onDestinationClick(wahana.id)
                            }
                        )
                    }
                }
            }
        } else {
            // Rasio lebih kecil bikin kartu penuh terlihat lebih tinggi.
            items(
                filteredWahana,
                key = {
                    it.id
                }
            ) { wahana ->
                // Kartu baru dan lama bergerak dengan durasi yang sama.
                DestinationCard(
                    wahana = wahana,
                    modifier = Modifier
                        .animateItem(
                            fadeInSpec = tween(TourifyMotion.standardMillis),
                            placementSpec = tween(TourifyMotion.standardMillis),
                            fadeOutSpec = tween(TourifyMotion.standardMillis)
                        )
                        .padding(horizontal = 20.dp)
                        .fillMaxWidth()
                        .aspectRatio(1.7f),
                    onClick = {
                        onDestinationClick(wahana.id)
                    }
                )
            }
        }
        // Kunjungan pribadi muncul setelah jelajah, hanya pada tampilan Beranda utama.
        if (upcomingTicket != null && searchQuery.isBlank() && selectedCategory == DestinationCategory.ALL) {
            item(key = "upcoming-visit") {
                UpcomingVisitSection(
                    ticket = upcomingTicket,
                    refreshError = viewModel.ticketsError,
                    isRefreshing = viewModel.isLoadingTickets,
                    onOpenTickets = onOpenTickets,
                    onOpenTicket = {
                        onOpenTicket(upcomingTicket.id)
                    },
                    onRetry = {
                        viewModel.loadTickets(
                            username = currentUser,
                            accessToken = accessToken
                        )
                    }
                )
            }
        }
        // Panduan berangkat tetap tersedia saat belum ada tiket dan tidak muncul di hasil pencarian.
        if (searchQuery.isBlank() && selectedCategory == DestinationCategory.ALL) {
            item(key = "before-departure") {
                BeforeDepartureSection(
                    onOpenVisit = {
                        if (upcomingTicket != null) {
                            onOpenTicket(upcomingTicket.id)
                        } else {
                            onOpenTickets()
                        }
                    },
                    onOpenAssistant = onOpenAssistant
                )
            }
        }
    }

}

// Tiga pintasan ringan membantu pengunjung menyiapkan tiket sebelum berangkat.
@Composable
internal fun BeforeDepartureSection(
    onOpenVisit: () -> Unit,
    onOpenAssistant: () -> Unit
) {
    var showEntryGuide by rememberSaveable {
        mutableStateOf(false)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Sebelum berangkat",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Ink
        )
        Surface(
            shape = TourifyShapes.card,
            color = TourifySurface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                // Setiap baris punya aksi sendiri dan ikon dekoratif tanpa label berulang.
                val titles = listOf(
                    "Cara masuk wahana",
                    "Cek detail kunjungan",
                    "Butuh bantuan?"
                )
                val icons = listOf(
                    Icons.Default.QrCode,
                    Icons.AutoMirrored.Filled.EventNote,
                    Icons.AutoMirrored.Filled.HelpOutline
                )
                titles.forEachIndexed { index, title ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                role = Role.Button,
                                onClick = {
                                    when (index) {
                                        0 -> showEntryGuide = true
                                        1 -> onOpenVisit()
                                        else -> onOpenAssistant()
                                    }
                                }
                            )
                            .heightIn(min = 48.dp)
                            .padding(
                                horizontal = 12.dp,
                                vertical = 10.dp
                            ),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icons[index],
                            contentDescription = null,
                            tint = Forest,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = title,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Ink
                        )
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = Muted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
    // Panduan bisa ditutup tanpa meninggalkan Beranda atau dilanjutkan ke tiket.
    if (showEntryGuide) {
        AlertDialog(
            onDismissRequest = {
                showEntryGuide = false
            },
            title = {
                Text(text = "Cara masuk wahana")
            },
            text = {
                Text(text = "1. Buka tiket sesuai tanggal kunjungan.\n\n2. Ketuk QR untuk memperbesar.\n\n3. Tunjukkan QR ke petugas saat masuk.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showEntryGuide = false
                        onOpenVisit()
                    }
                ) {
                    Text(text = "Buka tiket")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showEntryGuide = false
                    }
                ) {
                    Text(text = "Tutup")
                }
            }
        )
    }
}

// Ringkasan kunjungan terdekat memakai tinggi fleksibel agar teks besar tetap muat.
@Composable
internal fun UpcomingVisitSection(
    ticket: Ticket,
    refreshError: String,
    isRefreshing: Boolean,
    onOpenTickets: () -> Unit,
    onOpenTicket: () -> Unit,
    onRetry: () -> Unit
) {
    val largeText = LocalDensity.current.fontScale >= 1.3f
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Kunjungan berikutnya",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Ink
            )
            TextButton(onClick = onOpenTickets) {
                Text(
                    text = "Semua tiket ›",
                    color = Forest
                )
            }
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = TourifyShapes.card,
            color = TourifySurface,
            shadowElevation = TourifyElevation.card
        ) {
            // Tombol sejajar pada ukuran normal, turun saat ruang atau ukuran teks membutuhkannya.
            BoxWithConstraints {
                val inlineButton = maxWidth >= 320.dp && !largeText
                val qrButton: @Composable (Modifier) -> Unit = { buttonModifier ->
                    Button(
                        onClick = onOpenTicket,
                        modifier = buttonModifier.heightIn(min = 48.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        shape = TourifyShapes.pill,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Forest,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Lihat QR",
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(ticketWahanaImage(ticket.wahanaName)),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(14.dp))
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = ticket.wahanaName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Ink,
                                maxLines = if (largeText) Int.MAX_VALUE else 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${formatTicketDate(ticket.visitDate)} · ${ticket.quantity} tiket",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Muted
                            )
                        }
                        if (inlineButton) {
                            qrButton(Modifier)
                        }
                    }
                    if (!inlineButton) {
                        qrButton(Modifier.align(Alignment.End))
                    }
                    // Data lama tetap tersedia saat penyegaran gagal, dengan aksi coba lagi.
                    if (refreshError.isNotBlank()) {
                        Text(
                            text = "Status belum diperbarui",
                            style = MaterialTheme.typography.bodySmall,
                            color = Muted
                        )
                        TextButton(
                            onClick = onRetry,
                            enabled = !isRefreshing
                        ) {
                            Text(text = if (isRefreshing) "Memperbarui…" else "Coba lagi")
                        }
                    }
                }
            }
        }
    }
}

// Kartu foto punya bayangan jelas dan pintasan ke detail.
@Composable
internal fun DestinationCard(
    wahana: Wahana,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier,
        shape = TourifyShapes.card,
        elevation = CardDefaults.cardElevation(
            defaultElevation = TourifyElevation.destinationCard,
            pressedElevation = TourifyElevation.floating
        ),
        onClick = onClick
    ) {
        Box(
            Modifier.fillMaxSize()
        ) {
            Image(
                painter = painterResource(destinationImage(wahana.id)),
                contentDescription = "Foto ${wahana.name}",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                TourifyForestDeep.copy(alpha = 0.13f),
                                Color.Transparent,
                                TourifyForestDeep.copy(alpha = 0.67f),
                                TourifyForestDeep.copy(alpha = 0.9f)
                            )
                        )
                    )
            )
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = wahana.name,
                        color = Color.White,
                        fontSize = 19.sp,
                        lineHeight = 23.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = wahana.description,
                        color = Color.White.copy(alpha = 0.88f),
                        fontSize = 14.sp,
                        lineHeight = 19.sp,
                        // Dua baris menjaga kartu carousel tetap ringan saat dipindai.
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Surface(
                    modifier = Modifier.size(42.dp),
                    shape = CircleShape,
                    color = TourifyGlassSurface,
                    border = BorderStroke(
                        1.dp,
                        TourifyGlassHighlight
                    )
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ArrowOutward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }
            }
        }
    }
}

// Gambar lokal dipilih mengikuti ID destinasi.
internal fun destinationImage(id: Int): Int = when (id) {
    1 -> R.drawable.dunia_petualang
    2 -> R.drawable.samudra_biru
    3 -> R.drawable.pulau_air
    4 -> R.drawable.pantai_mentari
    5 -> R.drawable.taman_satwa_pesisir
    else -> R.drawable.dunia_petualang
}

@Composable
private fun FeaturedDestinationSkeleton() {
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1.45f)
    ) {
        SkeletonBlock(
            Modifier.fillMaxSize(),
            RoundedCornerShape(26.dp)
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SkeletonBlock(
                Modifier
                    .width(132.dp)
                    .height(22.dp),
                RoundedCornerShape(8.dp)
            )
            SkeletonBlock(
                Modifier
                    .width(208.dp)
                    .height(28.dp),
                RoundedCornerShape(8.dp)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                SkeletonBlock(
                    Modifier
                        .width(120.dp)
                        .height(15.dp),
                    RoundedCornerShape(7.dp)
                )
                Spacer(Modifier.width(20.dp))
                SkeletonBlock(
                    Modifier
                        .width(112.dp)
                        .height(36.dp),
                    RoundedCornerShape(18.dp)
                )
            }
        }
    }
}

@Composable
private fun DestinationCardSkeleton(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = TourifySurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column {
            SkeletonBlock(
                Modifier
                    .fillMaxWidth()
                    .height(142.dp),
                RoundedCornerShape(
                    topStart = 22.dp,
                    topEnd = 22.dp
                )
            )
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                SkeletonBlock(
                    Modifier
                        .fillMaxWidth(0.7f)
                        .height(15.dp),
                    RoundedCornerShape(7.dp)
                )
                SkeletonBlock(
                    Modifier
                        .fillMaxWidth()
                        .height(12.dp),
                    RoundedCornerShape(6.dp)
                )
                SkeletonBlock(
                    Modifier
                        .fillMaxWidth(0.78f)
                        .height(12.dp),
                    RoundedCornerShape(6.dp)
                )
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SkeletonBlock(
                        Modifier
                            .width(94.dp)
                            .height(16.dp),
                        RoundedCornerShape(7.dp)
                    )
                    Spacer(Modifier.weight(1f))
                    SkeletonBlock(
                        Modifier
                            .width(112.dp)
                            .height(36.dp),
                        RoundedCornerShape(12.dp)
                    )
                }
            }
        }
    }
}

// Animasi cahaya ini dipakai pada kartu sementara destinasi dan tiket.
@Composable
internal fun SkeletonBlock(
    modifier: Modifier,
    shape: RoundedCornerShape
) {
    val transition = rememberInfiniteTransition(label = "skeletonShimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1100,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "skeletonProgress"
    )
    val bandStart = 560f * (progress - 1f)
    val shimmer = Brush.linearGradient(
        colors = listOf(
            TourifyBorder,
            TourifySoftSurface,
            TourifyBorder
        ),
        start = Offset(
            bandStart,
            0f
        ),
        end = Offset(
            bandStart + 560f,
            0f
        )
    )
    Box(
        modifier.background(
            shimmer,
            shape
        )
    )
}

@Preview(
    showBackground = true,
    heightDp = 1000
)
@Composable
fun HomeScreenPreview() {
    HomeScreen(
        viewModel = TourifyViewModel(),
        currentUser = "admin",
        profileImageUrl = null,
        onDestinationClick = {},
        onExploreDestinations = {},
        welcomeTitleIntroPending = false,
        playWelcomeTitleIntro = false
    )
}
