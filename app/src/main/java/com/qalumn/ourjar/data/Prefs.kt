package com.qalumn.ourjar.data

import android.content.Context
import android.content.SharedPreferences
import com.qalumn.ourjar.domain.Mood
import org.json.JSONArray
import org.json.JSONObject

/** Small local store: who we are, plus a copy of the latest data for the home screen widgets. */
object Prefs {
    private lateinit var sp: SharedPreferences

    fun init(context: Context) {
        if (!::sp.isInitialized) {
            sp = context.applicationContext.getSharedPreferences("ourjar", Context.MODE_PRIVATE)
        }
    }

    /** 0 = follow the phone, 1 = light, 2 = dark. */
    val themeMode = kotlinx.coroutines.flow.MutableStateFlow(0)

    fun loadTheme() { themeMode.value = sp.getInt("themeMode", 0) }

    fun setTheme(mode: Int) {
        sp.edit().putInt("themeMode", mode).apply()
        themeMode.value = mode
    }

    fun session(): Session? {
        val code = sp.getString("code", null) ?: return null
        val id = sp.getString("memberId", null) ?: return null
        val name = sp.getString("myName", null) ?: return null
        return Session(code, id, name)
    }

    fun saveSession(s: Session) {
        sp.edit().putString("code", s.coupleCode).putString("memberId", s.memberId).putString("myName", s.myName).apply()
    }

    fun clearAll() {
        sp.edit().clear().apply()
    }

    // ---- widget cache ----

    var partnerName: String
        get() = sp.getString("partnerName", "") ?: ""
        set(v) { sp.edit().putString("partnerName", v).apply() }

    fun saveNotes(notes: List<Note>) {
        val arr = JSONArray()
        // Only the newest note from your partner is shown on the widget, so only keep its drawing.
        val myId = session()?.memberId
        val heroId = notes.firstOrNull { it.fromId != myId }?.id
        notes.take(20).forEach {
            val o = JSONObject().put("id", it.id).put("fromId", it.fromId).put("fromName", it.fromName)
                .put("text", it.text).put("createdAt", it.createdAt)
            if (it.id == heroId && it.drawing.isNotEmpty()) o.put("drawing", it.drawing)
            arr.put(o)
        }
        sp.edit().putString("cacheNotes", arr.toString()).apply()
    }

    fun loadNotes(): List<Note> {
        val raw = sp.getString("cacheNotes", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                Note(o.getString("id"), o.getString("fromId"), o.getString("fromName"), o.getString("text"), o.getLong("createdAt"), o.optString("drawing", ""))
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveMarbles(marbles: List<MarbleItem>) {
        val arr = JSONArray()
        marbles.forEach {
            arr.put(JSONObject().put("id", it.id).put("memberId", it.memberId).put("mood", it.mood.key).put("mood2", it.mood2?.key ?: "").put("createdAt", it.createdAt))
        }
        sp.edit().putString("cacheMarbles", arr.toString()).apply()
    }

    fun loadMarbles(): List<MarbleLite> {
        val raw = sp.getString("cacheMarbles", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                MarbleLite(o.getString("id"), o.optString("memberId", ""), Mood.fromKey(o.getString("mood")), o.getLong("createdAt"), Mood.fromKeyOrNull(o.optString("mood2", "")))
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
