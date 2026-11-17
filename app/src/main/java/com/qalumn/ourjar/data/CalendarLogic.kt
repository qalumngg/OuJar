package com.qalumn.ourjar.data

import com.qalumn.ourjar.domain.DateMath
import java.time.LocalDate

/** Turns the stored dates (plus the anniversary) into what the calendar and lists show. */
object CalendarLogic {

    fun withAnniversary(dates: List<DateItem>, anniversary: LocalDate?): List<DateItem> {
        if (anniversary == null) return dates
        val virtual = DateItem(
            id = "anniversary",
            title = "Our anniversary",
            date = anniversary,
            time = null,
            kind = DateKind.ANNIVERSARY,
            yearly = true,
            notes = "",
            createdBy = "",
            virtual = true,
        )
        return dates + virtual
    }

    fun eventsOn(day: LocalDate, all: List<DateItem>): List<DateItem> =
        all.filter { DateMath.occursOn(it.date, it.yearly, day) }
            .sortedWith(compareBy({ it.time == null }, { it.time }, { it.title.lowercase() }))

    fun markerKinds(day: LocalDate, all: List<DateItem>): List<DateKind> =
        eventsOn(day, all).map { it.kind }.distinct().take(3)

    /** The next [limit] things coming up from [from], soonest first. Each comes with the day it lands on. */
    fun upcoming(all: List<DateItem>, from: LocalDate, limit: Int): List<Pair<DateItem, LocalDate>> =
        all.mapNotNull { item ->
            DateMath.nextOccurrence(item.date, item.yearly, from)?.let { item to it }
        }.sortedWith(compareBy({ it.second }, { it.first.time == null }, { it.first.time }))
            .take(limit)

    /** Birthdays and other important dates, soonest first. Excludes date nights and the anniversary. */
    fun importantDates(all: List<DateItem>, from: LocalDate): List<Pair<DateItem, LocalDate?>> =
        all.filter { it.kind == DateKind.BIRTHDAY || it.kind == DateKind.IMPORTANT }
            .map { it to DateMath.nextOccurrence(it.date, it.yearly, from) }
            .sortedWith(compareBy({ it.second == null }, { it.second }))
}
