package com.qalumn.ourjar.domain

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Jar geometry in "jar units": the jar is 1.0 wide and [HEIGHT] tall.
 * Both the in-app jar and the home screen widget use these numbers, so they look identical.
 */
object JarGeometry {
    const val HEIGHT = 1.12f

    const val LID_LEFT = 0.20f
    const val LID_RIGHT = 0.80f
    const val LID_TOP = 0.0f
    const val LID_BOTTOM = 0.095f

    const val NECK_LEFT = 0.26f
    const val NECK_RIGHT = 0.74f
    const val NECK_BOTTOM = 0.19f

    const val BODY_LEFT = 0.07f
    const val BODY_RIGHT = 0.93f
    const val BODY_SHOULDER = 0.36f
    const val BODY_BOTTOM = 1.10f
    const val CORNER = 0.14f

    /** Glass wall thickness (stroke width). */
    const val WALL = 0.02f

    /** Marbles sit against the inside of the glass. */
    const val INNER_LEFT = BODY_LEFT + WALL
    const val INNER_RIGHT = BODY_RIGHT - WALL
    const val INNER_FLOOR = BODY_BOTTOM - WALL
    const val INNER_CORNER = CORNER - WALL

    /** Seven marbles fit across the bottom of the jar. */
    const val COLUMNS = 7
    const val MARBLE_RADIUS = (INNER_RIGHT - INNER_LEFT) / (2f * COLUMNS)

    /** The jar holds this many marbles before the oldest roll out of view. */
    const val CAPACITY = 30
}

data class MarblePos(val x: Float, val y: Float)

object JarLayout {

    /** Small stable hash so the same marble always lands in the same place on every device. */
    fun hash(s: String): Int {
        var h = 0x811C9DC5.toInt()
        for (c in s) {
            h = h xor c.code
            h *= 0x01000193
        }
        return h
    }

    private val r = JarGeometry.MARBLE_RADIUS
    private val minX = JarGeometry.INNER_LEFT + r
    private val maxX = JarGeometry.INNER_RIGHT - r

    /** Lowest centre height (largest y) a marble can have at horizontal position x, ignoring other marbles. */
    fun floorCenterY(x: Float): Float {
        val ri = JarGeometry.INNER_CORNER
        val rc = ri - r
        val cxL = JarGeometry.INNER_LEFT + ri
        val cxR = JarGeometry.INNER_RIGHT - ri
        val cy = JarGeometry.INNER_FLOOR - ri
        return when {
            x < cxL -> {
                val dx = cxL - x
                cy + sqrt(max(rc * rc - dx * dx, 0f))
            }
            x > cxR -> {
                val dx = x - cxR
                cy + sqrt(max(rc * rc - dx * dx, 0f))
            }
            else -> JarGeometry.INNER_FLOOR - r
        }
    }

    private fun restY(x: Float, placed: List<MarblePos>): Float {
        var y = floorCenterY(x)
        val twoR = 2f * r
        for (p in placed) {
            val dx = abs(p.x - x)
            if (dx < twoR) {
                val cand = p.y - sqrt(twoR * twoR - dx * dx)
                if (cand < y) y = cand
            }
        }
        return y
    }

    /**
     * Drops marbles into the jar one at a time, oldest first, and lets each one roll
     * into the nearest gap. Deterministic: the same ids always give the same pile.
     */
    fun settle(ids: List<String>): List<MarblePos> {
        val placed = ArrayList<MarblePos>(ids.size)
        val step = r * 0.2f
        for (id in ids) {
            val frac = ((hash(id) ushr 8) and 0xFFFF) / 65535f
            var x = minX + frac * (maxX - minX)
            var y = restY(x, placed)
            for (i in 0 until 80) {
                val xa = max(minX, x - step)
                val xb = min(maxX, x + step)
                val ya = restY(xa, placed)
                val yb = restY(xb, placed)
                if (ya > y + 1e-4f && ya >= yb) {
                    x = xa; y = ya
                } else if (yb > y + 1e-4f) {
                    x = xb; y = yb
                } else {
                    break
                }
            }
            placed.add(MarblePos(x, y))
        }
        return placed
    }
}
