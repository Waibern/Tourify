package com.example.tourify_v2.screen

import android.content.Context
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
import androidx.compose.material3.Icon
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
    var selectedImageUri by remember {
        mutableStateOf<Uri?>(null)
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
            selectedImageUri = uri
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
                    onProfileImageSelected(
                        selectedImage.bytes,
                        selectedImage.mimeType
                    )
                } catch (exception: Exception) {
                    selectedImageUri = null
                    selectedImageError = exception.message ?: "Foto gagal dibaca."
                } finally {
                    isPreparingImage = false
                }
            }
        }
    }
    LaunchedEffect(profileImageUrl) {
        if (profileImageUrl != null) {
            selectedImageUri = null
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
                            val imageModel = selectedImageUri ?: profileImageUrl
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
                        // Picker Android memberi foto lokal; path server dipakai setelah tersimpan.
                        TextButton(
                            onClick = {
                                imagePicker.launch("image/*")
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
    }
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

private const val MAX_PROFILE_IMAGE_BYTES = 5 * 1024 * 1024
