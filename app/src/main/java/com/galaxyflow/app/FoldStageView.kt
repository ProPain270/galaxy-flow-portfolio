package com.galaxyflow.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import kotlin.math.min

class FoldStageView(context: Context) : View(context) {
    private val devicePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val screenPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = android.graphics.Typeface.create("sans", android.graphics.Typeface.NORMAL) }
    var posture: FoldPosture = FoldPosture.OPEN
        set(value) { field = value; invalidate() }
    var active: Boolean = false
        set(value) { field = value; invalidate() }

    init { contentDescription = "Fold workspace preview" }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2f
        val cy = h / 2f
        val deviceW = min(w * if (posture == FoldPosture.COVER) .52f else .9f, 560f)
        val deviceH = deviceW * .66f
        val left = cx - deviceW / 2f
        val top = cy - deviceH / 2f
        devicePaint.color = Color.rgb(25, 34, 47)
        canvas.drawRoundRect(RectF(left, top, left + deviceW, top + deviceH), 26f, 26f, devicePaint)

        if (posture == FoldPosture.COVER) {
            drawScreen(canvas, RectF(left + 16f, top + 14f, left + deviceW - 16f, top + deviceH - 14f), "8:42", "Work workspace ready", false)
        } else {
            val gap = 3f
            val innerLeft = left + 12f
            val innerTop = top + 12f
            val innerBottom = top + deviceH - 12f
            val panelW = (deviceW - 27f) / 2f
            drawScreen(canvas, RectF(innerLeft, innerTop, innerLeft + panelW, innerBottom), "Good morning", "Your day, in focus.", false)
            drawScreen(canvas, RectF(innerLeft + panelW + gap, innerTop, innerLeft + panelW * 2 + gap, innerBottom), "Notes", "Launch brief.", true)
            devicePaint.color = Color.rgb(74, 87, 103)
            canvas.drawRect(cx - 1.5f, innerTop, cx + 1.5f, innerBottom, devicePaint)
        }

        if (active) {
            devicePaint.color = Color.argb(95, 201, 243, 109)
            canvas.drawRoundRect(RectF(left - 4f, top - 4f, left + deviceW + 4f, top + deviceH + 4f), 30f, 30f, devicePaint)
        }
    }

    private fun drawScreen(canvas: Canvas, rect: RectF, title: String, subtitle: String, purple: Boolean) {
        screenPaint.color = Color.rgb(244, 248, 253)
        canvas.drawRoundRect(rect, 18f, 18f, screenPaint)
        textPaint.color = Color.rgb(69, 121, 218)
        textPaint.textSize = 11f
        canvas.drawText(if (purple) "CONTINUE HERE" else "GOOD MORNING", rect.left + 14f, rect.top + 34f, textPaint)
        textPaint.color = Color.rgb(29, 51, 77)
        textPaint.textSize = 23f
        canvas.drawText(title, rect.left + 14f, rect.top + 70f, textPaint)
        textPaint.color = Color.rgb(91, 112, 135)
        textPaint.textSize = 11f
        canvas.drawText(subtitle, rect.left + 14f, rect.top + 91f, textPaint)
        screenPaint.color = if (purple) Color.rgb(242, 239, 255) else Color.rgb(234, 248, 241)
        val card = RectF(rect.left + 13f, rect.top + 112f, rect.right - 13f, rect.top + 170f)
        canvas.drawRoundRect(card, 13f, 13f, screenPaint)
        textPaint.color = Color.rgb(96, 120, 145)
        textPaint.textSize = 9f
        canvas.drawText(if (purple) "WORKSPACE LINK" else "NEXT UP", card.left + 10f, card.top + 20f, textPaint)
        textPaint.color = Color.rgb(29, 51, 77)
        textPaint.textSize = 12f
        canvas.drawText(if (purple) "Calendar + Notes connected" else "Client planning", card.left + 10f, card.top + 39f, textPaint)
        textPaint.color = Color.rgb(111, 132, 153)
        textPaint.textSize = 9f
        canvas.drawText(if (purple) "Opened by Galaxy Flow" else "10:00 AM · 45 min", card.left + 10f, card.top + 53f, textPaint)
    }
}
