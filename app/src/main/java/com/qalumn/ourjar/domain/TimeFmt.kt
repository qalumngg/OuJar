package com.qalumn.ourjar.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object TimeFmt {
    private val dayMonth = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
    private val dayMonthYear = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())
    private val longDate = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.getDefault())
    private val monthYear = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
    private val monthOnly = DateTimeFormatter.ofPattern("MMMM", Locale.getDefault())
    private val shortDay = DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault())
    private val clock = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())

    /** "Just now", "12 min ago", "3 h ago", "Yesterday", "4 Nov". */
    fun relative(createdAt: Long, now: Long = System.currentTimeMillis()): String {
        val diff = now - createdAt
        val min = diff / 60_000
        val zone = ZoneId.systemDefault()
        val then = Instant.ofEpochMilli(createdAt).atZone(zone).toLocalDate()
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        return when {
            min < 1 -> "Just now"
            min < 60 -> "$min min ago"
            then == today -> "${min / 60} h ago"
            then == today.minusDays(1) -> "Yesterday"
            then.year == today.year -> then.format(dayMonth)
            else -> then.format(dayMonthYear)
        }
    }

    fun date(d: LocalDate): String = d.format(dayMonthYear)
    fun shortDay(d: LocalDate): String = d.format(shortDay)
    fun longDate(d: LocalDate): String = d.format(longDate)
    fun monthYear(d: LocalDate): String = d.format(monthYear)
    fun monthName(d: LocalDate): String = d.format(monthOnly)
    fun time(t: java.time.LocalTime): String = t.format(clock)

    /** The jar resets every Monday. */
    fun startOfWeekMillis(today: LocalDate = LocalDate.now()): Long =
        today.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
            .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    fun weekOf(today: LocalDate = LocalDate.now()): String =
        "Week of " + today.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)).format(dayMonth)

    fun startOfMonthMillis(today: LocalDate = LocalDate.now()): Long =
        today.withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}
