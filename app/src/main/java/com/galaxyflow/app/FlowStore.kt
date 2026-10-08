package com.galaxyflow.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Private local state. Interrupted runs require explicit resume after a fresh process. */
class FlowStore(context: Context) {
    private val prefs = context.getSharedPreferences("workspace", Context.MODE_PRIVATE)
    fun save(state: FlowState): Boolean {
        val e = state.execution
        val json = JSONObject().apply {
            put("version", 1); put("notes", state.notes); put("priorities", state.priorities)
            put("active", state.workspaceActive); put("focus", state.focus)
            put("mode", e.mode); put("status", e.status.name); put("index", e.currentStepIndex)
            put("completed", JSONArray(e.completedIds.toList())); put("skipped", JSONArray(e.skippedIds.toList()))
            put("resume", e.resumeStatus.name); put("previousActive", e.previousWorkspaceActive); put("previousFocus", e.previousFocus)
            put("error", e.error ?: JSONObject.NULL)
        }
        return prefs.edit().putString("state", json.toString()).commit()
    }
    fun load(): FlowState = try {
        val raw = prefs.getString("state", null)
        if (raw == null) FlowState() else {
            val j = JSONObject(raw)
            require(j.getInt("version") == 1)
            val steps = defaultSteps()
            val ids = steps.map { it.id }.toSet()
            fun idsAt(key: String): Set<String> {
                val array = j.getJSONArray(key)
                return (0 until array.length()).map { array.getString(it) }.toSet().also { require(ids.containsAll(it)) }
            }
            val completed = idsAt("completed"); val skipped = idsAt("skipped")
            val savedStatus = ExecutionStatus.valueOf(j.getString("status"))
            val interrupted = savedStatus in setOf(ExecutionStatus.RUNNING, ExecutionStatus.EVALUATING)
            val mode = j.getString("mode").also { require(it in setOf("preview", "device")) }
            val index = j.getInt("index").also { require(it in -1 until steps.size) }
            if (savedStatus == ExecutionStatus.WAITING_APPROVAL) require(steps.getOrNull(index + 1)?.requiresApproval == true && mode == "device")
            require(completed.intersect(skipped).isEmpty())
            val status = if (interrupted) ExecutionStatus.PAUSED else savedStatus
            FlowState(
                notes = j.getString("notes").take(20_000), priorities = j.getString("priorities").take(10_000),
                workspaceActive = j.getBoolean("active"), focus = j.getBoolean("focus"),
                steps = steps.map { it.copy(state = when (it.id) { in skipped -> "skipped"; in completed -> if (it.requiresApproval && mode == "device") "handed off" else "complete"; else -> "pending" }) },
                execution = Execution(mode = mode, status = status, currentStepIndex = index, completedIds = completed, skippedIds = skipped,
                    resumeStatus = if (interrupted) savedStatus else ExecutionStatus.valueOf(j.getString("resume")),
                    previousWorkspaceActive = j.getBoolean("previousActive"), previousFocus = j.getBoolean("previousFocus"),
                    error = if (j.isNull("error")) null else j.getString("error"),
                    capsule = if (mode == "device" && (savedStatus == ExecutionStatus.COMPLETED || savedStatus == ExecutionStatus.PAUSED && j.getString("resume") == "COMPLETED")) Capsule(if (status == ExecutionStatus.PAUSED) "paused" else "active", FoldPosture.OPEN) else null,
                ),
            )
        }
    } catch (_: Exception) { FlowState() }
}
