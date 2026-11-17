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
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.qalumn.ourjar.data.CalendarLogic
import com.qalumn.ourjar.data.DateItem
import com.qalumn.ourjar.data.DateKind
import com.qalumn.ourjar.data.Repo
import com.qalumn.ourjar.domain.DateMath
import com.qalumn.ourjar.domain.TimeFmt
import com.qalumn.ourjar.ui.theme.Ours
import java.time.LocalDate
import java.time.YearMonth

fun newDateItem(date: LocalDate, kind: DateKind = DateKind.DATE_NIGHT) =
    DateItem("", "", date, null, kind, kind.defaultYearly, "", "")

@Composable
fun CalendarScreen() {
    val dates by Repo.dates.collectAsState()
    val couple by Repo.couple.collectAsState()
    val all = remember(dates, couple) { CalendarLogic.withAnniversary(dates, couple.anniversary) }

    val today = LocalDate.now()
    var month by remember { mutableStateOf(YearMonth.from(today)) }
    var selected by remember { mutableStateOf(today) }
    var editor by remember { mutableStateOf<DateItem?>(null) }

    val dayEvents = remember(selected, all) { CalendarLogic.eventsOn(selected, all) }
    val upcoming = remember(all, today) { CalendarLogic.upcoming(all, today, 5) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
    ) {
        item {
            ScreenTitle(
                "Dates",
                "Things to look forward to",
                modifier = Modifier.padding(horizontal = 4.dp),
                trailing = { SoftButton("Add", { editor = newDateItem(selected) }) },
            )
        }
        item {
            Column {
                MonthHeader(
                    month = month,
                    showToday = month != YearMonth.from(today) || selected != today,
                    onPrev = { month = month.minusMonths(1) },
                    onNext = { month = month.plusMonths(1) },
                    onToday = { month = YearMonth.from(today); selected = today },
                )
                Spacer(Modifier.height(8.dp))
                MonthGrid(month, selected, today, all) { selected = it }
            }
        }
        item {
            Column(Modifier.padding(horizontal = 4.dp)) {
                SectionLabel(TimeFmt.longDate(selected))
                if (dayEvents.isEmpty()) {
                    OurCard(tinted = true) {
                        Text("Nothing planned", style = MaterialTheme.typography.titleMedium, color = Ours.colors.ink)
                        Spacer(Modifier.height(4.dp))
                        Text("Add a date night or anything else.", style = MaterialTheme.typography.bodyMedium, color = Ours.colors.inkSoft)
                        Spacer(Modifier.height(14.dp))
                        SoftButton("Plan something", { editor = newDateItem(selected) })
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        dayEvents.forEach { e ->
                            EventCard(e, selected, onClick = if (e.virtual) null else ({ editor = e }))
                        }
                    }
                }
            }
        }
        if (upcoming.isNotEmpty()) {
            item {
                Column(Modifier.padding(horizontal = 4.dp)) {
                    SectionLabel("Coming up")
                    OurCard(padding = PaddingValues(horizontal = 18.dp, vertical = 6.dp)) {
                        upcoming.forEachIndexed { i, (item, day) ->
                            UpcomingRow(item, day, today)
                            if (i != upcoming.lastIndex) {
                                Box(Modifier.fillMaxWidth().height(1.dp).background(Ours.colors.line))
                            }
                        }
                    }
                }
            }
        }
    }

    editor?.let { DateEditorSheet(it, onDismiss = { editor = null }) }
}

@Composable
private fun MonthHeader(month: YearMonth, showToday: Boolean, onPrev: () -> Unit, onNext: () -> Unit, onToday: () -> Unit) {
    val c = Ours.colors
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrev) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Previous month", tint = c.ink) }
        Text(
            TimeFmt.monthYear(month.atDay(1)),
            style = MaterialTheme.typography.titleLarge,
            color = c.ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onNext) { Icon(Icons.AutoMirrored.Filled.ArrowForward, "Next month", tint = c.ink) }
    }
    if (showToday) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Pill("Back to today", false, onToday)
        }
    }
}

@Composable
private fun MonthGrid(month: YearMonth, selected: LocalDate, today: LocalDate, all: List<DateItem>, onSelect: (LocalDate) -> Unit) {
    val c = Ours.colors
    val lead = month.atDay(1).dayOfWeek.value - 1
    val length = month.lengthOfMonth()
    val rows = (lead + length + 6) / 7

    Column {
        Row(Modifier.fillMaxWidth()) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach {
                Text(
                    it,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = c.inkSoft,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        for (r in 0 until rows) {
            Row(Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val dayNum = r * 7 + col - lead + 1
                    if (dayNum in 1..length) {
                        val date = month.atDay(dayNum)
                        DayCell(date, date == selected, date == today, CalendarLogic.markerKinds(date, all)) { onSelect(date) }
                    } else {
                        Spacer(Modifier.weight(1f).height(52.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.DayCell(date: LocalDate, isSelected: Boolean, isToday: Boolean, kinds: List<DateKind>, onClick: () -> Unit) {
    val c = Ours.colors
    Column(
        Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier.size(34.dp).then(
                when {
                    isSelected -> Modifier.background(c.primary, CircleShape)
                    isToday -> Modifier.border(1.5.dp, c.accent, CircleShape)
                    else -> Modifier
                },
            ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                date.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = if (isSelected) c.onPrimary else c.ink,
            )
        }
        Row(Modifier.padding(top = 3.dp).height(5.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            kinds.forEach { k -> Box(Modifier.size(5.dp).background(k.color(c), CircleShape)) }
        }
    }
}

@Composable
private fun EventCard(e: DateItem, day: LocalDate, onClick: (() -> Unit)?) {
    val c = Ours.colors
    val bits = ArrayList<String>()
    e.time?.let { bits.add(TimeFmt.time(it)) }
    bits.add(e.kind.label)
    if (e.virtual) {
        val years = day.year - e.date.year
        if (years > 0) bits.add(pluralize(years, "year") + " together")
    } else if (e.yearly) {
        bits.add("Every year")
    }
    OurCard(onClick = onClick, padding = PaddingValues(horizontal = 18.dp, vertical = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).background(e.kind.color(c), CircleShape))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(e.title, style = MaterialTheme.typography.titleMedium, color = c.ink)
                Text(bits.joinToString("  ·  "), style = MaterialTheme.typography.labelMedium, color = c.inkSoft)
                if (e.notes.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(e.notes, style = MaterialTheme.typography.bodyMedium, color = c.inkSoft)
                }
            }
        }
    }
}

@Composable
private fun UpcomingRow(item: DateItem, day: LocalDate, today: LocalDate) {
    val c = Ours.colors
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).background(item.kind.color(c), CircleShape))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(item.title, style = MaterialTheme.typography.titleSmall, color = c.ink)
            Text(TimeFmt.shortDay(day), style = MaterialTheme.typography.labelMedium, color = c.inkSoft)
        }
        Text(
            DateMath.countdown(DateMath.daysUntil(day, today)),
            style = MaterialTheme.typography.labelMedium,
            color = c.accent,
        )
    }
}

/** Add or edit a date night, birthday or important date. */
@Composable
fun DateEditorSheet(initial: DateItem, onDismiss: () -> Unit) {
    val c = Ours.colors
    val isNew = initial.id.isEmpty()
    var title by remember { mutableStateOf(initial.title) }
    var kind by remember { mutableStateOf(initial.kind) }
    var date by remember { mutableStateOf(initial.date) }
    var time by remember { mutableStateOf(initial.time) }
    var yearly by remember { mutableStateOf(initial.yearly) }
    var notes by remember { mutableStateOf(initial.notes) }
    var pickDate by remember { mutableStateOf(false) }
    var pickTime by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.card,
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .imePadding()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(if (isNew) "New date" else "Edit date", style = MaterialTheme.typography.headlineSmall, color = c.ink)
            OurField(title, { title = it.take(60) }, "What is it?")

            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(DateKind.DATE_NIGHT, DateKind.BIRTHDAY, DateKind.IMPORTANT).forEach { k ->
                    Pill(
                        k.label,
                        selected = kind == k,
                        onClick = { kind = k; yearly = k.defaultYearly },
                        dot = k.color(c),
                    )
                }
            }

            PickerField("Date", TimeFmt.longDate(date) + ", " + date.year, onClick = { pickDate = true })

            if (kind == DateKind.DATE_NIGHT) {
                PickerField("Time (optional)", time?.let { TimeFmt.time(it) } ?: "No time set", onClick = { pickTime = true })
                if (time != null) QuietTextButton("Clear time", { time = null })
            }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Repeats every year", style = MaterialTheme.typography.titleSmall, color = c.ink)
                    Text("Good for birthdays and anniversaries", style = MaterialTheme.typography.bodySmall, color = c.inkSoft)
                }
                Switch(
                    checked = yearly,
                    onCheckedChange = { yearly = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = c.onPrimary,
                        checkedTrackColor = c.primary,
                        checkedBorderColor = c.primary,
                        uncheckedThumbColor = c.inkSoft,
                        uncheckedTrackColor = c.card,
                        uncheckedBorderColor = c.line,
                    ),
                )
            }

            OurField(notes, { notes = it.take(200) }, "Notes (optional)", singleLine = false, minLines = 2)

            PrimaryButton(
                "Save",
                onClick = {
                    Repo.saveDate(
                        initial.copy(
                            title = title.trim(),
                            date = date,
                            time = if (kind == DateKind.DATE_NIGHT) time else null,
                            kind = kind,
                            yearly = yearly,
                            notes = notes,
                        ),
                    )
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = title.isNotBlank(),
            )
            if (!isNew) {
                QuietTextButton(
                    "Delete",
                    onClick = { Repo.deleteDate(initial.id); onDismiss() },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    danger = true,
                )
            }
        }
    }

    if (pickDate) {
        OurDatePickerDialog(date, onDismiss = { pickDate = false }, onPicked = { date = it; pickDate = false })
    }
    if (pickTime) {
        OurTimePickerDialog(time, onDismiss = { pickTime = false }, onPicked = { time = it; pickTime = false })
    }
}
