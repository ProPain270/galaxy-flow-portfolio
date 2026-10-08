package com.galaxyflow.app

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import android.widget.Button
import android.view.View
import android.view.ViewGroup
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Calendar

@RunWith(AndroidJUnit4::class)
class WorkspaceDeviceTest {
    @get:org.junit.Rule val folding = androidx.window.testing.layout.WindowLayoutInfoPublisherRule()
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private fun clear() { context.getSharedPreferences("workspace", Context.MODE_PRIVATE).edit().clear().commit() }
    @Test fun savedWorkspaceSurvivesFreshStoreAndInterruptedRunPauses() {
        clear()
        val store = FlowStore(context)
        var state = FlowEngine.reduce(FlowState(), Event.SaveWorkspace("Sample note", "Sample priority"))
        state = FlowEngine.reduce(state, Event.StartWorkspace)
        repeat(3) { state = FlowEngine.reduce(state, Event.Advance) }
        assertTrue(store.save(state))
        val restored = FlowStore(context).load()
        assertEquals("Sample note", restored.notes); assertEquals("Sample priority", restored.priorities)
        assertTrue(restored.workspaceActive)
        assertEquals(ExecutionStatus.PAUSED, restored.execution.status)
        assertEquals(restored, FlowEngine.reduce(restored, Event.Advance))
        assertEquals(ExecutionStatus.RUNNING, FlowEngine.reduce(restored, Event.Resume).execution.status)
        clear()
    }
    @Test fun approvalSurvivesRestoreWithoutPerformingHandoff() {
        clear()
        var state = FlowEngine.reduce(FlowState(), Event.StartWorkspace)
        repeat(5) { state = FlowEngine.reduce(state, Event.Advance) }
        assertTrue(FlowStore(context).save(state))
        val restored = FlowStore(context).load()
        assertEquals(ExecutionStatus.WAITING_APPROVAL, restored.execution.status)
        assertFalse("reminder" in restored.execution.completedIds)
        clear()
    }
    @Test fun corruptedStorageFailsClosed() {
        clear()
        context.getSharedPreferences("workspace", Context.MODE_PRIVATE).edit().putString("state", "broken json").commit()
        assertEquals(FlowState(), FlowStore(context).load())
        clear()
    }
    @Test fun calendarDraftUsesStandardIntentAndNoPrivateNotes() {
        val now = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 7, 17, 0, 0) }
        val draft = CalendarDraft.intent(now)
        assertEquals(Intent.ACTION_INSERT, draft.action)
        assertEquals(CalendarContract.Events.CONTENT_URI, draft.data)
        assertEquals("Review work priorities", draft.getStringExtra(CalendarContract.Events.TITLE))
        assertFalse(draft.hasExtra(CalendarContract.Events.DESCRIPTION))
        assertFalse(draft.hasExtra(CalendarContract.Events.EVENT_LOCATION))
        val begin = draft.getLongExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, 0)
        assertTrue(begin > now.timeInMillis)
        assertEquals(15 * 60_000L, draft.getLongExtra(CalendarContract.EXTRA_EVENT_END_TIME, 0) - begin)
    }
    private fun buttons(view: View): List<Button> = when (view) {
        is Button -> listOf(view)
        is ViewGroup -> (0 until view.childCount).flatMap { buttons(view.getChildAt(it)) }
        else -> emptyList()
    }
    @Test fun installedActivityRestoresApprovalAndUndoKeepsNotes() {
        clear()
        var state = FlowEngine.reduce(FlowState(notes = "Sample note"), Event.StartWorkspace)
        repeat(5) { state = FlowEngine.reduce(state, Event.Advance) }
        assertTrue(FlowStore(context).save(state))
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.recreate()
            scenario.onActivity { activity ->
                val controls = buttons(activity.window.decorView)
                assertTrue(controls.any { it.text == "Open Calendar draft" })
                controls.first { it.text == "Undo workspace activation" }.performClick()
                val restored = FlowStore(context).load()
                assertFalse(restored.workspaceActive); assertFalse(restored.focus)
                assertEquals("Sample note", restored.notes)
                assertEquals(ExecutionStatus.IDLE, restored.execution.status)
            }
        }
        clear()
    }
    @Test fun installedApprovalOpensOnlyGenericDraftAfterUserTap() {
        clear()
        var state = FlowEngine.reduce(FlowState(notes = "Sample private note"), Event.StartWorkspace)
        repeat(5) { state = FlowEngine.reduce(state, Event.Advance) }
        assertTrue(FlowStore(context).save(state))
        val instrumentation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
        var captured: Intent? = null
        // Intercept the handoff so this test never creates a real Calendar event.
        val monitor = object : android.app.Instrumentation.ActivityMonitor() {
            override fun onStartActivity(intent: Intent): android.app.Instrumentation.ActivityResult? {
                if (intent.action != Intent.ACTION_INSERT) return null
                captured = intent
                return android.app.Instrumentation.ActivityResult(android.app.Activity.RESULT_CANCELED, null)
            }
        }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            instrumentation.addMonitor(monitor)
            try {
                scenario.onActivity { activity ->
                    assertNull(captured)
                    buttons(activity.window.decorView).first { it.text == "Open Calendar draft" }.performClick()
                    assertEquals(Intent.ACTION_INSERT, captured?.action)
                    assertEquals(CalendarContract.Events.CONTENT_URI, captured?.data)
                    assertFalse(captured!!.hasExtra(CalendarContract.Events.DESCRIPTION))
                    assertEquals("handed off", FlowStore(context).load().steps.last().state)
                }
            } finally { instrumentation.removeMonitor(monitor) }
        }
        clear()
    }
    @Test fun leavingForegroundPausesScheduledExecution() {
        clear()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                buttons(activity.window.decorView).first { it.text == "Start workspace" }.performClick()
            }
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
            val paused = FlowStore(context).load()
            assertEquals(ExecutionStatus.PAUSED, paused.execution.status)
            android.os.SystemClock.sleep(1_000)
            assertEquals(paused.execution.currentStepIndex, FlowStore(context).load().execution.currentStepIndex)
            assertEquals(ExecutionStatus.PAUSED, FlowStore(context).load().execution.status)
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
            scenario.onActivity { activity ->
                assertTrue(buttons(activity.window.decorView).any { it.text == "Resume interrupted flow" })
            }
        }
        clear()
    }

    private fun texts(view: View): List<String> = when (view) {
        is android.widget.TextView -> listOf(view.text.toString())
        is ViewGroup -> (0 until view.childCount).flatMap { texts(view.getChildAt(it)) }
        else -> emptyList()
    }
    @Test fun observedFoldAndManualPreviewRemainDistinct() {
        clear()
        val instrumentation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                folding.overrideWindowLayoutInfo(androidx.window.layout.WindowLayoutInfo(listOf(
                    androidx.window.testing.layout.FoldingFeature(activity = activity,
                        state = androidx.window.layout.FoldingFeature.State.HALF_OPENED,
                        orientation = androidx.window.layout.FoldingFeature.Orientation.HORIZONTAL))))
            }
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                assertTrue(texts(activity.window.decorView).contains("Partially folded"))
                assertTrue(texts(activity.window.decorView).contains("OBSERVED WINDOW"))
                buttons(activity.window.decorView).first { it.text == "Preview Compact" }.performClick()
                assertTrue(texts(activity.window.decorView).contains("LAYOUT PREVIEW"))
                assertTrue(texts(activity.window.decorView).contains("Compact"))
                buttons(activity.window.decorView).first { it.text == "Use observed window" }.performClick()
                assertTrue(texts(activity.window.decorView).contains("OBSERVED WINDOW"))
                assertTrue(texts(activity.window.decorView).contains("Partially folded"))
            }
        }
        clear()
    }

}
