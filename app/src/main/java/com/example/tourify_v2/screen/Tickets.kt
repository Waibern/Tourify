package com.example.tourify_v2.screen

import com.example.tourify_v2.model.Ticket
import com.example.tourify_v2.viewmodel.TourifyViewModel

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Verified
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.tourify_v2.R
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

@Composable
fun TicketsScreen(
    viewModel: TourifyViewModel,
    currentUser: String,
    showBookingSuccess: Boolean = false,
    onDismissBookingSuccess: () -> Unit = {}
) {
    // ambil tiket milik user yang lagi login
    val userTickets = viewModel.tickets
    LaunchedEffect(currentUser) {
        if (currentUser.isNotBlank()) viewModel.loadTickets(currentUser)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .blur(if (showBookingSuccess) 6.dp else 0.dp),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // judul, status muat, dan pesan error
        item {
            Column {
                Text(
                    text = "Tiket saya",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF183633)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    if (viewModel.isLoadingTickets) {
                        "Memuat tiket…"
                    } else {
                        "${userTickets.size} tiket tersimpan"
                    },
                    color = Color(0xFF56675F),
                    fontSize = 14.sp
                )
                if (viewModel.isLoadingTickets) {
                    LinearProgressIndicator(Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp))
                }
                if (viewModel.ticketsError.isNotEmpty()) {
                    Text(viewModel.ticketsError, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = { viewModel.loadTickets(currentUser) }) {
                        Text("Coba lagi")
                    }
                }
            }
        }

        // tampilan kalau belum punya tiket
        if (
            userTickets.isEmpty() &&
            !viewModel.isLoadingTickets &&
            viewModel.ticketsError.isEmpty()
        ) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = Color(0xFFF1F8F6)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 42.dp, horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = Color(0xFF259780),
                            modifier = Modifier.size(38.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Belum ada tiket", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Pesan tiket dari halaman Beranda.",
                            color = Color(0xFF70817E),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        } else {
            // daftar tiket yang udah dipesan
            items(userTickets, key = { it.id }) { ticket -> TicketCard(ticket) }
        }
    }

    if (showBookingSuccess) {
        BookingSuccessDialog(onDismiss = onDismissBookingSuccess)
    }
}

@Composable
private fun BookingSuccessDialog(onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 30.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.size(82.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Verified,
                            contentDescription = null,
                            tint = Color(0xFFD0F58F),
                            modifier = Modifier.fillMaxSize()
                        )
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFF145C4B),
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
                        color = Color(0xFF6B6B6B),
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

@Composable
internal fun TicketCard(ticket: Ticket) {
    // kartu tiket dan QR code
    var showQr by rememberSaveable(ticket.id) { mutableStateOf(false) }
    val total = remember(ticket.totalPrice) {
        "Rp ${
            NumberFormat.getIntegerInstance(Locale.forLanguageTag("id-ID"))
                .format(ticket.totalPrice)
        }"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp, pressedElevation = 8.dp),
        border = BorderStroke(1.dp, Color(0xFFDDE5DE))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // nama destinasi dan nomor tiket
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = ticket.wahanaName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B2824)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "No. tiket · ${ticket.id}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF56675F)
                    )
                }
                Image(
                    painter = painterResource(ticketImage(ticket.wahanaName)),
                    contentDescription = "Foto ${ticket.wahanaName}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(14.dp))
                )
            }
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TicketField(
                    label = "Tanggal kunjungan",
                    value = formatTicketDate(ticket.visitDate),
                    modifier = Modifier.weight(1f)
                )
                TicketField(
                    label = "Pengunjung",
                    value = "${ticket.quantity} orang",
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(20.dp))
            HorizontalDivider(color = Color(0xFFE4E9E4))
            Spacer(Modifier.height(16.dp))
            // total harga ditaruh sendiri biar gampang dibaca
            Text(
                "Total pembayaran",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF56675F)
            )
            Text(
                total,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF145C4B)
            )
            Spacer(Modifier.height(16.dp))
            // buka atau tutup QR tiket
            Button(
                onClick = { showQr = !showQr },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .semantics { stateDescription = if (showQr) "Terbuka" else "Tertutup" },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF145C4B),
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Icon(
                    if (showQr) Icons.Default.ExpandLess else Icons.Default.QrCode,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (showQr) "Tutup QR tiket" else "Lihat QR tiket",
                    textAlign = TextAlign.Center
                )
            }
            if (showQr) {
                Spacer(Modifier.height(20.dp))
                TicketQrCode(ticket)
                Spacer(Modifier.height(20.dp))
                HorizontalDivider(color = Color(0xFFE4E9E4))
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    TicketField(
                        label = "Dipesan pada",
                        value = formatTicketDate(ticket.bookingDate),
                        modifier = Modifier.weight(1f)
                    )
                    TicketField(
                        label = "Metode pembayaran",
                        value = ticket.paymentMethod,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

private fun ticketImage(wahanaName: String): Int = when {
    wahanaName.contains("samudra biru", ignoreCase = true) -> R.drawable.samudra_biru
    wahanaName.contains("pulau air", ignoreCase = true) -> R.drawable.pulau_air
    wahanaName.contains("pantai mentari", ignoreCase = true) -> R.drawable.pantai_mentari
    wahanaName.contains("taman satwa pesisir", ignoreCase = true) -> R.drawable.taman_satwa_pesisir
    else -> R.drawable.dunia_petualang
}

internal fun formatTicketDate(value: String): String {
    val date = value.substringBefore('T').substringBefore(' ')
    val parsed = sequenceOf("yyyy-MM-dd", "dd/MM/yyyy").mapNotNull { pattern ->
        try {
            LocalDate.parse(date, DateTimeFormatter.ofPattern(pattern))
        } catch (_: DateTimeParseException) {
            null
        }
    }.firstOrNull() ?: return value
    return parsed.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.forLanguageTag("id-ID")))
}

@Composable
private fun TicketQrCode(ticket: Ticket) {
    // QR buat dipindai saat masuk
    val qrContent = remember(ticket) {
        "TOURIFY|${ticket.id}|${ticket.username}|${ticket.wahanaName}|${ticket.visitDate}|${ticket.quantity}"
    }
    val qrBitmap = remember(qrContent) { generateQrCode(qrContent) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            bitmap = qrBitmap.asImageBitmap(),
            contentDescription = "QR code tiket ${ticket.id}",
            modifier = Modifier
                .sizeIn(maxWidth = 224.dp, maxHeight = 224.dp)
                .fillMaxWidth()
                .aspectRatio(1f),
            contentScale = ContentScale.FillBounds
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Tunjukkan QR ini kepada petugas di pintu masuk.",
            textAlign = TextAlign.Center,
            fontSize = 12.sp,
            color = Color(0xFF56675F)
        )
    }
}

private fun generateQrCode(content: String, size: Int = 512): Bitmap {
    val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
    return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
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

@Composable
private fun TicketField(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF56675F)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF183633)
        )
    }
}


@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 320, fontScale = 1.3f)
@Composable
fun TicketCardPreview() {
    MaterialTheme {
        Box(Modifier
            .background(Color.White)
            .padding(20.dp)) {
            TicketCard(
                Ticket(
                    id = "TRF-10248", wahanaName = "Taman Safari Indonesia",
                    bookingDate = "2026-10-16", visitDate = "2026-10-24",
                    quantity = 2, unitPrice = 150000, totalPrice = 300000,
                    paymentMethod = "QRIS"
                )
            )
        }
    }
}
