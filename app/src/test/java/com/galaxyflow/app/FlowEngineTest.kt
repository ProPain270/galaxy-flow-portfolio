package com.galaxyflow.app

import org.junit.Assert.*
import org.junit.Test

class FlowEngineTest {
    private fun advance(state: FlowState, times: Int): FlowState = (0 until times).fold(state) { s, _ -> FlowEngine.reduce(s, Event.Advance) }
    @Test fun previewDoesNotChangeLocalWorkspace() {
        val result = advance(FlowEngine.reduce(FlowState(notes = "draft"), Event.StartPreview), 6)
        assertEquals(ExecutionStatus.COMPLETED, result.execution.status)
        assertFalse(result.workspaceActive); assertFalse(result.focus); assertNull(result.execution.capsule)
    }
    @Test fun deviceRunStopsAtApprovalAndCannotAdvancePastIt() {
        val waiting = advance(FlowEngine.reduce(FlowState(), Event.StartWorkspace), 5)
        assertTrue(waiting.workspaceActive); assertTrue(waiting.focus)
        assertEquals(ExecutionStatus.WAITING_APPROVAL, waiting.execution.status)
        assertFalse("reminder" in waiting.execution.completedIds)
        assertEquals(waiting, advance(waiting, 10))
    }
    @Test fun skipAndHandoffHaveDistinctEvidence() {
        val waiting = advance(FlowEngine.reduce(FlowState(), Event.StartWorkspace), 5)
        val skipped = advance(FlowEngine.reduce(waiting, Event.SkipReminder), 1)
        assertEquals(ExecutionStatus.COMPLETED, skipped.execution.status)
        assertTrue("reminder" in skipped.execution.skippedIds)
        assertFalse("reminder" in skipped.execution.completedIds)
        val approved = FlowEngine.reduce(waiting, Event.ApproveReminder)
        assertEquals("handed off", approved.steps.last().state)
        assertEquals(FlowState(), FlowEngine.reduce(FlowState(), Event.ApproveReminder))
    }
    @Test fun undoRestoresPreviousFlagsAndKeepsUserEdits() {
        val original = FlowState(workspaceActive = true, focus = false, notes = "first")
        val running = advance(FlowEngine.reduce(original, Event.StartWorkspace), 4)
        val edited = FlowEngine.reduce(running, Event.SaveWorkspace("second", "priority"))
        val undone = FlowEngine.reduce(edited, Event.Undo)
        assertTrue(undone.workspaceActive); assertFalse(undone.focus)
        assertEquals("second", undone.notes); assertEquals("priority", undone.priorities)
    }
    @Test fun pausedRunDoesNotAdvanceUntilResume() {
        val running = advance(FlowEngine.reduce(FlowState(), Event.StartWorkspace), 2)
        val paused = FlowEngine.reduce(running, Event.Pause)
        assertEquals(paused, advance(paused, 5))
        assertEquals(ExecutionStatus.RUNNING, FlowEngine.reduce(paused, Event.Resume).execution.status)
    }
    @Test fun failedCalendarCanBeRetriedAndSkipped() {
        val waiting = advance(FlowEngine.reduce(FlowState(), Event.StartWorkspace), 5)
        val failed = FlowEngine.reduce(waiting, Event.Fail("unavailable"))
        val retried = advance(FlowEngine.reduce(FlowEngine.reduce(failed, Event.DismissError), Event.Resume), 1)
        assertEquals(ExecutionStatus.WAITING_APPROVAL, retried.execution.status)
        assertEquals(ExecutionStatus.RUNNING, FlowEngine.reduce(retried, Event.SkipReminder).execution.status)
    }
    @Test fun workspaceInputIsBounded() {
        val saved = FlowEngine.reduce(FlowState(), Event.SaveWorkspace("a".repeat(20_001), "b".repeat(10_001)))
        assertEquals(20_000, saved.notes.length); assertEquals(10_000, saved.priorities.length)
    }
}
