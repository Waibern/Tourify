package com.example.tourify_v2.screen

import android.graphics.Bitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.tourify_v2.model.ChatResponse
import com.example.tourify_v2.ui.theme.Tourify_v2Theme
import java.io.File
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

        assert(sentQuestion == "Ada wahana apa saja?")
        compose.onNodeWithText("Tourify membantu menemukan info wahana dan tiket.")
            .assertIsDisplayed()
        capture("chatbot-redesign-answer")
    }

    // Simpan dua tampilan layar untuk dicek di emulator.
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
