package com.qalumn.ourjar.data

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.qalumn.ourjar.domain.JarGeometry
import com.qalumn.ourjar.domain.Mood
import com.qalumn.ourjar.domain.Need
import com.qalumn.ourjar.domain.TimeFmt
import com.qalumn.ourjar.widget.WidgetSync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

/**
 * All talking to Firebase happens here. Data lives under couples/{code}/...
 * The code is the shared secret that links the two phones.
 */
object Repo {
    private const val ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"
    private const val CODE_LENGTH = 10

    private lateinit var appContext: Context
    private var initialised = false
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val session = MutableStateFlow<Session?>(null)
    val members = MutableStateFlow<List<Member>>(emptyList())
    val notes = MutableStateFlow<List<Note>>(emptyList())
    val marbles = MutableStateFlow<List<MarbleItem>>(emptyList())
    val dates = MutableStateFlow<List<DateItem>>(emptyList())
    val couple = MutableStateFlow(CoupleInfo(null))

    /** Set right after creating a couple so the app can show the share-code screen before the main app. */
    val welcomeCode = MutableStateFlow<String?>(null)

    private val registrations = mutableListOf<ListenerRegistration>()
    private var listenJob: Job? = null
    private val widgetPing = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private val db get() = FirebaseFirestore.getInstance()
    private val auth get() = FirebaseAuth.getInstance()

    @OptIn(FlowPreview::class)
    fun init(context: Context) {
        if (initialised) return
        initialised = true
        appContext = context.applicationContext
        Prefs.init(appContext)
        session.value = Prefs.session()
        scope.launch {
            widgetPing.debounce(700).collect {
                try {
                    WidgetSync.updateAll(appContext)
                } catch (e: Exception) {
                    // Widgets are a nice extra; never crash the app over them.
                }
            }
        }
        startListening()
    }

    fun pingWidgets() {
        widgetPing.tryEmit(Unit)
    }

    // ---- codes ----

    private fun generateCode(): String {
        val rnd = SecureRandom()
        return buildString { repeat(CODE_LENGTH) { append(ALPHABET[rnd.nextInt(ALPHABET.length)]) } }
    }

    fun normalizeCode(raw: String): String = raw.uppercase().filter { it in ALPHABET }

    fun formatCode(code: String): String =
        if (code.length == CODE_LENGTH) code.substring(0, 5) + "-" + code.substring(5) else code

    // ---- auth ----

    suspend fun ensureAuth() {
        if (auth.currentUser == null) auth.signInAnonymously().await()
    }

    // ---- pairing ----

    private fun couples() = db.collection("couples")

    suspend fun createCouple(name: String, anniversary: LocalDate?): String {
        ensureAuth()
        var code = generateCode()
        while (couples().document(code).get().await().exists()) code = generateCode()
        val now = System.currentTimeMillis()
        val base = couples().document(code)
        base.set(
            mapOf("createdAt" to now, "anniversary" to anniversary?.toString()),
        ).await()
        val memberId = UUID.randomUUID().toString()
        base.collection("members").document(memberId).set(mapOf("name" to name, "joinedAt" to now)).await()
        welcomeCode.value = code
        becomeMember(Session(code, memberId, name))
        return code
    }

    /** Checks a code and returns it (cleaned up) with the people already in that couple. */
    suspend fun lookupCouple(rawCode: String): Pair<String, List<Member>> {
        ensureAuth()
        val code = normalizeCode(rawCode)
        if (code.length != CODE_LENGTH) error("That code looks too short. It has 10 letters and numbers.")
        val snap = couples().document(code).get().await()
        if (!snap.exists()) error("We couldn't find that code. Check it and try again.")
        val people = snap.reference.collection("members").get().await().documents.mapNotNull { parseMember(it) }
        return code to people
    }

    /** Join as someone who is already in the couple (reinstalls) or as a brand new second person. */
    suspend fun joinCouple(code: String, existing: Member?, newName: String?) {
        ensureAuth()
        if (existing != null) {
            becomeMember(Session(code, existing.id, existing.name))
            return
        }
        val name = newName?.trim().orEmpty()
        if (name.isEmpty()) error("Add your name first.")
        val memberId = UUID.randomUUID().toString()
        couples().document(code).collection("members").document(memberId)
            .set(mapOf("name" to name, "joinedAt" to System.currentTimeMillis())).await()
        becomeMember(Session(code, memberId, name))
    }

    private fun becomeMember(s: Session) {
        Prefs.saveSession(s)
        session.value = s
        startListening()
    }

    fun leave() {
        stopListening()
        Prefs.clearAll()
        session.value = null
        members.value = emptyList()
        notes.value = emptyList()
        marbles.value = emptyList()
        dates.value = emptyList()
        couple.value = CoupleInfo(null)
        pingWidgets()
    }

    // ---- listening ----

    private fun stopListening() {
        listenJob?.cancel()
        listenJob = null
        registrations.forEach { it.remove() }
        registrations.clear()
    }

    private fun startListening() {
        stopListening()
        val s = session.value ?: return
        listenJob = scope.launch {
            while (true) {
                try {
                    ensureAuth()
                    break
                } catch (e: Exception) {
                    delay(8000)
                }
            }
            attach(s)
        }
    }

    private fun attach(s: Session) {
        val base = couples().document(s.coupleCode)

        registrations += base.addSnapshotListener { snap, _ ->
            if (snap == null) return@addSnapshotListener
            val ann = snap.getString("anniversary")?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            couple.value = CoupleInfo(ann)
        }

        registrations += base.collection("members").addSnapshotListener { qs, _ ->
            if (qs == null) return@addSnapshotListener
            val list = qs.documents.mapNotNull { parseMember(it) }
            members.value = list
            Prefs.partnerName = list.firstOrNull { it.id != s.memberId }?.name.orEmpty()
            pingWidgets()
        }

        registrations += base.collection("notes").orderBy("createdAt", Query.Direction.DESCENDING).limit(100)
            .addSnapshotListener { qs, _ ->
                if (qs == null) return@addSnapshotListener
                val list = qs.documents.mapNotNull { parseNote(it) }
                notes.value = list
                Prefs.saveNotes(list)
                pingWidgets()
            }

        registrations += base.collection("marbles").orderBy("createdAt", Query.Direction.DESCENDING).limit(200)
            .addSnapshotListener { qs, _ ->
                if (qs == null) return@addSnapshotListener
                val list = qs.documents.mapNotNull { parseMarble(it) }
                marbles.value = list
                Prefs.saveMarbles(thisWeek(list))
                pingWidgets()
            }

        registrations += base.collection("dates").addSnapshotListener { qs, _ ->
            if (qs == null) return@addSnapshotListener
            dates.value = qs.documents.mapNotNull { parseDate(it) }
        }
    }

    /** One-off fetch used by the background sync, when the app itself isn't running. */
    suspend fun fetchOnce() {
        val s = Prefs.session() ?: return
        ensureAuth()
        val base = couples().document(s.coupleCode)
        val people = base.collection("members").get().await().documents.mapNotNull { parseMember(it) }
        Prefs.partnerName = people.firstOrNull { it.id != s.memberId }?.name.orEmpty()
        val ns = base.collection("notes").orderBy("createdAt", Query.Direction.DESCENDING).limit(30)
            .get().await().documents.mapNotNull { parseNote(it) }
        Prefs.saveNotes(ns)
        val ms = base.collection("marbles")
            .whereGreaterThanOrEqualTo("createdAt", TimeFmt.startOfWeekMillis())
            .orderBy("createdAt", Query.Direction.DESCENDING).limit(60)
            .get().await().documents.mapNotNull { parseMarble(it) }
        Prefs.saveMarbles(thisWeek(ms))
    }

    private fun thisWeek(list: List<MarbleItem>): List<MarbleItem> {
        val start = TimeFmt.startOfWeekMillis()
        return list.filter { it.createdAt >= start }.take(JarGeometry.CAPACITY)
    }

    // ---- parsing ----

    private fun parseMember(d: DocumentSnapshot): Member? {
        val name = d.getString("name") ?: return null
        return Member(d.id, name)
    }

    private fun parseNote(d: DocumentSnapshot): Note? {
        val text = d.getString("text").orEmpty()
        val drawing = d.getString("drawing").orEmpty()
        if (text.isBlank() && drawing.isBlank()) return null
        return Note(d.id, d.getString("fromId").orEmpty(), d.getString("fromName").orEmpty(), text, d.getLong("createdAt") ?: 0L, drawing)
    }

    private fun parseMarble(d: DocumentSnapshot): MarbleItem? {
        val mood = d.getString("mood") ?: return null
        return MarbleItem(
            d.id, d.getString("memberId").orEmpty(), d.getString("memberName").orEmpty(),
            Mood.fromKey(mood), d.getString("note").orEmpty(), d.getLong("createdAt") ?: 0L,
            Mood.fromKeyOrNull(d.getString("mood2")), Need.fromKey(d.getString("need")),
        )
    }

    private fun parseDate(d: DocumentSnapshot): DateItem? {
        val title = d.getString("title") ?: return null
        val date = d.getString("date")?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return null
        val time = d.getString("time")?.takeIf { it.isNotBlank() }?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
        return DateItem(
            d.id, title, date, time, DateKind.fromKey(d.getString("kind")),
            d.getBoolean("yearly") ?: false, d.getString("notes").orEmpty(), d.getString("createdBy").orEmpty(),
        )
    }

    // ---- writing (fire and forget: the local cache updates instantly, Firebase syncs when online) ----

    private fun base(s: Session) = couples().document(s.coupleCode)

    fun sendNote(text: String, drawing: String = "") {
        val s = session.value ?: return
        val clean = text.trim()
        if (clean.isEmpty() && drawing.isEmpty()) return
        val data = mutableMapOf<String, Any>(
            "fromId" to s.memberId, "fromName" to s.myName, "text" to clean, "createdAt" to System.currentTimeMillis(),
        )
        if (drawing.isNotEmpty()) data["drawing"] = drawing
        base(s).collection("notes").document().set(data)
    }

    fun deleteNote(id: String) {
        val s = session.value ?: return
        base(s).collection("notes").document(id).delete()
    }

    fun addMarble(mood: Mood, mood2: Mood?, need: Need?, note: String) {
        val s = session.value ?: return
        base(s).collection("marbles").document().set(
            mapOf(
                "memberId" to s.memberId, "memberName" to s.myName, "mood" to mood.key,
                "mood2" to (mood2?.key ?: ""), "need" to (need?.key ?: ""),
                "note" to note.trim(), "createdAt" to System.currentTimeMillis(),
            ),
        )
    }

    fun deleteMarble(id: String) {
        val s = session.value ?: return
        base(s).collection("marbles").document(id).delete()
    }

    fun saveDate(item: DateItem) {
        val s = session.value ?: return
        val col = base(s).collection("dates")
        val ref = if (item.id.isEmpty()) col.document() else col.document(item.id)
        ref.set(
            mapOf(
                "title" to item.title.trim(),
                "date" to item.date.toString(),
                "time" to (item.time?.toString() ?: ""),
                "kind" to item.kind.key,
                "yearly" to item.yearly,
                "notes" to item.notes.trim(),
                "createdBy" to item.createdBy.ifEmpty { s.memberId },
            ),
        )
    }

    fun deleteDate(id: String) {
        val s = session.value ?: return
        base(s).collection("dates").document(id).delete()
    }

    fun setAnniversary(date: LocalDate?) {
        val s = session.value ?: return
        if (date == null) {
            base(s).set(mapOf("anniversary" to FieldValue.delete()), SetOptions.merge())
        } else {
            base(s).set(mapOf("anniversary" to date.toString()), SetOptions.merge())
        }
    }
}
