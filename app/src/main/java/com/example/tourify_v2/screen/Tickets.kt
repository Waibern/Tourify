package com.example.tourify_v2.screen

import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.People
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
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
import com.example.tourify_v2.model.Ticket
import com.example.tourify_v2.ui.theme.*
import com.example.tourify_v2.viewmodel.TourifyViewModel
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import kotlinx.coroutines.awaitCancellation

@Composable
fun TicketsScreen(
    viewModel: TourifyViewModel,
    currentUser: String,
    showBookingSuccess: Boolean = false,
    onDismissBookingSuccess: () -> Unit = {},
    onTicketDetailVisibilityChanged: (Boolean) -> Unit = {}
) {
    // Segarkan saat kembali ke app, tanpa membuang tiket kalau koneksi gagal.
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(
        currentUser,
        lifecycleOwner
    ) {
        if (currentUser.isNotBlank()) {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.loadTickets(currentUser)
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
                viewModel.loadTickets(currentUser)
            },
            showBookingSuccess = showBookingSuccess,
            onDismissBookingSuccess = onDismissBookingSuccess,
            onTicketDetailVisibilityChanged = onTicketDetailVisibilityChanged
        )
    }
}

// Isi layar dipisah dari jaringan supaya kondisi gagal dan font besar bisa diuji langsung.
@Composable
internal fun TicketsContent(
    tickets: List<Ticket>,
    isRefreshing: Boolean = false,
    refreshError: String = "",
    updatedAtMillis: Long? = null,
    onRefresh: () -> Unit = {},
    showBookingSuccess: Boolean = false,
    onDismissBookingSuccess: () -> Unit = {},
    onTicketDetailVisibilityChanged: (Boolean) -> Unit = {}
) {
    // Simpan ID saja; ukuran teks boleh berubah tanpa menutup tiket yang sedang dibuka.
    var showHistory by rememberSaveable {
        mutableStateOf(false)
    }
    var selectedTicketId by rememberSaveable {
        mutableStateOf<String?>(null)
    }
    // Kata pencarian tetap ada setelah membuka QR atau mengubah ukuran teks.
    var searchQuery by rememberSaveable {
        mutableStateOf("")
    }
    val focusManager = LocalFocusManager.current
    val ticketListState = rememberLazyListState()
    val selectedTicket = tickets.firstOrNull {
        it.id == selectedTicketId
    }
    val visibleTickets = visibleTickets(
        tickets,
        showHistory,
        searchQuery
    )
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

    // Data check-in boleh berubah tanpa mengulang animasi perpindahan halaman.
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
                onRefresh = onRefresh
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .blur(if (showBookingSuccess) 6.dp else 0.dp)
            ) {
                TicketTopBar(
                    title = "Tiket Saya",
                    isRefreshing = isRefreshing,
                    onRefresh = onRefresh
                )
                TicketTabs(
                    showHistory = showHistory,
                    onSelectHistory = {
                        showHistory = it
                    },
                    modifier = Modifier.padding(
                        horizontal = 20.dp,
                        vertical = 12.dp
                    )
                )
                TicketSearchBar(
                    searchQuery = searchQuery,
                    onSearchQueryChange = {
                        searchQuery = it
                    },
                    modifier = Modifier.padding(
                        start = 20.dp,
                        end = 20.dp,
                        top = 12.dp
                    )
                )
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    state = ticketListState,
                    // Tiket terakhir tetap bisa digulir melewati navbar yang mengambang.
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
                    // Pesan gagal menemani daftar yang ada, bukan menggantikannya.
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
                                }
                            )
                        }
                    }
                    items(
                        visibleTickets,
                        key = {
                            it.id
                        }
                    ) { item ->
                        TicketCard(
                            ticket = item,
                            onShowTicket = {
                                // Tutup keyboard supaya QR langsung mendapat ruang layar penuh.
                                focusManager.clearFocus()
                                selectedTicketId = item.id
                            }
                        )
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

// Bentuk, bayangan, dan warna mengikuti kolom pencarian di Beranda.
@Composable
private fun TicketSearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        placeholder = {
            Text(
                "Cari tiket",
                color = TourifyMuted
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = TourifyForest
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
                        imageVector = Icons.Default.Close,
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
            focusedBorderColor = TourifyForest
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(
            onSearch = {
                focusManager.clearFocus()
            }
        ),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .shadow(
                4.dp,
                RoundedCornerShape(18.dp)
            )
    )
}

// Header yang sama menjaga tombol kembali dan refresh punya area sentuh yang cukup.
@Composable
private fun TicketTopBar(
    title: String,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onBack: (() -> Unit)? = null,
    backDescription: String = "Kembali ke daftar tiket"
) {
    Column {
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
            } else {
                Spacer(Modifier.size(48.dp))
            }
            Text(
                text = title,
                modifier = Modifier
                    .weight(1f)
                    .semantics {
                        heading()
                    },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TourifyInk,
                textAlign = TextAlign.Center
            )
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
        if (isRefreshing) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = TourifyForest,
                trackColor = TourifyMint
            )
        }
    }
}

// Tab ikut membesar bersama teks dan mengumumkan pilihan aktif ke pembaca layar.
@Composable
private fun TicketTabs(
    showHistory: Boolean,
    onSelectHistory: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(50))
            .background(TourifySoftSurface)
            .selectableGroup()
            .padding(4.dp)
    ) {
        listOf(
            "Aktif",
            "Riwayat"
        ).forEachIndexed { index, label ->
            val selected = showHistory == (index == 1)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (selected) TourifyForest else Color.Transparent)
                    .selectable(
                        selected = selected,
                        role = Role.Tab,
                        onClick = {
                            onSelectHistory(index == 1)
                        }
                    )
                    .padding(
                        horizontal = 12.dp,
                        vertical = 12.dp
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (selected) Color.White else TourifyForest,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// Waktu verifikasi dan kegagalan koneksi tetap terlihat di daftar maupun detail.
@Composable
private fun TicketSyncNotice(
    isRefreshing: Boolean,
    refreshError: String,
    updatedAtMillis: Long?,
    onRefresh: () -> Unit
) {
    val timestamp = ticketUpdateTime(updatedAtMillis)
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
        Text(
            text = when {
                isRefreshing -> "Memperbarui status tiket…"
                timestamp != null -> "Terakhir diperbarui $timestamp"
                else -> "Status belum diperbarui"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = TourifyMuted
        )
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

// State kosong dan gagal tetap ringan, tanpa bayangan besar di halaman putih.
@Composable
private fun TicketMessage(
    title: String,
    detail: String,
    onRetry: (() -> Unit)? = null
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
    }
}

// cekungan dan sudut lebih bulat tetap bikin kartu terasa seperti tiket
private val ticketCardShape = GenericShape { size, _ ->
    val corner = size.width * 0.065f
    val notch = size.width * 0.045f
    val center = size.width / 2f

    moveTo(corner, 0f)
    lineTo(center - notch, 0f)
    cubicTo(center - notch, notch * 0.55f, center - notch * 0.55f, notch, center, notch)
    cubicTo(center + notch * 0.55f, notch, center + notch, notch * 0.55f, center + notch, 0f)
    lineTo(size.width - corner, 0f)
    quadraticTo(size.width, 0f, size.width, corner)
    lineTo(size.width, size.height - corner)
    quadraticTo(size.width, size.height, size.width - corner, size.height)
    lineTo(center + notch, size.height)
    cubicTo(
        center + notch,
        size.height - notch * 0.55f,
        center + notch * 0.55f,
        size.height - notch,
        center,
        size.height - notch
    )
    cubicTo(
        center - notch * 0.55f,
        size.height - notch,
        center - notch,
        size.height - notch * 0.55f,
        center - notch,
        size.height
    )
    lineTo(corner, size.height)
    quadraticTo(0f, size.height, 0f, size.height - corner)
    lineTo(0f, corner)
    quadraticTo(0f, 0f, corner, 0f)
    close()
}

// cekungan sisi yang lembut bikin detail QR terasa seperti tiket tanpa sudut tajam
private val ticketDetailShape = GenericShape { size, _ ->
    val corner = size.width * 0.065f
    val notch = size.width * 0.04f
    val notchY = size.height * 0.65f

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

// Kartu bersobek dan tombol kaca kembali mengikuti tiket sebelum perombakan.
@Composable
internal fun TicketCard(
    ticket: Ticket,
    onShowTicket: () -> Unit = {}
) {
    // Bentuk tiket lama dipakai lagi; aturan QR tetap mengikuti status terbaru.
    val available = canShowTicketQr(ticket)
    val used = isTicketUsed(ticket)
    val statusLabel = ticketStatusLabel(ticket)
    val total = ticketPrice(ticket.totalPrice)
    val unitPrice = ticketPrice(ticket.unitPrice)
    val qrContent = ticketQrPayload(ticket)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = ticketCardShape,
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 28.dp)
        ) {
            // tanggal dan label status ada di bagian atas kartu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatTicketDate(ticket.visitDate),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TourifyInk
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (!available) Color(0xFFE9EEEB) else TourifyMint
                ) {
                    Text(
                        text = statusLabel,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (!available) TourifyMuted else TourifyForest
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            // dua baris detail mengikuti susunan kiri dan kanan pada contoh
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TicketCompactField(
                    label = "Destinasi",
                    value = ticket.wahanaName,
                    modifier = Modifier.weight(1.35f)
                )
                TicketCompactField(
                    label = "Harga/tiket",
                    value = unitPrice,
                    modifier = Modifier.weight(1f),
                    alignRight = true
                )
            }
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TicketCompactField(
                    label = "Dipesan pada",
                    value = formatTicketDate(ticket.bookingDate),
                    modifier = Modifier.weight(1.35f)
                )
                TicketCompactField(
                    label = "Total pembayaran",
                    value = total,
                    modifier = Modifier.weight(1f),
                    alignRight = true
                )
            }
            Spacer(Modifier.height(16.dp))

            // jumlah pengunjung dan metode bayar tampil sebagai info singkat
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.People,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = TourifyMuted
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = ticketVisitorSummary(ticket),
                        style = MaterialTheme.typography.bodySmall,
                        color = TourifyMuted
                    )
                }
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CreditCard,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = TourifyMuted
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = ticket.paymentMethod,
                        style = MaterialTheme.typography.bodySmall,
                        color = TourifyMuted
                    )
                }
            }
            if (ticket.checkedInQuantity > 0) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Check-in ${ticket.checkedInQuantity} dari ${ticket.quantity} orang",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (used) TourifyForest else TourifyMuted
                )
            }
            Spacer(Modifier.height(18.dp))

            // teks kode kartu sama persis dengan isi QR
            Text(
                text = qrContent,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall,
                color = TourifyMuted,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            // Permukaan kaca gelap menjaga ikon dan teks tetap jelas.
            Button(
                onClick = onShowTicket,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xE6222926),
                    contentColor = Color.White
                ),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f)),
                elevation = ButtonDefaults.elevatedButtonElevation(
                    defaultElevation = 5.dp,
                    pressedElevation = 9.dp
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Icon(
                    imageVector = if (!available) {
                        Icons.AutoMirrored.Filled.ReceiptLong
                    } else {
                        Icons.Default.QrCode
                    },
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (available) "Lihat QR" else "Lihat detail",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun TicketCompactField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    alignRight: Boolean = false
) {
    // kolom kanan rata kanan seperti informasi waktu pada contoh tiket
    Column(
        modifier = modifier,
        horizontalAlignment = if (alignRight) Alignment.End else Alignment.Start
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TourifyMuted,
            textAlign = if (alignRight) TextAlign.End else TextAlign.Start
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = TourifyInk,
            textAlign = if (alignRight) TextAlign.End else TextAlign.Start
        )
    }
}

// Tanggal kunjungan tidak lagi bersaing dengan tanggal pembelian di ringkasan.
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

// Satu istilah dan warna status dipakai di seluruh layar tiket.
@Composable
private fun TicketStatusBadge(ticket: Ticket) {
    val available = canShowTicketQr(ticket)
    Surface(
        shape = RoundedCornerShape(50),
        color = if (available) TourifyMint else TourifySoftSurface
    ) {
        Text(
            text = ticketStatusLabel(ticket),
            modifier = Modifier.padding(
                horizontal = 12.dp,
                vertical = 4.dp
            ),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (available) TourifyForest else TourifyInk
        )
    }
}

@Composable
internal fun TicketDetailScreen(
    ticket: Ticket,
    onDismiss: () -> Unit,
    isRefreshing: Boolean = false,
    refreshError: String = "",
    updatedAtMillis: Long? = null,
    onRefresh: () -> Unit = {}
) {
    val available = canShowTicketQr(ticket)
    val isFinished = !available
    val statusLabel = ticketStatusLabel(ticket).uppercase(Locale.forLanguageTag("id-ID"))
    val passengerName = ticket.username.ifBlank {
        "Pengunjung"
    }
    var showLargeQr by rememberSaveable(ticket.id) {
        mutableStateOf(false)
    }
    val context = LocalContext.current
    val view = LocalView.current
    // Layar tetap menyala selama tiket aktif sedang ditunjukkan ke petugas.
    DisposableEffect(
        view,
        available
    ) {
        val previous = view.keepScreenOn
        view.keepScreenOn = available
        onDispose {
            view.keepScreenOn = previous
        }
    }
    // Pemilih file Android tetap menyimpan salinan PDF tanpa izin penyimpanan tambahan.
    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            val result = runCatching {
                writeTicketPdf(
                    context,
                    uri,
                    ticket
                )
            }
            Toast.makeText(
                context,
                if (result.isSuccess) "PDF tiket berhasil disimpan" else "PDF tiket gagal disimpan. Coba lagi.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .navigationBarsPadding()
            .testTag("ticketDetailViewport")
    ) {
        // Tombol kembali kaca dan judul tetap seperti tampilan tiket sebelumnya.
        TicketTopBar(
            title = "Tiket Saya",
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            onBack = onDismiss
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Text(
                text = if (isFinished) "RIWAYAT KUNJUNGAN" else "TIKET AKTIF",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = TourifyMuted,
                letterSpacing = 1.2.sp
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = ticket.wahanaName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TourifyInk,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = formatTicketDate(ticket.visitDate),
                style = MaterialTheme.typography.bodyLarge,
                color = TourifyMuted
            )
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
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
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

                    TicketDashedDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TicketDetailValue(
                            label = "TANGGAL KUNJUNGAN",
                            value = formatTicketDate(ticket.visitDate),
                            modifier = Modifier.weight(1f)
                        )
                        TicketDetailValue(
                            label = "PENGUNJUNG",
                            value = if (ticket.checkedInQuantity > 0) {
                                "${ticket.checkedInQuantity}/${ticket.quantity} masuk"
                            } else {
                                "${ticket.quantity} orang"
                            },
                            modifier = Modifier.weight(0.8f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TicketDetailValue(
                            label = "PEMBAYARAN",
                            value = ticket.paymentMethod,
                            modifier = Modifier.weight(1f)
                        )
                        TicketDetailValue(
                            label = "TOTAL",
                            value = ticketPrice(ticket.totalPrice),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(
                            text = "KODE TIKET",
                            style = MaterialTheme.typography.labelMedium,
                            color = TourifyMuted,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = ticket.id,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TourifyInk,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Dipesan ${formatTicketDate(ticket.bookingDate)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TourifyMuted
                        )
                    }

                    TicketDashedDivider()

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Bentuk lama tidak mengaktifkan lagi QR tiket yang sudah selesai.
                        if (!available) {
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
                                text = if (ticket.checkedInQuantity > 0) {
                                    "Sisa ${ticket.quantity - ticket.checkedInQuantity} orang belum check-in"
                                } else {
                                    "Tunjukkan QR ini saat masuk"
                                },
                                style = MaterialTheme.typography.bodySmall,
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
            Spacer(Modifier.height(20.dp))
            TicketSyncNotice(
                isRefreshing = isRefreshing,
                refreshError = refreshError,
                updatedAtMillis = updatedAtMillis,
                onRefresh = onRefresh
            )
        }

        // Tombol utama menyimpan salinan tiket yang bisa dibuka tanpa internet.
        Button(
            onClick = {
                val ticketFileId = ticket.id.replace(Regex("[^A-Za-z0-9_-]"), "_")
                pdfLauncher.launch("Tourify-Tiket-$ticketFileId.pdf")
            },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .widthIn(min = 178.dp)
                .padding(top = 8.dp, bottom = 16.dp)
                .heightIn(min = 52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = TourifyForestBright,
                contentColor = Color.White
            )
        ) {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(text = "Unduh PDF", fontWeight = FontWeight.SemiBold)
        }
    }
    if (showLargeQr && available) {
        LargeTicketQr(
            ticket = ticket,
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


// Mode pindai memakai ruang layar yang tersedia, termasuk saat ponsel diputar.
@Composable
private fun LargeTicketQr(
    ticket: Ticket,
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
                        )
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
                        color = TourifyForest
                    )
                    // Mode QR besar juga tidak menyembunyikan kegagalan pembaruan status.
                    Text(
                        text = when {
                            refreshError.isNotBlank() -> "Status belum bisa diperbarui. Periksa koneksi."
                            isRefreshing -> "Memperbarui status tiket…"
                            else -> ticketUpdateTime(updatedAtMillis)?.let {
                                "Terakhir diperbarui $it"
                            }
                                ?: "Status belum diperbarui"
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

// Garis putus-putus memisahkan kelompok informasi di kartu.
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

// Kolom turun ke bawah saat sempit atau font besar, sehingga label tidak dipotong.
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
        if (maxWidth < 360.dp || LocalDensity.current.fontScale >= 1.3f) {
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

// Label dan nilai boleh membungkus; informasi penting tidak diberi elipsis.
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

// Tiket terdekat tampil dulu; tanggal yang belum terbaca tidak mendahului kunjungan jelas.
internal fun visibleTickets(
    tickets: List<Ticket>,
    showHistory: Boolean,
    searchQuery: String = ""
): List<Ticket> {
    // Cari nama destinasi atau kode tanpa membedakan huruf besar dan spasi di ujung.
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

// Tanggal lewat tetap kedaluwarsa walau status lama belum diperbarui dari server.
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

// Pesanan baru selesai setelah semua pengunjung masuk.
internal fun isTicketUsed(ticket: Ticket): Boolean {
    return ticket.status.equals(
        "Used",
        ignoreCase = true
    )
        || (ticket.quantity > 0 && ticket.checkedInQuantity >= ticket.quantity)
}

// Hari kunjungan masih berlaku; hanya tanggal sebelumnya yang sudah lewat.
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
    val parsed = parseTicketDate(value) ?: return value
    return parsed.format(
        DateTimeFormatter.ofPattern(
            "dd MMM yyyy",
            Locale.forLanguageTag("id-ID")
        )
    )
}

// Satu aturan dipakai tombol, QR layar, dan PDF supaya statusnya tidak bertentangan.
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
        else -> "Aktif"
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

// Sisa kuota lebih berguna di gate daripada jumlah awal saja.
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

// Spasi tetap menjaga awalan mata uang dan nominal berada dalam satu kelompok.
private fun ticketPrice(value: Int): String = rupiah(value).replace(
    ' ',
    '\u00A0'
)

// Format waktu yang sama dipakai di detail dan mode pindai layar penuh.
private fun ticketUpdateTime(value: Long?): String? {
    return value?.let {
        Instant
            .ofEpochMilli(it)
            .atZone(ZoneId.systemDefault())
            .format(
                DateTimeFormatter.ofPattern(
                    "dd MMM, HH.mm",
                    Locale.forLanguageTag("id-ID")
                )
            )
    }
}

@Composable
private fun TicketQrCode(
    ticket: Ticket,
    modifier: Modifier = Modifier
) {
    // Payload dan margin putih QR tetap sama dengan format yang dipakai petugas.
    val qrContent = remember(ticket) {
        ticketQrPayload(ticket)
    }
    val qrBitmap = remember(qrContent) {
        generateQrCode(qrContent)
    }
    Image(
        bitmap = qrBitmap.asImageBitmap(),
        contentDescription = "QR code tiket ${ticket.id}",
        modifier = modifier.aspectRatio(1f),
        contentScale = ContentScale.Fit
    )
}

// Teks pada kartu dan QR tetap memakai payload yang sama dengan pemindai.
internal fun ticketQrPayload(ticket: Ticket): String {
    return "TOURIFY|${ticket.id}|${ticket.username}|${ticket.wahanaName}|${ticket.visitDate}|${ticket.quantity}"
}

// Notif pemesanan yang sudah ada tetap memakai alur sebelumnya.
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
                        color = Color(0xFF171717),
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
                            containerColor = Color(0xFF292929)
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
    size: Int = 512
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
                    if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
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
        // Data contoh memakai kunjungan mendatang supaya pratinjau menampilkan tiket aktif.
        Box(
            modifier = Modifier
                .background(Color.White)
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
