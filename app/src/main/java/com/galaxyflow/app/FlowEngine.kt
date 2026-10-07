package com.galaxyflow.app

enum class FoldPosture(val label: String) {
    COVER("Cover"), OPEN("Open"), FLEX("Flex"), DEX("DeX")
}

enum class ExecutionStatus { IDLE, EVALUATING, RUNNING, PAUSED, COMPLETED, FAILED }

data class FlowStep(
    val id: String,
    val group: String,
    val title: String,
    val detail: String,
    val risk: String,
    val requiresApproval: Boolean = false,
    val state: String = "pending",
)

data class Capsule(val status: String, val posture: FoldPosture)

data class Execution(
    val mode: String = "preview",
    val status: ExecutionStatus = ExecutionStatus.IDLE,
    val currentStepIndex: Int = -1,
    val completedIds: Set<String> = emptySet(),
    val error: String? = null,
    val capsule: Capsule? = null,
)

data class FlowState(
    val posture: FoldPosture = FoldPosture.OPEN,
    val selectedStepId: String = "workspace",
    val steps: List<FlowStep> = defaultSteps(),
    val execution: Execution = Execution(),
    val trustOpen: Boolean = false,
)

fun defaultSteps() = listOf(
    FlowStep("trigger", "Trigger", "Arrive at the office", "Weekday + Work Wi-Fi + Fold opened between 8:00 and 10:00 AM", "low"),
    FlowStep("workspace", "Workspace", "Open the Work stage", "Arrange Calendar and Notes across the inner display", "low"),
    FlowStep("priorities", "Context", "Show today’s priorities", "Surface the next meeting, due tasks, and the last edited brief", "medium"),
    FlowStep("focus", "Device", "Enable Work focus", "Allow starred contacts and silence nonessential alerts", "medium"),
    FlowStep("reminder", "Approval", "Prepare a status reminder", "Create a local reminder for 4:00 PM; never send automatically", "high", true),
)

object FlowEngine {
    fun reduce(state: FlowState, event: Event): FlowState = when (event) {
        is Event.SelectPosture -> state.copy(posture = event.posture)
        is Event.SelectStep -> state.copy(selectedStepId = event.id)
        Event.ToggleTrust -> state.copy(trustOpen = !state.trustOpen)
        Event.StartPreview -> start(state, "preview")
        Event.StartLive -> start(state, "live")
        Event.Advance -> advance(state)
        Event.Pause -> if (state.execution.status == ExecutionStatus.COMPLETED && state.execution.capsule != null) state.copy(execution = state.execution.copy(status = ExecutionStatus.PAUSED, capsule = state.execution.capsule.copy(status = "paused"))) else state
        Event.Resume -> if (state.execution.status == ExecutionStatus.PAUSED) state.copy(execution = state.execution.copy(status = ExecutionStatus.COMPLETED, capsule = state.execution.capsule?.copy(status = "active"))) else state
        Event.Undo -> state.copy(execution = Execution(), steps = state.steps.map { it.copy(state = "pending") })
        is Event.Fail -> state.copy(execution = state.execution.copy(status = ExecutionStatus.FAILED, error = event.message))
        Event.DismissError -> state.copy(execution = state.execution.copy(status = ExecutionStatus.IDLE, error = null))
    }

    private fun start(state: FlowState, mode: String) = state.copy(
        execution = Execution(mode = mode, status = ExecutionStatus.EVALUATING),
        steps = state.steps.map { it.copy(state = "pending") },
    )

    private fun advance(state: FlowState): FlowState {
        if (state.execution.status != ExecutionStatus.EVALUATING && state.execution.status != ExecutionStatus.RUNNING) return state
        val nextIndex = state.execution.currentStepIndex + 1
        if (nextIndex >= state.steps.size) {
            val live = state.execution.mode == "live"
            return state.copy(
                execution = state.execution.copy(
                    status = ExecutionStatus.COMPLETED,
                    currentStepIndex = state.steps.lastIndex,
                    capsule = if (live) Capsule("active", state.posture) else null,
                ),
                steps = state.steps.map { it.copy(state = "complete") },
            )
        }
        val id = state.steps[nextIndex].id
        val completed = state.execution.completedIds + id
        return state.copy(
            execution = state.execution.copy(status = ExecutionStatus.RUNNING, currentStepIndex = nextIndex, completedIds = completed),
            steps = state.steps.map { step -> if (step.id in completed) step.copy(state = "complete") else step },
        )
    }
}

sealed interface Event {
    data class SelectPosture(val posture: FoldPosture) : Event
    data class SelectStep(val id: String) : Event
    data object ToggleTrust : Event
    data object StartPreview : Event
    data object StartLive : Event
    data object Advance : Event
    data object Pause : Event
    data object Resume : Event
    data object Undo : Event
    data class Fail(val message: String) : Event
    data object DismissError : Event
}
