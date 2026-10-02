package com.example

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performTextInput
import com.example.model.PreparationMode
import com.example.ui.screens.AiTutorScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AiTutorScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @OptIn(ExperimentalLayoutApi::class)
    @Test
    fun testWindowInsetsImeVisibleCompiles() {
        composeTestRule.setContent {
            val isVisible = WindowInsets.isImeVisible
            AiTutorScreen(
                mode = PreparationMode.ACADEMIC,
                onSendMessage = { _, _, _ -> "Test reply" },
                onNavigateTab = {},
                onNavigateBack = {}
            )
        }
        composeTestRule.onNodeWithTag("screen_ai_tutor").assertIsDisplayed()
        composeTestRule.onNodeWithTag("input_ai_query").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_send_ai").assertIsDisplayed()
    }

    @Test
    fun testGateAiTutorDisplays() {
        composeTestRule.setContent {
            AiTutorScreen(
                mode = PreparationMode.PROFESSIONAL_GATE,
                onSendMessage = { _, _, _ -> "GATE reply" },
                onNavigateTab = {},
                onNavigateBack = {}
            )
        }
        composeTestRule.onNodeWithTag("screen_ai_tutor").assertIsDisplayed()
        composeTestRule.onNodeWithTag("input_ai_query").assertIsDisplayed()
    }
}
