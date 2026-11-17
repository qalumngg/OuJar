@file:OptIn(ExperimentalMaterial3Api::class)

package com.qalumn.ourjar.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.qalumn.ourjar.data.MarbleItem
import com.qalumn.ourjar.data.Repo
import com.qalumn.ourjar.domain.JarGeometry
import com.qalumn.ourjar.domain.Mood
import com.qalumn.ourjar.domain.TimeFmt
import com.qalumn.ourjar.render.JarScene
import com.qalumn.ourjar.ui.theme.Ours
import java.time.LocalDate

@Composable
fun JarScreen() {
    val session by Repo.session.collectAsState()
    val me = session ?: return
    val all by Repo.marbles.collectAsState()
    var adding by remember { mutableStateOf(false) }

    val today = LocalDate.now()
    val weekStart = TimeFmt.startOfWeekMillis(today)
    val month = remember(all, weekStart) { all.filter { it.createdAt >= weekStart } }
    val scene = remember(month) { JarScene.from(month) }
    val newestId = month.maxByOrNull { it.createdAt }?.id
    val counts = remember(month) { month.groupingBy { it.mood }.eachCount() }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
    ) {
        item {
            ScreenTitle(
                "Our jar",
                "${TimeFmt.weekOf(today)}, ${if (month.size == 1) "1 marble" else "${month.size} marbles"}",
            )
        }
        item {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                JarView(
                    marbles = scene,
                    newestId = newestId,
                    modifier = Modifier.fillMaxWidth(0.74f).aspectRatio(1f / JarGeometry.HEIGHT),
                )
            }
        }
        item {
            Column {
                Spacer(Modifier.height(16.dp))
                PrimaryButton("Add a marble", { adding = true }, Modifier.fillMaxWidth())
                if (month.isEmpty()) {
                    EmptyNote(
                        "An empty jar",
                        "Add a marble for how you feel today. Each of you can add as many as you like, and the jar starts fresh every Monday.",
                    )
                }
            }
        }
        if (counts.isNotEmpty()) {
            item {
                Column {
                    SectionLabel("This week")
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Mood.entries.filter { (counts[it] ?: 0) > 0 }.forEach { mood ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                MarbleDot(Color(mood.argb), 20.dp)
                                Spacer(Modifier.width(7.dp))
                                Text(
                                    "${mood.label} ${counts[mood]}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Ours.colors.ink,
                                )
                            }
                        }
                    }
                }
            }
        }
        if (all.isNotEmpty()) {
            item { SectionLabel("Recently") }
            items(all.take(15), key = { it.id }) { m ->
                MarbleRow(m, mine = m.memberId == me.memberId)
            }
        }
    }

    if (adding) {
        MoodSheet(
            onDismiss = { adding = false },
            onDrop = { mood, note -> Repo.addMarble(mood, note); adding = false },
        )
    }
}

@Composable
private fun MarbleRow(m: MarbleItem, mine: Boolean) {
    val c = Ours.colors
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MarbleDot(Color(m.mood.argb), 30.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                (if (mine) "You" else m.memberName.ifBlank { "Your partner" }) + " felt " + m.mood.label.lowercase(),
                style = MaterialTheme.typography.titleSmall,
                color = c.ink,
            )
            if (m.note.isNotBlank()) {
                Text(m.note, style = MaterialTheme.typography.bodyMedium, color = c.inkSoft)
            }
            Text(TimeFmt.relative(m.createdAt), style = MaterialTheme.typography.labelMedium, color = c.inkSoft)
        }
        if (mine) {
            IconButton(onClick = { Repo.deleteMarble(m.id) }) {
                Icon(Icons.Filled.Close, contentDescription = "Remove marble", tint = c.inkSoft, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun MoodSheet(onDismiss: () -> Unit, onDrop: (Mood, String) -> Unit) {
    val c = Ours.colors
    var selected by remember { mutableStateOf<Mood?>(null) }
    var note by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.card,
    ) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).navigationBarsPadding().imePadding().padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("How are you feeling?", style = MaterialTheme.typography.headlineSmall, color = c.ink)
            Mood.entries.chunked(4).forEach { rowMoods ->
                Row(Modifier.fillMaxWidth()) {
                    rowMoods.forEach { mood ->
                        val isSelected = selected == mood
                        Column(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { selected = mood }
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(
                                Modifier
                                    .size(60.dp)
                                    .then(if (isSelected) Modifier.border(2.dp, c.accent, CircleShape) else Modifier),
                                contentAlignment = Alignment.Center,
                            ) {
                                MarbleDot(Color(mood.argb), 44.dp)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                mood.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) c.ink else c.inkSoft,
                            )
                        }
                    }
                }
            }
            OurField(note, { note = it.take(60) }, "Anything to add? (optional)")
            PrimaryButton(
                "Drop it in",
                { selected?.let { onDrop(it, note) } },
                Modifier.fillMaxWidth(),
                enabled = selected != null,
            )
        }
    }
}
