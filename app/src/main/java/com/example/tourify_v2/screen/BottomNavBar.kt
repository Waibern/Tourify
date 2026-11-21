package com.example.tourify_v2.screen

import com.example.tourify_v2.ui.theme.*

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

private data class NavItem(
    val route: String?,
    val label: String,
    val icon: ImageVector
)

// Semua halaman dan Asisten AI memakai slot yang sama di navbar.
private val navItems = listOf(
    NavItem(
        "home",
        "Beranda",
        Icons.Default.Home
    ),
    NavItem(
        "destinations",
        "Destinasi",
        Icons.Default.Explore
    ),
    NavItem(
        null,
        "Asisten AI",
        Icons.Default.AutoAwesome
    ),
    NavItem(
        "ticket",
        "Tiket",
        Icons.AutoMirrored.Filled.ReceiptLong
    ),
    NavItem(
        "account",
        "Profil",
        Icons.Default.Person
    )
)

// Tab yang dipilih melebar dan menampilkan namanya.
@Composable
fun BottomNavBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onAssistantClick: () -> Unit,
    onAssistantCenterChanged: (Offset) -> Unit,
    modifier: Modifier = Modifier
) {
    // Nama tab tetap terbaca di layar sempit atau saat ukuran teks dibesarkan.
    val largeTextEnabled = LocalDensity.current.fontScale >= 1.3f
    val compactWidthEnabled = LocalConfiguration.current.screenWidthDp <= 360
    val selectedTabWeight = if (largeTextEnabled || compactWidthEnabled) 1f else 1.9f
    val activeIndex = navItems.indexOfFirst {
        it.route == currentRoute
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(
                // Lebarkan bar supaya tiap tab punya ruang lebih lega.
                horizontal = 10.dp,
                vertical = 8.dp
            )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                // Tinggi 72 dp memberi ruang ikon dan area sentuh yang lebih lega.
                .height(72.dp),
            shape = TourifyShapes.pill,
            // Putih solid menggantikan latar kaca navbar.
            color = TourifySurface,
            border = BorderStroke(
                1.dp,
                TourifyBorder
            ),
            shadowElevation = TourifyElevation.floating
        ) {
            // Pill bergerak sendiri, sementara slot navigasi tetap ringan.
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(6.dp)
            ) {
                val totalWeight = if (activeIndex >= 0) {
                    navItems.size - 1 + selectedTabWeight
                } else {
                    navItems.size.toFloat()
                }
                val slotWidth = maxWidth / totalWeight
                val activeItem = navItems.getOrNull(activeIndex)
                val targetPillOffset = if (activeIndex >= 0) {
                    slotWidth * activeIndex + 2.dp
                } else {
                    0.dp
                }
                val targetPillWidth = if (activeIndex >= 0) {
                    slotWidth * selectedTabWeight - 4.dp
                } else {
                    0.dp
                }
                val pillOffset by animateDpAsState(
                    targetValue = targetPillOffset,
                    animationSpec = tween(
                        durationMillis = TourifyMotion.standardMillis,
                        easing = TourifyMotion.navigationEasing
                    ),
                    label = "navPillOffset"
                )
                val pillWidth by animateDpAsState(
                    targetValue = targetPillWidth,
                    animationSpec = tween(
                        durationMillis = TourifyMotion.standardMillis,
                        easing = TourifyMotion.navigationEasing
                    ),
                    label = "navPillWidth"
                )

                if (activeItem != null) {
                    // Latar pill meluncur ke tab yang dipilih.
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .offset(x = pillOffset)
                            .width(pillWidth.coerceAtLeast(0.dp))
                            .fillMaxHeight()
                            .padding(vertical = 2.dp)
                            .clip(TourifyShapes.pill)
                            .background(TourifyForestBright)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Semua item memakai slot yang sama seperti susunan awal.
                    navItems.forEach { item ->
                        NavigationTab(
                            item = item,
                            currentRoute = currentRoute,
                            selectedTabWeight = selectedTabWeight,
                            onNavigate = onNavigate,
                            onAssistantClick = onAssistantClick,
                            onAssistantCenterChanged = onAssistantCenterChanged
                        )
                    }
                }

                if (activeItem != null) {
                    // Ikon dan teks ikut bergerak bersama pill.
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .offset(x = pillOffset)
                            .width(pillWidth.coerceAtLeast(0.dp))
                            .fillMaxHeight()
                            .padding(vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!largeTextEnabled && !compactWidthEnabled) {
                                Icon(
                                    imageVector = activeItem.icon,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Text(
                                text = activeItem.label,
                                modifier = Modifier.padding(
                                    start = if (largeTextEnabled || compactWidthEnabled) {
                                        0.dp
                                    } else {
                                        6.dp
                                    }
                                ),
                                color = Color.White,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

// Setiap item memakai ukuran dan animasi yang sama.
@Composable
private fun RowScope.NavigationTab(
    item: NavItem,
    currentRoute: String,
    selectedTabWeight: Float,
    onNavigate: (String) -> Unit,
    onAssistantClick: () -> Unit,
    onAssistantCenterChanged: (Offset) -> Unit
) {
    val isSelected = item.route != null && item.route == currentRoute
    val itemWeight = if (isSelected) selectedTabWeight else 1f
    // Simpan titik tengah item asisten untuk animasi membuka chat.
    val positionModifier = if (item.route == null) {
        Modifier.onGloballyPositioned { coordinates ->
            val position = coordinates.positionInRoot()
            onAssistantCenterChanged(
                Offset(
                    x = position.x + coordinates.size.width / 2f,
                    y = position.y + coordinates.size.height / 2f
                )
            )
        }
    } else {
        Modifier
    }
    // Label hanya muncul pada tab aktif agar navbar tetap ringkas.
    Row(
        modifier = Modifier
            .weight(itemWeight)
            .fillMaxHeight()
            .then(positionModifier)
            .clip(TourifyShapes.pill)
            .semantics(mergeDescendants = true) {
                contentDescription = item.label
                selected = isSelected
            }
            .clickable(
                role = Role.Tab,
                onClick = {
                    val route = item.route
                    if (route == null) {
                        onAssistantClick()
                    } else {
                        onNavigate(route)
                    }
                }
            )
            // Pertahankan area klik yang sama untuk semua tab.
            .padding(
                horizontal = 2.dp,
                vertical = 2.dp
            ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!isSelected) {
            // Ikon aktif ditampilkan di pill yang bergerak.
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = TourifyMuted,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BottomNavBarPreview() {
    MaterialTheme {
        BottomNavBar(
            currentRoute = "home",
            onNavigate = {},
            onAssistantClick = {},
            onAssistantCenterChanged = {}
        )
    }
}
