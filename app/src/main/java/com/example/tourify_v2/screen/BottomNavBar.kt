package com.example.tourify_v2.screen

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class NavItem(val route: String, val label: String, val icon: ImageVector)

private val navItems = listOf(
    NavItem("home", "Beranda", Icons.Default.Home),
    NavItem("destinations", "Destination", Icons.Default.Explore),
    NavItem("ticket", "Tiket", Icons.AutoMirrored.Filled.ReceiptLong),
    NavItem("account", "Akun", Icons.Default.Person)
)

private val NavGreen = Color(0xFF145C4B)
private val NavMuted = Color(0xFF87918C)
private val NavCanvas = Color.White

// navigasi bawah dan animasi tab aktif
@Composable
fun BottomNavBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedIndex = navItems.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 14.dp, top = 22.dp, end = 14.dp, bottom = 8.dp)
            .height(64.dp)
    ) {
        val bubbleSize = 58.dp
        val itemWidth = maxWidth / navItems.size.toFloat()
        val targetBubbleX = itemWidth * (selectedIndex + 0.5f) - bubbleSize / 2
        val bubbleX by animateDpAsState(
            targetValue = targetBubbleX,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "activeNavBubblePosition"
        )

        // bar dan tombol tiap tab
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(26.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFE9ECE8)),
            shadowElevation = 4.dp,
            tonalElevation = 0.dp
        ) {
            Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                navItems.forEachIndexed { index, item ->
                    NavBarButton(
                        item = item,
                        selected = selectedIndex == index,
                        onClick = { onNavigate(item.route) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // lingkaran putih bikin tab aktif kelihatan naik
        Box(
            modifier = Modifier
                .offset(x = bubbleX - 8.dp, y = (-28).dp)
                .size(74.dp)
                .background(NavCanvas, CircleShape)
        )

        Surface(
            modifier = Modifier
                .offset(x = bubbleX, y = (-22).dp)
                .size(bubbleSize)
                .clickable(
                    role = Role.Tab,
                    onClick = { onNavigate(navItems[selectedIndex].route) }),
            shape = CircleShape,
            color = NavGreen,
            border = BorderStroke(3.dp, Color.White),
            shadowElevation = 3.dp,
            tonalElevation = 0.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Crossfade(
                    targetState = selectedIndex,
                    animationSpec = tween(180),
                    label = "activeNavIcon"
                ) { index ->
                    Icon(
                        imageVector = navItems[index].icon,
                        contentDescription = navItems[index].label,
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun NavBarButton(
    item: NavItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // tampilan ikon dan label tab
    val iconAlpha by animateFloatAsState(
        targetValue = if (selected) 0f else 1f,
        animationSpec = tween(180),
        label = "navIconAlpha"
    )
    val labelOffset by animateDpAsState(
        targetValue = if (selected) 6.dp else 0.dp,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "navLabelOffset"
    )
    val labelColor by animateColorAsState(
        targetValue = if (selected) NavGreen else NavMuted,
        animationSpec = tween(180),
        label = "navLabelColor"
    )

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable(role = Role.Tab, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = null,
            tint = NavMuted,
            modifier = Modifier
                .size(23.dp)
                .graphicsLayer { alpha = iconAlpha }
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = item.label,
            color = labelColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.offset(y = labelOffset)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun BottomNavBarPreview() {
    MaterialTheme {
        BottomNavBar(currentRoute = "home", onNavigate = {})
    }
}
