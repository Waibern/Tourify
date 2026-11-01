package com.example.tourify_v2.screen

import com.example.tourify_v2.ui.theme.*

import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.tourify_v2.model.Wahana
import com.example.tourify_v2.R
import com.example.tourify_v2.viewmodel.TourifyViewModel
import java.text.NumberFormat
import java.util.Locale

private val Forest = TourifyForest
private val Mint = TourifyMint
private val Canvas = Color.White
private val Ink = TourifyInk
private val Muted = TourifyMuted

internal enum class DestinationCategory(
    val label: String,
    val icon: ImageVector,
    val keywords: List<String>

    // list buat kategori
) {
    ALL(
        "Semua",
        Icons.Filled.Explore,
        emptyList()
    ),
    BEACH(
        "Pantai",
        Icons.Filled.BeachAccess,
        listOf(
            "pantai",
            "pesisir"
        )
    ),
    WATER(
        "Air",
        Icons.Filled.WaterDrop,
        listOf(
            "air",
            "samudra",
            "laut"
        )
    ),
    ANIMALS(
        "Satwa",
        Icons.Filled.Pets,
        listOf(
            "satwa",
            "hewan"
        )
    ),
    ADVENTURE(
        "Petualangan",
        Icons.Filled.Terrain,
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
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        placeholder = {
            Text(
                "Cari destinasi",
                color = Muted
            )
        },
        leadingIcon = {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = Forest
            )
        },
        trailingIcon = if (searchQuery.isNotEmpty()) {
            {
                IconButton(
                    onClick = {
                        onSearchQueryChange("")
                    }
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Hapus pencarian"
                    )
                }
            }
        } else {
            null
        },
        singleLine = true,
        shape = RoundedCornerShape(18.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = Color.White,
            focusedContainerColor = Color.White,
            unfocusedBorderColor = Color(0xFFE4E9E4),
            focusedBorderColor = Forest
        ),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .shadow(
                4.dp,
                RoundedCornerShape(18.dp)
            )
    )

    Spacer(modifier = Modifier.height(12.dp))

    LazyRow(
        modifier = Modifier.selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        items(
            DestinationCategory.entries,
            key = {
                it.name
            }
        ) { category ->
            val isSelected = category == selectedCategory
            val borderColor = if (isSelected) Forest else Color(0xFFE7ECE8)

            Surface(
                modifier = Modifier
                    .height(44.dp)
                    .selectable(
                        selected = isSelected,
                        role = Role.RadioButton,
                        onClick = {
                            onCategorySelected(category)
                        }
                    ),
                shape = CircleShape,
                color = if (isSelected) Forest else Color.White,
                shadowElevation = 3.dp,
                border = BorderStroke(
                    1.dp,
                    borderColor
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = category.icon,
                        contentDescription = null,
                        tint = if (isSelected) Color.White else Muted,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = category.label,
                        color = if (isSelected) Color.White else Ink,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
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
    onDestinationClick: (Int) -> Unit
) {
    val wahanaList = viewModel.wahana
    LaunchedEffect(Unit) {
        viewModel.loadWahana()
    }
    // Simpan pencarian, kategori, dan favorit selama beranda dibuka.
    var searchQuery by remember {
        mutableStateOf("")
    }
    var selectedCategory by remember {
        mutableStateOf(DestinationCategory.ALL)
    }
    var favoriteIds by remember {
        mutableStateOf<Set<Int>>(emptySet())
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Canvas),
        // Sisakan ruang gulir agar isi terakhir tidak tertutup navbar.
        contentPadding = PaddingValues(
            start = 20.dp,
            top = 14.dp,
            end = 20.dp,
            bottom = 104.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        ),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        // sapaan & judul
        item {
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
                    Text(
                        "Hello, $displayName",
                        color = Forest,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Mau ke mana",
                        color = Ink,
                        fontSize = 40.sp,
                        lineHeight = 34.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.4).sp
                    )
                    Spacer(modifier = Modifier.height(0.dp))
                    Text(
                        "hari ini?",
                        color = Ink,
                        fontSize = 40.sp,
                        lineHeight = 34.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.4).sp
                    )
                }
                // Avatar ikut state profil yang sama dengan halaman akun.
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
                            fontSize = 19.sp,
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

        // search bar
        item {
            DestinationFilters(
                searchQuery = searchQuery,
                onSearchQueryChange = {
                    searchQuery = it
                },
                selectedCategory = selectedCategory,
                onCategorySelected = {
                    selectedCategory = it
                }
            )
        }

        //Featured Box Model awas jan lupa pantek

        if (searchQuery.isBlank() && selectedCategory == DestinationCategory.ALL &&
            (viewModel.isLoadingWahana || featured != null)
        ) item {
            Text(
                text = "Recommended Destination",
                color = Ink,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))

            if (viewModel.isLoadingWahana && featured == null) {
                FeaturedDestinationSkeleton()
            } else if (featured != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.45f),
                    shape = RoundedCornerShape(22.dp),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 4.dp,
                        pressedElevation = 6.dp
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

                                //Blur
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
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xD90B263A),
                                            Color(0x990B263A),
                                            Color(0x220B263A),
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
                                            Color(0x55081724)
                                        )
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth(0.78f)
                                .padding(
                                    horizontal = 16.dp,
                                    vertical = 25.dp
                                ),
                            verticalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            Text(
                                text = "Indonesia",
                                color = Color.White.copy(alpha = 0.82f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.8.sp
                            )
                            Text(
                                text = featured.name,
                                color = Color.White,
                                fontSize = 35.sp,
                                lineHeight = 27.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Mulai dari ${rupiah(featured.ticketPrice)}",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Surface(
                                color = TourifyGlassSurface,
                                shape = CircleShape,
                                border = BorderStroke(
                                    1.dp,
                                    TourifyGlassHighlight
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(
                                        horizontal = 12.dp,
                                        vertical = 7.dp
                                    ),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                                ) {
                                    Text(
                                        text = "Jelajahi sekarang",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Icon(
                                        imageVector = Icons.Filled.ArrowOutward,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // judul daftar destinasi
        item {
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        when {
                            searchQuery.isNotBlank() -> "Hasil pencarian"
                            selectedCategory != DestinationCategory.ALL ->
                                "Destinasi ${selectedCategory.label.lowercase(Locale.getDefault())}"
                            else -> "Jelajahi destinasi"
                        },
                        color = Ink,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (searchQuery.isBlank() && selectedCategory == DestinationCategory.ALL) {
                            "Pilih tempat yang ingin kamu kunjungi"
                        } else {
                            "${filteredWahana.size} destinasi ditemukan"
                        },
                        color = Muted,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // state loading, error, dan daftar kosong
        if (viewModel.isLoadingWahana && wahanaList.isEmpty()) {
            item {
                LazyRow(
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
                    color = Color(0xFFFFF1EC),
                    shape = RoundedCornerShape(16.dp)
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
                    color = Color.White,
                    shape = RoundedCornerShape(18.dp)
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
                    color = Color.White,
                    shape = RoundedCornerShape(18.dp)
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
        // card destination
        if (searchQuery.isBlank() && selectedCategory == DestinationCategory.ALL) {
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(
                        filteredWahana,
                        key = {
                            it.id
                        }
                    ) { wahana ->
                        DestinationCard(
                            wahana = wahana,
                            modifier = Modifier
                                .width(300.dp)
                                .aspectRatio(1.45f),
                            isFavorite = wahana.id in favoriteIds,
                            onClick = {
                                onDestinationClick(wahana.id)
                            },
                            onFavoriteClick = {
                                favoriteIds = if (wahana.id in favoriteIds) {
                                    favoriteIds - wahana.id
                                } else {
                                    favoriteIds + wahana.id
                                }
                            }
                        )
                    }
                }
            }
        } else {
            items(
                filteredWahana,
                key = {
                    it.id
                }
            ) { wahana ->
                DestinationCard(
                    wahana = wahana,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.8f),
                    isFavorite = wahana.id in favoriteIds,
                    onClick = {
                        onDestinationClick(wahana.id)
                    },
                    onFavoriteClick = {
                        favoriteIds = if (wahana.id in favoriteIds) {
                            favoriteIds - wahana.id
                        } else {
                            favoriteIds + wahana.id
                        }
                    }
                )
            }
        }
    }

}

@Composable
internal fun DestinationCard(
    wahana: Wahana,
    modifier: Modifier,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onFavoriteClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 5.dp,
            pressedElevation = 7.dp
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
                                Color(0x220C211C),
                                Color.Transparent,
                                Color(0xAA0C211C),
                                Color(0xE60C211C)
                            )
                        )
                    )
            )
            if (onFavoriteClick != null) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(42.dp),
                    shape = CircleShape,
                    color = TourifyGlassSurface,
                    border = BorderStroke(
                        1.dp,
                        TourifyGlassHighlight
                    )
                ) {
                    IconButton(
                        onClick = onFavoriteClick,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = if (isFavorite) {
                                Icons.Filled.Favorite
                            } else {
                                Icons.Filled.FavoriteBorder
                            },
                            contentDescription = if (isFavorite) {
                                "Hapus ${wahana.name} dari favorit"
                            } else {
                                "Simpan ${wahana.name} ke favorit"
                            },
                            tint = if (isFavorite) TourifyFavorite else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
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
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "TIKET · ${rupiah(wahana.ticketPrice)}",
                        color = Color.White.copy(alpha = 0.88f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = wahana.name,
                        color = Color.White,
                        fontSize = 22.sp,
                        lineHeight = 26.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = wahana.description,
                        color = Color.White.copy(alpha = 0.88f),
                        fontSize = 13.sp,
                        lineHeight = 17.sp,
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
        colors = CardDefaults.cardColors(containerColor = Color.White),
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

@Composable
private fun SkeletonBlock(
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
            Color(0xFFE8ECE8),
            Color(0xFFF4F6F3),
            Color(0xFFE8ECE8)
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
        onDestinationClick = {}
    )
}
