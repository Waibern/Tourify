package com.example.tourify_v2.screen

import com.example.tourify_v2.ui.theme.*
import com.example.tourify_v2.R

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.AddComment
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tourify_v2.model.ChatHistoryItem
import com.example.tourify_v2.model.ChatBookingProposal
import com.example.tourify_v2.model.ChatResponse
import com.example.tourify_v2.model.Ticket
import com.example.tourify_v2.model.Wahana
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import java.text.NumberFormat
import kotlin.math.hypot
import kotlinx.coroutines.launch

private val ChatCanvas = TourifyCanvas
private val ChatInk = TourifyInk
private val ChatMuted = TourifyMuted
// Tombol Asisten pakai hijau utama supaya warnanya konsisten.
private val ChatGreen = TourifyForest
private const val ChatMessageLimit = 1000

// Tombol ini membuka chat dari halaman utama.
@Composable
internal fun ChatbotFloatingButton(
    onClick: () -> Unit,
    onCenterChanged: (Offset) -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember {
        MutableInteractionSource()
    }
    val pressed by interactionSource.collectIsPressedAsState()
    val buttonScale by animateFloatAsState(
        targetValue = if (pressed) 0.93f else 1f,
        animationSpec = tween(180),
        label = "Tekanan tombol chat"
    )

    // Tombol mengecil sebentar saat ditekan, lalu chat terbuka.
    Surface(
        modifier = modifier
            .size(68.dp)
            .scale(buttonScale)
            .onGloballyPositioned { coordinates ->
                val position = coordinates.positionInRoot()
                onCenterChanged(
                    Offset(
                        x = position.x + coordinates.size.width / 2f,
                        y = position.y + coordinates.size.height / 2f
                    )
                )
            }
            .testTag("chatbot_floating_button")
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClickLabel = "Buka Asisten Tourify",
                onClick = onClick
            )
            .semantics {
                contentDescription = "Buka Asisten Tourify"
            },
        shape = CircleShape,
        color = TourifySurface,
        shadowElevation = TourifyElevation.floating,
        tonalElevation = 0.dp
    ) {
        Box(
            modifier = Modifier.padding(6.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = CircleShape,
                color = ChatGreen
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    ChatBrandMark(
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }
    }
}

// Efek buka chat dimulai dari tombol lalu menutupi layar.
internal fun Modifier.chatbotCircleReveal(
    progress: Float,
    origin: Offset
): Modifier = drawWithContent {
    val corners = listOf(
        Offset.Zero,
        Offset(
            size.width,
            0f
        ),
        Offset(
            0f,
            size.height
        ),
        Offset(
            size.width,
            size.height
        )
    )
    val maxRadius = corners.maxOf { corner ->
        hypot(
            (corner.x - origin.x).toDouble(),
            (corner.y - origin.y).toDouble()
        ).toFloat()
    }
    val radius = (maxRadius * progress).coerceAtLeast(1f)
    val revealPath = Path().apply {
        addOval(
            Rect(
                left = origin.x - radius,
                top = origin.y - radius,
                right = origin.x + radius,
                bottom = origin.y + radius
            )
        )
    }

    clipPath(revealPath) {
        this@drawWithContent.drawContent()
    }
}

// Tampilkan panduan sebelum chat dimulai.
@Composable
internal fun ChatbotAssistantScreen(
    currentUser: String,
    isSending: Boolean,
    isBooking: Boolean,
    onSendMessage: (
        String,
        List<ChatHistoryItem>,
        (ChatResponse) -> Unit
    ) -> Unit,
    onConfirmBooking: (
        ChatBookingProposal,
        (Boolean, String) -> Unit
    ) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    availableWahana: List<Wahana> = emptyList(),
    userTickets: List<Ticket> = emptyList(),
    conversationState: ChatbotConversationState? = null,
    onOpenTickets: () -> Unit = {}
) {
    // Pakai percakapan dari layar utama kalau ada, supaya chat nggak hilang saat panel ditutup.
    val localConversationState = remember {
        ChatbotConversationState()
    }
    val activeConversationState = conversationState ?: localConversationState
    val messages = activeConversationState.messages
    var draft by activeConversationState.draft
    val messageListState = rememberLazyListState()
    val composerFocus = remember {
        FocusRequester()
    }
    val scrollScope = rememberCoroutineScope()
    var showHelp by rememberSaveable {
        mutableStateOf(false)
    }
    var confirmNewChat by rememberSaveable {
        mutableStateOf(false)
    }
    // Tampilkan pintasan saat pengguna menggulir menjauh dari pesan terbaru.
    val showLatestShortcut by remember(messageListState, messages) {
        derivedStateOf {
            val lastVisible = messageListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
            lastVisible != null && lastVisible < messages.lastIndex
        }
    }

    // Tombol konfirmasi mengirim draf pesanan ke server.
    fun confirmBooking(
        index: Int,
        proposal: ChatBookingProposal
    ) {
        val message = messages.getOrNull(index) ?: return
        if (
            message.proposalStatus != BookingProposalStatus.PENDING ||
                isBooking ||
                currentUser.isBlank()
        ) return

        messages[index] = message.copy(proposalStatus = BookingProposalStatus.PROCESSING)
        onConfirmBooking(proposal) { success, _ ->
            if (index in messages.indices) {
                val latestMessage = messages[index]
                messages[index] = latestMessage.copy(
                    text = if (success) {
                        "Pesanan berhasil dibuat. Kamu bisa melihat tiket dan kode QR di tab Tiket."
                    } else {
                        "Status pesanan belum bisa dipastikan. Cek tab Tiket sebelum memesan lagi agar tidak membuat pesanan ganda."
                    },
                    proposalStatus = if (success) {
                        BookingProposalStatus.CONFIRMED
                    } else {
                        BookingProposalStatus.FAILED
                    }
                )
            }
        }
    }

    // Tombol Ubah membatalkan draf dan meminta detail baru.
    fun editProposal(index: Int) {
        val message = messages.getOrNull(index) ?: return
        if (message.proposalStatus != BookingProposalStatus.PENDING) return

        messages[index] = message.copy(
            text = "Tulis bagian yang ingin kamu ubah, misalnya tanggal, jumlah tiket, atau metode bayar.",
            proposalStatus = BookingProposalStatus.CANCELLED
        )
        // Langsung arahkan pengguna ke kolom untuk menulis perubahan.
        composerFocus.requestFocus()
    }

    // Tahan kirim ganda meski tombol diketuk sebelum status server sempat tampil.
    fun isChatBusy(): Boolean = isSending || isBooking || messages.any { it.isLoading }

    fun recentHistory(before: Int = messages.size): List<ChatHistoryItem> = messages
        .take(before)
        .filterNot { message ->
            message.isLoading || message.isFailed
        }
        .takeLast(10)
        .map { message ->
            ChatHistoryItem(
                role = if (message.fromUser) "user" else "assistant",
                text = message.asHistoryText()
            )
        }

    // Pakai alur yang sama untuk kirim baru dan coba lagi.
    fun requestAnswer(
        question: String,
        history: List<ChatHistoryItem>,
        answerIndex: Int
    ) {
        onSendMessage(
            question,
            history
        ) { response ->
            if (answerIndex in messages.indices && messages[answerIndex].isLoading) {
                val serverAnswer = response.answer?.takeIf(String::isNotBlank)
                    ?: response.message
                    ?: "Asisten gagal menjawab. Coba lagi nanti."
                // Kalau server belum menyiapkan draf, minta detail yang masih kurang.
                val localBookingReply = if (response.bookingProposal == null) {
                    buildBookingReply(
                        question = question,
                        history = history,
                        answer = serverAnswer,
                        availableWahana = availableWahana
                    )
                } else {
                    null
                }
                val answer = localBookingReply?.text ?: serverAnswer
                val proposal = response.bookingProposal ?: localBookingReply?.proposal
                if (proposal != null) {
                    messages.indices.forEach { index ->
                        val oldMessage = messages[index]
                        if (oldMessage.proposalStatus == BookingProposalStatus.PENDING) {
                            messages[index] = oldMessage.copy(
                                text = "Draf ini diganti dengan ringkasan pesanan terbaru.",
                                proposalStatus = BookingProposalStatus.CANCELLED
                            )
                        }
                    }
                }
                // Balasan gagal diberi aksi ulang di pesan yang sama.
                messages[answerIndex] = messages[answerIndex].copy(
                    text = answer,
                    isFailed = !response.success && localBookingReply == null,
                    isLoading = false,
                    bookingProposal = proposal,
                    proposalStatus = if (proposal == null) null else BookingProposalStatus.PENDING
                )
            }
        }
    }

    // Pertahankan pertanyaan dan draf yang sedang diketik saat mencoba ulang.
    fun retryAnswer(index: Int) {
        if (isChatBusy() || index != messages.lastIndex) return
        val failed = messages.getOrNull(index) ?: return
        val question = messages.getOrNull(index - 1)?.takeIf { it.fromUser } ?: return
        if (!failed.isFailed) return

        messages[index] = failed.copy(
            text = "",
            isLoading = true,
            isFailed = false
        )
        requestAnswer(
            question = question.text,
            history = recentHistory(before = index - 1),
            answerIndex = index
        )
    }

    // Saran dan pertanyaan yang diketik memakai alur yang sama.
    fun sendQuestion(rawQuestion: String) {
        val question = rawQuestion.trim()
        if (question.isEmpty() || isChatBusy()) return

        // Pertanyaan baru membatalkan draf yang masih menunggu konfirmasi.
        messages.indices.forEach { index ->
            val pendingMessage = messages[index]
            if (pendingMessage.proposalStatus == BookingProposalStatus.PENDING) {
                messages[index] = pendingMessage.copy(
                    text = "Draf dibatalkan karena detail percakapan berubah.",
                    proposalStatus = BookingProposalStatus.CANCELLED
                )
            }
        }

        val history = recentHistory()

        // Jawab jadwal terdekat dari tiket akun yang sama supaya tanggalnya akurat.
        val nearestTicketReply = buildNearestTicketReply(
            question = question,
            history = history,
            tickets = userTickets
        )
        messages.add(
            ChatMessage(
                fromUser = true,
                text = question
            )
        )
        draft = ""

        if (nearestTicketReply != null) {
            messages.add(
                ChatMessage(
                    fromUser = false,
                    text = nearestTicketReply
                )
            )
            return
        }

        // Pakai jawaban pengguna untuk melengkapi draf tanpa bertanya lagi ke server.
        val lastAssistantReply = history.lastOrNull {
            it.role == "assistant"
        }?.text.orEmpty()
        val hasPreviousBookingRequest = history.any { item ->
            item.role == "user" &&
                bookingIntentPattern.containsMatchIn(item.text)
        }
        val wasClarifyingBooking = hasPreviousBookingRequest && (
            lastAssistantReply.startsWith("Bisa, aku bantu pesan tiket") ||
                lastAssistantReply.contains(
                    "belum bisa menyiapkan draf",
                    ignoreCase = true
                ) ||
                lastAssistantReply.contains(
                    "form pemesanan",
                    ignoreCase = true
                )
        )
        if (wasClarifyingBooking) {
            val followUp = buildBookingReply(
                question,
                history,
                availableWahana
            )
            if (followUp != null) {
                messages.add(
                    ChatMessage(
                        fromUser = false,
                        text = followUp.text,
                        bookingProposal = followUp.proposal,
                        proposalStatus = if (followUp.proposal == null) {
                            null
                        } else {
                            BookingProposalStatus.PENDING
                        }
                    )
                )
                return
            }
        }

        messages.add(
            ChatMessage(
                fromUser = false,
                text = "",
                isLoading = true
            )
        )
        val answerIndex = messages.lastIndex
        requestAnswer(
            question = question,
            history = history,
            answerIndex = answerIndex
        )
    }

    // Gulir saat pesan ditambahkan. Balasan yang selesai tidak menarik pembaca dari riwayat.
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            messageListState.animateScrollToItem(messages.lastIndex)
        }
    }
    val chatBusy = isChatBusy()

    // Pisahkan membuka bantuan atau chat baru dari aksi yang benar-benar menghapus isi.
    if (showHelp) {
        ChatHelpDialog(
            onDismiss = {
                showHelp = false
            }
        )
    }
    if (confirmNewChat) {
        AlertDialog(
            onDismissRequest = {
                confirmNewChat = false
            },
            // Dialog memakai permukaan putih yang sama dengan panel asisten.
            containerColor = TourifySurface,
            titleContentColor = ChatInk,
            textContentColor = ChatMuted,
            title = {
                Text(text = "Mulai chat baru?")
            },
            text = {
                Text(text = "Percakapan ini dan pesan yang belum dikirim akan dihapus.")
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        confirmNewChat = false
                    }
                ) {
                    Text(text = "Tetap di chat")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmNewChat = false
                        if (!isChatBusy()) {
                            messages.clear()
                            draft = ""
                        }
                    }
                ) {
                    Text(text = "Mulai chat baru")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TourifySurface)
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Judul dan aksi chat tetap terlihat saat tab pesan digulir.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .heightIn(min = 64.dp)
                .padding(
                    horizontal = 12.dp,
                    vertical = 8.dp
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ChatHeaderButton(
                iconDescription = "Kembali ke aplikasi",
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
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .weight(1f)
                    .semantics {
                        heading()
                    }
            )
            ChatHeaderButton(
                iconDescription = "Bantuan chat",
                onClick = {
                    showHelp = true
                }
            ) {
                Icon(
                    imageVector = Icons.Outlined.HelpOutline,
                    contentDescription = null
                )
            }
            if (messages.isNotEmpty() || draft.isNotBlank()) {
                ChatHeaderButton(
                    iconDescription = "Chat baru",
                    enabled = !chatBusy,
                    onClick = {
                        confirmNewChat = true
                    }
                ) {
                    // Ikon tambah percakapan memperjelas bahwa ini memulai chat baru.
                    Icon(
                        imageVector = Icons.Outlined.AddComment,
                        contentDescription = null
                    )
                }
            }
        }
        HorizontalDivider(color = TourifyBorder)
        // Percakapan memakai lebar yang sama dengan kolom pesan pada layar lebar.
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(ChatCanvas),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxSize()
                    .testTag("chat_message_list"),
                state = messageListState,
                contentPadding = PaddingValues(
                    start = TourifySpacing.screen,
                    top = TourifySpacing.screen,
                    end = TourifySpacing.screen,
                    bottom = if (showLatestShortcut) 68.dp else TourifySpacing.screen
                ),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                if (messages.isEmpty()) {
                    item {
                        ChatEmptyState(
                            onSuggestionClick = ::sendQuestion
                        )
                    }
                }
                itemsIndexed(messages) { index, message ->
                    ChatBubble(
                        message = message,
                        isBooking = isBooking,
                        isSending = chatBusy,
                        canConfirmBooking = currentUser.isNotBlank(),
                        showBookingAction = index == messages.lastIndex &&
                            shouldShowBookingAction(message),
                        onRetry = if (index == messages.lastIndex && message.isFailed) {
                            { retryAnswer(index) }
                        } else {
                            null
                        },
                        onOpenTickets = onOpenTickets,
                        onStartBooking = {
                            sendQuestion("Iya, aku mau pesan tiket")
                        },
                        onConfirmBooking = { proposal ->
                            confirmBooking(
                                index,
                                proposal
                            )
                        },
                        onEditProposal = {
                            editProposal(index)
                        }
                    )
                }
            }
            if (showLatestShortcut) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp),
                    shape = CircleShape,
                    shadowElevation = TourifyElevation.card,
                    color = TourifySurface
                ) {
                    IconButton(
                        onClick = {
                            val lastIndex = messages.lastIndex
                            scrollScope.launch {
                                if (lastIndex >= 0) {
                                    messageListState.animateScrollToItem(lastIndex)
                                }
                            }
                        },
                        modifier = Modifier.semantics {
                            contentDescription = "Ke pesan terbaru"
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = null
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = TourifyBorder)
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Jelaskan dampak mengirim pesan ketika ringkasan pesanan masih menunggu.
            if (
                draft.isNotBlank() &&
                messages.any { it.proposalStatus == BookingProposalStatus.PENDING }
            ) {
                Text(
                    text = "Pesan baru akan menutup draf pesanan.",
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth()
                        .padding(
                            start = TourifySpacing.screen,
                            top = 8.dp,
                            end = TourifySpacing.screen
                        ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = ChatMuted
                )
            }
            ChatComposer(
                draft = draft,
                isSending = chatBusy,
                focusRequester = composerFocus,
                onDraftChange = {
                    draft = it.take(ChatMessageLimit).dropLastWhile(Char::isHighSurrogate)
                },
                onSend = {
                    sendQuestion(draft)
                }
            )
            // Beri tahu batas sebelum teks salinan terpotong.
            if (draft.length >= ChatMessageLimit * 9 / 10) {
                Text(
                    text = "${draft.length}/$ChatMessageLimit karakter",
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth()
                        .padding(
                            start = TourifySpacing.screen,
                            end = TourifySpacing.screen,
                            bottom = 8.dp
                        ),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.End,
                    color = ChatMuted
                )
            }
        }
    }
}

// Bantuan tetap bisa dibaca saat percakapan sudah panjang.
@Composable
private fun ChatHelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        // Samakan warna dengan dialog konfirmasi di asisten.
        containerColor = TourifySurface,
        titleContentColor = ChatInk,
        textContentColor = ChatMuted,
        title = {
            Text(text = "Bantuan Asisten")
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val topics = listOf(
                    "Cari wahana" to "Sebut nama wahana untuk menanyakan harga dan informasi kunjungan.",
                    "Cek tiket" to "Tanya “Kapan jadwal kunjunganku berikutnya?” untuk melihat jadwal terdekat.",
                    "Pesan tiket" to "Sebut wahana, tanggal, jumlah tiket, dan metode bayar. Periksa ringkasannya sebelum menekan konfirmasi."
                )
                topics.forEach { (title, description) ->
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            color = ChatInk
                        )
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = ChatMuted
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Tutup bantuan")
            }
        }
    )
}

private data class BookingReply(
    val text: String,
    val proposal: ChatBookingProposal? = null
)

// Ucapan seperti "mau pesen tiket" tetap dikenali.
private val bookingIntentPattern = Regex(
    "(?:pesan(?:kan|in)?|pesen(?:in|kan)?|memesan|beli|belikan)\\s+(?:kan\\s+)?tiket" +
        "|(?:mau|ingin|tolong)\\s+(?:pesan(?:kan|in)?|pesen(?:in|kan)?|memesan|booking|beli)" +
        "|booking|book\\s+(?:ticket|me)",
    RegexOption.IGNORE_CASE
)

private fun buildBookingReply(
    question: String,
    history: List<ChatHistoryItem>,
    availableWahana: List<Wahana>,
    answer: String? = null
): BookingReply? {
    // Pakai jawaban lokal hanya kalau server belum menyiapkan draf.
    if (answer != null) {
        val normalizedAnswer = answer.lowercase(Locale.ROOT)
        val asksForMissingBookingDetails =
            normalizedAnswer.contains("belum bisa menyiapkan") ||
                normalizedAnswer.contains("belum bisa membuat") ||
                normalizedAnswer.contains("pastikan") &&
                    listOf(
                        "wahana",
                        "tanggal",
                        "jumlah",
                        "metode"
                    )
                        .any {
                            normalizedAnswer.contains(it)
                        }
        if (!asksForMissingBookingDetails) return null
    }

    // Gabungkan pesan pengguna supaya detail chat sebelumnya ikut terbaca.
    val userContext = (history.filter {
        it.role == "user"
    }.map {
        it.text
    } + question)
        .joinToString(" ")
        .lowercase(Locale.ROOT)
    if (!bookingIntentPattern.containsMatchIn(userContext)) return null

    // Cocokkan nama wahana dengan katalog yang sudah dimuat.
    fun findWahana(text: String) = availableWahana
        .sortedByDescending {
            it.name.length
        }
        .firstOrNull { wahana ->
            Regex("(?<![\\p{L}\\p{N}])${Regex.escape(wahana.name.lowercase(Locale.ROOT))}(?![\\p{L}\\p{N}])")
                .containsMatchIn(text.lowercase(Locale.ROOT))
        }
        ?.name
    val requestedWahana = findWahana(question)
        ?: findWahana(userContext)
        ?: (history.filter {
            it.role == "user"
        }.map {
            it.text
        } + question)
            .asReversed()
            .firstNotNullOfOrNull(::extractRequestedWahana)
    // Angka seperti "5" dihitung sebagai jumlah hanya setelah bot menanyakannya.
    val quantityWasRequested = history.any { item ->
        item.role == "assistant" &&
            (item.text.contains(
                "berapa tiket",
                ignoreCase = true
            ) ||
                item.text.contains(
                    "jumlah tiket",
                    ignoreCase = true
                ))
    }
    // Ambil tanggal, jumlah tiket, dan cara bayar dari chat.
    val visitDate = parseVisitDate(userContext)
    val quantity = parseTicketQuantity(
        userContext,
        question,
        quantityWasRequested
    )
    val paymentMethod = parsePaymentMethod(userContext)
    val wahana = requestedWahana?.let { name ->
        availableWahana.firstOrNull {
            it.name.equals(
                name,
                ignoreCase = true
            )
        }
            ?: availableWahana.firstOrNull {
                it.name.contains(
                    name,
                    ignoreCase = true
                )
            }
    }

    // Tanyakan data yang belum ada sebelum membuat draf.
    val missing = buildList {
        if (wahana == null) add("Wahana apa yang ingin kamu kunjungi?")
        if (visitDate == null) add("Tanggal kunjungannya kapan? Contoh: 27 Oktober.")
        if (quantity == null) add("Mau berapa tiket (1–20)?")
        if (paymentMethod == null) {
            add("Pilih metode pembayaran: Bank, QRIS, atau E-Wallet?")
        }
    }
    if (missing.isNotEmpty()) {
        val destination = requestedWahana?.let {
            " untuk **$it**"
        }.orEmpty()
        return BookingReply(
            text = "Bisa, aku bantu pesan tiket$destination. Boleh info:\n${missing.joinToString("\n") { "- $it" }}"
        )
    }

    // Kalau data sudah lengkap, siapkan draf untuk dikonfirmasi.
    val selectedWahana = requireNotNull(wahana)
    val selectedDate = requireNotNull(visitDate)
    val selectedQuantity = requireNotNull(quantity)
    val selectedPaymentMethod = requireNotNull(paymentMethod)
    return BookingReply(
        text = "Sip, draf tiket untuk **${selectedWahana.name}** sudah siap. Cek detailnya dulu, lalu konfirmasi kalau sudah benar.",
        proposal = ChatBookingProposal(
            wahanaId = selectedWahana.id,
            wahanaName = selectedWahana.name,
            visitDate = selectedDate.toString(),
            quantity = selectedQuantity,
            paymentMethod = selectedPaymentMethod,
            unitPrice = selectedWahana.ticketPrice,
            totalPrice = selectedWahana.ticketPrice * selectedQuantity
        )
    )
}

// Ambil nama wahana dari pesan kalau katalog belum dimuat.
private fun extractRequestedWahana(question: String): String? {
    val match = Regex(
        "\\b(?:ke|untuk)\\s+([\\p{L}][\\p{L}0-9 ]{1,40}?)(?=\\s+(?:tanggal|tgl|besok|lusa|sebanyak|jumlah|pakai|metode|pembayaran|dong|ya|pls|please)\\b|[,?.!]|$)",
        RegexOption.IGNORE_CASE
    ).find(question) ?: return null
    return match.groupValues[1]
        .trim()
        .split(Regex("\\s+"))
        .filter(String::isNotBlank)
        .joinToString(" ") { word ->
            word.lowercase(Locale.ROOT).replaceFirstChar {
                it.titlecase(Locale.ROOT)
            }
        }
        .takeIf(String::isNotBlank)
}

private fun parseVisitDate(
    text: String,
    today: LocalDate = LocalDate.now()
): LocalDate? {
    // Terima tanggal lengkap dengan format tahun-bulan-hari.
    val isoDate = Regex("\\b(20\\d{2})-(\\d{1,2})-(\\d{1,2})\\b").find(text)
    if (isoDate != null) {
        return runCatching {
            LocalDate.of(
                isoDate.groupValues[1].toInt(),
                isoDate.groupValues[2].toInt(),
                isoDate.groupValues[3].toInt()
            )
        }.getOrNull()
    }

    // Nama bulan boleh ditulis dengan atau tanpa tahun.
    val monthNumbers = mapOf(
        "januari" to 1,
        "februari" to 2,
        "maret" to 3,
        "april" to 4,
        "mei" to 5,
        "juni" to 6,
        "juli" to 7,
        "agustus" to 8,
        "september" to 9,
        "oktober" to 10,
        "november" to 11,
        "desember" to 12
    )
    val namedDate = Regex(
        "\\b(\\d{1,2})\\s+(januari|februari|maret|april|mei|juni|juli|agustus|september|oktober|november|desember)(?:\\s+(20\\d{2}))?\\b"
    ).find(text)
    if (namedDate != null) {
        val yearText = namedDate.groupValues[3]
        val date = runCatching {
            LocalDate.of(
                yearText.toIntOrNull() ?: today.year,
                monthNumbers.getValue(namedDate.groupValues[2]),
                namedDate.groupValues[1].toInt()
            )
        }.getOrNull() ?: return null
        return if (yearText.isBlank() && date.isBefore(today)) date.plusYears(1) else date
    }

    // Baca angka sebagai tanggal/bulan, sesuai kebiasaan di Indonesia.
    val numericDate = Regex("\\b(\\d{1,2})[/-](\\d{1,2})(?:[/-](20\\d{2}))?\\b").find(text)
    if (numericDate != null) {
        val year = numericDate.groupValues[3].toIntOrNull() ?: today.year
        val date = runCatching {
            LocalDate.of(
                year,
                numericDate.groupValues[2].toInt(),
                numericDate.groupValues[1].toInt()
            )
        }.getOrNull() ?: return null
        return if (numericDate.groupValues[3].isBlank() && date.isBefore(today)) {
            date.plusYears(1)
        } else {
            date
        }
    }

    // Cocokkan sebutan hari dengan minggu ini atau minggu depan.
    val nextWeekDay = Regex(
        "\\b(senin|selasa|rabu|kamis|jumat|sabtu|minggu)(?:\\s+minggu)?\\s+depan\\b|\\bminggu depan\\s+(senin|selasa|rabu|kamis|jumat|sabtu|minggu)\\b"
    ).find(text)
    val weekdayName = nextWeekDay?.groupValues?.drop(1)?.firstOrNull {
        it.isNotBlank()
    }
        ?: if (Regex("\\bminggu depan\\b").containsMatchIn(text)) {
            null
        } else {
            Regex("\\b(senin|selasa|rabu|kamis|jumat|sabtu|minggu)\\b")
                .find(text)
                ?.groupValues
                ?.get(1)
        }
    val day = weekdayName?.let(::indonesianDayOfWeek)
    if (day != null) {
        if (nextWeekDay != null) {
            val mondayNextWeek = today
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .plusWeeks(1)
            return mondayNextWeek.plusDays((day.value - DayOfWeek.MONDAY.value).toLong())
        }
        return today.with(TemporalAdjusters.next(day))
    }

    // Pahami kata waktu singkat seperti "besok" tanpa tanggal lengkap.
    return when {
        Regex("\\bbesok\\b").containsMatchIn(text) -> today.plusDays(1)
        Regex("\\blusa\\b").containsMatchIn(text) -> today.plusDays(2)
        else -> null
    }
}

private fun indonesianDayOfWeek(value: String): DayOfWeek? = when (value) {
    "senin" -> DayOfWeek.MONDAY
    "selasa" -> DayOfWeek.TUESDAY
    "rabu" -> DayOfWeek.WEDNESDAY
    "kamis" -> DayOfWeek.THURSDAY
    "jumat" -> DayOfWeek.FRIDAY
    "sabtu" -> DayOfWeek.SATURDAY
    "minggu" -> DayOfWeek.SUNDAY
    else -> null
}

private fun parseTicketQuantity(
    fullContext: String,
    latestAnswer: String,
    quantityWasRequested: Boolean
): Int? {
    // Baca jumlah dari ucapan seperti "5 tiket" atau "orangnya 5".
    val digitQuantity = Regex(
        "\\b(\\d{1,2})\\s*(?:tiket|orang|pengunjung)\\b|\\b(?:tiket|orang|pengunjung)(?:\\s*nya)?\\s*(?:jumlah|sebanyak)?\\s*(\\d{1,2})\\b"
    ).find(fullContext)
    val digitCount = digitQuantity
        ?.groupValues
        ?.drop(1)
        ?.firstOrNull(String::isNotBlank)
        ?.toIntOrNull()
    if (digitCount != null) return digitCount.takeIf {
        it in 1..20
    }

    // Ubah kata angka seperti "dua" menjadi jumlah tiket.
    val numberWords = mapOf(
        "satu" to 1,
        "dua" to 2,
        "tiga" to 3,
        "empat" to 4,
        "lima" to 5,
        "enam" to 6,
        "tujuh" to 7,
        "delapan" to 8,
        "sembilan" to 9,
        "sepuluh" to 10,
        "sebelas" to 11,
        "dua belas" to 12,
        "tiga belas" to 13,
        "empat belas" to 14,
        "lima belas" to 15,
        "enam belas" to 16,
        "tujuh belas" to 17,
        "delapan belas" to 18,
        "sembilan belas" to 19,
        "dua puluh" to 20
    )
    val wordPattern = numberWords
        .keys
        .sortedByDescending(String::length)
        .joinToString("|") {
            Regex.escape(it)
        }
    val wordCount = Regex("\\b($wordPattern)\\s+(?:tiket|orang|pengunjung)\\b")
        .find(fullContext)
        ?.groupValues
        ?.get(1)
        ?.let(numberWords::get)
    if (wordCount != null) return wordCount

    // Angka tunggal dihitung sebagai jumlah hanya setelah bot menanyakannya.
    if (quantityWasRequested) {
        val shortAnswer = latestAnswer.trim().lowercase(Locale.ROOT)
        shortAnswer.toIntOrNull()?.let {
            return it.takeIf { count ->
                count in 1..20
            }
        }
        numberWords[shortAnswer]?.let {
            return it
        }

        // Kalau jawaban berisi tanggal dan jumlah, abaikan angka di dalam tanggal.
        val withoutDates = shortAnswer
            .replace(
                Regex("\\b\\d{1,2}\\s+(?:januari|februari|maret|april|mei|juni|juli|agustus|september|oktober|november|desember)(?:\\s+20\\d{2})?\\b"),
                " "
            )
            .replace(
                Regex("\\b\\d{1,2}[/-]\\d{1,2}(?:[/-]20\\d{2})?\\b"),
                " "
            )
        val bareNumber = Regex("\\b([1-9]|1\\d|20)\\b")
            .findAll(withoutDates)
            .map {
                it.groupValues[1].toInt()
            }
            .singleOrNull()
        if (bareNumber != null) return bareNumber

        val bareWord = Regex("\\b($wordPattern)\\b")
            .findAll(withoutDates)
            .mapNotNull {
                numberWords[it.groupValues[1]]
            }
            .singleOrNull()
        if (bareWord != null) return bareWord
    }
    return null
}

private fun parsePaymentMethod(text: String): String? = when {
    Regex("\\bqris\\b").containsMatchIn(text) -> "QRIS"
    Regex("\\bbank\\b").containsMatchIn(text) -> "Bank"
    Regex("\\b(?:e[- ]?wallet|ewallet|dompet digital|gopay|ovo|dana|shopeepay|linkaja)\\b")
        .containsMatchIn(text) -> "E-Wallet"
    else -> null
}

// Pilih tiket aktif terdekat dari daftar akun yang sudah dimuat di aplikasi.
private fun buildNearestTicketReply(
    question: String,
    history: List<ChatHistoryItem>,
    tickets: List<Ticket>
): String? {
    if (tickets.isEmpty()) return null

    val userConversation = (
        history.filter {
            it.role == "user"
        }.map {
            it.text
        } + question
        ).joinToString(" ")
        .lowercase(Locale.ROOT)
    val asksForNearestVisit = Regex("\\b(terdekat|berikutnya|selanjutnya|mendatang)\\b")
        .containsMatchIn(userConversation)
    val mentionsUserTicket = Regex("\\b(tiket|kunjungan|pesanan)\\b")
        .containsMatchIn(userConversation)
    if (!asksForNearestVisit || !mentionsUserTicket) return null

    // Samakan batas hari dengan server yang memakai zona waktu Jakarta.
    val today = LocalDate.now(ZoneId.of("Asia/Jakarta"))
    val nextTicket = tickets
        .asSequence()
        .filter {
            it.status.equals("Active", ignoreCase = true) &&
                it.checkedInQuantity < it.quantity
        }
        .mapNotNull { ticket ->
            runCatching {
                LocalDate.parse(ticket.visitDate)
            }.getOrNull()?.let { visitDate ->
                ticket to visitDate
            }
        }
        .filter { (_, visitDate) ->
            !visitDate.isBefore(today)
        }
        .minByOrNull { (_, visitDate) ->
            visitDate
        }

    if (nextTicket == null) {
        return "Belum ada tiket aktif untuk kunjungan hari ini atau setelahnya. Cek riwayat lengkapnya di menu Tiket Saya."
    }

    val (ticket, visitDate) = nextTicket
    val formattedDate = visitDate.format(
        DateTimeFormatter.ofPattern(
            "dd MMM yyyy",
            Locale.forLanguageTag("id-ID")
        )
    )
    val formattedPrice = NumberFormat.getIntegerInstance(
        Locale.forLanguageTag("id-ID")
    ).format(ticket.totalPrice)
    val payment = ticket.paymentMethod.takeIf(String::isNotBlank)?.let {
        " Pembayaran via $it."
    }.orEmpty()
    return "Kunjungan terdekatmu adalah ${ticket.wahanaName} pada $formattedDate: ${ticket.quantity} tiket, total Rp $formattedPrice.$payment"
}

// Saran awal langsung mengirim pertanyaan yang dipilih.
@Composable
private fun ChatEmptyState(
    onSuggestionClick: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Pakai foto wahana Tourify sebagai pembuka sebelum percakapan dimulai.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(TourifyShapes.card),
            contentAlignment = Alignment.BottomStart
        ) {
            Image(
                painter = painterResource(id = R.drawable.pantai_mentari),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.08f),
                            0.42f to Color.Black.copy(alpha = 0.28f),
                            1f to Color.Black.copy(alpha = 0.82f)
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 172.dp)
                    .padding(20.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text = "Mau tahu apa tentang Tourify?",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        lineBreak = LineBreak.Heading
                    ),
                    modifier = Modifier.semantics {
                        heading()
                    }
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Tanya soal wahana, tiket, dan pemesanan.",
            color = ChatMuted,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(24.dp))
        // Saran mengirim pertanyaan langsung, dengan ikon yang membedakan topiknya.
        Text(
            text = "Saran pertanyaan",
            color = ChatInk,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        val suggestions = listOf(
            Icons.Outlined.Explore to "Ada wahana apa saja?",
            Icons.Outlined.ConfirmationNumber to "Bagaimana cara pesan tiket?",
            Icons.Outlined.Sell to "Berapa harga tiket wahana?"
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = TourifyShapes.input,
            color = TourifySurface,
            border = BorderStroke(
                1.dp,
                TourifyBorder
            )
        ) {
            Column {
                suggestions.forEachIndexed { index, (icon, question) ->
                    if (index > 0) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = TourifyBorder
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .clickable(
                                role = Role.Button,
                                onClickLabel = "Kirim pertanyaan",
                                onClick = {
                                    onSuggestionClick(question)
                                }
                            )
                            .padding(
                                horizontal = 16.dp,
                                vertical = 12.dp
                            ),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = TourifySuccess,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = question,
                            color = ChatInk,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            tint = ChatMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

// Bedakan pesan pengguna dan jawaban asisten.
@Composable
private fun ChatBubble(
    message: ChatMessage,
    isBooking: Boolean,
    isSending: Boolean,
    canConfirmBooking: Boolean,
    showBookingAction: Boolean,
    onRetry: (() -> Unit)?,
    onOpenTickets: () -> Unit,
    onStartBooking: () -> Unit,
    onConfirmBooking: (ChatBookingProposal) -> Unit,
    onEditProposal: () -> Unit
) {
    // Posisi dan latar membedakan pengirim tanpa bergantung pada warna saja.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (message.fromUser) 32.dp else 0.dp,
                end = if (message.fromUser) 0.dp else 12.dp
            ),
        horizontalArrangement = if (message.fromUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = if (message.fromUser) 320.dp else 520.dp)
                .animateContentSize(animationSpec = tween(260)),
            shape = RoundedCornerShape(
                topStart = if (message.fromUser) 16.dp else 4.dp,
                topEnd = if (message.fromUser) 4.dp else 16.dp,
                bottomStart = 16.dp,
                bottomEnd = 16.dp
            ),
            color = when {
                // Hijau pekat membedakan pesan pengguna dari balasan asisten.
                message.fromUser -> TourifySuccess
                message.isFailed -> TourifyErrorContainer
                else -> TourifySurface
            },
            border = if (message.fromUser) {
                null
            } else {
                BorderStroke(
                    1.dp,
                    TourifyBorder
                )
            }
        ) {
            Column {
                // Taruh identitas asisten di atas supaya isi pesan lebih lebar.
                if (!message.fromUser) {
                    Row(
                        modifier = Modifier.padding(
                            start = 16.dp,
                            top = 12.dp,
                            end = 16.dp
                        ),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ChatBrandMark(
                            modifier = Modifier.size(16.dp),
                            color = if (message.isFailed) TourifyError else TourifySuccess
                        )
                        Text(
                            text = "Asisten",
                            color = if (message.isFailed) TourifyError else TourifySuccess,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                // Indikator memuat berubah jadi jawaban tanpa menggeser tampilan.
                AnimatedContent(
                    targetState = message.isLoading,
                    transitionSpec = {
                        fadeIn(tween(220)) togetherWith fadeOut(tween(100))
                    },
                    label = "Status balasan"
                ) { loading ->
                    if (loading) {
                        Row(
                            modifier = Modifier
                                .padding(
                                    horizontal = 16.dp,
                                    vertical = 14.dp
                                )
                                .semantics {
                                    liveRegion = LiveRegionMode.Polite
                                },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = ChatGreen,
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = "Sedang menyiapkan jawaban...",
                                color = ChatMuted,
                                fontSize = 15.sp
                            )
                        }
                    } else if (message.isFailed) {
                        // Letakkan pemulihan di balasan yang gagal supaya mudah ditemukan.
                        Column(
                            modifier = Modifier
                                .padding(
                                    start = 16.dp,
                                    top = 6.dp,
                                    end = 16.dp,
                                    bottom = 12.dp
                                )
                                .semantics {
                                    liveRegion = LiveRegionMode.Polite
                                },
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = message.text,
                                color = TourifyOnErrorContainer,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            if (onRetry != null) {
                                TextButton(
                                    onClick = onRetry,
                                    enabled = !isSending,
                                    // Hijau lebih gelap menjaga kontras di atas latar pesan gagal.
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = TourifySuccess
                                    )
                                ) {
                                    Text(text = "Coba lagi")
                                }
                            }
                        }
                    } else {
                        // Pilih dan salin teks tanpa membuka aksi tambahan.
                        SelectionContainer {
                            if (message.fromUser) {
                                Text(
                                    text = message.text,
                                    // Teks putih tetap terbaca di atas hijau pekat.
                                    color = TourifySurface,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(
                                        horizontal = 16.dp,
                                        vertical = 12.dp
                                    )
                                )
                            } else {
                                // Dekatkan isi jawaban dengan label Asisten supaya terbaca satu kelompok.
                                ChatFormattedAnswer(
                                    text = message.text,
                                    modifier = Modifier.padding(
                                        start = 16.dp,
                                        top = 6.dp,
                                        end = 16.dp,
                                        bottom = 14.dp
                                    )
                                )
                            }
                        }
                    }
                }
                // Tampilkan bantuan pesan tiket hanya di jawaban terakhir tentang pembayaran.
                if (showBookingAction) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = 12.dp,
                                end = 12.dp,
                                bottom = 12.dp
                            ),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Button(
                            onClick = onStartBooking,
                            enabled = !isSending && !isBooking,
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("chat_start_booking"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ChatGreen,
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                text = "Pesan tiket"
                            )
                        }
                    }
                }
                val proposal = message.bookingProposal
                val proposalStatus = message.proposalStatus
                if (proposal != null && proposalStatus != null) {
                    ChatBookingProposalCard(
                        proposal = proposal,
                        status = proposalStatus,
                        isBooking = isBooking,
                        canConfirmBooking = canConfirmBooking,
                        onOpenTickets = onOpenTickets,
                        onConfirm = {
                            onConfirmBooking(proposal)
                        },
                        onEdit = onEditProposal
                    )
                }
            }
        }
    }
}

// Kenali jawaban cara bayar supaya tombol nggak muncul di FAQ lain.
private fun shouldShowBookingAction(message: ChatMessage): Boolean {
    if (
        message.fromUser ||
            message.isLoading ||
            message.isFailed ||
            message.bookingProposal != null
    ) {
        return false
    }

    val answer = message.text.lowercase(Locale.ROOT)
    return answer.contains("form pemesanan") &&
        answer.contains("bank") &&
        answer.contains("qris") &&
        answer.contains("e-wallet")
}

@Composable
private fun ChatFormattedAnswer(
    text: String,
    modifier: Modifier = Modifier
) {
    // Pakai Markdown sederhana supaya jawaban API rapi tanpa paket tambahan.
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        text.lines().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty()) {
                Spacer(Modifier.height(3.dp))
            } else {
                val heading = line.takeWhile {
                    it == '#'
                }.length.coerceIn(
                    0,
                    3
                )
                val content = if (heading > 0 && line.getOrNull(heading) == ' ') {
                    line.drop(heading + 1)
                } else {
                    line
                }
                val bullet = content.startsWith("- ") || content.startsWith("* ") || content.startsWith("• ")
                val numbered = Regex("^\\d+[.)]\\s+").find(content)
                val listMarker = when {
                    bullet -> "•"
                    numbered != null -> numbered.value.trim()
                    else -> null
                }
                val body = when {
                    bullet -> content.drop(2)
                    numbered != null -> content.drop(numbered.value.length)
                    else -> content
                }
                val fontSize = when (heading) {
                    1 -> 21.sp
                    2 -> 19.sp
                    3 -> 17.sp
                    else -> 16.sp
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    if (listMarker != null) {
                        Text(
                            text = listMarker,
                            color = ChatGreen,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = formatChatInline(body),
                        color = ChatInk,
                        fontSize = fontSize,
                        lineHeight = if (heading > 0) 27.sp else 25.sp,
                        fontWeight = if (heading > 0) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

private fun formatChatInline(text: String) = buildAnnotatedString {
    // Ubah teks tebal, miring, dan kode sebaris sebelum ditampilkan.
    val tokens = Regex("(\\*\\*.+?\\*\\*|__.+?__|\\*.+?\\*|_.+?_|`[^`]+`)")
    var cursor = 0
    tokens.findAll(text).forEach { match ->
        append(
            text.substring(
                cursor,
                match.range.first
            )
        )
        val token = match.value
        val style = when {
            token.startsWith("**") || token.startsWith("__") -> SpanStyle(fontWeight = FontWeight.Bold)
            token.startsWith("`") -> SpanStyle(
                fontFamily = FontFamily.Monospace,
                background = TourifySoftSurface
            )
            else -> SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
        }
        val markerSize = if (token.startsWith("**") || token.startsWith("__")) 2 else 1
        pushStyle(style)
        append(
            token.substring(
                markerSize,
                token.length - markerSize
            )
        )
        pop()
        cursor = match.range.last + 1
    }
    append(text.substring(cursor))
}

// Tampilkan isi draf sebelum pengguna mengonfirmasi pesanan.
@Composable
private fun ChatBookingProposalCard(
    proposal: ChatBookingProposal,
    status: BookingProposalStatus,
    isBooking: Boolean,
    canConfirmBooking: Boolean,
    onOpenTickets: () -> Unit,
    onConfirm: () -> Unit,
    onEdit: () -> Unit
) {
    val currency = remember(
        proposal.totalPrice,
        proposal.unitPrice
    ) {
        NumberFormat.getIntegerInstance(Locale.forLanguageTag("id-ID"))
    }
    val visitDate = remember(proposal.visitDate) {
        runCatching {
            LocalDate.parse(proposal.visitDate).format(
                DateTimeFormatter.ofPattern(
                    "EEEE, d MMMM yyyy",
                    Locale.forLanguageTag("id-ID")
                )
            )
        }.getOrDefault(proposal.visitDate)
    }

    // Ringkasan draf punya latar terpisah agar mudah ditinjau sebelum dipesan.
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 10.dp,
                end = 10.dp,
                bottom = 12.dp
            )
            .animateContentSize(animationSpec = tween(240)),
        shape = TourifyShapes.input,
        color = ChatCanvas
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Ringkasan pesanan",
                color = ChatInk,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            BookingDetailRow(
                "Wahana",
                proposal.wahanaName
            )
            BookingDetailRow(
                "Tanggal",
                visitDate
            )
            BookingDetailRow(
                "Jumlah",
                "${proposal.quantity} tiket"
            )
            BookingDetailRow(
                "Harga/tiket",
                "Rp\u00A0${currency.format(proposal.unitPrice)}"
            )
            BookingDetailRow(
                "Metode",
                proposal.paymentMethod
            )
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = TourifySuccessContainer
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 12.dp,
                            vertical = 10.dp
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Total",
                        color = ChatMuted,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Rp ${currency.format(proposal.totalPrice)}",
                        color = TourifySuccess,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(2f)
                    )
                }
            }
            Text(
                text = "Pilihan metode dicatat pada tiket; pembayaran tidak diproses otomatis.",
                color = ChatMuted,
                fontSize = 14.sp,
                lineHeight = 21.sp
            )

            when (status) {
                BookingProposalStatus.PENDING -> {
                    // Tombol utama sudah menjelaskan bahwa konfirmasi membuat pesanan.
                    Button(
                        onClick = onConfirm,
                        enabled = !isBooking && canConfirmBooking,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("chat_confirm_booking"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ChatGreen,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            "Konfirmasi & pesan tiket"
                        )
                    }
                    OutlinedButton(
                        onClick = onEdit,
                        enabled = !isBooking,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                    ) {
                        Text(
                            "Ubah pesanan"
                        )
                    }
                    if (!canConfirmBooking) {
                        Text(
                            "Masuk ke akun Tourify untuk memesan tiket.",
                            color = ChatMuted,
                            fontSize = 14.sp
                        )
                    }
                }

                BookingProposalStatus.PROCESSING -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics {
                                liveRegion = LiveRegionMode.Polite
                            },
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = ChatGreen,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.size(10.dp))
                        Text(
                            "Sedang memesan tiket...",
                            color = ChatMuted,
                            fontSize = 15.sp
                        )
                    }
                }

                BookingProposalStatus.CANCELLED -> {
                    Text(
                        "Draf ini tidak dipesan.",
                        color = ChatMuted,
                        fontSize = 14.sp
                    )
                }

                BookingProposalStatus.CONFIRMED, BookingProposalStatus.FAILED -> {
                    // Buka tiket yang dibuat, atau periksa dulu jika status belum pasti.
                    OutlinedButton(
                        onClick = onOpenTickets,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                    ) {
                        Text("Buka Tiket")
                    }
                }
            }
        }
    }
}

// Tampilkan detail berpasangan supaya mudah dicek sebelum konfirmasi.
@Composable
private fun BookingDetailRow(
    label: String,
    value: String
) {
    // Turunkan nilai ke baris baru jika label dan nilai tidak muat berdampingan.
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            color = ChatMuted,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = value,
            color = ChatInk,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

// Kolom chat dipakai untuk mengetik dan mengirim pertanyaan.
@Composable
private fun ChatComposer(
    draft: String,
    isSending: Boolean,
    focusRequester: FocusRequester,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit
) {
    var hasFocus by remember {
        mutableStateOf(false)
    }
    val borderColor by animateColorAsState(
        targetValue = if (hasFocus) TourifySuccess else TourifyBorder,
        animationSpec = tween(durationMillis = TourifyMotion.standardMillis),
        label = "Fokus kolom chat"
    )

    // Kolom dan tombol kirim berada di area tetap, termasuk saat keyboard terbuka.
    Surface(
        modifier = Modifier
            .widthIn(max = 600.dp)
            .fillMaxWidth()
            .padding(
                horizontal = TourifySpacing.screen,
                vertical = 12.dp
            ),
        shape = TourifyShapes.input,
        color = ChatCanvas,
        border = BorderStroke(
            1.dp,
            borderColor
        )
    ) {
        Row(
            modifier = Modifier.padding(
                start = 16.dp,
                end = 6.dp,
                top = 6.dp,
                bottom = 6.dp
            ),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BasicTextField(
                value = draft,
                onValueChange = onDraftChange,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(
                        min = 48.dp,
                        max = 116.dp
                    )
                    .focusRequester(focusRequester)
                    .onFocusChanged {
                        hasFocus = it.isFocused
                    }
                    .testTag("chat_message_input")
                    .semantics {
                        contentDescription = "Tulis pesan tentang wahana, tiket, atau pemesanan"
                    },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        onSend()
                    }
                ),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = ChatInk),
                cursorBrush = SolidColor(TourifySuccess),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier.padding(vertical = 12.dp)
                    ) {
                        if (draft.isEmpty()) {
                            Text(
                                text = "Tulis pesan…",
                                color = ChatMuted,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        innerTextField()
                    }
                }
            )
            val canSend = draft.isNotBlank() && !isSending
            val sendColor by animateColorAsState(
                targetValue = if (canSend) TourifySuccess else TourifySecondaryContainer,
                animationSpec = tween(durationMillis = TourifyMotion.standardMillis),
                label = "Kesiapan kirim"
            )
            Surface(
                modifier = Modifier
                    .size(48.dp)
                    .clickable(
                        enabled = canSend,
                        role = Role.Button,
                        onClickLabel = "Kirim pesan",
                        onClick = onSend
                    )
                    .testTag("chat_send_button")
                    .semantics {
                        contentDescription = "Kirim pesan"
                    },
                shape = CircleShape,
                color = sendColor
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        tint = if (canSend) {
                            Color.White
                        } else {
                            ChatMuted.copy(alpha = 0.64f)
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// TalkBack membaca label tombol ini.
@Composable
private fun ChatHeaderButton(
    iconDescription: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.size(48.dp),
        shape = CircleShape,
        color = TourifySoftSurface
    ) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.semantics {
                contentDescription = iconDescription
            }
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                content()
            }
        }
    }
}

// Ikon bintang ini menyamakan tanda asisten dengan ikon di navbar.
@Composable
private fun ChatBrandMark(
    modifier: Modifier = Modifier,
    color: Color = Color.White
) {
    Icon(
        imageVector = Icons.Default.AutoAwesome,
        contentDescription = null,
        tint = color,
        modifier = modifier
    )
}

// Riwayat dan draf tetap ada saat panel ditutup sampai pengguna mulai chat baru.
internal class ChatbotConversationState {
    val messages = mutableStateListOf<ChatMessage>()
    val draft = mutableStateOf("")
}

internal data class ChatMessage(
    val fromUser: Boolean,
    val text: String,
    val isLoading: Boolean = false,
    val isFailed: Boolean = false,
    val bookingProposal: ChatBookingProposal? = null,
    val proposalStatus: BookingProposalStatus? = null
)

internal enum class BookingProposalStatus {
    PENDING,
    PROCESSING,
    CONFIRMED,
    CANCELLED,
    FAILED
}

// Sertakan detail draf di riwayat supaya AI paham saat pengguna minta perubahan.
private fun ChatMessage.asHistoryText(): String = buildString {
    append(text)
    bookingProposal?.let { proposal ->
        append("\nRingkasan draf: ${proposal.wahanaName}, tanggal ${proposal.visitDate}, ")
        append("${proposal.quantity} tiket, ${proposal.paymentMethod}, total Rp ${proposal.totalPrice}.")
        append(" Status draf: ${proposalStatus?.name ?: "tidak diketahui"}.")
    }
}
