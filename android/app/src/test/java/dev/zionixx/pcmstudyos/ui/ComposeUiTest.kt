package dev.zionixx.pcmstudyos.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.zionixx.pcmstudyos.data.local.entity.StudyTask
import dev.zionixx.pcmstudyos.ui.components.MarkdownText
import dev.zionixx.pcmstudyos.ui.components.Pill
import dev.zionixx.pcmstudyos.ui.components.StatusPill
import dev.zionixx.pcmstudyos.ui.screens.TaskRow
import dev.zionixx.pcmstudyos.ui.theme.PcmTheme
import dev.zionixx.pcmstudyos.ui.theme.Success
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Component-level Compose UI tests. They run on the JVM through Robolectric, so
 * they are fast enough to run on every push — no emulator required.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ComposeUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun task(status: String = "pending", title: String = "Read NCERT chapter 3") = StudyTask(
        id = "t1",
        yearId = "y1",
        title = title,
        status = status,
        dueDate = "2026-06-01",
        estimatedMinutes = 45,
        priority = "high",
    )

    @Test
    fun `task row shows its title and metadata`() {
        composeRule.setContent { PcmTheme { TaskRow(task = task(), onToggle = {}) } }
        composeRule.onNodeWithText("Read NCERT chapter 3").assertIsDisplayed()
        composeRule.onNodeWithText("45m · high", substring = true).assertIsDisplayed()
    }

    @Test
    fun `tapping a task row fires the toggle callback`() {
        var toggled = 0
        composeRule.setContent { PcmTheme { TaskRow(task = task(), onToggle = { toggled++ }) } }
        composeRule.onNodeWithText("Read NCERT chapter 3").performClick()
        assertEquals(1, toggled)
    }

    @Test
    fun `status pill reflects the task state`() {
        composeRule.setContent { PcmTheme { StatusPill("completed") } }
        composeRule.onNodeWithText("Done").assertIsDisplayed()
    }

    @Test
    fun `markdown note renders headings and bullet text`() {
        val note = "# Kinematics\n- Speed is distance over time\n- **Velocity** is a vector"
        composeRule.setContent { PcmTheme { MarkdownText(note) } }
        composeRule.onNodeWithText("Kinematics").assertIsDisplayed()
        composeRule.onNodeWithText("Speed is distance over time").assertIsDisplayed()
        composeRule.onNodeWithText("Velocity is a vector").assertIsDisplayed()
    }

    @Test
    fun `pill renders its label`() {
        composeRule.setContent { PcmTheme { Pill("due", Success) } }
        composeRule.onNodeWithText("due").assertIsDisplayed()
    }

    @Test
    fun `theme switching changes the surface colour`() {
        val theme = mutableStateOf("dark")
        var background: Color? = null

        composeRule.setContent {
            PcmTheme(theme = theme.value) { background = MaterialTheme.colorScheme.background }
        }
        composeRule.waitForIdle()
        val darkBackground = background

        theme.value = "light"
        composeRule.waitForIdle()

        assertNotEquals(darkBackground, background)
    }

    @Test
    fun `accent colour is applied to the scheme`() {
        var primary: Color? = null
        composeRule.setContent {
            PcmTheme(theme = "dark", accentHex = "#39C6A4") { Capture { primary = it } }
        }
        composeRule.waitForIdle()
        assertEquals(Color(0xFF39C6A4), primary)
    }

    @Composable
    private fun Capture(onColor: (Color) -> Unit) {
        onColor(MaterialTheme.colorScheme.primary)
    }
}
