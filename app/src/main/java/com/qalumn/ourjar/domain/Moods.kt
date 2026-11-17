package com.qalumn.ourjar.domain

/** The moods a marble can represent. Soft pastels that read on cream and on plum. Order is the order in the picker. */
enum class Mood(val key: String, val label: String, val argb: Int) {
    HAPPY("happy", "Happy", 0xFFFFD36E.toInt()),
    LOVED("loved", "Loved", 0xFFF7A8C0.toInt()),
    EXCITED("excited", "Excited", 0xFFFFAE84.toInt()),
    GRATEFUL("grateful", "Grateful", 0xFFB5D99C.toInt()),
    PLAYFUL("playful", "Playful", 0xFF7FD6D9.toInt()),
    FLIRTY("flirty", "Flirty", 0xFFE08FC8.toInt()),
    HOPEFUL("hopeful", "Hopeful", 0xFFA9D6F5.toInt()),
    CALM("calm", "Calm", 0xFF9FD8C4.toInt()),
    COZY("cozy", "Cozy", 0xFFE3C7A0.toInt()),
    TIRED("tired", "Tired", 0xFFBDB1E8.toInt()),
    BORED("bored", "Bored", 0xFFC9C2BB.toInt()),
    SAD("sad", "Sad", 0xFF8FBBE3.toInt()),
    LONELY("lonely", "Lonely", 0xFF8E9FE6.toInt()),
    ANXIOUS("anxious", "Anxious", 0xFF9FA3C7.toInt()),
    STRESSED("stressed", "Stressed", 0xFFE8998A.toInt()),
    ANGRY("angry", "Angry", 0xFFD64550.toInt());

    companion object {
        fun fromKey(key: String?): Mood = entries.firstOrNull { it.key == key } ?: HAPPY
    }
}
