package com.qalumn.ourjar.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import com.qalumn.ourjar.render.JarMarble
import com.qalumn.ourjar.render.JarPalettes
import com.qalumn.ourjar.render.JarRenderer
import com.qalumn.ourjar.ui.theme.Ours

private val BounceOut = Easing { x ->
    val n1 = 7.5625f
    val d1 = 2.75f
    when {
        x < 1f / d1 -> n1 * x * x
        x < 2f / d1 -> { val t = x - 1.5f / d1; n1 * t * t + 0.75f }
        x < 2.5f / d1 -> { val t = x - 2.25f / d1; n1 * t * t + 0.9375f }
        else -> { val t = x - 2.625f / d1; n1 * t * t + 0.984375f }
    }
}

/**
 * The jar. When a new marble shows up (yours or your partner's) it drops in and bounces once.
 * [newestId] is the id of the newest marble; a change after first composition triggers the drop.
 */
@Composable
fun JarView(marbles: List<JarMarble>, newestId: String?, modifier: Modifier = Modifier) {
    val palette = if (Ours.colors.isDark) JarPalettes.Dark else JarPalettes.Light
    val progress = remember { Animatable(1f) }
    var seenId by remember { mutableStateOf(newestId) }

    LaunchedEffect(newestId) {
        if (newestId != null && seenId != null && newestId != seenId) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(durationMillis = 900, easing = BounceOut))
        }
        seenId = newestId
    }

    Canvas(modifier) {
        val p = progress.value
        val last = marbles.lastOrNull()
        val dropY = if (p < 1f && last != null) {
            val start = -0.12f
            start + (last.pos.y - start) * p
        } else null
        drawIntoCanvas { canvas ->
            JarRenderer.draw(canvas.nativeCanvas, size.width, size.height, marbles, palette, dropY)
        }
    }
}
