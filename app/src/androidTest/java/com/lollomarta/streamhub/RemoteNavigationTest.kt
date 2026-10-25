package com.lollomarta.streamhub

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lollomarta.streamhub.ui.components.TvButton
import com.lollomarta.streamhub.ui.components.TvFocusBox
import com.lollomarta.streamhub.ui.theme.StreamHubTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Test strumentali: si eseguono su emulatore Android TV o dispositivo (vedi README). */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class RemoteNavigationTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test fun dpadMovesFocusBetweenCardsAndOkClicks() {
        var clicked by mutableIntStateOf(0)
        rule.setContent {
            StreamHubTheme {
                Row {
                    TvFocusBox(onClick = {}, modifier = Modifier.testTag("a")) { Text("A") }
                    TvFocusBox(onClick = { clicked++ }, modifier = Modifier.testTag("b")) { Text("B") }
                }
            }
        }
        rule.onNodeWithTag("a").requestFocus()
        rule.onNodeWithTag("a").assertIsFocused()
        rule.onRoot().performKeyInput { pressKey(Key.DirectionRight) }
        rule.onNodeWithTag("b").assertIsFocused()
        rule.onRoot().performKeyInput { pressKey(Key.DirectionCenter) }
        rule.runOnIdle { assertEquals(1, clicked) }
    }

    @Test fun dpadDownReachesSecondRow() {
        rule.setContent {
            StreamHubTheme {
                Column {
                    TvButton("Primo", {}, Modifier.testTag("top"))
                    TvButton("Secondo", {}, Modifier.testTag("bottom"))
                }
            }
        }
        rule.onNodeWithTag("top").requestFocus()
        rule.onRoot().performKeyInput { pressKey(Key.DirectionDown) }
        rule.onNodeWithTag("bottom").assertIsFocused()
    }

    @Test fun backReturnsToPreviousScreen() {
        rule.setContent {
            var details by remember { mutableStateOf(true) }
            StreamHubTheme {
                if (details) {
                    BackHandler { details = false }
                    Text("Scheda")
                } else Text("Home")
            }
        }
        rule.onNodeWithText("Scheda").assertExists()
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.onNodeWithText("Home").assertExists()
    }
}
