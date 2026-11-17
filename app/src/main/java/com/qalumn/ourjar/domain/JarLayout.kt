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

data class MarblePos(val x: Float, val y: Float, val r: Float)

object JarLayout {

    /** Small stable hash so the same marble always lands in the same place on every device. */
    fun hash(s: String): Int {
        var h = 0x811C9DC5.toInt()
        for (c in s) {
            h = h xor c.code
            h *= 0x01000193
        }
        // Extra mixing so neighbouring ids give very different numbers.
        h = h xor (h ushr 15)
        h *= 0x2C1B3C6D
        h = h xor (h ushr 12)
        return h
    }

    private val base = JarGeometry.MARBLE_RADIUS

    /** Marbles are not all the same size: a little bigger or smaller, decided by the marble's id. */
    fun radiusFor(id: String): Float {
        val f = ((hash(id) ushr 4) and 0xFF) / 255f
        return base * (0.86f + 0.24f * f)
    }

    /** Lowest centre height (largest y) a marble of radius [r] can have at horizontal position x, ignoring other marbles. */
    fun floorCenterY(x: Float, r: Float): Float {
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

    private fun restY(x: Float, r: Float, placed: List<MarblePos>): Float {
        var y = floorCenterY(x, r)
        for (p in placed) {
            val dx = abs(p.x - x)
            val reach = r + p.r
            if (dx < reach) {
                val cand = p.y - sqrt(reach * reach - dx * dx)
                if (cand < y) y = cand
            }
        }
        return y
    }

    /**
     * Drops marbles into the jar one at a time, oldest first. Each marble has its own size, falls from its
     * own spot, and rolls a different distance and direction, so the pile looks natural rather than stacked.
     * Deterministic: the same ids always give the same pile.
     */
    fun settle(ids: List<String>): List<MarblePos> {
        val placed = ArrayList<MarblePos>(ids.size)
        for (id in ids) {
            val h = hash(id)
            val r = radiusFor(id)
            val minX = JarGeometry.INNER_LEFT + r
            val maxX = JarGeometry.INNER_RIGHT - r
            val frac = ((h ushr 8) and 0xFFFF) / 65535f
            var x = minX + frac * (maxX - minX)
            var y = restY(x, r, placed)
            val step = r * 0.22f
            val rolls = 6 + ((h ushr 20) and 0x3F)
            for (i in 0 until rolls) {
                val xa = max(minX, x - step)
                val xb = min(maxX, x + step)
                val ya = restY(xa, r, placed)
                val yb = restY(xb, r, placed)
                val downLeft = ya > y + 1e-4f
                val downRight = yb > y + 1e-4f
                if (!downLeft && !downRight) break
                val goLeft = when {
                    downLeft && downRight -> ((h ushr (i % 20)) and 1) == 0
                    else -> downLeft
                }
                if (goLeft) { x = xa; y = ya } else { x = xb; y = yb }
            }
            placed.add(MarblePos(x, y, r))
        }
        return placed
    }
}
