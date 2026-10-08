package com.galaxyflow.app

import android.app.AlertDialog
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var store: FlowStore
    private var state = FlowState()
    private var runToken = 0
    private var detectedPosture = FoldPosture.COVER
    private var posturePreview = false
    private var scroll: ScrollView? = null
    private val bg = Color.rgb(8, 12, 19)
    private val surface = Color.rgb(21, 30, 43)
    private val textColor = Color.rgb(244, 247, 251)
    private val muted = Color.rgb(145, 160, 178)
    private val cyan = Color.rgb(120, 217, 255)
    private val lime = Color.rgb(201, 243, 109)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = FlowStore(this)
        state = store.load()
        detectWindow()
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                WindowInfoTracker.getOrCreate(this@MainActivity).windowLayoutInfo(this@MainActivity).collect { info ->
                    val fold = info.displayFeatures.filterIsInstance<FoldingFeature>().firstOrNull()
                    detectedPosture = when {
                        isExternalDisplay() -> FoldPosture.DEX
                        fold?.state == FoldingFeature.State.HALF_OPENED -> FoldPosture.FLEX
                        fold != null || resources.configuration.screenWidthDp >= 600 -> FoldPosture.OPEN
                        else -> FoldPosture.COVER
                    }
                    if (!posturePreview && state.posture != detectedPosture) {
                        state = state.copy(posture = detectedPosture)
                        render()
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        render()
    }

    override fun onStop() {
        runToken++
        handler.removeCallbacksAndMessages(null)
        if (state.execution.status in setOf(ExecutionStatus.RUNNING, ExecutionStatus.EVALUATING)) {
            dispatch(Event.Pause, redraw = false)
        }
        super.onStop()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        detectWindow()
        render()
    }

    private fun isExternalDisplay(): Boolean = Build.VERSION.SDK_INT >= 30 &&
        display?.displayId?.let { it != android.view.Display.DEFAULT_DISPLAY } == true

    private fun detectWindow() {
        detectedPosture = when {
            isExternalDisplay() -> FoldPosture.DEX
            resources.configuration.screenWidthDp >= 600 -> FoldPosture.OPEN
            else -> FoldPosture.COVER
        }
        if (!posturePreview) state = state.copy(posture = detectedPosture)
    }

    private fun dispatch(event: Event, redraw: Boolean = true) {
        val next = FlowEngine.reduce(state, event)
        state = if (store.save(next)) next else {
            FlowEngine.reduce(state, Event.Fail("Local storage could not be saved. Retry before continuing."))
        }
        if (redraw) render()
    }

    private fun render() {
        val previousScroll = scroll?.scrollY ?: 0
        val page = column().apply {
            setPadding(dp(20), dp(20), dp(20), dp(30))
            addView(label("GALAXY FLOW · ON-DEVICE WORKSPACE", cyan))
            addView(body("Start Work", 32f, textColor))
            addView(body("Your notes and priorities, saved locally. Start when you choose."))
            addView(workspaceCard())
            addView(card().apply {
                addView(label("${state.execution.status.name.replace('_', ' ')} · ${if (state.execution.mode == "device") "LOCAL WORKSPACE" else "PREVIEW"}", lime))
                addView(body("Manual start · No location or Wi-Fi monitoring"))
                addView(control("Preview workspace") { startRun(false) })
                addView(control("Start workspace") { startRun(true) })
                when (state.execution.status) {
                    ExecutionStatus.WAITING_APPROVAL -> addView(approvalCard())
                    ExecutionStatus.PAUSED -> addView(control("Resume interrupted flow") {
                        dispatch(Event.Resume)
                        scheduleNext()
                    })
                    ExecutionStatus.EVALUATING, ExecutionStatus.RUNNING, ExecutionStatus.COMPLETED -> addView(control("Pause flow") {
                        cancelRun()
                        dispatch(Event.Pause)
                    })
                    else -> Unit
                }
                if (state.execution.status != ExecutionStatus.IDLE) {
                    addView(control("Undo workspace activation") {
                        cancelRun()
                        dispatch(Event.Undo)
                    })
                }
                state.execution.error?.let { message ->
                    addView(body(message, color = Color.rgb(255, 170, 170)))
                    addView(control("Dismiss error") { dispatch(Event.DismissError) })
                }
            })
            if (!state.focus) {
                addView(label("FLOW PLAN", cyan))
                state.steps.forEach { step ->
                    addView(card().apply {
                        addView(body("${step.title} · ${step.state}", 14f, textColor))
                        addView(body(step.detail))
                    })
                }
            }
            addView(trustCard())
            addView(stagePanel())
        }
        val nextScroll = ScrollView(this).apply {
            isFillViewport = true
            setBackgroundColor(bg)
            setOnApplyWindowInsetsListener { view, insets ->
                val bars = WindowInsetsCompat.toWindowInsetsCompat(insets).getInsets(
                    WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
                insets
            }
            addView(page)
        }
        scroll = nextScroll
        setContentView(nextScroll)
        nextScroll.post { nextScroll.scrollTo(0, previousScroll) }
    }

    private fun workspaceCard(): View = card().apply {
        addView(label(if (state.workspaceActive) "YOUR ACTIVE WORKSPACE" else "YOUR SAVED WORKSPACE", cyan))
        addView(body("Notes: ${state.notes.ifBlank { "No notes saved yet" }}"))
        addView(body("Priorities: ${state.priorities.ifBlank { "No priorities saved yet" }}"))
        addView(control("Edit notes and priorities") { editWorkspace() })
        if (state.focus) {
            addView(control("Leave app focus") { dispatch(Event.LeaveFocus) })
        }
    }

    private fun editWorkspace() {
        val notes = EditText(this).apply {
            hint = "Notes"
            setText(state.notes)
            minLines = 3
            filters = arrayOf(android.text.InputFilter.LengthFilter(20_000))
        }
        val priorities = EditText(this).apply {
            hint = "Priorities"
            setText(state.priorities)
            minLines = 2
            filters = arrayOf(android.text.InputFilter.LengthFilter(10_000))
        }
        val fields = column().apply {
            setPadding(dp(20), dp(12), dp(20), dp(12))
            addView(notes)
            addView(priorities)
        }
        AlertDialog.Builder(this).setTitle("Local workspace").setView(fields)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->
                dispatch(Event.SaveWorkspace(notes.text.toString(), priorities.text.toString()))
            }.show()
    }

    private fun approvalCard(): View = column().apply {
        addView(label("YOUR APPROVAL IS REQUIRED", lime))
        addView(body("Open a generic work reminder draft in Calendar? No notes are shared. You decide whether to save in Calendar. Undo cannot remove a reminder you save there."))
        addView(control("Open Calendar draft") {
            if (CalendarDraft.open(this@MainActivity)) {
                dispatch(Event.ApproveReminder)
                scheduleNext()
            } else {
                dispatch(Event.Fail("No Calendar app could open the draft. Dismiss, resume, and skip this optional step."))
            }
        })
        addView(control("Skip reminder") {
            dispatch(Event.SkipReminder)
            scheduleNext()
        })
    }

    private fun trustCard(): View = card().apply {
        addView(label("LOCAL DATA AND EXPLICIT HANDOFF", lime))
        addView(body("Notes stay in this app. Calendar opens only after approval."))
        addView(control(if (state.trustOpen) "Hide details" else "Show details") { dispatch(Event.ToggleTrust) })
        if (state.trustOpen) {
            addView(body("Private local storage; cloud backup and device transfer are excluded. Focus hides this app’s planning rail only. Calendar receives a generic draft title and time after approval. Saving or cancelling happens in Calendar. No network, location, Calendar access, or notification policy permissions."))
        }
    }

    private fun stagePanel(): View = card().apply {
        addView(label(if (posturePreview) "LAYOUT PREVIEW" else "OBSERVED WINDOW", cyan))
        addView(body(state.posture.label, 20f, textColor))
        addView(body("Window/fold signals describe this Android window. External display does not establish Samsung DeX. The drawing below is illustrative."))
        FoldPosture.entries.forEach { posture ->
            addView(control("Preview ${posture.label}") {
                posturePreview = true
                dispatch(Event.SelectPosture(posture))
            })
        }
        addView(control("Use observed window") {
            posturePreview = false
            state = state.copy(posture = detectedPosture)
            render()
        })
        addView(FoldStageView(this@MainActivity).apply {
            posture = state.posture
            active = state.workspaceActive
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(220)))
    }

    private fun cancelRun() {
        runToken++
        handler.removeCallbacksAndMessages(null)
    }

    private fun startRun(device: Boolean) {
        cancelRun()
        dispatch(if (device) Event.StartWorkspace else Event.StartPreview)
        scheduleNext()
    }

    private fun scheduleNext() {
        if (state.execution.status !in setOf(ExecutionStatus.EVALUATING, ExecutionStatus.RUNNING)) return
        val token = runToken
        handler.postDelayed({
            if (token == runToken && lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                dispatch(Event.Advance)
                scheduleNext()
            }
        }, 550L)
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).roundToInt()
    private fun column() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    private fun card() = column().apply {
        setPadding(dp(14), dp(12), dp(14), dp(12))
        background = GradientDrawable().apply {
            setColor(surface)
            cornerRadius = dp(14).toFloat()
        }
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(12)
            bottomMargin = dp(12)
        }
    }
    private fun label(value: String, color: Int) = body(value, 11f, color).apply {
        setTypeface(Typeface.DEFAULT, Typeface.BOLD)
    }
    private fun body(value: String, size: Float = 13f, color: Int = muted) = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(color)
        setPadding(0, dp(6), 0, dp(6))
    }
    private fun control(title: String, action: () -> Unit) = Button(this).apply {
        text = title
        textSize = 12f
        setTextColor(cyan)
        setOnClickListener { action() }
    }
}
