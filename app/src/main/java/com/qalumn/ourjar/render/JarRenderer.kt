package com.qalumn.ourjar.render

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import com.qalumn.ourjar.data.MarbleItem
import com.qalumn.ourjar.domain.JarGeometry
import com.qalumn.ourjar.domain.JarLayout
import com.qalumn.ourjar.domain.MarblePos
import kotlin.math.min

class JarPalette(
    val glassStroke: Int,
    val glassFill: Int,
    val lid: Int,
    val lidShade: Int,
    val shine: Int,
)

object JarPalettes {
    val Light = JarPalette(
        glassStroke = 0xFFD9C3B8.toInt(),
        glassFill = 0x66FFFFFF,
        lid = 0xFFF2A9BB.toInt(),
        lidShade = 0xFFD98BA0.toInt(),
        shine = 0xCCFFFFFF.toInt(),
    )
    val Dark = JarPalette(
        glassStroke = 0xFF6F5963.toInt(),
        glassFill = 0x14FFFFFF,
        lid = 0xFFEFB0C1.toInt(),
        lidShade = 0xFFC98CA0.toInt(),
        shine = 0x55FFFFFF,
    )
}

class JarMarble(val color: Int, val pos: MarblePos, val seed: Int)

/** Turns marble records into positioned, coloured marbles. Oldest first; only the newest fit in the jar. */
object JarScene {
    fun from(items: List<MarbleItem>): List<JarMarble> {
        val visible = items.sortedBy { it.createdAt }.takeLast(JarGeometry.CAPACITY)
        val positions = JarLayout.settle(visible.map { it.id })
        return visible.mapIndexed { i, m -> JarMarble(m.mood.argb, positions[i], JarLayout.hash(m.id)) }
    }
}

/**
 * Draws the jar with plain android.graphics so the app screen and the home screen
 * widget are pixel-for-pixel the same picture.
 */
object JarRenderer {

    private fun mix(a: Int, b: Int, t: Float): Int {
        val ar = (a shr 16) and 0xFF; val ag = (a shr 8) and 0xFF; val ab = a and 0xFF
        val br = (b shr 16) and 0xFF; val bg = (b shr 8) and 0xFF; val bb = b and 0xFF
        val r = (ar + (br - ar) * t).toInt()
        val g = (ag + (bg - ag) * t).toInt()
        val bl = (ab + (bb - ab) * t).toInt()
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or bl
    }

    private fun alpha(color: Int, a: Int): Int = (a shl 24) or (color and 0xFFFFFF)

    private fun bodyPath(): Path {
        val g = JarGeometry
        return Path().apply {
            moveTo(g.NECK_LEFT, g.LID_BOTTOM)
            lineTo(g.NECK_LEFT, g.NECK_BOTTOM)
            cubicTo(g.NECK_LEFT, 0.27f, g.BODY_LEFT, 0.25f, g.BODY_LEFT, g.BODY_SHOULDER)
            lineTo(g.BODY_LEFT, g.BODY_BOTTOM - g.CORNER)
            arcTo(RectF(g.BODY_LEFT, g.BODY_BOTTOM - 2 * g.CORNER, g.BODY_LEFT + 2 * g.CORNER, g.BODY_BOTTOM), 180f, -90f)
            lineTo(g.BODY_RIGHT - g.CORNER, g.BODY_BOTTOM)
            arcTo(RectF(g.BODY_RIGHT - 2 * g.CORNER, g.BODY_BOTTOM - 2 * g.CORNER, g.BODY_RIGHT, g.BODY_BOTTOM), 90f, -90f)
            lineTo(g.BODY_RIGHT, g.BODY_SHOULDER)
            cubicTo(g.BODY_RIGHT, 0.25f, g.NECK_RIGHT, 0.27f, g.NECK_RIGHT, g.NECK_BOTTOM)
            lineTo(g.NECK_RIGHT, g.LID_BOTTOM)
        }
    }

    /**
     * @param dropY if set, the newest marble is drawn at this height (jar units) instead of its resting spot,
     * which is how the "drop into the jar" animation works.
     */
    fun draw(
        canvas: Canvas,
        w: Float,
        h: Float,
        marbles: List<JarMarble>,
        palette: JarPalette,
        dropY: Float? = null,
    ) {
        val g = JarGeometry
        val u = min(w, h / g.HEIGHT)
        val ox = (w - u) / 2f
        val oy = (h - u * g.HEIGHT) / 2f

        canvas.save()
        canvas.translate(ox, oy)
        canvas.scale(u, u)

        val body = bodyPath()

        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = palette.glassFill }
        canvas.drawPath(body, fill)

        // Marbles
        val marblePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
        val swirl = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND
        }
        val spec = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = 0xCCFFFFFF.toInt() }

        marbles.forEachIndexed { i, m ->
            val rr = m.pos.r * 0.95f
            val cx = m.pos.x
            val cy = if (dropY != null && i == marbles.lastIndex) dropY else m.pos.y
            val light = mix(m.color, 0xFFFFFFFF.toInt(), 0.55f)
            val dark = mix(m.color, 0xFF000000.toInt(), 0.17f)
            marblePaint.shader = RadialGradient(
                cx - rr * 0.35f, cy - rr * 0.4f, rr * 1.55f,
                intArrayOf(light, m.color, dark), floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(cx, cy, rr, marblePaint)

            swirl.color = alpha(mix(m.color, 0xFFFFFFFF.toInt(), 0.65f), 120)
            swirl.strokeWidth = rr * 0.17f
            val a = (m.seed and 0xFF) * 360f / 256f
            canvas.drawArc(RectF(cx - rr * 0.62f, cy - rr * 0.62f, cx + rr * 0.62f, cy + rr * 0.62f), a, 105f, false, swirl)

            ring.strokeWidth = rr * 0.07f
            ring.color = alpha(dark, 110)
            canvas.drawCircle(cx, cy, rr * 0.97f, ring)

            canvas.drawOval(RectF(cx - rr * 0.55f, cy - rr * 0.66f, cx - rr * 0.17f, cy - rr * 0.32f), spec)
        }

        // Glass
        val glass = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = g.WALL
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            color = palette.glassStroke
        }
        canvas.drawPath(body, glass)

        val shine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 0.024f
            strokeCap = Paint.Cap.ROUND
            color = palette.shine
        }
        val sx = g.BODY_LEFT + 0.055f
        canvas.drawLine(sx, g.BODY_SHOULDER + 0.07f, sx, g.BODY_BOTTOM - 0.30f, shine)
        canvas.drawLine(sx, g.BODY_BOTTOM - 0.24f, sx, g.BODY_BOTTOM - 0.22f, shine)

        // Lid
        val lid = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = palette.lid }
        val lidRect = RectF(g.LID_LEFT, g.LID_TOP, g.LID_RIGHT, g.LID_BOTTOM)
        canvas.drawRoundRect(lidRect, 0.03f, 0.03f, lid)
        val shade = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = palette.lidShade }
        canvas.drawRoundRect(RectF(g.LID_LEFT + 0.012f, g.LID_BOTTOM - 0.028f, g.LID_RIGHT - 0.012f, g.LID_BOTTOM - 0.008f), 0.01f, 0.01f, shade)

        canvas.restore()
    }
}
