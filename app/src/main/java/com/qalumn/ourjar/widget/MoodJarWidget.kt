package com.qalumn.ourjar.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import com.qalumn.ourjar.MainActivity
import com.qalumn.ourjar.data.MarbleItem
import com.qalumn.ourjar.data.Prefs
import com.qalumn.ourjar.domain.TimeFmt
import com.qalumn.ourjar.render.CardRenderer
import java.time.LocalDate

/** This week's mood jar. Tap to open the jar and add a marble. */
class MoodJarWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val weekStart = TimeFmt.startOfWeekMillis()
        val marbles = Prefs.loadMarbles()
            .filter { it.createdAt >= weekStart }
            .map { MarbleItem(it.id, "", "", it.mood, "", it.createdAt) }
        provideContent { JarWidgetContent(context, marbles) }
    }
}

@Composable
private fun JarWidgetContent(context: Context, marbles: List<MarbleItem>) {
    val size = LocalSize.current
    val density = context.resources.displayMetrics.density
    val widthPx = (size.width.value * density).toInt().coerceAtLeast(1)
    val heightPx = (size.height.value * density).toInt().coerceAtLeast(1)

    val bitmap = remember(size, marbles) {
        CardRenderer.jarBitmap(
            context = context,
            widthPx = widthPx,
            heightPx = heightPx,
            density = density,
            month = "This week",
            marbles = marbles,
        )
    }

    val intent = Intent(context, MainActivity::class.java).apply {
        action = "com.qalumn.ourjar.OPEN_JAR"
        data = Uri.parse("ourjar://tab/1")
        putExtra(MainActivity.EXTRA_TAB, 1)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

    Image(
        provider = ImageProvider(bitmap),
        contentDescription = "Mood jar with ${marbles.size} marbles this week",
        contentScale = ContentScale.Fit,
        modifier = GlanceModifier.fillMaxSize().clickable(actionStartActivity(intent)),
    )
}

class MoodJarReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MoodJarWidget()

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        WidgetSync.enqueueNow(context)
    }
}
