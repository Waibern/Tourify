package com.example.tourify_v2.screen

import com.example.tourify_v2.ui.theme.*

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

private val ChatCanvas = TourifyCanvas
private val ChatInk = TourifyInk
private val ChatMuted = TourifyMuted
private val ChatBubble = TourifySoftSurface
// Tombol Asisten pakai hijau utama supaya warnanya konsisten.
private val ChatGreen = TourifyForest

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
        onConfirmBooking(proposal) { success, detail ->
            if (index in messages.indices) {
                val latestMessage = messages[index]
                messages[index] = latestMessage.copy(
                    text = if (success) {
                        "Pesanan berhasil dibuat. Buka menu Tiket untuk melihat tiket dan QR-nya."
                    } else {
                        "Hasil pemesanan belum bisa dipastikan: $detail. Cek menu Tiket sebelum mencoba lagi agar tidak membuat pesanan ganda."
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
            text = "Oke, belum ada tiket yang dibuat. Kirim detail yang ingin diubah, misalnya wahana, tanggal, jumlah, atau metode pembayaran.",
            proposalStatus = BookingProposalStatus.CANCELLED
        )
    }

    // Saran dan pertanyaan yang diketik memakai alur yang sama.
    fun sendQuestion(rawQuestion: String) {
        val question = rawQuestion.trim()
        if (question.isEmpty() || isSending || isBooking) return

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

        val history = messages
            .filterNot {
                it.isLoading
            }
            .map { message ->
                ChatHistoryItem(
                    role = if (message.fromUser) "user" else "assistant",
                    text = message.asHistoryText()
                )
            }
            .takeLast(10)

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
                messages[answerIndex] = messages[answerIndex].copy(
                    text = answer,
                    isLoading = false,
                    bookingProposal = proposal,
                    proposalStatus = if (proposal == null) null else BookingProposalStatus.PENDING
                )
            }
        }
    }

    // Gulir ke jawaban baru setelah indikator memuat selesai.
    LaunchedEffect(
        messages.size,
        messages.lastOrNull()?.isLoading
    ) {
        if (messages.isNotEmpty()) {
            messageListState.animateScrollToItem(messages.lastIndex)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ChatCanvas)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Tombol chat baru muncul setelah pengguna mengirim pesan.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 68.dp)
                .padding(
                    horizontal = 14.dp,
                    vertical = 10.dp
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
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
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            if (messages.isNotEmpty()) {
                ChatHeaderButton(
                    iconDescription = "Chat baru",
                    enabled = !isSending && !isBooking,
                    onClick = {
                        messages.clear()
                        draft = ""
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = null
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(48.dp))
            }
        }

        Surface(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            shape = RoundedCornerShape(
                topStart = 28.dp,
                topEnd = 28.dp
            ),
            color = TourifySurface
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("chat_message_list"),
                state = messageListState,
                contentPadding = PaddingValues(
                    horizontal = 18.dp,
                    vertical = 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                if (messages.isEmpty()) {
                    item {
                        ChatEmptyState(
                            modifier = Modifier.fillParentMaxHeight(),
                            onSuggestionClick = ::sendQuestion
                        )
                    }
                }
                itemsIndexed(messages) { index, message ->
                    ChatBubble(
                        message = message,
                        isBooking = isBooking,
                        isSending = isSending,
                        canConfirmBooking = currentUser.isNotBlank(),
                        showBookingAction = index == messages.lastIndex &&
                            shouldShowBookingAction(message),
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
        }

        ChatComposer(
            draft = draft,
            isSending = isSending || isBooking,
            onDraftChange = {
                draft = it.take(1000)
            },
            onSend = {
                sendQuestion(draft)
            }
        )
    }
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
    modifier: Modifier = Modifier,
    onSuggestionClick: (String) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        horizontalAlignment = Alignment.Start,
        // Seimbangkan ruang kosong di atas dan bawah welcome screen.
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(48.dp),
            shape = RoundedCornerShape(16.dp),
            color = ChatGreen
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                ChatBrandMark(
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Mau tahu apa tentang Tourify?",
            color = ChatInk,
            fontSize = 24.sp,
            lineHeight = 30.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Tanya soal wahana, tiket, atau cara memesan.",
            color = ChatMuted,
            fontSize = 16.sp,
            lineHeight = 24.sp
        )
        Spacer(modifier = Modifier.height(24.dp))
        listOf(
            "Ada wahana apa saja?",
            "Bagaimana cara pesan tiket?",
            "Berapa harga tiket wahana?"
        ).forEach { suggestion ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .heightIn(min = 54.dp)
                    .clickable(
                        role = Role.Button,
                        onClickLabel = "Tanyakan: $suggestion",
                        onClick = {
                            onSuggestionClick(suggestion)
                        }
                    ),
                shape = RoundedCornerShape(16.dp),
                color = ChatCanvas
            ) {
                Row(
                    modifier = Modifier.padding(
                        horizontal = 16.dp,
                        vertical = 14.dp
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = suggestion,
                        color = ChatInk,
                        fontSize = 16.sp,
                        lineHeight = 23.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        tint = ChatGreen,
                        modifier = Modifier.size(17.dp)
                    )
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
    onOpenTickets: () -> Unit,
    onStartBooking: () -> Unit,
    onConfirmBooking: (ChatBookingProposal) -> Unit,
    onEditProposal: () -> Unit
) {
    var visible by remember {
        mutableStateOf(false)
    }
    LaunchedEffect(Unit) {
        visible = true
    }

    // Pesan baru muncul perlahan supaya alur chat gampang diikuti.
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(220)) + slideInVertically(
            animationSpec = tween(220),
            initialOffsetY = {
                it / 4
            }
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (message.fromUser) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Top
        ) {
            if (!message.fromUser) {
                Surface(
                    modifier = Modifier
                        .padding(
                            top = 5.dp,
                            end = 8.dp
                        )
                        .size(30.dp),
                    shape = CircleShape,
                    // Logo putih tetap terlihat di latar gelap.
                    color = TourifyGlassSurface,
                    border = BorderStroke(
                        1.dp,
                        TourifyGlassHighlight
                    )
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        ChatBrandMark(
                            modifier = Modifier.size(18.dp),
                            color = Color.White
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .widthIn(max = 310.dp)
                    .animateContentSize(animationSpec = tween(260)),
                shape = RoundedCornerShape(24.dp),
                color = if (message.fromUser) TourifyMint else ChatBubble
            ) {
                Column {
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
                                modifier = Modifier.padding(
                                    horizontal = 16.dp,
                                    vertical = 14.dp
                                ),
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
                        } else {
                            if (message.fromUser) {
                                Text(
                                    text = message.text,
                                    color = ChatInk,
                                    fontSize = 16.sp,
                                    lineHeight = 25.sp,
                                    modifier = Modifier.padding(
                                        horizontal = 16.dp,
                                        vertical = 14.dp
                                    )
                                )
                            } else {
                                // Tampilkan jawaban per baris supaya judul dan daftar gampang dibaca.
                                ChatFormattedAnswer(
                                    text = message.text,
                                    modifier = Modifier.padding(
                                        horizontal = 16.dp,
                                        vertical = 14.dp
                                    )
                                )
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
                                    text = "Iya"
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
}

// Kenali jawaban cara bayar supaya tombol nggak muncul di FAQ lain.
private fun shouldShowBookingAction(message: ChatMessage): Boolean {
    if (
        message.fromUser ||
            message.isLoading ||
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

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 10.dp,
                end = 10.dp,
                bottom = 12.dp
            )
            .animateContentSize(animationSpec = tween(240)),
        shape = TourifyShapes.card,
        color = TourifySurface,
        border = BorderStroke(
            1.dp,
            TourifyBorder
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Ringkasan pesanan",
                color = ChatInk,
                fontSize = 15.sp,
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
                "Rp ${currency.format(proposal.unitPrice)}"
            )
            BookingDetailRow(
                "Metode",
                proposal.paymentMethod
            )
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = TourifyMint
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
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Rp ${currency.format(proposal.totalPrice)}",
                        color = ChatGreen,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
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
                    Text(
                        text = "Apakah pesanan ini sudah benar?",
                        color = ChatInk,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
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
                        modifier = Modifier.fillMaxWidth(),
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

                BookingProposalStatus.CONFIRMED -> {
                    Text(
                        text = "Pesanan berhasil dibuat",
                        color = ChatGreen,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                BookingProposalStatus.CANCELLED -> {
                    Text(
                        "Draf ini tidak dipesan.",
                        color = ChatMuted,
                        fontSize = 14.sp
                    )
                }

                BookingProposalStatus.FAILED -> {
                    Text(
                        "Cek menu Tiket sebelum mencoba lagi.",
                        color = TourifyWarning,
                        fontSize = 14.sp
                    )
                    // Muat status pesanan dulu sebelum pengguna mencoba lagi.
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            label,
            color = ChatMuted,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            color = ChatInk,
            fontSize = 14.sp,
            textAlign = TextAlign.End
        )
    }
}

// Kolom chat dipakai untuk mengetik dan mengirim pertanyaan.
@Composable
private fun ChatComposer(
    draft: String,
    isSending: Boolean,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit
) {
    var hasFocus by remember {
        mutableStateOf(false)
    }
    val borderColor by animateColorAsState(
        targetValue = if (hasFocus) ChatGreen else TourifyOutline,
        animationSpec = tween(TourifyMotion.standardMillis),
        label = "Fokus kolom chat"
    )

    // Kolom tetap ringkas, dan tombol kirim terlihat jelas.
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 14.dp,
                vertical = 12.dp
            ),
        shape = TourifyShapes.card,
        color = TourifySurface,
        border = BorderStroke(
            1.dp,
            borderColor
        ),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(
                start = 16.dp,
                end = 6.dp,
                top = 8.dp,
                bottom = 8.dp
            ),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BasicTextField(
                value = draft,
                onValueChange = onDraftChange,
                enabled = !isSending,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(
                        min = 48.dp,
                        max = 116.dp
                    )
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
                textStyle = TextStyle(
                    color = ChatInk,
                    // Samakan ukuran input dengan teks chat supaya mudah dibaca.
                    fontSize = 16.sp,
                    lineHeight = 24.sp
                ),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier.padding(vertical = 12.dp)
                    ) {
                        if (draft.isEmpty()) {
                            Text(
                                text = "Tanya tentang Tourify...",
                                color = ChatMuted,
                                fontSize = 16.sp
                            )
                        }
                        innerTextField()
                    }
                }
            )
            val sendColor by animateColorAsState(
                targetValue = if (draft.isNotBlank() && !isSending) ChatGreen else TourifySoftSurface,
                animationSpec = tween(220),
                label = "Kesiapan kirim"
            )
            Surface(
                modifier = Modifier
                    .size(48.dp)
                    .clickable(
                        enabled = draft.isNotBlank() && !isSending,
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
                        tint = if (draft.isNotBlank() && !isSending) {
                            Color.White
                        } else {
                            ChatMuted.copy(alpha = TourifyInteraction.disabledContentAlpha)
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
