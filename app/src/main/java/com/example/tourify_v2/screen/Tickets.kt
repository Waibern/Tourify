package com.example.tourify_v2.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.example.tourify_v2.R
import com.example.tourify_v2.model.Ticket
import com.example.tourify_v2.ui.theme.*
import com.example.tourify_v2.viewmodel.TourifyViewModel
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import kotlinx.coroutines.awaitCancellation

// Pakai token akun aktif saat memuat tiket.
@Composable
fun TicketsScreen(
    viewModel: TourifyViewModel,
    currentUser: String,
    accessToken: String,
    showBookingSuccess: Boolean = false,
    onDismissBookingSuccess: () -> Unit = {},
    onTicketDetailVisibilityChanged: (Boolean) -> Unit = {},
    onExploreDestinations: () -> Unit = {},
    // ID dari Beranda dikonsumsi sekali supaya tombol kembali tetap membuka daftar.
    requestedTicketId: String? = null,
    onTicketRequestConsumed: () -> Unit = {}
) {
    // Muat ulang tiket saat kembali ke aplikasi, tapi simpan data lama kalau gagal.
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(
        currentUser,
        accessToken,
        lifecycleOwner
    ) {
        if (currentUser.isNotBlank()) {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.loadTickets(
                    username = currentUser,
                    accessToken = accessToken
                )
                awaitCancellation()
            }
        }
    }
    key(currentUser) {
        TicketsContent(
            tickets = if (viewModel.ticketsOwner == currentUser) viewModel.tickets else emptyList(),
            isRefreshing = viewModel.isLoadingTickets,
            refreshError = viewModel.ticketsError,
            updatedAtMillis = viewModel.ticketsUpdatedAtMillis,
            onRefresh = {
                viewModel.loadTickets(
                    username = currentUser,
                    accessToken = accessToken
                )
            },
            showBookingSuccess = showBookingSuccess,
            onDismissBookingSuccess = onDismissBookingSuccess,
            onTicketDetailVisibilityChanged = onTicketDetailVisibilityChanged,
            onExploreDestinations = onExploreDestinations,
            requestedTicketId = requestedTicketId,
            onTicketRequestConsumed = onTicketRequestConsumed,
            onHideHistoryTicket = { ticketId, onComplete ->
                viewModel.hideTicketFromHistory(
                    username = currentUser,
                    accessToken = accessToken,
                    ticketId = ticketId,
                    onComplete = onComplete
                )
            }
        )
    }
}

// Pisahkan isi layar dari permintaan server supaya kondisi gagal dan teks besar bisa diuji.
@Composable
internal fun TicketsContent(
    tickets: List<Ticket>,
    isRefreshing: Boolean = false,
    refreshError: String = "",
    updatedAtMillis: Long? = null,
    onRefresh: () -> Unit = {},
    showBookingSuccess: Boolean = false,
    onDismissBookingSuccess: () -> Unit = {},
    onTicketDetailVisibilityChanged: (Boolean) -> Unit = {},
    onExploreDestinations: () -> Unit = {},
    // ID dari Beranda dikonsumsi sekali supaya tombol kembali tetap membuka daftar.
    requestedTicketId: String? = null,
    onTicketRequestConsumed: () -> Unit = {},
    // Aksi hapus hanya tersedia untuk tiket milik akun yang sedang login.
    onHideHistoryTicket: (String, (Boolean, String) -> Unit) -> Unit = { _, onComplete ->
        onComplete(
            false,
            "Riwayat tiket gagal dihapus. Coba lagi."
        )
    }
) {
    // Simpan ID tiket supaya perubahan ukuran teks nggak menutup tiket yang dibuka.
    var showHistory by rememberSaveable {
        mutableStateOf(false)
    }
    var selectedTicketId by rememberSaveable {
        mutableStateOf<String?>(null)
    }
    // Simpan kata pencarian setelah QR dibuka atau ukuran teks diubah.
    var searchQuery by rememberSaveable {
        mutableStateOf("")
    }
    val focusManager = LocalFocusManager.current
    val ticketListState = rememberLazyListState()
    // Tunggu tiket tersedia sebelum membuka detail yang diminta Beranda.
    LaunchedEffect(requestedTicketId, tickets) {
        if (requestedTicketId != null && tickets.any { it.id == requestedTicketId }) {
            selectedTicketId = requestedTicketId
            onTicketRequestConsumed()
        }
    }
    val selectedTicket = tickets.firstOrNull {
        it.id == selectedTicketId
    }
    val visibleTickets = visibleTickets(
        tickets,
        showHistory,
        searchQuery
    )
    // Tandai tiket yang sudah muncul supaya animasinya nggak berulang saat digulir.
    val appearedTicketIds = remember {
        mutableSetOf<String>()
    }
    LaunchedEffect(
        searchQuery,
        showHistory
    ) {
        ticketListState.scrollToItem(0)
    }
    LaunchedEffect(selectedTicket?.id) {
        onTicketDetailVisibilityChanged(selectedTicket != null)
    }
    DisposableEffect(Unit) {
        onDispose {
            onTicketDetailVisibilityChanged(false)
        }
    }
    BackHandler(enabled = selectedTicket != null) {
        selectedTicketId = null
    }

    // Perubahan check-in nggak mengulang animasi pindah halaman.
    AnimatedContent(
        targetState = selectedTicket?.id,
        modifier = Modifier.fillMaxSize(),
        transitionSpec = {
            val direction = if (targetState != null) 1 else -1
            slideInHorizontally(
                initialOffsetX = {
                    it * direction
                },
                animationSpec = tween(260)
            ) togetherWith slideOutHorizontally(
                targetOffsetX = {
                    -it * direction
                },
                animationSpec = tween(260)
            )
        },
        label = "ticketScreenSlide"
    ) { ticketId ->
        val ticket = tickets.firstOrNull {
            it.id == ticketId
        }
        if (ticket != null) {
            TicketDetailScreen(
                ticket = ticket,
                onDismiss = {
                    selectedTicketId = null
                },
                isRefreshing = isRefreshing,
                refreshError = refreshError,
                updatedAtMillis = updatedAtMillis,
                onRefresh = onRefresh,
                onHideHistoryTicket = onHideHistoryTicket
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(TourifyCanvas)
                    .blur(if (showBookingSuccess) 6.dp else 0.dp)
            ) {
                TicketTopBar(
                    title = "Tiket Saya",
                    isRefreshing = isRefreshing,
                    onRefresh = onRefresh
                )
                // Satukan pencarian dan pilihan status tiket.
                TicketSearchBar(
                    searchQuery = searchQuery,
                    onSearchQueryChange = {
                        searchQuery = it
                    },
                    showHistory = showHistory,
                    onSelectHistory = {
                        showHistory = it
                    },
                    modifier = Modifier.padding(
                        horizontal = 20.dp,
                        vertical = 12.dp
                    )
                )
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    state = ticketListState,
                    // Beri ruang bawah supaya tiket terakhir bisa digulir melewati navbar.
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        top = 4.dp,
                        bottom = 104.dp + WindowInsets
                            .navigationBars
                            .asPaddingValues()
                            .calculateBottomPadding()
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (tickets.isNotEmpty()) {
                        item(key = "sync") {
                            TicketSyncNotice(
                                isRefreshing = isRefreshing,
                                refreshError = refreshError,
                                updatedAtMillis = updatedAtMillis,
                                onRefresh = onRefresh
                            )
                        }
                    }
                    // Tampilkan kartu sementara saat tiket pertama kali dimuat.
                    if (isRefreshing && tickets.isEmpty() && refreshError.isBlank()) {
                        items(
                            count = 2,
                            key = {
                                "loading-ticket-$it"
                            }
                        ) {
                            TicketCardSkeleton()
                        }
                    }
                    // Tampilkan pesan gagal tanpa menghapus daftar tiket yang ada.
                    if (refreshError.isNotBlank() && tickets.isEmpty()) {
                        item(key = "error") {
                            TicketMessage(
                                title = "Tiket belum bisa dimuat",
                                detail = "Periksa koneksi lalu coba lagi.",
                                onRetry = onRefresh
                            )
                        }
                    } else if (!isRefreshing && visibleTickets.isEmpty()) {
                        item(key = "empty") {
                            TicketMessage(
                                title = when {
                                    searchQuery.isNotBlank() -> "Tiket tidak ditemukan"
                                    showHistory -> "Belum ada riwayat tiket"
                                    else -> "Belum ada tiket aktif"
                                },
                                detail = when {
                                    searchQuery.isNotBlank() -> "Coba nama destinasi atau kode tiket lain."
                                    showHistory -> "Tiket yang sudah terpakai atau kedaluwarsa tampil di sini."
                                    tickets.isNotEmpty() -> "Tiket yang sudah selesai ada di Riwayat."
                                    else -> "Pesan tiket dari halaman Beranda."
                                },
                                onExplore = if (
                                    tickets.isEmpty() &&
                                    searchQuery.isBlank() &&
                                    !showHistory
                                ) {
                                    onExploreDestinations
                                } else {
                                    null
                                }
                            )
                        }
                    }
                    itemsIndexed(
                        items = visibleTickets,
                        key = { _, item ->
                            item.id
                        }
                    ) { index, item ->
                        val delayMillis = if (index < 4) index * 40 else 0
                        val enterTransition = remember(item.id) {
                            if (item.id in appearedTicketIds) {
                                EnterTransition.None
                            } else {
                                fadeIn(
                                    animationSpec = tween(
                                        durationMillis = 260,
                                        delayMillis = delayMillis,
                                        easing = FastOutSlowInEasing
                                    )
                                ) + slideInVertically(
                                    initialOffsetY = {
                                        it / 12
                                    },
                                    animationSpec = tween(
                                        durationMillis = 260,
                                        delayMillis = delayMillis,
                                        easing = FastOutSlowInEasing
                                    )
                                )
                            }
                        }
                        SideEffect {
                            appearedTicketIds.add(item.id)
                        }
                        AnimatedVisibility(
                            visible = true,
                            enter = enterTransition,
                            label = "ticketCardFadeUp"
                        ) {
                            TicketCard(
                                ticket = item,
                                onShowTicket = {
                                    // Tutup keyboard supaya QR punya ruang penuh.
                                    focusManager.clearFocus()
                                    selectedTicketId = item.id
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    if (showBookingSuccess) {
        BookingSuccessDialog(
            onDismiss = onDismissBookingSuccess
        )
    }
}

// Satukan pencarian dan filter status dalam satu kapsul tanpa garis tepi.
@Composable
private fun TicketSearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    showHistory: Boolean,
    onSelectHistory: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    var statusMenuExpanded by remember {
        mutableStateOf(false)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .shadow(
                3.dp,
                TourifyShapes.pill
            )
            .clip(TourifyShapes.pill)
            .background(TourifySurface),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Ikon dan input pencarian tetap berada di sisi kiri.
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = TourifyForest,
            modifier = Modifier.padding(horizontal = 10.dp)
        )
        BasicTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = TourifyInk
            ),
            cursorBrush = SolidColor(TourifyForest),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    focusManager.clearFocus()
                }
            ),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp),
            decorationBox = { innerTextField ->
                Box {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "Cari tiket",
                            color = TourifyMuted,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    innerTextField()
                }
            }
        )
        if (searchQuery.isNotEmpty()) {
            IconButton(
                onClick = {
                    onSearchQueryChange("")
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Hapus pencarian"
                )
            }
        }
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(28.dp)
                .background(TourifyOutline.copy(alpha = 0.35f))
        )
        // Pilihan Aktif atau Riwayat ada di ujung kanan kolom cari.
        Box {
            TextButton(
                onClick = {
                    statusMenuExpanded = true
                },
                modifier = Modifier.heightIn(min = 52.dp),
                contentPadding = PaddingValues(
                    start = 8.dp,
                    end = 12.dp
                )
            ) {
                Text(
                    text = if (showHistory) "Riwayat" else "Aktif",
                    color = TourifyForest,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = "Pilih status tiket",
                    tint = TourifyForest
                )
            }
            DropdownMenu(
                expanded = statusMenuExpanded,
                onDismissRequest = {
                    statusMenuExpanded = false
                }
            ) {
                listOf(
                    false to "Aktif",
                    true to "Riwayat"
                ).forEach { (history, label) ->
                    DropdownMenuItem(
                        text = {
                            Text(label)
                        },
                        onClick = {
                            focusManager.clearFocus()
                            onSelectHistory(history)
                            statusMenuExpanded = false
                        },
                        trailingIcon = if (showHistory == history) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = TourifyForest
                                )
                            }
                        } else {
                            null
                        }
                    )
                }
            }
        }
    }
}

// Judul Tiket sejajar dengan judul layar lain.
@Composable
private fun TicketTopBar(
    title: String,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onDeleteHistory: (() -> Unit)? = null,
    canDeleteHistory: Boolean = true,
    onBack: (() -> Unit)? = null,
    backDescription: String = "Kembali ke daftar tiket"
) {
    // Latar menyambung sampai status bar; judul tetap di bawah ikon.
    Column(
        modifier = Modifier.statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(
                    horizontal = 12.dp,
                    vertical = 4.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                IconButton(
                    onClick = onBack
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = backDescription,
                        tint = TourifyForest
                    )
                }
            }
            // Judul daftar tiket rata kiri; detail tiket tetap rata tengah.
            // TalkBack tetap mengenali judul ini sebagai heading.
            Text(
                text = title,
                modifier = Modifier
                    .weight(1f)
                    .semantics {
                        heading()
                    },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TourifyInk,
                textAlign = if (onBack == null) TextAlign.Start else TextAlign.Center
            )
            // Hapus riwayat lewat kanan atas; tiket aktif belum bisa dihapus.
            if (onDeleteHistory != null) {
                IconButton(
                    onClick = onDeleteHistory,
                    enabled = canDeleteHistory
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Hapus riwayat tiket",
                        tint = if (canDeleteHistory) TourifyError else TourifyMuted
                    )
                }
            } else {
                IconButton(
                    onClick = onRefresh,
                    enabled = !isRefreshing
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Perbarui tiket",
                        tint = if (isRefreshing) TourifyMuted else TourifyForest
                    )
                }
            }
        }
        if (isRefreshing) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = TourifyForest,
                trackColor = TourifyMint
            )
        }
    }
}

// Tampilkan status saat tiket dimuat ulang atau permintaan server gagal.
@Composable
private fun TicketSyncNotice(
    isRefreshing: Boolean,
    refreshError: String,
    updatedAtMillis: Long?,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                liveRegion = LiveRegionMode.Polite
            },
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (refreshError.isNotBlank()) {
            Text(
                text = "Status belum bisa diperbarui",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = TourifyError
            )
            Text(
                text = "Tiket yang sudah dimuat tetap bisa dibuka. Periksa koneksi lalu coba lagi.",
                style = MaterialTheme.typography.bodyMedium,
                color = TourifyMuted
            )
        }
        if (isRefreshing) {
            Text(
                text = "Memperbarui status tiket…",
                style = MaterialTheme.typography.bodyMedium,
                color = TourifyInfo
            )
        }
        if (refreshError.isNotBlank()) {
            TextButton(
                onClick = onRefresh,
                enabled = !isRefreshing
            ) {
                Text(
                    "Coba lagi",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

// Tampilkan status kosong atau gagal dengan tampilan sederhana.
@Composable
private fun TicketMessage(
    title: String,
    detail: String,
    onRetry: (() -> Unit)? = null,
    onExplore: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 12.dp,
                vertical = 28.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
            contentDescription = null,
            tint = TourifyForest,
            modifier = Modifier.size(36.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TourifyInk,
            textAlign = TextAlign.Center
        )
        Text(
            text = detail,
            style = MaterialTheme.typography.bodyLarge,
            color = TourifyMuted,
            textAlign = TextAlign.Center
        )
        if (onRetry != null) {
            Button(
                onClick = onRetry
            ) {
                Text(
                    "Coba lagi"
                )
            }
        }
        if (onExplore != null) {
            Button(
                onClick = onExplore,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TourifyForestBright,
                    contentColor = Color.White
                )
            ) {
                Text(
                    "Jelajahi destinasi"
                )
            }
        }
    }
}

// Cekungan di sisi kartu bikin bentuknya mirip tiket.
private val ticketDetailShape = GenericShape { size, _ ->
    val corner = size.width * 0.065f
    val notch = size.width * 0.04f
    // Cekungan sejajar dengan garis putus-putus di atas QR.
    val notchY = size.height * 0.645f

    moveTo(corner, 0f)
    lineTo(size.width - corner, 0f)
    quadraticTo(size.width, 0f, size.width, corner)
    lineTo(size.width, notchY - notch)
    cubicTo(
        size.width - notch * 0.6f,
        notchY - notch,
        size.width - notch,
        notchY - notch * 0.6f,
        size.width - notch,
        notchY
    )
    cubicTo(
        size.width - notch,
        notchY + notch * 0.6f,
        size.width - notch * 0.6f,
        notchY + notch,
        size.width,
        notchY + notch
    )
    lineTo(size.width, size.height - corner)
    quadraticTo(size.width, size.height, size.width - corner, size.height)
    lineTo(corner, size.height)
    quadraticTo(0f, size.height, 0f, size.height - corner)
    lineTo(0f, notchY + notch)
    cubicTo(
        notch * 0.6f,
        notchY + notch,
        notch,
        notchY + notch * 0.6f,
        notch,
        notchY
    )
    cubicTo(
        notch,
        notchY - notch * 0.6f,
        notch * 0.6f,
        notchY - notch,
        0f,
        notchY - notch
    )
    lineTo(0f, corner)
    quadraticTo(0f, 0f, corner, 0f)
    close()
}

// Kartu tiket menampilkan foto wahana, info kunjungan, dan kode booking.
@Composable
internal fun TicketCard(
    ticket: Ticket,
    onShowTicket: () -> Unit = {}
) {
    // Bedakan tiket yang masih berlaku dengan tiket riwayat.
    val canShowQrCode = canShowTicketQr(ticket)
    val isExpired = isTicketExpired(
        ticket.status,
        ticket.visitDate
    )
    val total = ticketPrice(ticket.totalPrice)
    // Foto hitam-putih hanya untuk tiket kedaluwarsa.
    val photoColorFilter = remember(isExpired) {
        if (isExpired) {
            ColorFilter.colorMatrix(
                ColorMatrix().apply {
                    setToSaturation(0f)
                }
            )
        } else {
            null
        }
    }
    val context = LocalContext.current
    val clipboardManager = remember(context) {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = TourifyShapes.card,
        color = TourifySurface,
        border = BorderStroke(1.dp, TourifyBorder),
        shadowElevation = TourifyElevation.floating
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Foto memenuhi bagian atas kartu dan mengikuti sudut kartunya.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(128.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 20.dp,
                            topEnd = 20.dp,
                            bottomEnd = 0.dp,
                            bottomStart = 0.dp,
                        )
                    )
            ) {
                Image(
                    painter = painterResource(ticketWahanaImage(ticket.wahanaName)),
                    contentDescription = "Foto wahana ${ticket.wahanaName}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    colorFilter = photoColorFilter
                )
                // Nama wahana tetap besar, putih, dan terbaca di atas foto.
                Text(
                    text = ticket.wahanaName,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp),
                    style = MaterialTheme.typography.titleLarge.copy(
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.8f),
                            offset = Offset(0f, 3f),
                            blurRadius = 10f
                        )
                    ),
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Info tiket tetap berjarak dari tepi kartu.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Tampilkan tanggal kunjungan, jumlah tiket, dan status.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatTicketDate(ticket.visitDate),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TourifyForest
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TourifyMuted
                        )
                        Text(
                            text = "${ticket.quantity} tiket",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TourifyInk
                        )
                    }
                    // Tampilkan status sebagai chip yang konsisten dan mudah dibaca.
                    TicketStatusBadge(ticket)
                }

                // Ringkas tanggal pemesanan dan metode, lalu taruh harga di kanan.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${formatTicketDate(ticket.bookingDate)} · ${ticket.paymentMethod}",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = TourifyMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = total,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TourifyForest
                    )
                }
                if (ticket.checkedInQuantity > 0) {
                    Text(
                        text = ticketVisitorSummary(ticket),
                        style = MaterialTheme.typography.bodySmall,
                        color = TourifyMuted
                    )
                }

                TicketDashedDivider()

                // Salin kode booking, atau ketuk teksnya untuk membuka tiket lengkap.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = "Kode booking",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TourifyMuted
                        )
                        Text(
                            text = ticket.id,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TourifyInk,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = {
                            clipboardManager.setPrimaryClip(
                                ClipData.newPlainText(
                                    "Kode booking",
                                    ticket.id
                                )
                            )
                            Toast.makeText(
                                context,
                                "Kode booking disalin",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Salin kode booking ${ticket.id}",
                            tint = TourifyMuted
                        )
                    }
                    Row(
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .clip(TourifyShapes.pill)
                            .background(TourifyForestBright)
                            .clickable(
                                role = Role.Button,
                                onClickLabel = if (canShowQrCode) {
                                    "Lihat QR"
                                } else {
                                    "Lihat tiket"
                                },
                                onClick = onShowTicket
                            )
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (canShowQrCode) {
                                Icons.Default.QrCode
                            } else {
                                Icons.AutoMirrored.Filled.ReceiptLong
                            },
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (canShowQrCode) "Lihat QR" else "Lihat tiket",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

// Kartu sementara mengikuti susunan kartu tiket.
@Composable
private fun TicketCardSkeleton() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = "Memuat tiket"
            },
        shape = TourifyShapes.card,
        color = TourifySurface,
        border = BorderStroke(1.dp, TourifyBorder),
        shadowElevation = TourifyElevation.floating
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SkeletonBlock(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(128.dp),
                shape = RoundedCornerShape(16.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SkeletonBlock(
                    modifier = Modifier
                        .fillMaxWidth(0.62f)
                        .height(22.dp),
                    shape = RoundedCornerShape(7.dp)
                )
                SkeletonBlock(
                    modifier = Modifier
                        .width(60.dp)
                        .height(26.dp),
                    shape = RoundedCornerShape(50.dp)
                )
            }
            SkeletonBlock(
                modifier = Modifier
                    .fillMaxWidth(0.48f)
                    .height(16.dp),
                shape = RoundedCornerShape(6.dp)
            )
            TicketDashedDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SkeletonBlock(
                        modifier = Modifier
                            .width(96.dp)
                            .height(14.dp),
                        shape = RoundedCornerShape(6.dp)
                    )
                    SkeletonBlock(
                        modifier = Modifier
                            .fillMaxWidth(0.82f)
                            .height(18.dp),
                        shape = RoundedCornerShape(6.dp)
                    )
                }
                SkeletonBlock(
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(50.dp)
                )
                SkeletonBlock(
                    modifier = Modifier
                        .width(116.dp)
                        .height(48.dp),
                    shape = RoundedCornerShape(50.dp)
                )
            }
        }
    }
}

// Tanggal kunjungan ditonjolkan; tanggal beli tetap ada di ringkasan.
@Composable
private fun TicketVisitDate(
    value: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = formatTicketDate(value),
        modifier = modifier,
        style = MaterialTheme.typography.bodyLarge,
        color = TourifyInk
    )
}

// Samakan nama dan warna status di semua layar tiket.
@Composable
private fun TicketStatusBadge(ticket: Ticket) {
    // Warna dan ikon badge mengikuti status QR tiket.
    val canShowQrCode = canShowTicketQr(ticket)
    val isExpired = isTicketExpired(
        ticket.status,
        ticket.visitDate
    )
    val statusContainerColor = if (isExpired) {
        TourifyWarningContainer
    } else {
        TourifySoftSurface
    }
    val statusContentColor = if (isExpired) {
        TourifyWarning
    } else {
        TourifyMuted
    }
    if (canShowQrCode) {
        // Tiket aktif ditandai lingkaran hijau dengan centang putih kecil.
        Surface(
            modifier = Modifier.size(24.dp),
            shape = CircleShape,
            color = TourifyForest
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Aktif",
                    modifier = Modifier.size(12.dp),
                    tint = Color.White
                )
            }
        }
    } else {
        Surface(
            shape = TourifyShapes.pill,
            color = statusContainerColor
        ) {
            Text(
                text = ticketStatusLabel(ticket),
                modifier = Modifier.padding(
                    horizontal = 10.dp,
                    vertical = 6.dp
                ),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = statusContentColor,
                maxLines = 1
            )
        }
    }
}

@Composable
internal fun TicketDetailScreen(
    ticket: Ticket,
    onDismiss: () -> Unit,
    isRefreshing: Boolean = false,
    refreshError: String = "",
    updatedAtMillis: Long? = null,
    onRefresh: () -> Unit = {},
    // Gunakan API agar riwayat yang dihapus tetap hilang setelah aplikasi dibuka ulang.
    onHideHistoryTicket: (String, (Boolean, String) -> Unit) -> Unit = { _, onComplete ->
        onComplete(
            false,
            "Riwayat tiket gagal dihapus. Coba lagi."
        )
    }
) {
    // Nyalakan layar QR hanya saat tiket masih bisa dipakai.
    val canShowQrCode = canShowTicketQr(ticket)
    val used = isTicketUsed(ticket)
    val isFinished = !canShowQrCode
    val statusLabel = ticketStatusLabel(ticket).uppercase(Locale.forLanguageTag("id-ID"))
    val passengerName = ticket.username.ifBlank {
        "Pengunjung"
    }
    var showLargeQr by rememberSaveable(ticket.id) {
        mutableStateOf(false)
    }
    // Simpan dialog hapus dan status permintaan selama detail tiket terbuka.
    var showDeleteHistoryConfirmation by rememberSaveable(ticket.id) {
        mutableStateOf(false)
    }
    var isDeletingHistory by remember(ticket.id) {
        mutableStateOf(false)
    }
    var deleteHistoryError by rememberSaveable(ticket.id) {
        mutableStateOf("")
    }
    val context = LocalContext.current
    val view = LocalView.current
    // Biarkan layar menyala saat tiket aktif ditunjukkan ke petugas.
    DisposableEffect(
        view,
        canShowQrCode
    ) {
        val previous = view.keepScreenOn
        view.keepScreenOn = canShowQrCode
        onDispose {
            view.keepScreenOn = previous
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TourifyCanvas)
            .navigationBarsPadding()
            .testTag("ticketDetailViewport")
    ) {
        // Tombol kembali dan judul tetap seperti tampilan tiket sebelumnya.
        TicketTopBar(
            title = "Tiket Saya",
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            // Buka konfirmasi sebelum menghapus riwayat tiket yang selesai.
            onDeleteHistory = {
                deleteHistoryError = ""
                showDeleteHistoryConfirmation = true
            },
            canDeleteHistory = isFinished && !isDeletingHistory,
            onBack = onDismiss
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            // Tampilkan kartu tiket aktif tanpa judul status tambahan.
            if (isFinished) {
                Text(
                    text = "RIWAYAT KUNJUNGAN",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TourifyMuted,
                    letterSpacing = 1.2.sp
                )
                Spacer(Modifier.height(5.dp))
            }
            Spacer(Modifier.height(16.dp))

            // Kartu putih merangkum kunjungan dan kode QR tiket.
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = ticketDetailShape,
                color = Color.White,
                border = BorderStroke(1.dp, TourifyBorder),
                shadowElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(13.dp)
                ) {
                    // Foto dibuat lebih tinggi supaya pas dengan lebar kartu.
                    Image(
                        painter = painterResource(ticketWahanaImage(ticket.wahanaName)),
                        contentDescription = "Foto ${ticket.wahanaName}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(14.dp)),
                        contentScale = ContentScale.Crop
                    )

                    // Tampilkan nama wahana dan tanggal kunjungan di isi tiket.
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(
                            text = ticket.wahanaName,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = TourifyInk,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = formatTicketDate(ticket.visitDate),
                            style = MaterialTheme.typography.bodyLarge,
                            color = TourifyMuted
                        )
                    }

                    // Tampilkan nama dan jumlah pengunjung sebelum detail pembayaran.
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                text = "PENGUNJUNG",
                                style = MaterialTheme.typography.labelMedium,
                                color = TourifyMuted,
                                letterSpacing = 0.8.sp
                            )
                            Text(
                                text = passengerName,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = TourifyInk,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                text = "JUMLAH PENGUNJUNG",
                                style = MaterialTheme.typography.labelMedium,
                                color = TourifyMuted,
                                letterSpacing = 0.8.sp
                            )
                            Text(
                                text = if (ticket.checkedInQuantity > 0) {
                                    "${ticket.checkedInQuantity}/${ticket.quantity} masuk"
                                } else {
                                    "${ticket.quantity} orang"
                                },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = TourifyInk,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    TicketDashedDivider()

                    AdaptiveTicketFields(
                        firstLabel = "METODE DIPILIH",
                        firstValue = ticket.paymentMethod,
                        secondLabel = "TOTAL",
                        secondValue = ticketPrice(ticket.totalPrice)
                    )

                    // Letakkan kode tiket dan tanggal pesan di baris kedua supaya sejajar.
                    AdaptiveTicketFields(
                        firstLabel = "KODE TIKET",
                        firstValue = ticket.id,
                        secondLabel = "DIPESAN",
                        secondValue = formatTicketDate(ticket.bookingDate)
                    )

                    TicketDashedDivider()

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // QR riwayat tetap terlihat dan petunjuknya bisa dibaca tanpa zoom.
                        if (used) {
                            TicketQrCode(
                                ticket = ticket,
                                modifier = Modifier.size(144.dp),
                                isUsed = true
                            )
                            Text(
                                text = ticketEntryMessage(ticket),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TourifyMuted,
                                textAlign = TextAlign.Center
                            )
                        } else if (!canShowQrCode) {
                            Text(
                                text = ticketEntryMessage(ticket),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TourifyMuted,
                                textAlign = TextAlign.Center
                            )
                        } else {
                            TicketQrCode(
                                ticket = ticket,
                                modifier = Modifier
                                    .size(144.dp)
                                    .clickable(
                                        role = Role.Button,
                                        onClickLabel = "Perbesar QR tiket",
                                        onClick = {
                                            showLargeQr = true
                                        }
                                    )
                            )
                            Text(
                                text = "Ketuk QR untuk memperbesar",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TourifyForest,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = if (ticket.checkedInQuantity > 0) {
                                    "Sisa ${ticket.quantity - ticket.checkedInQuantity} orang belum check-in"
                                } else {
                                    "Tunjukkan QR ini saat masuk"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = TourifyMuted,
                                textAlign = TextAlign.Center
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = if (isFinished) TourifySoftSurface else TourifyMint
                        ) {
                            Text(
                                text = statusLabel,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isFinished) TourifyMuted else TourifyForest
                            )
                        }
                    }
                }
            }
            // Tampilkan status refresh hanya saat tiket sedang dimuat atau gagal diperbarui.
            if (refreshError.isNotBlank() || isRefreshing) {
                Spacer(Modifier.height(16.dp))
                TicketSyncNotice(
                    isRefreshing = isRefreshing,
                    refreshError = refreshError,
                    updatedAtMillis = updatedAtMillis,
                    onRefresh = onRefresh
                )
            }
        }
    }
    // Minta konfirmasi sebelum menyembunyikan riwayat tiket dari akun.
    if (showDeleteHistoryConfirmation) {
        AlertDialog(
            onDismissRequest = {
                if (!isDeletingHistory) {
                    showDeleteHistoryConfirmation = false
                }
            },
            title = {
                Text("Hapus riwayat tiket?")
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Tiket ${ticket.wahanaName} akan dihapus dari riwayat akun."
                    )
                    Text(
                        "Catatan booking tetap tersimpan."
                    )
                    if (deleteHistoryError.isNotBlank()) {
                        Text(
                            text = deleteHistoryError,
                            color = TourifyError,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        isDeletingHistory = true
                        deleteHistoryError = ""
                        onHideHistoryTicket(
                            ticket.id
                        ) { success, message ->
                            isDeletingHistory = false
                            if (success) {
                                showDeleteHistoryConfirmation = false
                                Toast.makeText(
                                    context,
                                    message,
                                    Toast.LENGTH_SHORT
                                ).show()
                                onDismiss()
                            } else {
                                deleteHistoryError = message
                            }
                        }
                    },
                    enabled = !isDeletingHistory
                ) {
                    Text(
                        if (isDeletingHistory) "Menghapus..." else "Hapus"
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteHistoryConfirmation = false
                    },
                    enabled = !isDeletingHistory
                ) {
                    Text("Batal")
                }
            }
        )
    }
    if (showLargeQr && (canShowQrCode || used)) {
        LargeTicketQr(
            ticket = ticket,
            isUsed = used,
            onDismiss = {
                showLargeQr = false
            },
            isRefreshing = isRefreshing,
            refreshError = refreshError,
            updatedAtMillis = updatedAtMillis,
            onRefresh = onRefresh
        )
    }
}


// Mode QR memakai seluruh ruang layar, termasuk saat ponsel diputar.
@Composable
private fun LargeTicketQr(
    ticket: Ticket,
    isUsed: Boolean,
    onDismiss: () -> Unit,
    isRefreshing: Boolean,
    refreshError: String,
    updatedAtMillis: Long?,
    onRefresh: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
            ) {
                TicketTopBar(
                    title = "QR masuk",
                    isRefreshing = isRefreshing,
                    onRefresh = onRefresh,
                    onBack = onDismiss,
                    backDescription = "Kembali ke tiket"
                )
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    TicketQrCode(
                        ticket,
                        Modifier.size(
                            minOf(
                                maxWidth,
                                maxHeight
                            )
                        ),
                        isUsed = isUsed
                    )
                }
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = ticket.wahanaName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TourifyInk
                    )
                    TicketVisitDate(
                        ticket.visitDate
                    )
                    Text(
                        text = ticketEntryMessage(ticket),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isUsed) TourifyMuted else TourifyForest
                    )
                    // Tampilkan juga pesan gagal saat QR dibuka besar.
                    if (refreshError.isNotBlank() || isRefreshing) {
                        Text(
                            text = if (refreshError.isNotBlank()) {
                                "Status belum bisa diperbarui. Periksa koneksi."
                            } else {
                                "Memperbarui status tiket…"
                            },
                            modifier = Modifier.semantics {
                                liveRegion = LiveRegionMode.Polite
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (refreshError.isNotBlank()) TourifyError else TourifyMuted
                        )
                    }
                }
            }
        }
    }
}

// Garis putus-putus memisahkan bagian info tiket.
@Composable
private fun TicketDashedDivider() {
    Canvas(modifier = Modifier.fillMaxWidth().height(1.dp)) {
        drawLine(
            color = TourifyBorder,
            start = Offset.Zero,
            end = Offset(size.width, 0f),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(
                floatArrayOf(4.dp.toPx(), 4.dp.toPx())
            )
        )
    }
}

// Kolom turun ke bawah saat layar sempit atau font besar supaya label nggak kepotong.
@Composable
private fun AdaptiveTicketFields(
    firstLabel: String,
    firstValue: String,
    secondLabel: String,
    secondValue: String
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth()
    ) {
        if (maxWidth < 280.dp || LocalDensity.current.fontScale >= 1.3f) {
            Column(
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                TicketDetailValue(
                    firstLabel,
                    firstValue
                )
                TicketDetailValue(
                    secondLabel,
                    secondValue
                )
            }
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                TicketDetailValue(
                    firstLabel,
                    firstValue,
                    Modifier.weight(1f)
                )
                TicketDetailValue(
                    secondLabel,
                    secondValue,
                    Modifier.weight(1f)
                )
            }
        }
    }
}

// Label dan nilai boleh turun baris supaya teks penting tetap utuh.
@Composable
private fun TicketDetailValue(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TourifyMuted
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = TourifyInk
        )
    }
}

// Urutkan tiket terdekat dulu; tanggal yang nggak terbaca ditaruh belakangan.
internal fun visibleTickets(
    tickets: List<Ticket>,
    showHistory: Boolean,
    searchQuery: String = ""
): List<Ticket> {
    // Cari nama destinasi atau kode tanpa peduli huruf besar dan spasi di ujung.
    val query = searchQuery.trim()
    val filtered = tickets.filter { ticket ->
        val finished = isTicketUsed(ticket) || isTicketExpired(
            ticket.status,
            ticket.visitDate
        )
        val matchesSearch = ticket.wahanaName.contains(
            query,
            ignoreCase = true
        )
            || ticket.id.contains(
                query,
                ignoreCase = true
            )
        finished == showHistory && matchesSearch
    }
    return if (showHistory) {
        filtered.sortedByDescending {
            parseTicketDate(it.visitDate) ?: LocalDate.MIN
        }
    } else {
        filtered.sortedBy {
            parseTicketDate(it.visitDate) ?: LocalDate.MAX
        }
    }
}

// Tanggal lewat tetap kedaluwarsa walau server belum memperbarui status.
internal fun isTicketExpired(
    status: String,
    visitDate: String,
    today: LocalDate = LocalDate.now()
): Boolean {
    return when (status.trim().lowercase(Locale.ROOT)) {
        "expire", "expired" -> true
        "used" -> false
        else -> isTicketExpired(
            visitDate,
            today
        )
    }
}

// Pesanan selesai setelah semua pengunjung check-in.
internal fun isTicketUsed(ticket: Ticket): Boolean {
    return ticket.status.equals(
        "Used",
        ignoreCase = true
    )
        || (ticket.quantity > 0 && ticket.checkedInQuantity >= ticket.quantity)
}

// Tiket masih berlaku hari ini; yang kedaluwarsa hanya tanggal sebelumnya.
internal fun isTicketExpired(
    value: String,
    today: LocalDate = LocalDate.now()
): Boolean {
    return parseTicketDate(value)?.isBefore(today) == true
}

private fun parseTicketDate(value: String): LocalDate? {
    val date = value.substringBefore('T').substringBefore(' ')
    return sequenceOf(
        "yyyy-MM-dd",
        "dd/MM/yyyy"
    ).firstNotNullOfOrNull { pattern ->
        try {
            LocalDate.parse(
                date,
                DateTimeFormatter.ofPattern(pattern)
            )
        } catch (_: DateTimeParseException) {
            null
        }
    }
}

internal fun formatTicketDate(value: String): String {
    // Kalau format tanggal nggak dikenali, tampilkan seperti dari server.
    val parsedDate = parseTicketDate(value) ?: return value
    return parsedDate.format(
        DateTimeFormatter.ofPattern(
            "dd MMM yyyy",
            Locale.forLanguageTag("id-ID")
        )
    )
}

// Tombol, QR, dan PDF memakai aturan status yang sama.
internal fun canShowTicketQr(ticket: Ticket): Boolean {
    return !isTicketUsed(ticket) && !isTicketExpired(
        ticket.status,
        ticket.visitDate
    )
}

internal fun ticketStatusLabel(ticket: Ticket): String {
    return when {
        isTicketUsed(ticket) -> "Terpakai"
        isTicketExpired(
            ticket.status,
            ticket.visitDate
        ) -> "Kedaluwarsa"
        else -> ""
    }
}

internal fun ticketEntryMessage(ticket: Ticket): String {
    return when {
        isTicketUsed(ticket) -> "Semua tiket pada pesanan ini sudah dipakai."
        isTicketExpired(
            ticket.status,
            ticket.visitDate
        ) -> "Tidak dapat digunakan untuk masuk."
        else -> "Tunjukkan QR ini saat masuk"
    }
}

// Sisa kuota membantu petugas tahu berapa orang yang masih bisa masuk.
internal fun ticketVisitorSummary(ticket: Ticket): String {
    return when {
        isTicketUsed(ticket) -> "${ticket.quantity} orang sudah masuk"
        ticket.checkedInQuantity > 0 -> {
            val remaining = (ticket.quantity - ticket.checkedInQuantity).coerceAtLeast(0)
            "Sisa $remaining dari ${ticket.quantity} orang"
        }
        else -> "${ticket.quantity} orang"
    }
}

// Tambahkan spasi tipis antara simbol mata uang dan nominal.
private fun ticketPrice(value: Int): String = rupiah(value).replace(
    ' ',
    '\u00A0'
)

// Pilih foto tiket dari nama wahana yang dikirim API.
internal fun ticketWahanaImage(wahanaName: String): Int {
    return when {
        wahanaName.contains(
            "samudra biru",
            ignoreCase = true
        ) -> R.drawable.samudra_biru
        wahanaName.contains(
            "pulau air",
            ignoreCase = true
        ) -> R.drawable.pulau_air
        wahanaName.contains(
            "pantai mentari",
            ignoreCase = true
        ) -> R.drawable.pantai_mentari
        wahanaName.contains(
            "taman satwa pesisir",
            ignoreCase = true
        ) -> {
            R.drawable.taman_satwa_pesisir
        }
        else -> R.drawable.dunia_petualang
    }
}

@Composable
private fun TicketQrCode(
    ticket: Ticket,
    modifier: Modifier = Modifier,
    isUsed: Boolean = false
) {
    // Isi QR tetap sama; hanya warnanya berubah untuk tiket terpakai.
    val qrContent = remember(ticket) {
        ticketQrPayload(ticket)
    }
    val qrBitmap = remember(qrContent, isUsed) {
        generateQrCode(
            qrContent,
            moduleColor = if (isUsed) 0xFF888888.toInt() else android.graphics.Color.BLACK
        )
    }
    Image(
        bitmap = qrBitmap.asImageBitmap(),
        contentDescription = if (isUsed) {
            "QR code tiket ${ticket.id}, sudah terpakai"
        } else {
            "QR code tiket ${ticket.id}"
        },
        modifier = modifier.aspectRatio(1f),
        contentScale = ContentScale.Fit
    )
}

// Teks kartu dan QR memakai isi yang sama dengan pemindai.
internal fun ticketQrPayload(ticket: Ticket): String {
    return "TOURIFY|${ticket.id}|${ticket.username}|${ticket.wahanaName}|${ticket.visitDate}|${ticket.quantity}"
}

// Notifikasi pemesanan tetap memakai alur yang sama.
@Composable
private fun BookingSuccessDialog(onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = 24.dp,
                    vertical = 24.dp
                ),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(
                        horizontal = 28.dp,
                        vertical = 30.dp
                    ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.size(82.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Verified,
                            contentDescription = null,
                            tint = TourifyAccent,
                            modifier = Modifier.fillMaxSize()
                        )
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = TourifyForest,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Pemesanan berhasil!",
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Medium,
                        color = TourifyInk,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tiketmu berhasil dipesan. Detailnya sudah tersedia di halaman Tiket.",
                        fontSize = 14.sp,
                        color = TourifyMuted,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            // Tombol selesai pakai warna utama setelah pemesanan.
                            containerColor = TourifyForest
                        ),
                        elevation = ButtonDefaults.elevatedButtonElevation(
                            defaultElevation = 4.dp,
                            pressedElevation = 8.dp
                        )
                    ) {
                        Text(
                            text = "Selesai",
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

internal fun generateQrCode(
    content: String,
    size: Int = 512,
    moduleColor: Int = android.graphics.Color.BLACK
): Bitmap {
    val matrix = QRCodeWriter().encode(
        content,
        BarcodeFormat.QR_CODE,
        size,
        size
    )
    return Bitmap.createBitmap(
        size,
        size,
        Bitmap.Config.ARGB_8888
    ).apply {
        for (x in 0 until size) {
            for (y in 0 until size) {
                setPixel(
                    x,
                    y,
                    if (matrix[x, y]) moduleColor else android.graphics.Color.WHITE
                )
            }
        }
    }
}

@Preview(
    showBackground = true,
    widthDp = 360
)
@Preview(
    showBackground = true,
    widthDp = 320,
    fontScale = 1.5f
)
@Composable
fun TicketCardPreview() {
    Tourify_v2Theme {
        // Tanggal contoh dibuat mendatang supaya tiket tampil aktif di pratinjau.
        Box(
            modifier = Modifier
                .background(TourifyCanvas)
                .padding(20.dp)
        ) {
            TicketCard(
                ticket = Ticket(
                    id = "TRF-10248",
                    wahanaName = "Taman Safari Indonesia",
                    bookingDate = "2099-10-16",
                    visitDate = "2099-10-26",
                    quantity = 2,
                    unitPrice = 150000,
                    totalPrice = 300000,
                    paymentMethod = "QRIS"
                )
            )
        }
    }
}
