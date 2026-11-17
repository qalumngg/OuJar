@file:OptIn(ExperimentalMaterial3Api::class)

package com.qalumn.ourjar.ui

import androidx.compose.foundation.background
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
import com.qalumn.ourjar.domain.Need
import com.qalumn.ourjar.domain.TimeFmt
import com.qalumn.ourjar.render.JarScene
import com.qalumn.ourjar.ui.theme.Ours
import java.time.LocalDate

@Composable
fun JarScreen() {
    val session by Repo.session.collectAsState()
    val me = session ?: return
    val all by Repo.marbles.collectAsState()
    val members by Repo.members.collectAsState()
    var adding by remember { mutableStateOf(false) }

    val today = LocalDate.now()
    val weekStart = TimeFmt.startOfWeekMillis(today)
    val week = remember(all, weekStart) { all.filter { it.createdAt >= weekStart } }
    val mine = remember(week) { week.filter { it.memberId == me.memberId } }
    val theirs = remember(week) { week.filter { it.memberId != me.memberId } }
    val myScene = remember(mine) { JarScene.from(mine) }
    val theirScene = remember(theirs) { JarScene.from(theirs) }
    val partnerName = members.firstOrNull { it.id != me.memberId }?.name

    val counts = remember(week) { week.flatMap { listOfNotNull(it.mood, it.mood2) }.groupingBy { it }.eachCount() }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
    ) {
        item { ScreenTitle("Our jars", TimeFmt.weekOf(today)) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                JarColumn(me.myName, mine.size, myScene, mine.maxByOrNull { it.createdAt }?.id, Modifier.weight(1f))
                JarColumn(partnerName ?: "Not joined yet", theirs.size, theirScene, theirs.maxByOrNull { it.createdAt }?.id, Modifier.weight(1f))
            }
        }
        item {
            Column {
                Spacer(Modifier.height(20.dp))
                PrimaryButton("Add a marble", { adding = true }, Modifier.fillMaxWidth())
                if (week.isEmpty()) {
                    EmptyNote(
                        "Nothing in here yet",
                        "Drop in a marble for how you feel. The jars empty out every Monday.",
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
            onDrop = { mood, mood2, need, note -> Repo.addMarble(mood, mood2, need, note); adding = false },
        )
    }
}

@Composable
private fun JarColumn(name: String, count: Int, scene: List<com.qalumn.ourjar.render.JarMarble>, newestId: String?, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        JarView(
            marbles = scene,
            newestId = newestId,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f / JarGeometry.HEIGHT),
        )
        Spacer(Modifier.height(8.dp))
        Text(name, style = MaterialTheme.typography.titleMedium, color = Ours.colors.ink, maxLines = 1)
        Text(
            if (count == 1) "1 marble" else "$count marbles",
            style = MaterialTheme.typography.labelMedium,
            color = Ours.colors.inkSoft,
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
        MarbleDot(Color(m.mood.argb), 30.dp, m.mood2?.let { Color(it.argb) }, com.qalumn.ourjar.domain.JarLayout.hash(m.id))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                feelingLine(m, mine),
                style = MaterialTheme.typography.titleSmall,
                color = c.ink,
            )
            m.need?.let {
                Text(it.line, style = MaterialTheme.typography.bodyMedium, color = c.accent)
            }
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

private fun feelingLine(m: MarbleItem, mine: Boolean): String {
    val who = if (mine) "You" else m.memberName.ifBlank { "They" }
    return when {
        m.mood == Mood.NOTSURE -> if (mine) "You could not tell how you felt" else "$who could not tell how they felt"
        m.mood2 != null -> "$who felt ${m.mood.label.lowercase()} and ${m.mood2.label.lowercase()}"
        else -> "$who felt ${m.mood.label.lowercase()}"
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun MoodSheet(onDismiss: () -> Unit, onDrop: (Mood, Mood?, Need?, String) -> Unit) {
    val c = Ours.colors
    var mixed by remember { mutableStateOf(false) }
    var picked by remember { mutableStateOf(listOf<Mood>()) }
    var need by remember { mutableStateOf<Need?>(null) }
    var note by remember { mutableStateOf("") }

    fun toggle(m: Mood) {
        picked = when {
            m in picked -> picked - m
            mixed -> (picked + m).takeLast(2)
            else -> listOf(m)
        }
    }

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

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill("One feeling", !mixed, { mixed = false; picked = picked.take(1) })
                Pill("Mixed feelings", mixed, { mixed = true })
            }

            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.cardAlt).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                    if (picked.isEmpty()) {
                        MarbleDot(Color(Mood.NOTSURE.argb).copy(alpha = 0.35f), 44.dp)
                    } else {
                        MarbleDot(Color(picked[0].argb), 56.dp, picked.getOrNull(1)?.let { Color(it.argb) }, 0x55)
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (picked.isEmpty()) {
                        Text(
                            if (mixed) "Pick two and they swirl together." else "Tap the one closest to how you feel. Not sure is a fine answer.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = c.inkSoft,
                        )
                    } else {
                        picked.forEach {
                            Text("${it.label}: ${it.blurb}", style = MaterialTheme.typography.bodyMedium, color = c.ink)
                        }
                        if (mixed && picked.size == 1) {
                            Text("Pick one more.", style = MaterialTheme.typography.bodySmall, color = c.inkSoft)
                        }
                    }
                }
            }

            Mood.groups.forEach { (title, moods) ->
                Text(title, style = MaterialTheme.typography.labelMedium, color = c.inkSoft)
                moods.chunked(4).forEach { rowMoods ->
                    Row(Modifier.fillMaxWidth()) {
                        rowMoods.forEach { mood ->
                            val isSelected = mood in picked
                            Column(
                                Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { toggle(mood) }
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
                        repeat(4 - rowMoods.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }

            Text("What would help? (optional)", style = MaterialTheme.typography.labelMedium, color = c.inkSoft)
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Need.entries.forEach { n ->
                    Pill(n.label, need == n, { need = if (need == n) null else n })
                }
            }

            OurField(note, { note = it.take(60) }, "Anything to add? (optional)")
            PrimaryButton(
                "Drop it in",
                { if (picked.isNotEmpty()) onDrop(picked[0], picked.getOrNull(1), need, note) },
                Modifier.fillMaxWidth(),
                enabled = picked.isNotEmpty() && (!mixed || picked.size == 2),
            )
        }
    }
}
