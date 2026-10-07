package com.galaxyflow.app

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import kotlin.math.roundToInt

class MainActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private var state = FlowState()
    private var runToken = 0
    private lateinit var root: FrameLayout

    private val bg = Color.rgb(8, 12, 19)
    private val surface = Color.rgb(16, 23, 34)
    private val surfaceRaised = Color.rgb(21, 30, 43)
    private val textColor = Color.rgb(244, 247, 251)
    private val muted = Color.rgb(145, 160, 178)
    private val blue = Color.rgb(110, 161, 255)
    private val cyan = Color.rgb(120, 217, 255)
    private val lime = Color.rgb(201, 243, 109)
    private val purple = Color.rgb(155, 140, 255)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        render()
    }

    private fun render() {
        val previousScroll = if (::root.isInitialized) {
            (root.getChildAt(0) as? ScrollView)?.scrollY ?: 0
        } else {
            0
        }
        root = FrameLayout(this).apply {
            setBackgroundColor(bg)
            setPadding(0, statusBarHeight(), 0, 0)
        }
        val scroll = ScrollView(this).apply { isFillViewport = true }
        val page = column(0)
        page.addView(topBar())
        page.addView(content())
        scroll.addView(page)
        root.addView(scroll, FrameLayout.LayoutParams(-1, -1))
        setContentView(root)
        scroll.post { scroll.scrollTo(0, previousScroll) }
    }

    private fun topBar(): View = row(16).apply {
        setPadding(dp(20), dp(17), dp(20), dp(17))
        addView(TextView(this@MainActivity).apply {
            text = "◈  Galaxy Flow\n     Make your moment"
            setTextColor(textColor)
            textSize = 15f
            setTypeface(Typeface.DEFAULT, Typeface.BOLD)
        }, LinearLayout.LayoutParams(0, -2, 1f))
        addView(TextView(this@MainActivity).apply { text = "●  Local prototype   DY"; setTextColor(muted); textSize = 11f; gravity = Gravity.CENTER_VERTICAL })
    }

    private fun content(): View = column(18).apply {
        setPadding(dp(20), dp(24), dp(20), dp(30))
        addView(label("ADAPTIVE WORKSPACE", cyan))
        addView(title("Start Work"))
        addView(body("Arrive at work. Your Fold sets up the day."))
        addView(contextCard())
        addView(sectionTitle("FLOW RAIL", "Then do these things", "${state.steps.size - 1} actions"))
        addView(flowRail())
        addView(detailCard())
        addView(trustCard())
        addView(actionRow())
        state.execution.error?.let { addView(errorCard(it)) }
        addView(stagePanel())
    }

    private fun contextCard(): View = card(surfaceRaised, 16).apply {
        val inner = row(12)
        inner.setPadding(dp(14), dp(14), dp(14), dp(14))
        inner.addView(TextView(this@MainActivity).apply { text = "⌖"; setTextColor(cyan); textSize = 26f; gravity = Gravity.CENTER }, LinearLayout.LayoutParams(dp(36), dp(50)))
        inner.addView(column(3).apply {
            addView(label("THIS FLOW STARTS WHEN", muted))
            addView(TextView(this@MainActivity).apply { text = "you arrive at the office"; setTextColor(textColor); textSize = 15f; setTypeface(Typeface.DEFAULT, Typeface.BOLD) })
            addView(body("Weekdays · 8:00–10:00 AM · Work Wi-Fi detected · Fold opened"))
        }, LinearLayout.LayoutParams(0, -2, 1f))
        addView(inner)
    }

    private fun flowRail(): View = column(6).apply {
        state.steps.forEachIndexed { index, step ->
            val selected = state.selectedStepId == step.id
            val completed = step.state == "complete"
            val button = Button(this@MainActivity).apply {
                text = "${if (completed) "✓" else index + 1}   ${step.group}\n       ${step.title}\n       ${step.detail}${if (step.requiresApproval) "   · Ask first" else ""}"
                gravity = Gravity.CENTER_VERTICAL or Gravity.START
                setTextColor(if (selected) textColor else Color.rgb(215, 224, 235))
                textSize = 12f
                setPadding(dp(14), dp(9), dp(14), dp(9))
                background = rounded(if (selected) Color.rgb(25, 42, 72) else Color.TRANSPARENT, 14)
                setOnClickListener { state = FlowEngine.reduce(state, Event.SelectStep(step.id)); render() }
            }
            addView(button, LinearLayout.LayoutParams(-1, dp(88)))
        }
    }

    private fun detailCard(): View {
        val selected = state.steps.firstOrNull { it.id == state.selectedStepId } ?: state.steps.first()
        return card(surfaceRaised, 14).apply {
            setPadding(dp(14), dp(12), dp(14), dp(12))
            addView(column(3).apply {
                addView(label("${selected.group} · ${selected.risk} RISK", purple))
                addView(TextView(this@MainActivity).apply { text = selected.title; setTextColor(textColor); textSize = 14f; setTypeface(Typeface.DEFAULT, Typeface.BOLD) })
                addView(body(selected.detail))
            })
        }
    }

    private fun trustCard(): View = card(Color.rgb(19, 32, 24), 14).apply {
        val inner = column(6)
        inner.setPadding(dp(14), dp(12), dp(14), dp(12))
        inner.addView(row(10).apply {
            addView(TextView(this@MainActivity).apply { text = "✓"; setTextColor(lime); textSize = 19f })
            addView(column(2).apply {
                addView(TextView(this@MainActivity).apply { text = "Protected by design"; setTextColor(lime); textSize = 12f; setTypeface(Typeface.DEFAULT, Typeface.BOLD) })
                addView(body("Uses Wi-Fi, calendar, tasks, and focus settings"))
            }, LinearLayout.LayoutParams(0, -2, 1f))
            addView(Button(this@MainActivity).apply { text = if (state.trustOpen) "Hide" else "Details"; setTextColor(lime); textSize = 10f; background = transparent(); setOnClickListener { state = FlowEngine.reduce(state, Event.ToggleTrust); render() } })
        })
        if (state.trustOpen) inner.addView(body("Can use: Work Wi-Fi · Calendar · Tasks · Focus\nAlways asks: messages · file changes · sharing · purchases"))
        addView(inner)
    }

    private fun actionRow(): View = row(10).apply {
        setPadding(0, dp(14), 0, 0)
        addView(Button(this@MainActivity).apply {
            text = "▶  Preview workspace"
            setTextColor(Color.WHITE)
            background = rounded(blue, 12)
            setOnClickListener { startRun(false) }
        }, LinearLayout.LayoutParams(0, dp(50), 1f))
        addView(Button(this@MainActivity).apply {
            text = "Run on Fold"
            setTextColor(lime)
            background = rounded(Color.rgb(28, 43, 25), 12)
            setOnClickListener { startRun(true) }
        }, LinearLayout.LayoutParams(0, dp(50), 1f))
    }

    private fun stagePanel(): View = card(Color.rgb(13, 20, 30), 18).apply {
        val panel = column(10)
        panel.setPadding(dp(15), dp(16), dp(15), dp(16))
        panel.addView(row(8).apply {
            addView(column(2).apply { addView(label("STAGE PREVIEW", cyan)); addView(TextView(this@MainActivity).apply { text = postureLabel(); setTextColor(textColor); textSize = 21f; setTypeface(Typeface.DEFAULT, Typeface.BOLD) }) }, LinearLayout.LayoutParams(0, -2, 1f))
            addView(label(if (state.execution.mode == "live") "LIVE SIMULATION" else "PREVIEW ONLY", blue))
        })
        val tabs = HorizontalScrollView(this@MainActivity).apply { isHorizontalScrollBarEnabled = false }
        val tabRow = row(5)
        FoldPosture.values().forEach { posture -> tabRow.addView(Button(this@MainActivity).apply { text = posture.label; textSize = 11f; setTextColor(if (posture == state.posture) textColor else muted); background = rounded(if (posture == state.posture) Color.rgb(35, 57, 95) else surfaceRaised, 10); setPadding(0, 0, 0, 0); setOnClickListener { state = FlowEngine.reduce(state, Event.SelectPosture(posture)); render() } }, LinearLayout.LayoutParams(0, dp(45), 1f)) }
        tabs.addView(tabRow)
        panel.addView(tabs)
        val stage = FoldStageView(this@MainActivity).apply { posture = state.posture; active = state.execution.capsule != null }
        panel.addView(stage, LinearLayout.LayoutParams(-1, dp(280)))
        panel.addView(stageResult())
        if (state.execution.capsule != null) panel.addView(capsule())
        panel.addView(body(if (state.execution.mode == "live") "Simulation only · no device settings changed" else "Preview only · nothing changes on your device"))
        addView(panel)
    }

    private fun stageResult(): View = card(surfaceRaised, 12).apply {
        setPadding(dp(12), dp(10), dp(12), dp(10))
        addView(column(3).apply {
            addView(TextView(this@MainActivity).apply { text = if (state.execution.status == ExecutionStatus.COMPLETED) if (state.execution.mode == "live") "Work stage is active" else "Rehearsal complete" else "Your Fold will be ready with"; setTextColor(textColor); textSize = 12f; setTypeface(Typeface.DEFAULT, Typeface.BOLD) })
            addView(body("Calendar + Notes · Priorities visible · Work focus prepared"))
        })
    }

    private fun capsule(): View = card(Color.rgb(24, 35, 24), 12).apply {
        setPadding(dp(12), dp(10), dp(12), dp(10))
        addView(column(4).apply {
            addView(label("●  ACTIVE SCENE CAPSULE", lime))
            addView(TextView(this@MainActivity).apply { text = "Start Work"; setTextColor(textColor); textSize = 14f; setTypeface(Typeface.DEFAULT, Typeface.BOLD) })
            addView(body("Calendar · Notes · Work focus"))
            addView(row(8).apply {
                val paused = state.execution.status == ExecutionStatus.PAUSED
                addView(Button(this@MainActivity).apply { text = if (paused) "Resume" else "Pause"; setTextColor(lime); textSize = 10f; background = transparent(); setOnClickListener { state = FlowEngine.reduce(state, if (paused) Event.Resume else Event.Pause); render() } })
                addView(Button(this@MainActivity).apply { text = "Undo"; setTextColor(lime); textSize = 10f; background = transparent(); setOnClickListener { state = FlowEngine.reduce(state, Event.Undo); render() } })
            })
        })
    }

    private fun errorCard(message: String): View = card(Color.rgb(55, 27, 31), 12).apply { setPadding(dp(12), dp(10), dp(12), dp(10)); addView(row(8).apply { addView(body("Flow paused safely · $message"), LinearLayout.LayoutParams(0, -2, 1f)); addView(Button(this@MainActivity).apply { text = "Dismiss"; setTextColor(Color.rgb(255, 170, 170)); background = transparent(); setOnClickListener { state = FlowEngine.reduce(state, Event.DismissError); render() } }) }) }

    private fun startRun(live: Boolean) {
        runToken += 1
        val token = runToken
        state = FlowEngine.reduce(state, if (live) Event.StartLive else Event.StartPreview)
        render()
        repeat(state.steps.size + 1) { index ->
            handler.postDelayed({ if (token == runToken) { state = FlowEngine.reduce(state, Event.Advance); render() } }, (index + 1) * 550L)
        }
    }

    private fun postureLabel() = when (state.posture) {
        FoldPosture.COVER -> "Cover screen"
        FoldPosture.OPEN -> "Inner screen"
        FoldPosture.FLEX -> "Flex mode"
        FoldPosture.DEX -> "DeX display"
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()
    private fun statusBarHeight(): Int {
        val id = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (id > 0) resources.getDimensionPixelSize(id) else dp(24)
    }
    private fun column(spacing: Int) = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, dp(spacing), 0, dp(spacing)) }
    private fun row(spacing: Int) = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(spacing), 0, dp(spacing), 0) }
    private fun label(value: String, color: Int) = TextView(this).apply { text = value; setTextColor(color); textSize = 10f; setTypeface(Typeface.DEFAULT, Typeface.BOLD) }
    private fun title(value: String) = TextView(this).apply { text = value; setTextColor(textColor); textSize = 38f; setTypeface(Typeface.DEFAULT, Typeface.BOLD); setPadding(0, dp(10), 0, 0) }
    private fun body(value: String) = TextView(this).apply { text = value; setTextColor(muted); textSize = 11f; setPadding(0, dp(3), 0, dp(3)) }
    private fun sectionTitle(kicker: String, heading: String, count: String) = row(0).apply { setPadding(0, dp(25), 0, dp(10)); addView(column(2).apply { addView(label(kicker, muted)); addView(TextView(this@MainActivity).apply { text = heading; setTextColor(textColor); textSize = 18f; setTypeface(Typeface.DEFAULT, Typeface.BOLD) }) }, LinearLayout.LayoutParams(0, -2, 1f)); addView(label(count, muted)) }
    private fun card(color: Int, radius: Int) = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; background = rounded(color, radius); setPadding(0, 0, 0, 0); val params = LinearLayout.LayoutParams(-1, -2); params.topMargin = dp(12); layoutParams = params }
    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat() }
    private fun transparent() = GradientDrawable().apply { setColor(Color.TRANSPARENT) }
}
