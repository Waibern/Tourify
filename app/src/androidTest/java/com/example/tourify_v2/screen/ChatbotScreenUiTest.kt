package com.example.tourify_v2.screen

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.tourify_v2.model.ChatResponse
import com.example.tourify_v2.model.ChatBookingProposal
import com.example.tourify_v2.ui.theme.Tourify_v2Theme
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatbotScreenUiTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun suggestionSendsAndShowsAnswer() {
        var sentQuestion = ""

        // Tampilkan layar chat asli tanpa perlu akun atau server.
        compose.setContent {
            Tourify_v2Theme {
                ChatbotAssistantScreen(
                    currentUser = "test-user",
                    isSending = false,
                    isBooking = false,
                    onSendMessage = { question, _, onComplete ->
                        sentQuestion = question
                        onComplete(
                            ChatResponse(
                                success = true,
                                answer = "Tourify membantu menemukan info wahana dan tiket."
                            )
                        )
                    },
                    onConfirmBooking = { _, onComplete -> onComplete(true, "Tiket berhasil dipesan.") },
                    onClose = {}
                )
            }
        }

        compose.onNodeWithText("Mau tahu apa tentang Tourify?").assertIsDisplayed()
        capture("chatbot-redesign-empty")
        compose.onNodeWithText("Ada wahana apa saja?").performClick()
        compose.waitForIdle()

        assertEquals("Ada wahana apa saja?", sentQuestion)
        compose.onNodeWithText("Tourify membantu menemukan info wahana dan tiket.")
            .assertIsDisplayed()
        capture("chatbot-redesign-answer")

        // Kolom pesan dan tombol kirim tetap bisa dipakai saat pengguna mulai mengetik.
        compose.onNodeWithTag("chat_message_input").performClick()
        compose.onNodeWithTag("chat_message_input").performTextInput("Harga tiket Pantai Mentari?")
        compose.onNodeWithTag("chat_send_button").assertIsEnabled().assertIsDisplayed()
        capture("chatbot-redesign-keyboard")
        compose.onNodeWithTag("chat_send_button").performClick()
        assertEquals("Harga tiket Pantai Mentari?", sentQuestion)
    }

    // Ringkasan panjang harus tetap bisa dibaca dan dikonfirmasi di ponsel sempit.
    @Test
    fun largeTextKeepsBookingActionsReachable() {
        val proposal = ChatBookingProposal(
            wahanaId = 1,
            wahanaName = "Pantai Mentari",
            visitDate = "2099-11-18",
            quantity = 3,
            paymentMethod = "Bank",
            unitPrice = 40000,
            totalPrice = 120000
        )
        val conversation = ChatbotConversationState().apply {
            messages.add(
                ChatMessage(
                    fromUser = false,
                    text = "Periksa rincian pesananmu.",
                    bookingProposal = proposal,
                    proposalStatus = BookingProposalStatus.PENDING
                )
            )
        }
        var confirmed = 0
        var openedTickets = false
        compose.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(
                    density = LocalDensity.current.density,
                    fontScale = 1.5f
                )
            ) {
                Tourify_v2Theme {
                    Box(modifier = Modifier.width(320.dp)) {
                        ChatbotAssistantScreen(
                            currentUser = "test-user",
                            isSending = false,
                            isBooking = false,
                            conversationState = conversation,
                            onSendMessage = { _, _, _ -> },
                            onConfirmBooking = { suppliedProposal, onComplete ->
                                assertEquals(proposal, suppliedProposal)
                                confirmed++
                                onComplete(
                                    false,
                                    "koneksi terputus"
                                )
                            },
                            onClose = {},
                            onOpenTickets = {
                                openedTickets = true
                            }
                        )
                    }
                }
            }
        }

        compose.onNodeWithText("Ringkasan pesanan").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Rp 120.000").performScrollTo().assertIsDisplayed()
        val confirmButton = compose.onNodeWithText("Konfirmasi & pesan tiket")
        confirmButton.performScrollTo().assertIsDisplayed()
        assertEquals(0, confirmed)
        capture("chatbot-redesign-large-booking")
        confirmButton.performClick()
        compose.onNodeWithText("Buka Tiket").performScrollTo().performClick()
        assertEquals(1, confirmed)
        assertTrue(openedTickets)
    }

    // Pulihkan balasan di tempat tanpa menggandakan pertanyaan atau menghapus draf berikutnya.
    @Test
    fun failedAnswerCanBeRetriedInPlace() {
        val question = "Ada wahana apa saja?"
        val nextDraft = "Berapa jam buka Pantai Mentari?"
        val sent = mutableListOf<String>()
        var respond: ((ChatResponse) -> Unit)? = null
        compose.setContent {
            Tourify_v2Theme {
                ChatbotAssistantScreen(
                    currentUser = "test-user",
                    isSending = false,
                    isBooking = false,
                    onSendMessage = { text, history, onComplete ->
                        sent.add(text)
                        assertTrue(history.isEmpty())
                        respond = onComplete
                    },
                    onConfirmBooking = { _, _ -> },
                    onClose = {}
                )
            }
        }

        compose.onNodeWithText(question).performClick()
        compose.onNodeWithText("Sedang menyiapkan jawaban...").assertIsDisplayed()
        val composer = compose.onNodeWithTag("chat_message_input")
        composer.performTextInput(nextDraft)
        compose.onNodeWithTag("chat_send_button").assertIsNotEnabled()
        compose.runOnIdle {
            requireNotNull(respond).invoke(ChatResponse(message = "Koneksi ke asisten terputus."))
        }
        compose.onNodeWithText("Coba lagi").performScrollTo().assertIsDisplayed()
        capture(
            name = "chatbot-critique-retry",
            includeSystemUi = true
        )
        compose.onNodeWithText("Coba lagi").performClick()
        compose.runOnIdle {
            requireNotNull(respond).invoke(
                ChatResponse(
                    success = true,
                    answer = "Ada Pantai Mentari dan Pulau Air."
                )
            )
        }
        assertEquals(listOf(question, question), sent)
        compose.onAllNodesWithText(question).assertCountEquals(1)
        compose.onNodeWithText("Ada Pantai Mentari dan Pulau Air.").assertIsDisplayed()
        composer.assertTextContains(nextDraft)
    }

    // Pengunjung dapat kembali ke pesan terbaru, membuka bantuan, atau batal menghapus chat.
    @Test
    fun helpAndResetKeepConversationUntilConfirmed() {
        val conversation = ChatbotConversationState()
        repeat(20) { index ->
            conversation.messages.add(
                ChatMessage(
                    fromUser = index % 2 == 0,
                    text = "Pesan nomor ${index + 1}"
                )
            )
        }
        conversation.draft.value = "Draf pertanyaan"
        compose.setContent {
            Tourify_v2Theme {
                ChatbotAssistantScreen(
                    currentUser = "test-user",
                    isSending = false,
                    isBooking = false,
                    onSendMessage = { _, _, _ -> },
                    onConfirmBooking = { _, _ -> },
                    onClose = {},
                    conversationState = conversation
                )
            }
        }

        compose.onNodeWithTag("chat_message_list").performScrollToIndex(0)
        compose.onNodeWithContentDescription("Ke pesan terbaru").performClick()
        compose.onNodeWithText("Pesan nomor 20").assertIsDisplayed()
        compose.onNodeWithContentDescription("Bantuan chat").performClick()
        compose.onNodeWithText("Bantuan Asisten").assertIsDisplayed()
        capture(
            name = "chatbot-critique-help",
            includeSystemUi = true
        )
        compose.onNodeWithText("Tutup bantuan").performClick()
        compose.onNodeWithContentDescription("Chat baru").performClick()
        compose.onNodeWithText("Tetap di chat").performClick()
        compose.onNodeWithText("Pesan nomor 20").assertIsDisplayed()
        compose.onNodeWithTag("chat_message_input").assertTextContains("Draf pertanyaan")
        compose.onNodeWithContentDescription("Chat baru").performClick()
        compose.onNodeWithText("Mulai chat baru").performClick()
        compose.onNodeWithText("Mau tahu apa tentang Tourify?").assertIsDisplayed()
        assertTrue(conversation.messages.isEmpty())
        assertEquals("", conversation.draft.value)
    }

    // Ambil gambar dari root yang sudah dirender supaya foto pembuka ikut tertangkap.
    private fun capture(
        name: String,
        includeSystemUi: Boolean = false
    ) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val file = File(instrumentation.targetContext.getExternalFilesDir(null), "$name.png")
        // Dialog dan keyboard ada di jendela terpisah dari root percakapan.
        val screenshot = if (includeSystemUi) {
            instrumentation.uiAutomation.waitForIdle(
                400L,
                3_000L
            )
            instrumentation.uiAutomation.takeScreenshot()
        } else {
            compose.onRoot().captureToImage().asAndroidBitmap()
        }
        file.outputStream().use { output ->
            screenshot.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
    }
}
