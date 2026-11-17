@file:OptIn(ExperimentalMaterial3Api::class)

package com.qalumn.ourjar.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import com.qalumn.ourjar.ui.theme.Ours
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

@Composable
fun OurDatePickerDialog(initial: LocalDate?, onDismiss: () -> Unit, onPicked: (LocalDate) -> Unit) {
    val c = Ours.colors
    val state = rememberDatePickerState(
        initialSelectedDateMillis = (initial ?: LocalDate.now()).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val ms = state.selectedDateMillis
                if (ms != null) onPicked(Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate())
            }) { Text("OK", color = c.accent) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = c.inkSoft) } },
        colors = DatePickerDefaults.colors(containerColor = c.card),
    ) {
        DatePicker(
            state = state,
            colors = DatePickerDefaults.colors(
                containerColor = c.card,
                selectedDayContainerColor = c.primary,
                selectedDayContentColor = c.onPrimary,
                todayContentColor = c.accent,
                todayDateBorderColor = c.accent,
            ),
        )
    }
}

@Composable
fun OurTimePickerDialog(initial: LocalTime?, onDismiss: () -> Unit, onPicked: (LocalTime) -> Unit) {
    val c = Ours.colors
    val state = rememberTimePickerState(
        initialHour = initial?.hour ?: 19,
        initialMinute = initial?.minute ?: 0,
        is24Hour = false,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.card,
        confirmButton = {
            TextButton(onClick = { onPicked(LocalTime.of(state.hour, state.minute)) }) { Text("OK", color = c.accent) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = c.inkSoft) } },
        text = { TimePicker(state = state) },
    )
}
