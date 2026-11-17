@file:OptIn(ExperimentalMaterial3Api::class)

package com.qalumn.ourjar.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qalumn.ourjar.data.Note
import com.qalumn.ourjar.data.Repo
import com.qalumn.ourjar.domain.DateMath
import com.qalumn.ourjar.domain.TimeFmt
import com.qalumn.ourjar.ui.theme.Hand
import com.qalumn.ourjar.ui.theme.Ours
import java.time.LocalDate

@Composable
fun NotesScreen(onOpenUs: () -> Unit) {
    val session by Repo.session.collectAsState()
    val me = session ?: return
    val notes by Repo.notes.collectAsState()
    val members by Repo.members.collectAsState()
    val couple by Repo.couple.collectAsState()
    var composing by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Note?>(null) }

    val partner = members.firstOrNull { it.id != me.memberId }?.name
    val hero = notes.firstOrNull { it.fromId != me.memberId }
    val rest = notes.filter { it.id != hero?.id }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 112.dp),
        ) {
            item { ScreenTitle("Notes", "Little things, left for each other") }
            item { TogetherStrip(couple.anniversary, onOpenUs) }

            if (notes.isEmpty()) {
                item {
                    EmptyNote(
                        "No notes yet",
                        if (partner != null) "Write the first one for $partner. It will also show on their home screen widget."
                        else "Write the first one. Once your partner joins, they will see it right away.",
                    )
                }
            } else {
                if (hero != null) {
                    item {
                        Column {
                            SectionLabel("Latest from ${hero.fromName}")
                            PaperNote(hero)
                        }
                    }
                }
                if (rest.isNotEmpty()) {
                    item { SectionLabel(if (hero != null) "Earlier" else "All notes") }
                    items(rest, key = { it.id }) { n ->
                        Column {
                            NoteRow(n, mine = n.fromId == me.memberId, onClick = if (n.fromId == me.memberId) ({ deleting = n }) else null)
                            Spacer(Modifier.height(10.dp))
                        }
                    }
                }
            }
        }

        PrimaryButton(
            "Write a note",
            onClick = { composing = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
        )
    }

    if (composing) {
        NoteComposer(
            partnerName = partner,
            onDismiss = { composing = false },
            onSend = { Repo.sendNote(it); composing = false },
        )
    }

    deleting?.let { n ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            containerColor = Ours.colors.card,
            title = { Text("Delete this note?", style = MaterialTheme.typography.titleLarge, color = Ours.colors.ink) },
            text = { Text("It will be removed for both of you.", color = Ours.colors.inkSoft) },
            confirmButton = {
                TextButton(onClick = { Repo.deleteNote(n.id); deleting = null }) { Text("Delete", color = Ours.colors.accent) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Keep", color = Ours.colors.inkSoft) } },
        )
    }
}

@Composable
private fun TogetherStrip(anniversary: LocalDate?, onOpenUs: () -> Unit) {
    val c = Ours.colors
    val together = anniversary?.let { DateMath.together(it, LocalDate.now()) }
    OurCard(tinted = true, onClick = onOpenUs, padding = PaddingValues(horizontal = 18.dp, vertical = 14.dp)) {
        if (together != null) {
            Text("TOGETHER", style = MaterialTheme.typography.labelSmall, color = c.inkSoft)
            Spacer(Modifier.height(2.dp))
            Text(together.long(), style = MaterialTheme.typography.titleLarge, color = c.ink)
        } else {
            Text("Add your anniversary", style = MaterialTheme.typography.titleMedium, color = c.ink)
            Spacer(Modifier.height(2.dp))
            Text("Count the days together in the Us tab.", style = MaterialTheme.typography.bodyMedium, color = c.inkSoft)
        }
    }
}

/** The newest note from your partner, set on a piece of paper with a strip of tape. */
@Composable
private fun PaperNote(note: Note) {
    val c = Ours.colors
    Box(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        OurCard(padding = PaddingValues(start = 22.dp, end = 22.dp, top = 28.dp, bottom = 20.dp)) {
            Text(
                note.text,
                style = MaterialTheme.typography.bodyLarge.copy(fontFamily = Hand, fontSize = 30.sp, lineHeight = 34.sp),
                color = c.ink,
            )
            Spacer(Modifier.height(14.dp))
            Text(
                "${note.fromName}, ${TimeFmt.relative(note.createdAt)}",
                style = MaterialTheme.typography.labelMedium,
                color = c.inkSoft,
            )
        }
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-8).dp)
                .rotate(-3f)
                .size(width = 72.dp, height = 20.dp)
                .background(c.primary.copy(alpha = 0.85f), RoundedCornerShape(3.dp)),
        )
    }
}

@Composable
private fun NoteRow(note: Note, mine: Boolean, onClick: (() -> Unit)?) {
    val c = Ours.colors
    OurCard(onClick = onClick, tinted = mine, padding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Initial(note.fromName, isMe = mine, size = 30.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (mine) "You, ${TimeFmt.relative(note.createdAt)}" else "${note.fromName}, ${TimeFmt.relative(note.createdAt)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = c.inkSoft,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    note.text,
                    style = MaterialTheme.typography.bodyLarge.copy(fontFamily = Hand, fontSize = 23.sp, lineHeight = 26.sp),
                    color = c.ink,
                )
            }
        }
    }
}

@Composable
private fun NoteComposer(partnerName: String?, onDismiss: () -> Unit, onSend: (String) -> Unit) {
    val c = Ours.colors
    var text by remember { mutableStateOf("") }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.card,
    ) {
        Column(
            Modifier.padding(horizontal = 20.dp).navigationBarsPadding().imePadding().padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                if (partnerName != null) "A note for $partnerName" else "A new note",
                style = MaterialTheme.typography.headlineSmall,
                color = c.ink,
            )
            OurField(
                value = text,
                onValueChange = { text = it.take(280) },
                label = "Write something sweet",
                singleLine = false,
                minLines = 4,
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = Hand, fontSize = 26.sp, lineHeight = 30.sp),
            )
            Text(
                "${text.length}/280",
                style = MaterialTheme.typography.labelMedium,
                color = c.inkSoft,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth(),
            )
            PrimaryButton("Send", { onSend(text) }, Modifier.fillMaxWidth(), enabled = text.isNotBlank())
        }
    }
}
