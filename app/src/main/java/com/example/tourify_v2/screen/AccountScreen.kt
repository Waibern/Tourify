package com.example.tourify_v2.screen

import android.content.ActivityNotFoundException
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.content.pm.PackageManager
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tourify_v2.model.Ticket
import com.example.tourify_v2.ui.theme.TourifyBorder
import com.example.tourify_v2.ui.theme.TourifyCanvas
import com.example.tourify_v2.ui.theme.TourifyElevation
import com.example.tourify_v2.ui.theme.TourifyError
import com.example.tourify_v2.ui.theme.TourifyErrorContainer
import com.example.tourify_v2.ui.theme.TourifyForest
import com.example.tourify_v2.ui.theme.TourifyInk
import com.example.tourify_v2.ui.theme.TourifyMint
import com.example.tourify_v2.ui.theme.TourifyMuted
import com.example.tourify_v2.ui.theme.TourifyShapes
import com.example.tourify_v2.ui.theme.TourifySpacing
import com.example.tourify_v2.ui.theme.TourifySoftSurface
import com.example.tourify_v2.ui.theme.TourifySurface
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException

// Tampilkan info akun dan tiket yang sudah dimuat.
@Composable
fun AccountScreen(
    currentUser: String,
    profileImageUrl: String?,
    isUploadingProfileImage: Boolean,
    profileImageError: String,
    tickets: List<Ticket>,
    isLoadingTickets: Boolean,
    ticketsError: String,
    onRetryTickets: () -> Unit,
    onProfileImageSelected: (ByteArray, String) -> Unit,
    onTicketsClick: () -> Unit,
    onDestinationsClick: () -> Unit,
    onLogout: () -> Unit
) {
    // Simpan foto yang dipilih dan status akun untuk layar ini.
    val accountName = currentUser.ifBlank {
        "Pengguna Tourify"
    }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedImageBitmap by remember {
        mutableStateOf<Bitmap?>(null)
    }
    // Atur posisi crop dari awal setiap kali foto baru dipilih.
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
    var cameraPhotoPath by rememberSaveable {
        mutableStateOf<String?>(null)
    }
    // Abaikan tiket dari akun lama saat akun berganti.
    val accountTickets = tickets.filter {
        it.username == currentUser
    }
    // Foto dari kamera dan galeri diproses dengan cara yang sama sebelum di-crop.
    fun prepareSelectedImage(
        uri: Uri,
        cleanupFile: File? = null
    ) {
        selectedImageBitmap = null
        selectedImageError = ""
        isPreparingImage = true
        scope.launch {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    if (cleanupFile != null) {
                        decodeCameraProfileImage(
                            context,
                            uri
                        )
                    } else {
                        val selectedImage = readProfileImage(
                            context,
                            uri
                        )
                        decodeProfileImage(selectedImage.bytes)
                    }
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
                cleanupFile?.delete()
                cameraPhotoPath = null
            }
        }
    }
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            prepareSelectedImage(uri)
        }
    }
    val cameraPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { saved ->
        val photoFile = cameraPhotoPath?.let(::File)
        if (saved && photoFile != null) {
            try {
                prepareSelectedImage(
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        photoFile
                    ),
                    photoFile
                )
            } catch (exception: Exception) {
                photoFile.delete()
                cameraPhotoPath = null
                selectedImageError = exception.message ?: "Foto kamera gagal dibaca."
            }
        } else {
            photoFile?.delete()
            cameraPhotoPath = null
            selectedImageError = "Foto kamera tidak tersimpan. Coba ambil foto lagi."
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
            .background(TourifyCanvas)
    ) {
        // Header profil polos, tanpa gambar latar.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(bottom = 64.dp),
            verticalArrangement = Arrangement.spacedBy(TourifySpacing.section)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = TourifySpacing.screen,
                        top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 24.dp,
                        end = TourifySpacing.screen,
                        bottom = 24.dp
                    ),
                verticalArrangement = Arrangement.spacedBy(TourifySpacing.section)
            ) {
                // Judul profil rata kiri, sama seperti layar lain.
                Text(
                    text = "Profil",
                    modifier = Modifier.semantics {
                        heading()
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = TourifyInk
                )

                // Susun foto, nama akun, dan tombol ganti foto di tengah.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            // Foto profil dibuat lebih besar supaya lebih menonjol.
                            .size(128.dp)
                            .clip(CircleShape)
                            .background(TourifySurface)
                            .border(
                                width = 1.dp,
                                color = TourifyBorder,
                                shape = CircleShape
                            )
                            .semantics {
                                contentDescription = "Foto profil $accountName"
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = accountName.firstOrNull()?.uppercase() ?: "?",
                            color = TourifyForest,
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.semantics {
                                hideFromAccessibility()
                            }
                        )
                        val imageModel = selectedImageBitmap
                            ?: profileImageUrl
                        if (imageModel != null) {
                            AsyncImage(
                                model = imageModel,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    Text(
                        text = accountName,
                        color = TourifyInk,
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Tombol ganti foto pakai hijau di atas latar polos.
                    OutlinedButton(
                        onClick = {
                            showImageSourceDialog = true
                        },
                        enabled = !isImageBusy,
                        modifier = Modifier.heightIn(min = 48.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(
                            width = 1.dp,
                            color = TourifyBorder
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = TourifyForest
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (isImageBusy) "Menyimpan foto…" else "Ubah foto"
                        )
                    }
                }
            }
            if (imageError.isNotBlank()) {
                Text(
                    text = imageError,
                    color = TourifyError,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }

            // Ringkasan aktivitas akun.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = TourifySpacing.screen),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Aktivitas perjalanan",
                    color = TourifyInk,
                    style = MaterialTheme.typography.titleMedium
                )
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = TourifyShapes.card,
                    color = TourifySurface,
                    shadowElevation = TourifyElevation.card
                ) {
                    when {
                        isLoadingTickets -> {
                            Row(
                                modifier = Modifier.padding(18.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    text = "Memuat ringkasan perjalanan…",
                                    color = TourifyMuted,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        ticketsError.isNotBlank() -> {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Ringkasan belum tersedia",
                                    color = TourifyError,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = ticketsError,
                                    color = TourifyMuted,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                TextButton(
                                    onClick = onRetryTickets,
                                    modifier = Modifier.heightIn(min = 48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Coba lagi")
                                }
                            }
                        }

                        else -> {
                            Column {
                                ProfileActivityRow(
                                    label = "Pesanan",
                                    value = accountTickets.size.toString()
                                )
                                ProfileActivityDivider()
                                ProfileActivityRow(
                                    label = "Tiket",
                                    value = accountTickets.sumOf {
                                        it.quantity
                                    }.toString()
                                )
                                ProfileActivityDivider()
                                ProfileActivityRow(
                                    label = "Check-in",
                                    value = accountTickets.sumOf {
                                        it.checkedInQuantity
                                    }.toString()
                                )
                            }
                        }
                    }
                }
            }

            // Pintasan ke pengaturan akun.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = TourifySpacing.screen),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Akses cepat",
                    color = TourifyInk,
                    style = MaterialTheme.typography.titleMedium
                )
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = TourifyShapes.card,
                    color = TourifySurface,
                    shadowElevation = TourifyElevation.card
                ) {
                    Column {
                        ProfileMenuRow(
                            label = "Tiket saya",
                            supportingText = "Pesanan dan kode QR",
                            icon = Icons.AutoMirrored.Filled.ReceiptLong,
                            onClick = onTicketsClick
                        )
                        ProfileMenuDivider()
                        ProfileMenuRow(
                            label = "Jelajahi destinasi",
                            supportingText = "Temukan wahana berikutnya",
                            icon = Icons.Default.Explore,
                            onClick = onDestinationsClick
                        )
                    }
                }
            }

            // Pisahkan tombol keluar dari menu lainnya.
            Box(
                modifier = Modifier.padding(horizontal = TourifySpacing.screen)
            ) {
                ProfileMenuRow(
                    label = "Keluar dari akun",
                    icon = Icons.AutoMirrored.Filled.Logout,
                    onClick = onLogout,
                    tint = TourifyError,
                    iconBackground = TourifyErrorContainer,
                    showArrow = false
                )
            }
        }

        // Pilih foto lewat pemilih bawaan Android.
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
                                if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
                                    selectedImageError = "Kamera tidak tersedia di perangkat ini."
                                } else {
                                    try {
                                        val photoDirectory = File(
                                            context.cacheDir,
                                            "profile-photos"
                                        )
                                        if (!photoDirectory.exists() && !photoDirectory.mkdirs()) {
                                            throw IOException("Penyimpanan foto sementara tidak tersedia.")
                                        }
                                        val photoFile = File.createTempFile(
                                            "profile-",
                                            ".jpg",
                                            photoDirectory
                                        )
                                        cameraPhotoPath = photoFile.absolutePath
                                        cameraPicker.launch(
                                            FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.fileprovider",
                                                photoFile
                                            )
                                        )
                                    } catch (exception: Exception) {
                                        cameraPhotoPath?.let(::File)?.delete()
                                        cameraPhotoPath = null
                                        selectedImageError = if (exception is ActivityNotFoundException) {
                                            "Aplikasi kamera tidak tersedia."
                                        } else {
                                            exception.message ?: "Kamera gagal dibuka. Coba lagi."
                                        }
                                    }
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

        // Tinjau foto dulu, lalu atur posisi dan zoom sebelum dipotong.
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

// Atur posisi foto di sini sebelum diunggah.
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
            Text(
                text = "Atur foto profil",
                style = MaterialTheme.typography.titleLarge
            )
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
                        .background(TourifyMint),
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
                    text = "Atur pembesaran dan posisi foto dengan penggeser di bawah.",
                    fontSize = 14.sp,
                    color = TourifyMuted
                )
                Text(
                    text = "Zoom ${((zoom - 1f) * 100).toInt()}%",
                    fontSize = 14.sp,
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
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Slider(
                    value = positionX,
                    onValueChange = onPositionXChange,
                    enabled = !isBusy
                )
                Text(
                    text = "Posisi vertikal",
                    fontSize = 14.sp,
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
                        fontSize = 14.sp
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

// Tampilkan angka dan label aktivitas sejajar.
@Composable
private fun ProfileActivityRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 16.dp,
                vertical = 14.dp
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            color = TourifyMuted,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = value,
            color = TourifyInk,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

// Pisahkan tiap aktivitas dengan garis.
@Composable
private fun ProfileActivityDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(TourifyBorder)
    )
}

@Composable
private fun ProfileMenuDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 74.dp,
                end = 16.dp
            )
            .height(1.dp)
            .background(TourifyBorder)
    )
}

@Composable
private fun ProfileMenuRow(
    label: String,
    supportingText: String? = null,
    icon: ImageVector,
    onClick: () -> Unit,
    tint: Color = TourifyInk,
    iconBackground: Color = TourifySoftSurface,
    showArrow: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 68.dp)
            .padding(
                horizontal = 16.dp,
                vertical = 12.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(44.dp),
            shape = RoundedCornerShape(12.dp),
            color = iconBackground
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                color = tint,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            supportingText?.let {
                Text(
                    text = it,
                    color = TourifyMuted,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        if (showArrow) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = TourifyMuted,
                modifier = Modifier.size(20.dp)
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
        onRetryTickets = {},
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

// Baca foto dalam ukuran kecil dulu supaya foto besar tetap ringan.
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

// Kecilkan foto kamera dulu supaya gambar besar nggak memenuhi memori.
private fun decodeCameraProfileImage(
    context: Context,
    uri: Uri
): Bitmap {
    val mimeType = context.contentResolver.getType(uri)
    if (mimeType != "image/jpeg") {
        throw IOException("Foto kamera tidak bisa dibaca. Coba ambil foto lagi.")
    }

    val bounds = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    // Tahap ini cuma membaca ukuran foto; bitmap-nya belum dibuat.
    val boundsStream = context.contentResolver.openInputStream(uri)
        ?: throw IOException("Foto kamera tidak bisa dibuka.")
    boundsStream.use { stream ->
        BitmapFactory.decodeStream(
            stream,
            null,
            bounds
        )
    }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
        throw IOException("Foto kamera tidak valid.")
    }

    var sampleSize = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / sampleSize > MAX_PROFILE_PREVIEW_DIMENSION) {
        sampleSize *= 2
    }
    val options = BitmapFactory.Options().apply {
        inSampleSize = sampleSize
    }
    val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(
            stream,
            null,
            options
        )
    } ?: throw IOException("Foto kamera tidak bisa disiapkan.")
    val orientation = context.contentResolver.openInputStream(uri)?.use { stream ->
        ExifInterface(stream).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
    } ?: ExifInterface.ORIENTATION_NORMAL
    val rotation = when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
    }
    if (rotation == 0f) {
        return bitmap
    }

    val matrix = Matrix().apply {
        postRotate(rotation)
    }
    return Bitmap.createBitmap(
        bitmap,
        0,
        0,
        bitmap.width,
        bitmap.height,
        matrix,
        true
    )
}

// Potong foto dengan ukuran yang sama seperti pratinjau sebelum dikirim.
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

// Baca foto di latar belakang dan kecilkan sebelum diunggah.
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

// Kirim foto kamera sebagai JPEG lewat endpoint profil.
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
