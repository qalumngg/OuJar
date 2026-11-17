@file:OptIn(ExperimentalMaterial3Api::class)

package com.qalumn.ourjar.ui

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qalumn.ourjar.data.CalendarLogic
import com.qalumn.ourjar.data.DateItem
import com.qalumn.ourjar.data.DateKind
import com.qalumn.ourjar.data.Repo
import com.qalumn.ourjar.domain.DateMath
import com.qalumn.ourjar.domain.TimeFmt
import com.qalumn.ourjar.ui.theme.Fraunces
import com.qalumn.ourjar.ui.theme.Ours
import java.time.LocalDate

@Composable
fun UsScreen() {
    val session by Repo.session.collectAsState()
    val me = session ?: return
    val members by Repo.members.collectAsState()
    val dates by Repo.dates.collectAsState()
    val couple by Repo.couple.collectAsState()
    val ctx = LocalContext.current
    val clipboard = LocalClipboardManager.current

    val today = LocalDate.now()
    val anniversary = couple.anniversary
    val together = anniversary?.let { DateMath.together(it, today) }
    val important = remember(dates, today) { CalendarLogic.importantDates(dates, today) }
    val milestones = remember(anniversary, today) {
        if (anniversary != null && together != null) DateMath.nextMilestones(anniversary, today, 4) else emptyList()
    }
    val partner = members.firstOrNull { it.id != me.memberId }?.name

    var pickingAnniversary by remember { mutableStateOf(false) }
    var editor by remember { mutableStateOf<DateItem?>(null) }
    var confirmLeave by remember { mutableStateOf(false) }
    var copied by remember { mutableStateOf(false) }
    val code = Repo.formatCode(me.coupleCode)

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 40.dp),
    ) {
        item { ScreenTitle("Us", if (partner != null) "${me.myName} and $partner" else me.myName) }

        // ---- anniversary ----
        item {
            OurCard(tinted = true, padding = PaddingValues(22.dp)) {
                if (anniversary == null) {
                        Text("Our anniversary", style = MaterialTheme.typography.titleLarge, color = Ours.colors.ink)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Add the day you got together and we will count the days, months and years for you.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Ours.colors.inkSoft,
                        )
                        Spacer(Modifier.height(16.dp))
                        PrimaryButton("Set our anniversary", { pickingAnniversary = true })
                } else if (together == null) {
                        val daysToGo = DateMath.daysUntil(anniversary, today)
                        Text("COUNTING DOWN", style = MaterialTheme.typography.labelSmall, color = Ours.colors.inkSoft)
                        Spacer(Modifier.height(4.dp))
                        Text(pluralize(daysToGo.toInt(), "day") + " to go", style = MaterialTheme.typography.headlineMedium, color = Ours.colors.ink)
                        Spacer(Modifier.height(4.dp))
                        Text(TimeFmt.date(anniversary), style = MaterialTheme.typography.bodyMedium, color = Ours.colors.inkSoft)
                        QuietTextButton("Change date", { pickingAnniversary = true })
                } else {
                    run {
                        val next = DateMath.nextOccurrence(anniversary, true, today)
                        val toNext = next?.let { DateMath.daysUntil(it, today) }
                        Text("TOGETHER FOR", style = MaterialTheme.typography.labelSmall, color = Ours.colors.inkSoft)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            if (together.years > 0) pluralize(together.years, "year") else pluralize(together.months, "month"),
                            style = MaterialTheme.typography.displaySmall,
                            color = Ours.colors.ink,
                        )
                        val rest = if (together.years > 0) {
                            listOfNotNull(
                                if (together.months > 0) pluralize(together.months, "month") else null,
                                if (together.days > 0) pluralize(together.days, "day") else null,
                            )
                        } else {
                            listOfNotNull(if (together.days > 0) pluralize(together.days, "day") else null)
                        }
                        if (rest.isNotEmpty()) {
                            Text(rest.joinToString(", "), style = MaterialTheme.typography.titleMedium, color = Ours.colors.inkSoft)
                        }
                        Spacer(Modifier.height(18.dp))
                        Box(Modifier.fillMaxWidth().height(1.dp).background(Ours.colors.line))
                        Spacer(Modifier.height(14.dp))
                        Text(
                            String.format("%,d days since %s", together.totalDays, TimeFmt.date(anniversary)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Ours.colors.ink,
                        )
                        if (next != null && toNext != null) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                if (toNext == 0L) "Today is our anniversary"
                                else "Next anniversary: ${TimeFmt.date(next)}, ${DateMath.countdown(toNext).replaceFirstChar { it.lowercase() }}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Ours.colors.accent,
                            )
                        }
                        QuietTextButton("Change date", { pickingAnniversary = true })
                    }
                }
            }
        }

        // ---- milestones ----
        if (milestones.isNotEmpty()) {
            item {
                Column {
                    SectionLabel("Next milestones")
                    OurCard(padding = PaddingValues(horizontal = 18.dp, vertical = 6.dp)) {
                        milestones.forEachIndexed { i, m ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(m.label, style = MaterialTheme.typography.titleSmall, color = Ours.colors.ink)
                                    Text(TimeFmt.date(m.date), style = MaterialTheme.typography.labelMedium, color = Ours.colors.inkSoft)
                                }
                                Text(
                                    DateMath.countdown(m.daysAway),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Ours.colors.accent,
                                )
                            }
                            if (i != milestones.lastIndex) Box(Modifier.fillMaxWidth().height(1.dp).background(Ours.colors.line))
                        }
                    }
                }
            }
        }

        // ---- important dates ----
        item {
            Column {
                SectionLabel("Important dates")
                if (important.isEmpty()) {
                    OurCard {
                        Text("Birthdays, first date, moving day", style = MaterialTheme.typography.titleMedium, color = Ours.colors.ink)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Keep the days that matter in one place. They repeat every year and show on the calendar too.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Ours.colors.inkSoft,
                        )
                    }
                } else {
                    OurCard(padding = PaddingValues(horizontal = 18.dp, vertical = 6.dp)) {
                        important.forEachIndexed { i, (item, next) ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { editor = item }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(Modifier.size(8.dp).background(item.kind.color(Ours.colors), CircleShape))
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(item.title, style = MaterialTheme.typography.titleSmall, color = Ours.colors.ink)
                                    Text(
                                        TimeFmt.date(next ?: item.date) + if (item.yearly) "  ·  every year" else "",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Ours.colors.inkSoft,
                                    )
                                }
                                if (next != null) {
                                    Text(
                                        DateMath.countdown(DateMath.daysUntil(next, today)),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Ours.colors.accent,
                                    )
                                }
                            }
                            if (i != important.lastIndex) Box(Modifier.fillMaxWidth().height(1.dp).background(Ours.colors.line))
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                SoftButton("Add an important date", { editor = newDateItem(today, DateKind.IMPORTANT) })
            }
        }

        // ---- our space ----
        item {
            Column {
                SectionLabel("Our space")
                OurCard {
                    Text("Couple code", style = MaterialTheme.typography.labelMedium, color = Ours.colors.inkSoft)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        code,
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 26.sp, letterSpacing = 1.5.sp, fontFamily = Fraunces),
                        color = Ours.colors.ink,
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SoftButton(
                            if (copied) "Copied" else "Copy",
                            { clipboard.setText(AnnotatedString(code)); copied = true },
                        )
                        SoftButton(
                            "Share",
                            {
                                val send = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, "Join me in Jarlings. Open the app, tap Join my partner, and enter this code: $code")
                                }
                                ctx.startActivity(Intent.createChooser(send, "Share code"))
                            },
                        )
                    }
                    Spacer(Modifier.height(18.dp))
                    members.sortedBy { if (it.id == me.memberId) 0 else 1 }.forEach { m ->
                        Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Initial(m.name, isMe = m.id == me.memberId)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                if (m.id == me.memberId) "${m.name} (you)" else m.name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = Ours.colors.ink,
                            )
                        }
                    }
                    if (members.size < 2) {
                        Text(
                            "Waiting for your partner to join with the code above.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Ours.colors.inkSoft,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
            }
        }

        item {
            Column(Modifier.fillMaxWidth().padding(top = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Widgets: press and hold your home screen, tap Widgets, then look for Jarlings to add the love note and the mood jar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Ours.colors.inkSoft,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                QuietTextButton("Leave this couple on this phone", { confirmLeave = true }, danger = true)
            }
        }
    }

    if (pickingAnniversary) {
        OurDatePickerDialog(
            initial = anniversary,
            onDismiss = { pickingAnniversary = false },
            onPicked = { Repo.setAnniversary(it); pickingAnniversary = false },
        )
    }
    editor?.let { DateEditorSheet(it, onDismiss = { editor = null }) }

    if (confirmLeave) {
        AlertDialog(
            onDismissRequest = { confirmLeave = false },
            containerColor = Ours.colors.card,
            title = { Text("Leave this couple?", style = MaterialTheme.typography.titleLarge, color = Ours.colors.ink) },
            text = {
                Text(
                    "This only signs this phone out. Your notes, marbles and dates stay safe, and you can come back with the code.",
                    color = Ours.colors.inkSoft,
                )
            },
            confirmButton = {
                TextButton(onClick = { confirmLeave = false; Repo.leave() }) { Text("Leave", color = Ours.colors.accent) }
            },
            dismissButton = { TextButton(onClick = { confirmLeave = false }) { Text("Stay", color = Ours.colors.inkSoft) } },
        )
    }
}
