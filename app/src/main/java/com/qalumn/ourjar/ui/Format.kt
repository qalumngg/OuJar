package com.qalumn.ourjar.ui

import androidx.compose.ui.graphics.Color
import com.qalumn.ourjar.data.DateKind
import com.qalumn.ourjar.domain.Together
import com.qalumn.ourjar.ui.theme.OurColors

fun pluralize(n: Int, word: String): String = "$n $word" + if (n == 1) "" else "s"

/** "3 years, 8 months, 3 days", skipping parts that are zero. */
fun Together.long(): String {
    val parts = ArrayList<String>()
    if (years > 0) parts.add(pluralize(years, "year"))
    if (months > 0) parts.add(pluralize(months, "month"))
    if (days > 0) parts.add(pluralize(days, "day"))
    return if (parts.isEmpty()) "Today" else parts.joinToString(", ")
}

fun DateKind.color(c: OurColors): Color = when (this) {
    DateKind.DATE_NIGHT -> c.dateNight
    DateKind.BIRTHDAY -> c.birthday
    DateKind.IMPORTANT -> c.important
    DateKind.ANNIVERSARY -> c.accent
}
