package com.qalumn.ourjar.data

import com.qalumn.ourjar.domain.Mood
import java.time.LocalDate
import java.time.LocalTime

data class Session(val coupleCode: String, val memberId: String, val myName: String)

data class Member(val id: String, val name: String)

data class Note(
    val id: String,
    val fromId: String,
    val fromName: String,
    val text: String,
    val createdAt: Long,
    /** Hand-drawn note, encoded by DrawingCodec. Empty for typed notes. */
    val drawing: String = "",
)

data class MarbleItem(
    val id: String,
    val memberId: String,
    val memberName: String,
    val mood: Mood,
    val note: String,
    val createdAt: Long,
)

enum class DateKind(val key: String, val label: String, val defaultYearly: Boolean) {
    DATE_NIGHT("date_night", "Date night", false),
    BIRTHDAY("birthday", "Birthday", true),
    IMPORTANT("important", "Important date", true),
    ANNIVERSARY("anniversary", "Anniversary", true);

    companion object {
        fun fromKey(key: String?): DateKind = entries.firstOrNull { it.key == key } ?: IMPORTANT
    }
}

data class DateItem(
    val id: String,
    val title: String,
    val date: LocalDate,
    val time: LocalTime?,
    val kind: DateKind,
    val yearly: Boolean,
    val notes: String,
    val createdBy: String,
    /** True for the anniversary entry that is generated from the couple's anniversary date. */
    val virtual: Boolean = false,
)

data class CoupleInfo(val anniversary: LocalDate?)

/** What a widget needs, kept in plain SharedPreferences so it works without the app running. */
data class MarbleLite(val id: String, val memberId: String, val mood: Mood, val createdAt: Long)
