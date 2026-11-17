package com.qalumn.ourjar.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.res.ResourcesCompat
import com.qalumn.ourjar.R
import com.qalumn.ourjar.data.MarbleItem
import com.qalumn.ourjar.domain.Mood
import kotlin.math.max
import kotlin.math.min

/**
 * Home screen widgets are drawn as pictures so they can use the app's own fonts and the exact same jar.
 * Widgets always use the light "paper" look, so they read well on any wallpaper and never go out of sync with the system theme.
 */
object CardRenderer {
    private const val PAPER = 0xFFFFFBF3.toInt()
    private const val CREAM = 0xFFFFF7EE.toInt()
    private const val LINE = 0xFFEFDDD0.toInt()
    private const val INK = 0xFF4A2D37.toInt()
    private const val INK_SOFT = 0xFF7D5C67.toInt()
    private const val ACCENT = 0xFFB5476A.toInt()
    private const val TAPE = 0xFFF2A9BB.toInt()

    private fun font(context: Context, res: Int): Typeface =
        ResourcesCompat.getFont(context, res) ?: Typeface.DEFAULT

    /** Keeps very large widgets from creating bitmaps the system will refuse. */
    private fun clampSize(w: Int, h: Int): Pair<Int, Int> {
        val maxPixels = 900_000f
        val px = w.toFloat() * h.toFloat()
        if (px <= maxPixels) return max(w, 64) to max(h, 64)
        val scale = kotlin.math.sqrt(maxPixels / px)
        return max((w * scale).toInt(), 64) to max((h * scale).toInt(), 64)
    }

    private fun card(canvas: Canvas, w: Float, h: Float, d: Float, fillColor: Int) {
        val r = 26f * d
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = fillColor }
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1.2f * d; color = LINE }
        val rect = RectF(0.6f * d, 0.6f * d, w - 0.6f * d, h - 0.6f * d)
        canvas.drawRoundRect(rect, r, r, fill)
        canvas.drawRoundRect(rect, r, r, stroke)
    }

    /** The latest love note, as handwriting on a piece of paper. */
    fun noteBitmap(
        context: Context,
        widthPx: Int,
        heightPx: Int,
        density: Float,
        from: String?,
        text: String?,
        whenText: String?,
        emptyMessage: String,
    ): Bitmap {
        val (w, h) = clampSize(widthPx, heightPx)
        val d = density * (w.toFloat() / max(widthPx, 1))
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        card(canvas, w.toFloat(), h.toFloat(), d, PAPER)

        // Tape
        canvas.save()
        canvas.rotate(-3f, w / 2f, 12f * d)
        val tape = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = TAPE; alpha = 215 }
        canvas.drawRoundRect(RectF(w / 2f - 34f * d, 3f * d, w / 2f + 34f * d, 21f * d), 3f * d, 3f * d, tape)
        canvas.restore()

        val pad = 20f * d
        val hand = font(context, R.font.caveat_semibold)
        val sans = font(context, R.font.dmsans_medium)

        if (text == null) {
            val p = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = hand; color = INK_SOFT; textSize = 22f * d }
            val layout = StaticLayout.Builder.obtain(emptyMessage, 0, emptyMessage.length, p, (w - 2 * pad).toInt().coerceAtLeast(1))
                .setAlignment(Layout.Alignment.ALIGN_CENTER).build()
            canvas.save()
            canvas.translate(pad, (h - layout.height) / 2f + 4f * d)
            layout.draw(canvas)
            canvas.restore()
            return bmp
        }

        // Header
        val headerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = sans; color = INK_SOFT; textSize = 11f * d; letterSpacing = 0.12f
        }
        val header = "FROM " + (from ?: "").uppercase()
        val headerBaseline = 40f * d
        canvas.drawText(header, pad, headerBaseline, headerPaint)

        // Footer
        val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = sans; color = ACCENT; textSize = 11.5f * d }
        val footerBaseline = h - 16f * d
        if (whenText != null) canvas.drawText(whenText, pad, footerBaseline, footerPaint)

        // Body: shrink the handwriting until it fits
        val bodyTop = headerBaseline + 10f * d
        val bodyBottom = footerBaseline - 20f * d
        val avail = max(bodyBottom - bodyTop, 20f * d)
        val width = (w - 2 * pad).toInt().coerceAtLeast(1)
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = hand; color = INK }
        var layout: StaticLayout? = null
        var size = 34f
        while (size >= 16f) {
            paint.textSize = size * d
            val l = StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
                .setLineSpacing(0f, 0.95f).build()
            if (l.height <= avail) { layout = l; break }
            size -= 2f
        }
        if (layout == null) {
            paint.textSize = 16f * d
            val probe = StaticLayout.Builder.obtain("Ag", 0, 2, paint, width).build()
            val lines = max((avail / max(probe.height, 1)).toInt(), 1)
            layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
                .setMaxLines(lines).setEllipsize(TextUtils.TruncateAt.END).setLineSpacing(0f, 0.95f).build()
        }
        canvas.save()
        canvas.translate(pad, bodyTop + max((avail - layout.height) / 2f, 0f))
        layout.draw(canvas)
        canvas.restore()
        return bmp
    }

    /** Both mood jars side by side: yours on the left, your partner's on the right. */
    fun jarBitmap(
        context: Context,
        widthPx: Int,
        heightPx: Int,
        density: Float,
        myName: String,
        mine: List<MarbleItem>,
        partnerName: String,
        theirs: List<MarbleItem>,
    ): Bitmap {
        val (w, h) = clampSize(widthPx, heightPx)
        val d = density * (w.toFloat() / max(widthPx, 1))
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        card(canvas, w.toFloat(), h.toFloat(), d, CREAM)

        val display = font(context, R.font.fraunces_semibold)
        val sans = font(context, R.font.dmsans_medium)
        val name = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = display; color = INK; textSize = 13.5f * d }
        val sub = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = sans; color = INK_SOFT; textSize = 11f * d }

        val inset = 12f * d
        val gap = 8f * d
        val captionH = 36f * d
        val colW = (w - 2 * inset - gap) / 2f
        val jarH = (h - 2 * inset - captionH).coerceAtLeast(10f)

        val columns = listOf(
            Triple(myName, JarScene.from(mine), mine.size),
            Triple(partnerName, JarScene.from(theirs), theirs.size),
        )
        columns.forEachIndexed { i, (who, scene, count) ->
            val left = inset + i * (colW + gap)
            canvas.save()
            canvas.translate(left, inset)
            canvas.clipRect(0f, 0f, colW, jarH)
            JarRenderer.draw(canvas, colW, jarH, scene, JarPalettes.Light)
            canvas.restore()

            val label = TextUtils.ellipsize(who, name, colW, TextUtils.TruncateAt.END).toString()
            canvas.drawText(label, left + (colW - name.measureText(label)) / 2f, inset + jarH + 16f * d, name)
            val countText = if (count == 1) "1 marble" else "$count marbles"
            canvas.drawText(countText, left + (colW - sub.measureText(countText)) / 2f, inset + jarH + 30f * d, sub)
        }
        return bmp
    }
}
