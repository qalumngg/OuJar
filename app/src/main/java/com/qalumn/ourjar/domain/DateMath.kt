package com.qalumn.ourjar.domain

import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoUnit

data class Together(val years: Int, val months: Int, val days: Int, val totalDays: Long)

data class Milestone(val label: String, val date: LocalDate, val daysAway: Long)

object DateMath {

    /** How long they have been together as of [today]. Null if the start date is in the future. */
    fun together(start: LocalDate, today: LocalDate): Together? {
        if (start.isAfter(today)) return null
        val p = Period.between(start, today)
        return Together(p.years, p.months, p.days, ChronoUnit.DAYS.between(start, today))
    }

    /** The next time a date lands on the calendar, on or after [from]. */
    fun nextOccurrence(date: LocalDate, yearly: Boolean, from: LocalDate): LocalDate? {
        if (!yearly) return if (date.isBefore(from)) null else date
        var candidate = date.withYear(from.year)
        if (candidate.isBefore(from)) candidate = date.withYear(from.year + 1)
        return if (candidate.isBefore(date)) date else candidate
    }

    /** True if an event stored on [date] shows up on the calendar day [day]. */
    fun occursOn(date: LocalDate, yearly: Boolean, day: LocalDate): Boolean {
        if (!yearly) return date == day
        if (day.isBefore(date)) return false
        return date.withYear(day.year) == day
    }

    fun daysUntil(target: LocalDate, today: LocalDate): Long = ChronoUnit.DAYS.between(today, target)

    /** "Today", "Tomorrow", "In 12 days", "In 3 months". */
    fun countdown(daysAway: Long): String = when {
        daysAway < 0 -> "Passed"
        daysAway == 0L -> "Today"
        daysAway == 1L -> "Tomorrow"
        daysAway < 60 -> "In $daysAway days"
        daysAway < 365 -> "In ${daysAway / 30} months"
        else -> {
            val years = daysAway / 365
            val months = (daysAway % 365) / 30
            if (months == 0L) "In $years ${plural(years, "year")}"
            else "In $years ${plural(years, "year")}, $months ${plural(months, "month")}"
        }
    }

    private fun plural(n: Long, word: String) = if (n == 1L) word else word + "s"

    private val DAY_MILESTONES = listOf(100L, 200L, 300L, 500L, 750L, 1000L, 1500L, 2000L, 2500L, 3000L, 4000L, 5000L, 7500L, 10000L)

    private fun group(n: Long): String = String.format("%,d", n)

    /** Upcoming day-count and yearly milestones, soonest first. */
    fun nextMilestones(start: LocalDate, today: LocalDate, count: Int): List<Milestone> {
        val out = ArrayList<Milestone>()
        for (d in DAY_MILESTONES) {
            val date = start.plusDays(d)
            if (!date.isBefore(today)) out.add(Milestone("${group(d)} days", date, ChronoUnit.DAYS.between(today, date)))
        }
        for (y in 1..60) {
            val date = start.plusYears(y.toLong())
            if (!date.isBefore(today)) {
                out.add(Milestone(if (y == 1) "1 year" else "$y years", date, ChronoUnit.DAYS.between(today, date)))
            }
            if (date.isAfter(today.plusYears(12))) break
        }
        return out.sortedBy { it.daysAway }.take(count)
    }
}
