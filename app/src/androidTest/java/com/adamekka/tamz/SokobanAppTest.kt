package com.adamekka.tamz

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SokobanAppTest {
    @get:Rule
    val compose = createEmptyComposeRule()
    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.filesDir, "sokoban.json").delete()
        scenario = ActivityScenario.launch(MainActivity::class.java)
        compose.waitUntil(10_000) { compose.onAllNodesWithText("První bedna").fetchSemanticsNodes().isNotEmpty() }
    }

    @After
    fun cleanup() { scenario.close() }

    @Test
    fun solvingAdvancesToNextLevelAndRestoresProgressAfterRecreation() {
        compose.onNodeWithContentDescription("Doprava").performClick()
        compose.onNodeWithText("Chodba").assertIsDisplayed()
        compose.onNodeWithContentDescription("Doprava").performClick()
        compose.onNodeWithText("Tahy: 1     Bedny v cíli: 0/1").assertIsDisplayed()
        scenario.recreate()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Chodba").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Tahy: 1     Bedny v cíli: 0/1").assertIsDisplayed()
        compose.onNodeWithText("Úrovně").performClick()
        compose.onNodeWithText("Nejméně tahů: 1").assertIsDisplayed()
        compose.onNodeWithText("Chodba").performClick()
        compose.onNodeWithText("Začít znovu").performClick()
        compose.onNodeWithText("Tahy: 0     Bedny v cíli: 0/1").assertIsDisplayed()
    }

    @Test
    fun editorCreatesEditsAndDeletesCustomLevels() {
        compose.onNodeWithText("Editor").performClick()
        compose.onNodeWithText("Název úrovně").performTextInput("Moje úroveň")
        compose.onNodeWithText("Hráč").performScrollTo().performClick()
        compose.onNodeWithTag("editor-1-1").performScrollTo().performClick()
        compose.onNodeWithText("Bedna").performScrollTo().performClick()
        compose.onNodeWithTag("editor-2-1").performScrollTo().performClick()
        compose.onNodeWithText("Cíl").performScrollTo().performClick()
        compose.onNodeWithTag("editor-3-1").performScrollTo().performClick()
        compose.onNodeWithText("Uložit a hrát").performScrollTo().performClick()
        compose.onNodeWithText("Moje úroveň").assertIsDisplayed()
        compose.onNodeWithContentDescription("Doprava").performClick()
        compose.onNodeWithText("Úroveň splněna.").assertIsDisplayed()
        compose.onNodeWithText("Úrovně").performClick()
        compose.onNodeWithTag("level-list").performScrollToNode(hasText("Moje úroveň"))
        compose.onNodeWithText("Upravit").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithText("Název úrovně").performTextReplacement("Přejmenovaná")
        compose.onNodeWithText("Uložit a hrát").performScrollTo().performClick()
        compose.onNodeWithText("Úrovně").performClick()
        compose.onNodeWithTag("level-list").performScrollToNode(hasText("Přejmenovaná"))
        compose.onNodeWithText("Smazat").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithText("Smazat úroveň Přejmenovaná?").assertIsDisplayed()
        compose.onAllNodesWithText("Smazat").onLast().performClick()
        compose.onNodeWithText("Přejmenovaná").assertDoesNotExist()
    }

    @Test
    fun incompleteEditorBoardShowsValidationInsteadOfSaving() {
        compose.onNodeWithText("Editor").performClick()
        compose.onNodeWithText("Název úrovně").performTextInput("Prázdná")
        compose.onNodeWithText("Uložit a hrát").performScrollTo().performClick()
        compose.onNodeWithText("Umístěte právě jednoho hráče.").assertIsDisplayed()
    }

    @Test
    fun swipesMoveThePlayer() {
        compose.onNodeWithTag("game-board").performTouchInput { swipeRight() }
        compose.onNodeWithText("Chodba").assertIsDisplayed()
        compose.onNodeWithTag("game-board").performTouchInput { swipeRight() }
        compose.onNodeWithText("Tahy: 1     Bedny v cíli: 0/1").assertIsDisplayed()
    }
}
