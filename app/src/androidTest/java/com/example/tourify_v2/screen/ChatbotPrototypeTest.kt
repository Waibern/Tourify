package com.example.tourify_v2.screen

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.tourify_v2.ui.theme.Tourify_v2Theme
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private val ChatCanvas = Color(0xFFF5F5F3)
private val ChatInk = Color(0xFF242521)
private val ChatMuted = Color(0xFF777873)
private val ChatBubble = Color(0xFFF1F1EF)
private val ChatYellow = Color(0xFFFFDF58)

// Tes ini menampilkan tombol Asisten dan chat contoh tanpa memanggil API.
@RunWith(AndroidJUnit4::class)
class ChatbotPrototypeTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun sparkleButtonOpensChatAndCloseReturnsHome() {
        compose.setContent {
            Tourify_v2Theme {
                ChatbotPrototype()
            }
        }

        compose.onNodeWithText("Jelajahi Tourify").assertIsDisplayed()
        compose.onNodeWithContentDescription("Buka Asisten Tourify").performClick()
        compose.onNodeWithText("Asisten Tourify").assertIsDisplayed()
        compose.onNodeWithText("Aku bisa bantu memberi info tentang wahana, deskripsi, dan harga tiket.")
            .assertIsDisplayed()
        capture("chatbot-preview-open")

        compose.onNodeWithContentDescription("Tutup chat").performClick()
        compose.onNodeWithText("Jelajahi Tourify").assertIsDisplayed()
        capture("chatbot-preview-home")
    }

    @Test
    fun chatComposerAddsLocalPreviewMessages() {
        compose.setContent {
            Tourify_v2Theme {
                ChatbotPrototype(startInChat = true)
            }
        }

        compose.onNodeWithContentDescription("Kirim pesan").assertIsNotEnabled()
        compose.onNodeWithTag("chat_message_input").performTextInput("Harga tiket wahana berapa?")
        compose.onNodeWithContentDescription("Kirim pesan").performClick()

        compose.onNodeWithText("Harga tiket wahana berapa?").assertIsDisplayed()
        compose.onNodeWithText("Mode pratinjau: pertanyaan siap dikirim setelah API dipasang.")
            .assertIsDisplayed()
    }

    // Simpan screenshot supaya susunan chat bisa dicek di emulator.
    private fun capture(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val file = File(instrumentation.targetContext.getExternalFilesDir(null), "$name.png")
        file.outputStream().use { output ->
            instrumentation.uiAutomation.takeScreenshot()
                .compress(Bitmap.CompressFormat.PNG, 100, output)
        }
    }
}

// Beranda contoh ini dipakai untuk mengecek posisi tombol Asisten.
// Chat contoh memakai ikon yang sama dengan aplikasi.
@Composable
private fun ChatbotPrototype(startInChat: Boolean = false) {
    var isChatOpen by rememberSaveable { mutableStateOf(startInChat) }
    val view = LocalView.current

    // Ikon status bar dibuat gelap supaya terlihat di latar terang.
    SideEffect {
        view.context.findHostActivity()?.window?.let { window ->
            WindowInsetsControllerCompat(window, view).apply {
                isAppearanceLightStatusBars = true
                isAppearanceLightNavigationBars = true
            }
        }
    }

    if (isChatOpen) {
        ChatbotPreviewScreen(
            onClose = { isChatOpen = false }
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ChatCanvas)
                    .statusBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 22.dp, vertical = 30.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "TOURIFY",
                    color = ChatMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "Jelajahi Tourify",
                    color = ChatInk,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    shadowElevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Cari pengalaman seru",
                            color = ChatInk,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Buka asisten Tourify untuk bertanya tentang wahana dan tiket.",
                            color = ChatMuted,
                            fontSize = 14.sp,
                            lineHeight = 21.sp
                        )
                    }
                }
            }

            // Tombol kuning dan ikon Asisten mengikuti desain contoh.
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 22.dp, bottom = 34.dp)
                    .size(68.dp)
                    .testTag("chatbot_floating_button")
                    .clickable(
                        role = Role.Button,
                        onClickLabel = "Buka Asisten Tourify"
                    ) {
                        isChatOpen = true
                    }
                    .semantics {
                        contentDescription = "Buka Asisten Tourify"
                    },
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 8.dp,
                tonalElevation = 0.dp
            ) {
                Box(
                    modifier = Modifier.padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = CircleShape,
                        color = ChatYellow
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            SparkleMark(modifier = Modifier.size(30.dp))
                        }
                    }
                }
            }
        }
    }
}

// Chat contoh memakai balon abu terang dan kolom input beraksen kuning.
@Composable
private fun ChatbotPreviewScreen(onClose: () -> Unit) {
    val messages = remember {
        mutableStateListOf(
            ChatPreviewMessage(
                fromUser = true,
                text = "Apa aja yang bisa aku cari di Tourify?"
            ),
            ChatPreviewMessage(
                fromUser = false,
                text = "Aku bisa bantu memberi info tentang wahana, deskripsi, dan harga tiket."
            ),
            ChatPreviewMessage(
                fromUser = false,
                text = "Kamu juga bisa bertanya tentang cara melihat destinasi dan tiket yang sudah dipesan."
            )
        )
    }
    var draft by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ChatCanvas)
            .statusBarsPadding()
            .imePadding()
    ) {
        // Header punya tombol kembali, chat baru, dan tutup.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 68.dp)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            PreviewHeaderButton(
                iconDescription = "Kembali ke beranda",
                onClick = onClose
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null
                )
            }
            Text(
                text = "Asisten Tourify",
                color = ChatInk,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PreviewHeaderButton(
                    iconDescription = "Chat baru",
                    onClick = {
                        messages.clear()
                        messages.add(
                            ChatPreviewMessage(
                                fromUser = false,
                                text = "Halo! Mau cari info wahana apa?"
                            )
                        )
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = null
                    )
                }
                PreviewHeaderButton(
                    iconDescription = "Tutup chat",
                    onClick = onClose
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = null
                    )
                }
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
                .weight(1f),
            shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
            color = Color.White
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("chat_message_list"),
                    contentPadding = PaddingValues(
                        horizontal = 18.dp,
                        vertical = 22.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    itemsIndexed(messages) { _, message ->
                        ChatPreviewBubble(message)
                    }
                }

                ChatPreviewComposer(
                    draft = draft,
                    onDraftChange = { draft = it },
                    onSend = {
                        val sentText = draft.trim()
                        if (sentText.isNotEmpty()) {
                            messages.add(ChatPreviewMessage(fromUser = true, text = sentText))
                            messages.add(
                                ChatPreviewMessage(
                                    fromUser = false,
                                    text = "Mode pratinjau: pertanyaan siap dikirim setelah API dipasang."
                                )
                            )
                            draft = ""
                        }
                    }
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

// Posisi dan warna membedakan pesan pengguna dari balasan asisten.
@Composable
private fun ChatPreviewBubble(message: ChatPreviewMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.fromUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!message.fromUser) {
            Surface(
                modifier = Modifier
                    .padding(top = 5.dp, end = 8.dp)
                    .size(30.dp),
                shape = CircleShape,
                color = Color(0xFFFFF4CC)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    SparkleMark(modifier = Modifier.size(18.dp))
                }
            }
        }

        Surface(
            modifier = Modifier.widthIn(max = 310.dp),
            shape = RoundedCornerShape(24.dp),
            color = ChatBubble
        ) {
            Text(
                text = message.text,
                color = ChatInk,
                fontSize = 15.sp,
                lineHeight = 23.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
            )
        }
    }
}

// Kolom input menyimpan draf dan menambahkan balasan contoh untuk tes tampilan.
@Composable
private fun ChatPreviewComposer(
    draft: String,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit
) {
    val borderBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFFC9A8FF),
            Color(0xFFFFE36A)
        )
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, borderBrush),
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp)) {
            BasicTextField(
                value = draft,
                onValueChange = onDraftChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 72.dp, max = 116.dp)
                    .testTag("chat_message_input"),
                textStyle = TextStyle(
                    color = ChatInk,
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                ),
                decorationBox = { innerTextField ->
                    Box {
                        if (draft.isEmpty()) {
                            Text(
                                text = "Tanyakan tentang wahana...",
                                color = ChatMuted,
                                fontSize = 14.sp
                            )
                        }
                        innerTextField()
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = ChatBubble
                    ) {
                        IconButton(
                            onClick = { onDraftChange("Info wahana") },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Isi contoh pertanyaan",
                                tint = ChatMuted
                            )
                        }
                    }
                    Text(
                        text = "Pratinjau",
                        color = ChatMuted,
                        fontSize = 12.sp
                    )
                }

                Surface(
                    modifier = Modifier
                        .heightIn(min = 44.dp)
                        .clickable(
                            enabled = draft.isNotBlank(),
                            onClickLabel = "Kirim pesan",
                            onClick = onSend
                        )
                        .testTag("chat_send_button"),
                    shape = CircleShape,
                    color = if (draft.isNotBlank()) ChatYellow else Color(0xFFFFF2B5)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Kirim",
                            color = ChatInk,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Kirim pesan",
                            tint = ChatInk,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// Tombol header berbentuk bulat seperti di gambar contoh.
@Composable
private fun PreviewHeaderButton(
    iconDescription: String,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.size(40.dp),
        shape = CircleShape,
        color = Color(0xFFF2F2F0)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .testTag("header_$iconDescription")
                .semantics {
                    contentDescription = iconDescription
                }
        ) {
            Box(contentAlignment = Alignment.Center) {
                content()
            }
        }
    }
}

// Chat contoh memakai ikon Asisten yang sama dengan navbar.
@Composable
private fun SparkleMark(modifier: Modifier = Modifier) {
    Icon(
        imageVector = Icons.Filled.AutoAwesome,
        contentDescription = null,
        tint = ChatInk,
        modifier = modifier
    )
}

// Data sederhana untuk isi chat di tes UI.
private data class ChatPreviewMessage(
    val fromUser: Boolean,
    val text: String
)

// Ambil activity host dari context supaya tes bisa mengatur system bar.
private tailrec fun Context.findHostActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findHostActivity()
    else -> null
}
