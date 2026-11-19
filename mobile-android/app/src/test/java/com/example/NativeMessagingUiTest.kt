package com.example

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.data.model.ChatConversation
import com.example.ui.screens.DirectChatRoom
import com.example.ui.theme.Okul360Theme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w640dp-h360dp-mdpi")
class NativeMessagingUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun verifiedResultControlRetainsDraftInLandscapeAtLargeFont() {
        val font = RuntimeEnvironment.getFontScale()
        RuntimeEnvironment.setFontScale(1.5f)
        var verifications = 0
        try {
            compose.setContent { Okul360Theme {
                DirectChatRoom(
                    partner = ChatConversation(id = "recorded", participant_name = "Kayıtlı okul kullanıcısı"),
                    messages = emptyList(), text = "Doğrulamayı bekleyen taslak", historyBusy = false,
                    historyError = false, writeError = "Gönderim sonucu doğrulanamadı.", canSend = true,
                    busy = false, pending = true, retry = false, olderAvailable = false,
                    onBack = {}, onRefresh = {}, onOlder = {}, onInput = {},
                    onSend = { error("A pending draft must not send again") }, onVerify = { verifications++ },
                    onRetry = { error("Retry is not enabled") },
                )
            } }
            compose.onNodeWithText("Doğrulamayı bekleyen taslak").assertExists()
            compose.onNodeWithTag("send_message_button").assertIsNotEnabled()
            compose.onNodeWithText("Sonucu kontrol et").assertIsDisplayed().performClick()
            compose.onNodeWithContentDescription("Dosya Ekle").assertDoesNotExist()
            compose.runOnIdle { assertEquals(1, verifications) }
        } finally {
            RuntimeEnvironment.setFontScale(font)
        }
    }
}
