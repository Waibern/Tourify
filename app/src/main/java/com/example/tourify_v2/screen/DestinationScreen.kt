package com.example.tourify_v2.screen

import com.example.tourify_v2.ui.theme.*

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import com.example.tourify_v2.model.TicketBookingRequest
import com.example.tourify_v2.model.Wahana
import com.example.tourify_v2.viewmodel.TourifyViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DestinationGreen = TourifyForest
private val DestinationInk = TourifyInk
private val DestinationMuted = TourifyMuted
private val DestinationMint = TourifyMint
private val DestinationCanvas = TourifyCanvas

// Pakai token login saat memesan tiket.
@Composable
fun DestinationScreen(
    destinationId: Int,
    viewModel: TourifyViewModel,
    currentUser: String,
    accessToken: String,
    onBack: () -> Unit,
    onBookingSuccess: () -> Unit
) {
    // Ambil destinasi yang dipilih dan siapkan form tiket.
    val destination = viewModel.wahana.firstOrNull {
        it.id == destinationId
    }
    var showBookingForm by remember(destinationId) {
        mutableStateOf(false)
    }
    val context = LocalContext.current

    BackHandler(enabled = showBookingForm) {
        showBookingForm = false
    }

    DisposableEffect(
        context,
        destination != null,
        showBookingForm
    ) {
        val window = (context as? Activity)?.window
        val insetsController = window?.let {
            WindowCompat.getInsetsController(
                it,
                it.decorView
            )
        }
        insetsController?.isAppearanceLightStatusBars = destination == null
        // Ikon di atas foto berwarna terang; saat form putih dibuka, ikonnya jadi gelap.
        insetsController?.isAppearanceLightNavigationBars = destination == null || showBookingForm
        onDispose {
            insetsController?.isAppearanceLightStatusBars = true
            insetsController?.isAppearanceLightNavigationBars = true
        }
    }

    LaunchedEffect(destinationId) {
        if (viewModel.wahana.isEmpty()) viewModel.loadWahana()
    }

    // Tampilkan detail atau status memuat/gagal.
    when {
        destination != null -> {
            Box(
                Modifier.fillMaxSize()
            ) {
                DestinationContent(
                    destination = destination,
                    onBack = onBack,
                    onShare = {
                        val shareText = "${destination.name} — ${destination.description} Tiket ${
                            rupiah(destination.ticketPrice)
                        } per orang di Tourify."
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                shareText
                            )
                        }
                        context.startActivity(
                            Intent.createChooser(
                                intent,
                                "Bagikan destinasi"
                            )
                        )
                    },
                    onBook = {
                        viewModel.clearBookingMessage()
                        showBookingForm = true
                    }
                )
                DestinationBookingForm(
                    destination = destination,
                    currentUser = currentUser,
                    accessToken = accessToken,
                    viewModel = viewModel,
                    visible = showBookingForm,
                    onDismiss = {
                        showBookingForm = false
                    },
                    onBookingSuccess = onBookingSuccess
                )
            }
        }

        else -> DestinationUnavailable(
            message = when {
                viewModel.isLoadingWahana ||
                    (
                        viewModel.wahana.isEmpty() &&
                            viewModel.wahanaError.isEmpty()
                    ) -> "Memuat destinasi..."
                viewModel.wahanaError.isNotEmpty() -> viewModel.wahanaError
                else -> "Destinasi tidak ditemukan."
            },
            isLoading =
                viewModel.isLoadingWahana ||
                    (
                        viewModel.wahana.isEmpty() &&
                            viewModel.wahanaError.isEmpty()
                    ),
            onBack = onBack,
            onRetry = {
                viewModel.loadWahana()
            }
        )
    }
}

// Tampilkan foto, detail, dan tombol pesan tiket.
@Composable
internal fun DestinationContent(
    destination: Wahana,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onBook: () -> Unit
) {
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val destinationPhoto = painterResource(destinationImage(destination.id))

        Image(
            painter = destinationPhoto,
            contentDescription = "Foto ${destination.name}",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        // Foto blur berubah perlahan jadi tajam supaya sambungannya nggak kelihatan.
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                }
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.55f to Color.Transparent,
                            0.74f to Color.Black,
                            1f to Color.Black
                        ),
                        blendMode = BlendMode.DstIn
                    )
                }
        ) {
            Image(
                painter = destinationPhoto,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(10.dp)
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    // Fade hitam bikin judul putih terbaca tanpa menutup foto sepenuhnya.
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.34f),
                        0.35f to Color.Transparent,
                        0.55f to Color.Black.copy(alpha = 0.27f),
                        0.78f to Color.Black.copy(alpha = 0.79f),
                        1f to Color.Black.copy(alpha = 0.95f)
                    )
                )
        )

        // Tombol kembali dan bagikan destinasi.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(
                    start = 20.dp,
                    top = 12.dp,
                    end = 20.dp
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DestinationIconButton(
                onClick = onBack,
                onPhoto = true,
                icon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = Color.White
                    )
                }
            )
            DestinationIconButton(
                onClick = onShare,
                onPhoto = true,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Bagikan destinasi",
                        tint = Color.White
                    )
                }
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .heightIn(max = maxHeight * 0.67f)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = 24.dp,
                    end = 24.dp,
                    bottom = 20.dp
                ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = TourifyGlassSurface,
                border = BorderStroke(
                    1.dp,
                    TourifyGlassHighlight
                )
            ) {
                Text(
                    "${rupiah(destination.ticketPrice)} / orang",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(
                        horizontal = 14.dp,
                        vertical = 8.dp
                    )
                )
            }
            Text(
                text = destination.name,
                color = Color.White,
                fontSize = 32.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
            Text(
                text = destination.description,
                color = Color.White.copy(alpha = 0.88f),
                fontSize = 15.sp,
                lineHeight = 22.sp
            )
            // Tombol utama pakai hijau biar aksennya nggak terlalu kuning.
            Button(
                onClick = onBook,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .heightIn(min = 58.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = DestinationGreen)
            ) {
                Text(
                    "Pesan tiket",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// Tombol bulat untuk kembali atau membagikan destinasi.
@Composable
private fun DestinationIconButton(
    onClick: () -> Unit,
    onPhoto: Boolean = false,
    icon: @Composable () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = if (onPhoto) {
            TourifyGlassSurface
        } else {
            TourifySurface.copy(alpha = 0.94f)
        },
        border = if (onPhoto) BorderStroke(
            1.dp,
            TourifyGlassHighlight
        ) else null,
        shadowElevation = 2.dp
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(48.dp)
        ) {
            icon()
        }
    }
}

// Tampilkan tombol kembali kalau destinasi belum tersedia.
@Composable
private fun DestinationUnavailable(
    message: String,
    isLoading: Boolean,
    onBack: () -> Unit,
    onRetry: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(DestinationCanvas)
            .statusBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        DestinationIconButton(
            onClick = onBack,
            icon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Kembali",
                    tint = DestinationInk
                )
            }
        )
        Spacer(Modifier.weight(1f))
        if (isLoading) CircularProgressIndicator(
            color = DestinationGreen
        )
        Text(
            message,
            color = DestinationInk,
            fontSize = 18.sp,
            lineHeight = 25.sp
        )
        if (!isLoading && message != "Destinasi tidak ditemukan.") {
            OutlinedButton(
                onClick = onRetry
            ) {
                Text(
                    "Coba lagi"
                )
            }
        }
        Spacer(Modifier.weight(1f))
    }
}

// Form ini menyimpan tanggal, jumlah tiket, dan cara bayar.
@Composable
private fun DestinationBookingForm(
    destination: Wahana,
    currentUser: String,
    accessToken: String,
    viewModel: TourifyViewModel,
    visible: Boolean,
    onDismiss: () -> Unit,
    onBookingSuccess: () -> Unit
) {
    // Data yang dipilih untuk pesanan.
    var quantity by remember(destination.id) {
        mutableIntStateOf(1)
    }
    var paymentMethod by remember(destination.id) {
        mutableStateOf("Bank")
    }
    var showDatePicker by remember {
        mutableStateOf(false)
    }
    var selectedDate by remember(destination.id) {
        mutableStateOf<LocalDate?>(null)
    }
    val visitDate = selectedDate?.format(
        DateTimeFormatter.ofPattern(
            "dd MMM yyyy",
            Locale.forLanguageTag("id-ID")
        )
    ).orEmpty()
    val paymentMethods = listOf(
        "Bank",
        "QRIS",
        "E-Wallet"
    )
    val totalPrice = quantity * destination.ticketPrice

    LaunchedEffect(visible) {
        if (!visible) showDatePicker = false
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        AnimatedVisibility(
            visible = visible,
            modifier = Modifier.matchParentSize(),
            enter = fadeIn(tween(240)),
            exit = fadeOut(tween(200))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.54f))
                    .clickable(onClick = onDismiss)
            )
        }
        AnimatedVisibility(
            visible = visible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(
                initialOffsetY = { height ->
                    height
                },
                animationSpec = tween(
                    420,
                    easing = FastOutSlowInEasing
                )
            ),
            exit = slideOutVertically(
                targetOffsetY = { height ->
                    height
                },
                animationSpec = tween(
                    320,
                    easing = FastOutSlowInEasing
                )
            )
        ) {
            Card(
                onClick = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxHeight * 0.9f),
                shape = RoundedCornerShape(
                    topStart = 28.dp,
                    topEnd = 28.dp
                ),
                colors = CardDefaults.cardColors(containerColor = TourifySurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .navigationBarsPadding()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Pesan tiket",
                            modifier = Modifier.weight(1f),
                            fontSize = 25.sp,
                            fontWeight = FontWeight.Bold,
                            color = DestinationInk
                        )
                        IconButton(
                            onClick = onDismiss
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Tutup form pemesanan",
                                tint = DestinationMuted
                            )
                        }
                    }
                    Surface(
                        shape = TourifyShapes.card,
                        color = DestinationMint
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Image(
                                painter = painterResource(destinationImage(destination.id)),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.matchParentSize()
                            )
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                Color.Black.copy(alpha = 0.62f),
                                                Color.Black.copy(alpha = 0.25f)
                                            )
                                        )
                                    )
                            )
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 96.dp)
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    Modifier.size(48.dp),
                                    shape = TourifyShapes.input,
                                    color = Color.Transparent
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = Color.White
                                        )
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(
                                    Modifier.weight(1f)
                                ) {
                                    Text(
                                        destination.name,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        "${rupiah(destination.ticketPrice)} per tiket",
                                        color = Color.White,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column(
                            modifier = Modifier.weight(1.5f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                "Tanggal kunjungan",
                                fontWeight = FontWeight.SemiBold,
                                color = DestinationInk,
                                fontSize = 14.sp
                            )
                            OutlinedButton(
                                onClick = {
                                    showDatePicker = true
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 52.dp),
                                shape = TourifyShapes.input,
                                border = BorderStroke(
                                    1.dp,
                                    TourifyOutline
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = DestinationGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    if (visitDate.isEmpty()) "Pilih tanggal" else visitDate,
                                    color = DestinationInk,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                "Jumlah tiket",
                                fontWeight = FontWeight.SemiBold,
                                color = DestinationInk,
                                fontSize = 14.sp
                            )
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = TourifyShapes.input,
                                color = TourifySurface,
                                border = BorderStroke(
                                    1.dp,
                                    TourifyOutline
                                )
                            ) {
                                Row(
                                    modifier = Modifier.heightIn(min = 52.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    IconButton(
                                        onClick = {
                                            quantity--
                                        },
                                        enabled = quantity > 1,
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Remove,
                                            contentDescription = "Kurangi tiket",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Text(
                                        quantity.toString(),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DestinationInk
                                    )
                                    IconButton(
                                        onClick = {
                                            quantity++
                                        },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Tambah tiket",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "Metode pembayaran",
                            fontWeight = FontWeight.SemiBold,
                            color = DestinationInk
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            paymentMethods.forEach { method ->
                                FilterChip(
                                    selected = paymentMethod == method,
                                    onClick = {
                                        paymentMethod = method
                                    },
                                    label = {
                                        Text(
                                            method,
                                            fontSize = 12.sp
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = TourifySurface,
                                        labelColor = DestinationInk,
                                        selectedContainerColor = DestinationMint,
                                        selectedLabelColor = DestinationGreen
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    // Tampilkan total langsung tanpa panel latar.
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Total",
                            color = DestinationMuted,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            rupiah(totalPrice),
                            color = DestinationGreen,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    // Tombol konfirmasi pakai hijau yang sama dengan layar destinasi.
                    Button(
                        onClick = {
                            val date = selectedDate?.takeUnless {
                                it.isBefore(LocalDate.now())
                            } ?: return@Button
                            viewModel.bookTicket(
                                username = currentUser,
                                accessToken = accessToken,
                                request = TicketBookingRequest(
                                    wahanaId = destination.id,
                                    visitDate = date.toString(),
                                    quantity = quantity,
                                    paymentMethod = paymentMethod
                                )
                            ) { success ->
                                if (success) {
                                    onDismiss()
                                    onBookingSuccess()
                                }
                            }
                        },
                        enabled =
                            !viewModel.isBooking &&
                                currentUser.isNotBlank() &&
                                selectedDate?.isBefore(LocalDate.now()) == false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DestinationGreen,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            if (viewModel.isBooking) "Memproses..." else "Pesan tiket",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (visitDate.isEmpty()) Text(
                        "Pilih tanggal kunjungan untuk melanjutkan.",
                        color = DestinationMuted,
                        fontSize = 12.sp
                    )
                    if (viewModel.bookingMessage.isNotEmpty()) {
                        Text(
                            viewModel.bookingMessage,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        CompactDatePicker(
            selectedDate = selectedDate,
            onSelect = { date ->
                selectedDate = date
                showDatePicker = false
            },
            onDismiss = {
                showDatePicker = false
            }
        )
    }
}

// Tanggal kunjungan bisa dipilih mulai hari ini.
@Composable
private fun CompactDatePicker(
    selectedDate: LocalDate?,
    onSelect: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val locale = remember {
        Locale.forLanguageTag("id-ID")
    }
    val monthFormatter = remember {
        DateTimeFormatter.ofPattern(
            "MMMM yyyy",
            locale
        )
    }
    val dayFormatter = remember {
        DateTimeFormatter.ofPattern(
            "EEEE, d MMMM yyyy",
            locale
        )
    }
    val today = LocalDate.now()
    var displayedMonth by remember {
        mutableStateOf(
            YearMonth.from(
                selectedDate?.takeUnless {
                    it.isBefore(today)
                } ?: today
            )
        )
    }
    val firstDayOffset = displayedMonth.atDay(1).dayOfWeek.value - 1
    val weekCount = (firstDayOffset + displayedMonth.lengthOfMonth() + 6) / 7
    val weekDays = listOf(
        "Sen",
        "Sel",
        "Rab",
        "Kam",
        "Jum",
        "Sab",
        "Min"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .widthIn(max = 340.dp)
                .fillMaxWidth(),
            shape = TourifyShapes.sheet,
            color = TourifySurface,
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Pilih tanggal",
                        modifier = Modifier.weight(1f),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = DestinationInk
                    )
                    IconButton(
                        onClick = onDismiss
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup kalender",
                            tint = DestinationMuted
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = {
                            displayedMonth = displayedMonth.minusMonths(1)
                        },
                        enabled = displayedMonth > YearMonth.from(today)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Bulan sebelumnya",
                            tint = DestinationGreen
                        )
                    }
                    Text(
                        displayedMonth.atDay(1).format(monthFormatter),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DestinationInk
                    )
                    IconButton(
                        onClick = {
                            displayedMonth = displayedMonth.plusMonths(1)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Bulan berikutnya",
                            tint = DestinationGreen
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    weekDays.forEach { day ->
                        Text(
                            text = day,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            fontSize = 11.sp,
                            color = DestinationMuted
                        )
                    }
                }
                repeat(weekCount) { week ->
                    Row(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        repeat(7) { weekday ->
                            val day = week * 7 + weekday - firstDayOffset + 1
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (day in 1..displayedMonth.lengthOfMonth()) {
                                    val date = displayedMonth.atDay(day)
                                    val isPast = date.isBefore(today)
                                    val isSelected = date == selectedDate && !isPast
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected) DestinationGreen else Color.Transparent
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (date == today && !isSelected) {
                                                    DestinationGreen
                                                } else {
                                                    Color.Transparent
                                                },
                                                shape = CircleShape
                                            )
                                            .clickable(enabled = !isPast) {
                                                if (!date.isBefore(LocalDate.now())) {
                                                    onSelect(date)
                                                }
                                            }
                                            .semantics {
                                                contentDescription = if (isPast) {
                                                    "${date.format(dayFormatter)}, sudah lewat"
                                                } else {
                                                    date.format(dayFormatter)
                                                }
                                                selected = isSelected
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = day.toString(),
                                            fontSize = 13.sp,
                                            color = when {
                                                isSelected -> Color.White
                                                isPast -> TourifyMuted
                                                else -> DestinationInk
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(
    showBackground = true,
    widthDp = 390,
    heightDp = 844
)
@Composable
private fun DestinationContentPreview() {
    DestinationContent(
        destination = Wahana(
            1,
            "Ocean Park",
            "Taman bermain air di BSD City.",
            100000
        ),
        onBack = {},
        onShare = {},
        onBook = {}
    )
}
