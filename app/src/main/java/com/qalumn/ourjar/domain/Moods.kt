package com.qalumn.ourjar.domain

/** The moods a marble can represent. Soft pastels that read on cream and on plum. */
enum class Mood(val key: String, val label: String, val argb: Int, val blurb: String) {
    HAPPY("happy", "Happy", 0xFFFFD36E.toInt(), "Feeling good, light, glad about things."),
    LOVED("loved", "Loved", 0xFFF7A8C0.toInt(), "Cared for and close to someone."),
    EXCITED("excited", "Excited", 0xFFFFAE84.toInt(), "Looking forward to something, a bit fizzy."),
    GRATEFUL("grateful", "Grateful", 0xFFB5D99C.toInt(), "Thankful for something or someone."),
    PLAYFUL("playful", "Playful", 0xFF7FD6D9.toInt(), "Silly, wanting fun and teasing."),
    FLIRTY("flirty", "Flirty", 0xFFE08FC8.toInt(), "Cheeky and romantic."),
    HOPEFUL("hopeful", "Hopeful", 0xFFA9D6F5.toInt(), "Expecting things to go well."),
    CALM("calm", "Calm", 0xFF9FD8C4.toInt(), "Settled, nothing pressing on you."),
    COZY("cozy", "Cozy", 0xFFE3C7A0.toInt(), "Warm, comfortable and safe."),
    TIRED("tired", "Tired", 0xFFBDB1E8.toInt(), "Low on energy and needing rest."),
    BORED("bored", "Bored", 0xFFC9C2BB.toInt(), "Nothing is grabbing you."),
    SAD("sad", "Sad", 0xFF8FBBE3.toInt(), "Heavy or down, maybe teary."),
    LONELY("lonely", "Lonely", 0xFF8E9FE6.toInt(), "Wanting company or closeness."),
    ANXIOUS("anxious", "Anxious", 0xFF9FA3C7.toInt(), "Worried or buzzing, hard to settle."),
    STRESSED("stressed", "Stressed", 0xFFE8998A.toInt(), "Too much on, feeling the pressure."),
    ANGRY("angry", "Angry", 0xFFD64550.toInt(), "Hot and frustrated, something feels unfair."),
    LOVEY("lovey", "Lovey", 0xFFFFB8A8.toInt(), "Soft and affectionate, wanting cuddles."),
    NEEDY("needy", "Needy", 0xFFD3A8EA.toInt(), "Wanting love or attention."),
    CLINGY("clingy", "Clingy", 0xFFD9A3A8.toInt(), "Wanting to be close and held."),
    NOTSURE("notsure", "Not sure", 0xFFD2CCD6.toInt(), "Something feels off or mixed and you cannot name it. That is a real answer.");

    companion object {
        fun fromKey(key: String?): Mood = entries.firstOrNull { it.key == key } ?: HAPPY
        fun fromKeyOrNull(key: String?): Mood? = entries.firstOrNull { it.key == key }

        /** How the picker groups moods, to make it easier to narrow down a feeling. */
        val groups: List<Pair<String, List<Mood>>> = listOf(
            "Good, and buzzing" to listOf(HAPPY, EXCITED, PLAYFUL, FLIRTY),
            "Good, and soft" to listOf(LOVED, GRATEFUL, HOPEFUL, CALM, COZY),
            "Wanting closeness" to listOf(LOVEY, NEEDY, CLINGY),
            "Low or flat" to listOf(TIRED, BORED, SAD, LONELY),
            "Wound up" to listOf(ANXIOUS, STRESSED, ANGRY),
            "Cannot tell" to listOf(NOTSURE),
        )
    }
}

/** An optional "what would help" attached to a marble. */
enum class Need(val key: String, val label: String, val line: String) {
    HUG("hug", "A hug", "A hug would help"),
    SPACE("space", "Space", "Some space would help"),
    TALK("talk", "Talk to me", "Wants to talk"),
    DISTRACT("distract", "Distract me", "A distraction would help"),
    NEAR("near", "Just be near me", "Wants you nearby, no talking needed"),
    NOTHING("nothing", "Nothing, just telling you", "Nothing needed, just letting you know");

    companion object {
        fun fromKey(key: String?): Need? = entries.firstOrNull { it.key == key }
    }
}
