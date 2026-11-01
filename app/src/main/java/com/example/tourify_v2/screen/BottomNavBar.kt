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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Route kosong dipakai untuk aksi yang membuka asisten.
private data class NavItem(
    val route: String?,
    val label: String,
    val icon: ImageVector
)

// Tab akun dibuka sebagai halaman profil.
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
        "ticket",
        "Tiket",
        Icons.AutoMirrored.Filled.ReceiptLong
    ),
    NavItem(
        "account",
        "Profil",
        Icons.Default.Person
    ),
    NavItem(
        null,
        "Asisten",
        Icons.Default.AutoAwesome
    )
)

// Tab aktif melebar dan menampilkan nama seperti referensi.
@Composable
fun BottomNavBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onAssistantClick: () -> Unit,
    onAssistantCenterChanged: (Offset) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(
                horizontal = 14.dp,
                vertical = 8.dp
            ),
        shape = RoundedCornerShape(50),
        // Abu-abu transparan memberi efek kaca dan membiarkan konten belakang terlihat.
        color = TourifyGlassSurface,
        border = BorderStroke(
            1.dp,
            TourifyGlassHighlight
        ),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .padding(5.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Atur ukuran, warna, dan aksi setiap tab sesuai pilihan aktif.
            navItems.forEach { item ->
                val isSelected = item.route != null && item.route == currentRoute
                val itemWeight by animateFloatAsState(
                    targetValue = if (isSelected) 1.9f else 1f,
                    animationSpec = tween(250),
                    label = "navItemWidth"
                )
                val background by animateColorAsState(
                    targetValue = if (isSelected) TourifyForestBright else Color.Transparent,
                    animationSpec = tween(250),
                    label = "navItemColor"
                )
                // Simpan posisi tombol asisten untuk animasi membuka chat.
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
                Row(
                    modifier = Modifier
                        .weight(itemWeight)
                        .fillMaxHeight()
                        .then(positionModifier)
                        // Biar efek tekan ikut bentuk bulat tombol.
                        .clip(RoundedCornerShape(50))
                        .background(
                            background,
                            RoundedCornerShape(50)
                        )
                        .semantics(mergeDescendants = true) {
                            contentDescription = item.label
                            if (item.route != null) selected = isSelected
                        }
                        .clickable(
                            role = if (item.route == null) Role.Button else Role.Tab,
                            onClick = {
                                val route = item.route
                                if (route == null) {
                                    onAssistantClick()
                                } else {
                                    onNavigate(route)
                                }
                            }
                        ),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = if (isSelected) 1f else 0.9f),
                        modifier = Modifier.size(23.dp)
                    )
                    AnimatedVisibility(
                        visible = isSelected
                    ) {
                        Text(
                            text = item.label,
                            modifier = Modifier.padding(start = 6.dp),
                            color = Color.White,
                            fontSize = 12.sp,
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
