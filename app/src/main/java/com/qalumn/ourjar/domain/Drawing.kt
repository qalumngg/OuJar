package com.qalumn.ourjar.domain

import kotlin.math.roundToInt

/** A point on the drawing paper. Both x and y run from 0 to 1 across the paper, which is always 4:3. */
data class Pt(val x: Float, val y: Float)

/** One pen stroke. [width] is a fraction of the paper width. [color] is opaque ARGB. */
class Stroke(val color: Int, val width: Float, val points: List<Pt>)

/**
 * Drawings are saved as plain text inside the note, so no extra storage service is needed.
 * Format: strokes separated by "|", each stroke "rrggbb,width,x:y;x:y;..." with whole numbers
 * (width in 1/10000 of the paper width, coordinates in 1/1000).
 */
object DrawingCodec {
    /** Firestore allows about 1 MB per note; stay well under that. */
    const val MAX_CHARS = 400_000

    const val ASPECT = 4f / 3f

    fun encode(strokes: List<Stroke>): String = strokes.filter { it.points.isNotEmpty() }.joinToString("|") { s ->
        val rgb = String.format("%06x", s.color and 0xFFFFFF)
        val w = (s.width * 10000f).roundToInt()
        val pts = s.points.joinToString(";") {
            "${(it.x * 1000f).roundToInt().coerceIn(0, 1000)}:${(it.y * 1000f).roundToInt().coerceIn(0, 1000)}"
        }
        "$rgb,$w,$pts"
    }

    fun decode(raw: String): List<Stroke> {
        if (raw.isBlank()) return emptyList()
        return try {
            raw.split("|").mapNotNull { part ->
                val bits = part.split(",", limit = 3)
                if (bits.size < 3) return@mapNotNull null
                val color = (0xFF000000L or bits[0].toLong(16)).toInt()
                val width = bits[1].toInt() / 10000f
                val pts = bits[2].split(";").mapNotNull { p ->
                    val xy = p.split(":")
                    if (xy.size != 2) null else Pt(xy[0].toInt() / 1000f, xy[1].toInt() / 1000f)
                }
                if (pts.isEmpty()) null else Stroke(color, width, pts)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
