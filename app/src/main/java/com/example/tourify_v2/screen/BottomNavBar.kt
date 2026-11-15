package com.example.tourify_v2.screen

import com.example.tourify_v2.ui.theme.*

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
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

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(
                horizontal = 14.dp,
                vertical = 8.dp
            )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                // Padding 5 dp menyisakan area tab 50 dp untuk disentuh.
                .height(60.dp),
            shape = TourifyShapes.pill,
            // Putih solid menggantikan latar kaca navbar.
            color = TourifySurface,
            border = BorderStroke(
                1.dp,
                TourifyBorder
            ),
            shadowElevation = TourifyElevation.floating
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Semua item, termasuk Asisten AI, tersusun rata di bar yang sama.
                navItems.forEach { item ->
                    NavigationTab(
                        item = item,
                        currentRoute = currentRoute,
                        selectedTabWeight = selectedTabWeight,
                        largeTextEnabled = largeTextEnabled,
                        compactWidthEnabled = compactWidthEnabled,
                        onNavigate = onNavigate,
                        onAssistantClick = onAssistantClick,
                        onAssistantCenterChanged = onAssistantCenterChanged
                    )
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
    largeTextEnabled: Boolean,
    compactWidthEnabled: Boolean,
    onNavigate: (String) -> Unit,
    onAssistantClick: () -> Unit,
    onAssistantCenterChanged: (Offset) -> Unit
) {
    val isSelected = item.route != null && item.route == currentRoute
    val itemWeight by animateFloatAsState(
        targetValue = if (isSelected) {
            selectedTabWeight
        } else {
            1f
        },
        animationSpec = tween(TourifyMotion.standardMillis),
        label = "navItemWidth"
    )
    val background by animateColorAsState(
        targetValue = if (isSelected) TourifyForestBright else Color.Transparent,
        animationSpec = tween(TourifyMotion.standardMillis),
        label = "navItemColor"
    )
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
            // Inset mengecilkan pill aktif tanpa mengurangi area klik.
            .padding(
                vertical = if (isSelected) 2.dp else 0.dp
            )
            .background(
                color = background,
                shape = TourifyShapes.pill
            ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!isSelected || (!largeTextEnabled && !compactWidthEnabled)) {
            // Ikon tetap terlihat di tab yang tidak aktif.
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else TourifyMuted,
                modifier = Modifier.size(26.dp)
            )
        }
        AnimatedVisibility(
            visible = isSelected
        ) {
            Text(
                text = item.label,
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
