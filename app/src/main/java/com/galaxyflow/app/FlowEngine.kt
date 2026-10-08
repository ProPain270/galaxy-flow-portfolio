package com.galaxyflow.app

enum class FoldPosture(val label: String) { COVER("Compact"), OPEN("Expanded"), FLEX("Partially folded"), DEX("External") }
enum class ExecutionStatus { IDLE, EVALUATING, RUNNING, WAITING_APPROVAL, PAUSED, COMPLETED, FAILED }
data class FlowStep(val id: String, val group: String, val title: String, val detail: String, val risk: String, val requiresApproval: Boolean = false, val state: String = "pending")
data class Capsule(val status: String, val posture: FoldPosture)
data class Execution(
    val mode: String = "preview", val status: ExecutionStatus = ExecutionStatus.IDLE,
    val currentStepIndex: Int = -1, val completedIds: Set<String> = emptySet(), val skippedIds: Set<String> = emptySet(),
    val error: String? = null, val capsule: Capsule? = null, val resumeStatus: ExecutionStatus = ExecutionStatus.RUNNING,
    val previousWorkspaceActive: Boolean = false, val previousFocus: Boolean = false,
)
data class FlowState(
    val posture: FoldPosture = FoldPosture.OPEN, val selectedStepId: String = "workspace",
    val steps: List<FlowStep> = defaultSteps(), val execution: Execution = Execution(), val trustOpen: Boolean = false,
    val workspaceActive: Boolean = false, val focus: Boolean = false, val notes: String = "", val priorities: String = "",
)
fun defaultSteps() = listOf(
    FlowStep("trigger", "Trigger", "Start when you choose", "Run this workspace manually on your device", "low"),
    FlowStep("workspace", "Workspace", "Open your local workspace", "Show your saved notes and priorities", "low"),
    FlowStep("priorities", "Context", "Bring your priorities forward", "Use the priorities you saved in this app", "low"),
    FlowStep("focus", "Device", "Enable app focus", "Hide the planning rail inside Galaxy Flow; system notifications stay unchanged", "low"),
    FlowStep("reminder", "Approval", "Prepare a Calendar reminder", "Open a reminder draft in your Calendar app; you decide whether to save it", "medium", true),
)
object FlowEngine {
    fun reduce(state: FlowState, event: Event): FlowState = when (event) {
        is Event.SelectPosture -> state.copy(posture = event.posture)
        is Event.SelectStep -> if (state.steps.any { it.id == event.id }) state.copy(selectedStepId = event.id) else state
        Event.ToggleTrust -> state.copy(trustOpen = !state.trustOpen)
        Event.StartPreview -> start(state, "preview")
        Event.StartWorkspace -> start(state, "device")
        Event.Advance -> advance(state)
        Event.ApproveReminder -> resolveApproval(state, false)
        Event.SkipReminder -> resolveApproval(state, true)
        Event.Pause -> if (state.execution.status in setOf(ExecutionStatus.EVALUATING, ExecutionStatus.RUNNING, ExecutionStatus.COMPLETED)) {
            state.copy(execution = state.execution.copy(status = ExecutionStatus.PAUSED, resumeStatus = state.execution.status, capsule = state.execution.capsule?.copy(status = "paused")))
        } else state
        Event.Resume -> if (state.execution.status == ExecutionStatus.PAUSED) state.copy(execution = state.execution.copy(status = state.execution.resumeStatus, capsule = state.execution.capsule?.copy(status = "active"))) else state
        Event.Undo -> state.copy(
            workspaceActive = if (state.execution.mode == "device") state.execution.previousWorkspaceActive else state.workspaceActive,
            focus = if (state.execution.mode == "device") state.execution.previousFocus else state.focus,
            execution = Execution(), steps = state.steps.map { it.copy(state = "pending") },
        )
        is Event.Fail -> state.copy(execution = state.execution.copy(status = ExecutionStatus.FAILED, error = event.message))
        Event.DismissError -> state.copy(execution = state.execution.copy(status = ExecutionStatus.PAUSED, resumeStatus = ExecutionStatus.RUNNING, error = null))
        is Event.SaveWorkspace -> state.copy(notes = event.notes.take(20_000), priorities = event.priorities.take(10_000))
        Event.LeaveFocus -> state.copy(focus = false)
    }
    private fun start(state: FlowState, mode: String) = state.copy(
        execution = Execution(mode = mode, status = ExecutionStatus.EVALUATING, previousWorkspaceActive = state.workspaceActive, previousFocus = state.focus),
        steps = state.steps.map { it.copy(state = "pending") },
    )
    private fun advance(state: FlowState): FlowState {
        if (state.execution.status !in setOf(ExecutionStatus.EVALUATING, ExecutionStatus.RUNNING)) return state
        val nextIndex = state.execution.currentStepIndex + 1
        if (nextIndex >= state.steps.size) return state.copy(execution = state.execution.copy(
            status = ExecutionStatus.COMPLETED, capsule = if (state.execution.mode == "device") Capsule("active", state.posture) else null,
        ))
        val step = state.steps[nextIndex]
        if (state.execution.mode == "device" && step.requiresApproval) return state.copy(execution = state.execution.copy(status = ExecutionStatus.WAITING_APPROVAL))
        val device = state.execution.mode == "device"
        return completeStep(state.copy(
            workspaceActive = state.workspaceActive || (device && step.id == "workspace"),
            focus = state.focus || (device && step.id == "focus"),
        ), nextIndex, false)
    }
    private fun resolveApproval(state: FlowState, skipped: Boolean): FlowState {
        if (state.execution.status != ExecutionStatus.WAITING_APPROVAL) return state
        return completeStep(state, state.execution.currentStepIndex + 1, skipped)
    }
    private fun completeStep(state: FlowState, index: Int, skipped: Boolean): FlowState {
        val id = state.steps[index].id
        val completed = if (skipped) state.execution.completedIds else state.execution.completedIds + id
        val skippedIds = if (skipped) state.execution.skippedIds + id else state.execution.skippedIds
        return state.copy(
            execution = state.execution.copy(status = ExecutionStatus.RUNNING, currentStepIndex = index, completedIds = completed, skippedIds = skippedIds, error = null),
            steps = state.steps.map { step -> step.copy(state = when (step.id) {
                in skippedIds -> "skipped"
                in completed -> if (step.requiresApproval && state.execution.mode == "device") "handed off" else "complete"
                else -> "pending"
            }) },
        )
    }
}
sealed interface Event {
    data class SelectPosture(val posture: FoldPosture) : Event
    data class SelectStep(val id: String) : Event
    data object ToggleTrust : Event
    data object StartPreview : Event
    data object StartWorkspace : Event
    data object Advance : Event
    data object ApproveReminder : Event
    data object SkipReminder : Event
    data object Pause : Event
    data object Resume : Event
    data object Undo : Event
    data class Fail(val message: String) : Event
    data object DismissError : Event
    data class SaveWorkspace(val notes: String, val priorities: String) : Event
    data object LeaveFocus : Event
}
