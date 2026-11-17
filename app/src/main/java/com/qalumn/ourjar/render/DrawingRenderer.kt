package com.qalumn.ourjar.render

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import com.qalumn.ourjar.domain.Stroke
import kotlin.math.max

/** Draws hand-drawn notes. Used by the app and by the home screen widget, so both look the same. */
object DrawingRenderer {

    fun draw(canvas: Canvas, left: Float, top: Float, w: Float, h: Float, strokes: List<Stroke>) {
        val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

        for (s in strokes) {
            val pts = s.points
            if (pts.isEmpty()) continue
            val sw = max(s.width * w, 1.5f)
            if (pts.size == 1) {
                dot.color = s.color
                canvas.drawCircle(left + pts[0].x * w, top + pts[0].y * h, sw / 2f, dot)
                continue
            }
            line.color = s.color
            line.strokeWidth = sw
            val path = Path()
            path.moveTo(left + pts[0].x * w, top + pts[0].y * h)
            for (i in 1 until pts.size - 1) {
                val cx = left + pts[i].x * w
                val cy = top + pts[i].y * h
                val mx = left + (pts[i].x + pts[i + 1].x) / 2f * w
                val my = top + (pts[i].y + pts[i + 1].y) / 2f * h
                path.quadTo(cx, cy, mx, my)
            }
            val last = pts.last()
            path.lineTo(left + last.x * w, top + last.y * h)
            canvas.drawPath(path, line)
        }
    }
}
