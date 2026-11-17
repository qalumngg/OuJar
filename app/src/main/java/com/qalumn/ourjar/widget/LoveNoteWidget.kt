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
import com.qalumn.ourjar.data.Note
import com.qalumn.ourjar.data.Prefs
import com.qalumn.ourjar.domain.TimeFmt
import com.qalumn.ourjar.render.CardRenderer

/** Shows the newest love note from your partner. Tap to open the app. */
class LoveNoteWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val session = Prefs.session()
        val note = if (session == null) null else Prefs.loadNotes().firstOrNull { it.fromId != session.memberId }
        val partner = Prefs.partnerName
        val signedIn = session != null
        provideContent { NoteWidgetContent(context, note, partner, signedIn) }
    }
}

@Composable
private fun NoteWidgetContent(context: Context, note: Note?, partner: String, signedIn: Boolean) {
    val size = LocalSize.current
    val density = context.resources.displayMetrics.density
    val widthPx = (size.width.value * density).toInt().coerceAtLeast(1)
    val heightPx = (size.height.value * density).toInt().coerceAtLeast(1)

    val bitmap = remember(size, note, partner, signedIn) {
        CardRenderer.noteBitmap(
            context = context,
            widthPx = widthPx,
            heightPx = heightPx,
            density = density,
            from = note?.fromName,
            text = note?.text,
            whenText = note?.let { TimeFmt.relative(it.createdAt) },
            emptyMessage = when {
                !signedIn -> "Open Our Jar to get started"
                partner.isNotBlank() -> "No notes from $partner yet. Tap to write one."
                else -> "No notes yet. Tap to write one."
            },
        )
    }

    val intent = Intent(context, MainActivity::class.java).apply {
        action = "com.qalumn.ourjar.OPEN_NOTES"
        data = Uri.parse("ourjar://tab/0")
        putExtra(MainActivity.EXTRA_TAB, 0)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

    Image(
        provider = ImageProvider(bitmap),
        contentDescription = if (note != null) "Love note from ${note.fromName}: ${note.text}" else "Love note",
        contentScale = ContentScale.Fit,
        modifier = GlanceModifier.fillMaxSize().clickable(actionStartActivity(intent)),
    )
}

class LoveNoteReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LoveNoteWidget()

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        WidgetSync.enqueueNow(context)
    }
}
