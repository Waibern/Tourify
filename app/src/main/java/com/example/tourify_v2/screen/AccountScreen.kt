package com.example.tourify_v2.screen

import android.content.ActivityNotFoundException
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tourify_v2.model.Ticket
import com.example.tourify_v2.ui.theme.TourifyError
import com.example.tourify_v2.ui.theme.TourifyForest
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException

// Profil memakai data akun dan tiket Tourify yang sudah tersedia.
@Composable
fun AccountScreen(
    currentUser: String,
    profileImageUrl: String?,
    isUploadingProfileImage: Boolean,
    profileImageError: String,
    tickets: List<Ticket>,
    isLoadingTickets: Boolean,
    ticketsError: String,
    onProfileImageSelected: (ByteArray, String) -> Unit,
    onTicketsClick: () -> Unit,
    onDestinationsClick: () -> Unit,
    onLogout: () -> Unit
) {
    // Simpan pilihan foto dan status akun sebelum menampilkan profil.
    val accountName = currentUser.ifBlank {
        "Pengguna Tourify"
    }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedImageBitmap by remember {
        mutableStateOf<Bitmap?>(null)
    }
    // Nilai ini jadi titik awal pengaturan crop untuk setiap foto baru.
    var cropZoom by remember {
        mutableStateOf(1f)
    }
    var cropPositionX by remember {
        mutableStateOf(0.5f)
    }
    var cropPositionY by remember {
        mutableStateOf(0.5f)
    }
    var showImageSourceDialog by remember {
        mutableStateOf(false)
    }
    var isPreparingImage by remember {
        mutableStateOf(false)
    }
    var selectedImageError by remember {
        mutableStateOf("")
    }
    val statsReady = !isLoadingTickets && ticketsError.isBlank()
    // Abaikan tiket dari sesi akun sebelumnya saat pengguna berganti.
    val accountTickets = tickets.filter {
        it.username == currentUser
    }
    val countText: (Int) -> String = {
        if (statsReady) it.toString() else "—"
    }
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedImageBitmap = null
            selectedImageError = ""
            isPreparingImage = true
            scope.launch {
                try {
                    val selectedImage = withContext(Dispatchers.IO) {
                        readProfileImage(
                            context,
                            uri
                        )
                    }
                    val bitmap = withContext(Dispatchers.IO) {
                        decodeProfileImage(selectedImage.bytes)
                    }
                    selectedImageBitmap = bitmap
                    cropZoom = 1f
                    cropPositionX = 0.5f
                    cropPositionY = 0.5f
                } catch (exception: Exception) {
                    selectedImageBitmap = null
                    selectedImageError = exception.message ?: "Foto gagal dibaca."
                } finally {
                    isPreparingImage = false
                }
            }
        }
    }
    val cameraPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            selectedImageBitmap = bitmap
            selectedImageError = ""
            cropZoom = 1f
            cropPositionX = 0.5f
            cropPositionY = 0.5f
        }
    }
    LaunchedEffect(profileImageUrl) {
        if (profileImageUrl != null) {
            selectedImageBitmap = null
        }
    }
    val imageError = selectedImageError.ifBlank {
        profileImageError
    }
    val isImageBusy = isPreparingImage || isUploadingProfileImage

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6FAF8))
    ) {
        // Latar polos biar bayangan kartu kelihatan.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(
                    start = 24.dp,
                    top = 28.dp,
                    end = 24.dp,
                    bottom = 116.dp
                ),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "Profil",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            // Foto dari server dipakai di sini; inisial tetap jadi cadangan.
            // Kartu utama bikin identitas akun lebih rapi.
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                shadowElevation = 5.dp
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(76.dp),
                        shape = CircleShape,
                        color = Color(0xFFD9F5EF)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center
                        ) {
                            val imageModel = selectedImageBitmap
                                ?: profileImageUrl
                            if (imageModel != null) {
                                AsyncImage(
                                    model = imageModel,
                                    contentDescription = "Foto profil $accountName",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    accountName.firstOrNull()?.uppercase() ?: "?",
                                    color = TourifyForest,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(18.dp))
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            accountName,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "Akun Tourify",
                            fontSize = 14.sp,
                            color = Color(0xFF777D80)
                        )
                        // Galeri dan kamera memakai alur upload foto profil yang sama.
                        TextButton(
                            onClick = {
                                showImageSourceDialog = true
                            },
                            enabled = !isImageBusy
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (isImageBusy) "Menyimpan foto…" else "Ubah foto"
                            )
                        }
                    }
                }
            }
            if (imageError.isNotBlank()) {
                Text(
                    imageError,
                    color = TourifyError,
                    fontSize = 13.sp
                )
            }

            // Angka ringkasan dihitung dari tiket akun yang dimuat server.
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ProfileStat(
                    countText(accountTickets.size),
                    "Pesanan",
                    Modifier.weight(1f)
                )
                ProfileStat(
                    countText(
                        accountTickets.sumOf {
                            it.quantity
                        }
                    ),
                    "Tiket",
                    Modifier.weight(1f)
                )
                ProfileStat(
                    countText(
                        accountTickets.sumOf {
                            it.checkedInQuantity
                        }
                    ),
                    "Check-in",
                    Modifier.weight(1f)
                )
            }
            if (ticketsError.isNotBlank()) {
                Text(
                    ticketsError,
                    color = TourifyError,
                    fontSize = 13.sp
                )
            }

            // Baris menu membuka fitur Tourify yang sudah ada.
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Umum",
                    color = Color(0xFF777D80),
                    fontSize = 14.sp
                )
                ProfileMenuRow(
                    "Tiket Saya",
                    Icons.AutoMirrored.Filled.ReceiptLong,
                    onTicketsClick
                )
                ProfileMenuRow(
                    "Jelajahi Destinasi",
                    Icons.Default.Explore,
                    onDestinationsClick
                )
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Akun",
                    color = Color(0xFF777D80),
                    fontSize = 14.sp
                )
                ProfileMenuRow(
                    "Keluar dari akun",
                    Icons.AutoMirrored.Filled.Logout,
                    onLogout,
                    TourifyError
                )
            }
        }

        // Pilihan sumber foto tetap lewat pemilih sistem Android.
        if (showImageSourceDialog) {
            AlertDialog(
                onDismissRequest = {
                    showImageSourceDialog = false
                },
                title = {
                    Text("Pilih foto profil")
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            onClick = {
                                showImageSourceDialog = false
                                imagePicker.launch("image/*")
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pilih dari galeri")
                        }
                        TextButton(
                            onClick = {
                                showImageSourceDialog = false
                                selectedImageError = ""
                                try {
                                    cameraPicker.launch(null)
                                } catch (exception: ActivityNotFoundException) {
                                    selectedImageError = "Aplikasi kamera tidak tersedia."
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Ambil foto")
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(
                        onClick = {
                            showImageSourceDialog = false
                        }
                    ) {
                        Text("Batal")
                    }
                }
            )
        }

        // Foto baru masuk ke pratinjau dulu, lalu dipotong sesuai posisi dan zoom.
        selectedImageBitmap?.let { bitmap ->
            ProfileImageCropDialog(
                bitmap = bitmap,
                zoom = cropZoom,
                positionX = cropPositionX,
                positionY = cropPositionY,
                isBusy = isImageBusy,
                error = imageError,
                onZoomChange = {
                    cropZoom = it
                },
                onPositionXChange = {
                    cropPositionX = it
                },
                onPositionYChange = {
                    cropPositionY = it
                },
                onDismiss = {
                    if (!isImageBusy) {
                        selectedImageBitmap = null
                    }
                },
                onConfirm = {
                    if (!isImageBusy) {
                        isPreparingImage = true
                        selectedImageError = ""
                        scope.launch {
                            try {
                                val imageBytes = withContext(Dispatchers.IO) {
                                    val croppedBitmap = cropProfileImage(
                                        bitmap,
                                        cropZoom,
                                        cropPositionX,
                                        cropPositionY
                                    )
                                    encodeProfileImage(croppedBitmap)
                                }
                                onProfileImageSelected(
                                    imageBytes,
                                    "image/jpeg"
                                )
                            } catch (exception: Exception) {
                                selectedImageError = exception.message ?: "Foto gagal disiapkan."
                            } finally {
                                isPreparingImage = false
                            }
                        }
                    }
                }
            )
        }
    }
}

// Dialog ini menampilkan hasil crop dan kontrol posisi yang dipakai saat upload.
@Composable
private fun ProfileImageCropDialog(
    bitmap: Bitmap,
    zoom: Float,
    positionX: Float,
    positionY: Float,
    isBusy: Boolean,
    error: String,
    onZoomChange: (Float) -> Unit,
    onPositionXChange: (Float) -> Unit,
    onPositionYChange: (Float) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Atur foto profil")
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .align(Alignment.CenterHorizontally)
                        .clip(CircleShape)
                        .background(Color(0xFFE8F3EF)),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Pratinjau foto profil",
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                val baseScale = maxOf(
                                    size.width / bitmap.width,
                                    size.height / bitmap.height
                                )
                                val renderedWidth = bitmap.width * baseScale * zoom
                                val renderedHeight = bitmap.height * baseScale * zoom
                                scaleX = zoom
                                scaleY = zoom
                                translationX = (0.5f - positionX) * (renderedWidth - size.width)
                                translationY = (0.5f - positionY) * (renderedHeight - size.height)
                            },
                        contentScale = ContentScale.Crop
                    )
                }
                Text(
                    text = "Geser posisi dan zoom sampai pas.",
                    fontSize = 13.sp,
                    color = Color(0xFF646B6E)
                )
                Text(
                    text = "Zoom ${((zoom - 1f) * 100).toInt()}%",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Slider(
                    value = zoom,
                    onValueChange = onZoomChange,
                    valueRange = 1f..3f,
                    enabled = !isBusy
                )
                Text(
                    text = "Posisi horizontal",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Slider(
                    value = positionX,
                    onValueChange = onPositionXChange,
                    enabled = !isBusy
                )
                Text(
                    text = "Posisi vertikal",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Slider(
                    value = positionY,
                    onValueChange = onPositionYChange,
                    enabled = !isBusy
                )
                if (error.isNotBlank()) {
                    Text(
                        text = error,
                        color = TourifyError,
                        fontSize = 13.sp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !isBusy
            ) {
                Text(if (isBusy) "Menyimpan…" else "Pakai foto")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isBusy
            ) {
                Text("Batal")
            }
        }
    )
}

@Composable
private fun ProfileStat(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    // Kartu kecil dengan bayangan tipis buat ringkasan.
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 3.dp
    ) {
        Column(
            modifier = Modifier.padding(
                vertical = 18.dp,
                horizontal = 4.dp
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                value,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                label,
                fontSize = 13.sp,
                color = Color(0xFF646B6E)
            )
        }
    }
}

@Composable
private fun ProfileMenuRow(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    tint: Color = Color(0xFF202729)
) {
    // Bayangan halus bantu bedakan baris menu dari latar.
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        shadowElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .clickable(onClick = onClick)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(23.dp)
            )
            Spacer(Modifier.width(16.dp))
            Text(
                label,
                color = tint,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AccountScreenPreview() {
    AccountScreen(
        currentUser = "admin",
        profileImageUrl = null,
        isUploadingProfileImage = false,
        profileImageError = "",
        tickets = emptyList(),
        isLoadingTickets = false,
        ticketsError = "",
        onProfileImageSelected = { _, _ -> },
        onTicketsClick = {},
        onDestinationsClick = {},
        onLogout = {}
    )
}

private data class SelectedProfileImage(
    val bytes: ByteArray,
    val mimeType: String
)

// Decode gambar dengan ukuran pratinjau agar foto resolusi tinggi tetap ringan.
private fun decodeProfileImage(bytes: ByteArray): Bitmap {
    val bounds = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    BitmapFactory.decodeByteArray(
        bytes,
        0,
        bytes.size,
        bounds
    )
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
        throw IOException("Foto tidak bisa dibaca.")
    }

    var sampleSize = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / sampleSize > MAX_PROFILE_PREVIEW_DIMENSION) {
        sampleSize *= 2
    }
    val options = BitmapFactory.Options().apply {
        inSampleSize = sampleSize
    }
    return BitmapFactory.decodeByteArray(
        bytes,
        0,
        bytes.size,
        options
    ) ?: throw IOException("Foto tidak bisa disiapkan.")
}

// Ambil potongan persegi yang sama dengan pratinjau sebelum dikirim ke server.
private fun cropProfileImage(
    bitmap: Bitmap,
    zoom: Float,
    positionX: Float,
    positionY: Float
): Bitmap {
    val side = (minOf(bitmap.width, bitmap.height) / zoom)
        .toInt()
        .coerceAtLeast(1)
    val left = ((bitmap.width - side) * positionX)
        .toInt()
        .coerceIn(0, bitmap.width - side)
    val top = ((bitmap.height - side) * positionY)
        .toInt()
        .coerceIn(0, bitmap.height - side)

    return Bitmap.createBitmap(
        bitmap,
        left,
        top,
        side,
        side
    )
}

// Baca foto di background dan batasi ukurannya sebelum upload.
private fun readProfileImage(
    context: Context,
    uri: Uri
): SelectedProfileImage {
    val selectedMimeType = context.contentResolver.getType(uri)?.lowercase()
    val mimeType = when (selectedMimeType) {
        "image/jpg" -> "image/jpeg"
        "image/jpeg" -> "image/jpeg"
        "image/png" -> "image/png"
        "image/webp" -> "image/webp"
        else -> throw IOException("Pilih foto JPG, PNG, atau WebP.")
    }
    val output = ByteArrayOutputStream()
    val input = context.contentResolver.openInputStream(uri)
        ?: throw IOException("Foto tidak bisa dibuka.")

    input.use { stream ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val bytesRead = stream.read(buffer)
            if (bytesRead < 0) break
            if (output.size() + bytesRead > MAX_PROFILE_IMAGE_BYTES) {
                throw IOException("Ukuran foto maksimal 5 MB.")
            }
            output.write(
                buffer,
                0,
                bytesRead
            )
        }
    }

    if (output.size() == 0) {
        throw IOException("Foto kosong atau tidak bisa dibaca.")
    }
    return SelectedProfileImage(
        output.toByteArray(),
        mimeType
    )
}

// Foto kamera dikirim sebagai JPEG lewat endpoint profil yang sama.
private fun encodeProfileImage(bitmap: Bitmap): ByteArray {
    val output = ByteArrayOutputStream()
    if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 92, output)) {
        throw IOException("Foto kamera gagal disiapkan.")
    }
    if (output.size() == 0) {
        throw IOException("Foto kamera kosong.")
    }
    if (output.size() > MAX_PROFILE_IMAGE_BYTES) {
        throw IOException("Ukuran foto maksimal 5 MB.")
    }
    return output.toByteArray()
}

private const val MAX_PROFILE_IMAGE_BYTES = 5 * 1024 * 1024
private const val MAX_PROFILE_PREVIEW_DIMENSION = 2048
