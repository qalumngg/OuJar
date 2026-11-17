package com.qalumn.ourjar.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.unit.dp
import com.qalumn.ourjar.domain.DrawingCodec
import com.qalumn.ourjar.domain.Pt
import com.qalumn.ourjar.domain.Stroke
import com.qalumn.ourjar.render.DrawingRenderer
import com.qalumn.ourjar.ui.theme.Ours
import kotlin.math.abs

/** Drawings are always on cream paper in dark ink colours, in both light and dark mode. */
val PaperColor = Color(0xFFFFFBF3)
private val PaperLine = Color(0xFFEFDDD0)

val PenColors = listOf(
    0xFF4A2D37.toInt(), // plum ink
    0xFFD4688A.toInt(), // rose
    0xFFE07A7A.toInt(), // coral
    0xFFF0A93A.toInt(), // honey
    0xFF6BBF9F.toInt(), // mint
    0xFF6FA8DC.toInt(), // sky
    0xFF9A8AD6.toInt(), // lavender
)

/** Pen widths as a fraction of the paper width: fine, medium, thick. */
val PenWidths = listOf(0.005f, 0.011f, 0.022f)

/** A finished drawing, shown on paper. */
@Composable
fun DrawingView(drawing: String, modifier: Modifier = Modifier) {
    val strokes = remember(drawing) { DrawingCodec.decode(drawing) }
    Canvas(
        modifier
            .fillMaxWidth()
            .aspectRatio(DrawingCodec.ASPECT)
            .clip(RoundedCornerShape(16.dp))
            .background(PaperColor)
            .border(BorderStroke(1.dp, PaperLine), RoundedCornerShape(16.dp)),
    ) {
        drawIntoCanvas { c -> DrawingRenderer.draw(c.nativeCanvas, 0f, 0f, size.width, size.height, strokes) }
    }
}

/** The paper you draw on with a finger or stylus. [strokes] collects the finished strokes. */
@Composable
fun DrawingPad(
    strokes: SnapshotStateList<Stroke>,
    penColor: Int,
    penWidth: Float,
    modifier: Modifier = Modifier,
) {
    val current = remember { mutableStateListOf<Pt>() }

    Canvas(
        modifier
            .fillMaxWidth()
            .aspectRatio(DrawingCodec.ASPECT)
            .clip(RoundedCornerShape(16.dp))
            .background(PaperColor)
            .border(BorderStroke(1.dp, PaperLine), RoundedCornerShape(16.dp))
            .pointerInput(penColor, penWidth) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    val w = size.width.toFloat().coerceAtLeast(1f)
                    val h = size.height.toFloat().coerceAtLeast(1f)
                    current.clear()
                    current.add(Pt((down.position.x / w).coerceIn(0f, 1f), (down.position.y / h).coerceIn(0f, 1f)))
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (change.positionChanged()) {
                            val p = Pt((change.position.x / w).coerceIn(0f, 1f), (change.position.y / h).coerceIn(0f, 1f))
                            val last = current.last()
                            if (abs(p.x - last.x) + abs(p.y - last.y) > 0.004f) current.add(p)
                            change.consume()
                        }
                        if (change.changedToUpIgnoreConsumed() || !change.pressed) break
                    }
                    if (current.isNotEmpty()) strokes.add(Stroke(penColor, penWidth, current.toList()))
                    current.clear()
                }
            },
    ) {
        val live = if (current.isEmpty()) strokes.toList() else strokes.toList() + Stroke(penColor, penWidth, current.toList())
        drawIntoCanvas { c -> DrawingRenderer.draw(c.nativeCanvas, 0f, 0f, size.width, size.height, live) }
    }
}

/** Row of colour dots to pick a pen colour. */
@Composable
fun PenColorRow(selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val c = Ours.colors
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        PenColors.forEach { col ->
            val isSelected = col == selected
            Box(
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .clickable { onSelect(col) },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(if (isSelected) 30.dp else 26.dp)
                        .background(Color(col), CircleShape)
                        .then(if (isSelected) Modifier.border(2.5.dp, c.card, CircleShape).border(1.5.dp, c.ink, CircleShape) else Modifier),
                )
            }
        }
    }
}

/** Fine, medium and thick pen buttons. */
@Composable
fun PenWidthRow(selected: Float, penColor: Int, onSelect: (Float) -> Unit) {
    val c = Ours.colors
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        PenWidths.forEachIndexed { i, wFrac ->
            val isSelected = wFrac == selected
            val dot = (6 + i * 5).dp
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) c.primaryContainer else c.card, CircleShape)
                    .border(1.dp, if (isSelected) c.accent else c.line, CircleShape)
                    .clickable { onSelect(wFrac) },
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(dot).background(Color(penColor), CircleShape))
            }
        }
    }
}
