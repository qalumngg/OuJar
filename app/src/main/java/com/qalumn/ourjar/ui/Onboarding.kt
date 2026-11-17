package com.qalumn.ourjar.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qalumn.ourjar.data.MarbleItem
import com.qalumn.ourjar.data.Member
import com.qalumn.ourjar.data.Repo
import com.qalumn.ourjar.domain.Mood
import com.qalumn.ourjar.domain.TimeFmt
import com.qalumn.ourjar.render.JarScene
import com.qalumn.ourjar.ui.theme.Fraunces
import com.qalumn.ourjar.ui.theme.Ours
import kotlinx.coroutines.launch
import java.time.LocalDate

private sealed interface Step {
    data object Welcome : Step
    data object Create : Step
    data object Join : Step
    data class Pick(val code: String, val people: List<Member>) : Step
}

private val sampleMarbles = JarScene.from(
    listOf(
        Mood.LOVED, Mood.CALM, Mood.HAPPY, Mood.COZY, Mood.EXCITED, Mood.TIRED, Mood.LOVED,
        Mood.CALM, Mood.HAPPY, Mood.SAD, Mood.LOVED,
    ).mapIndexed { i, m -> MarbleItem("welcome-$i", "", "", m, "", i.toLong()) },
)

@Composable
fun OnboardingScreen(welcomeCode: String?) {
    var step by remember { mutableStateOf<Step>(Step.Welcome) }

    Column(
        Modifier
            .fillMaxSize()
            .background(Ours.colors.bg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {
        if (welcomeCode != null) {
            CodeShown(welcomeCode)
        } else {
            when (val s = step) {
                Step.Welcome -> Welcome(onCreate = { step = Step.Create }, onJoin = { step = Step.Join })
                Step.Create -> CreateForm(onBack = { step = Step.Welcome })
                Step.Join -> JoinForm(onBack = { step = Step.Welcome }, onFound = { code, people -> step = Step.Pick(code, people) })
                is Step.Pick -> PickPerson(s.code, s.people, onBack = { step = Step.Join })
            }
        }
    }
}

@Composable
private fun ColumnScope.Welcome(onCreate: () -> Unit, onJoin: () -> Unit) {
    Spacer(Modifier.height(32.dp))
    JarView(
        marbles = sampleMarbles,
        newestId = null,
        modifier = Modifier.fillMaxWidth(0.6f).height(230.dp).align(Alignment.CenterHorizontally),
    )
    Spacer(Modifier.height(20.dp))
    Text(
        "Our Jar",
        style = MaterialTheme.typography.displayMedium,
        color = Ours.colors.ink,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(10.dp))
    Text(
        "A small shared space for love notes, moods, and the dates you are looking forward to.",
        style = MaterialTheme.typography.bodyLarge,
        color = Ours.colors.inkSoft,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
    )
    Spacer(Modifier.height(36.dp))
    PrimaryButton("Start our space", onCreate, Modifier.fillMaxWidth())
    Spacer(Modifier.height(12.dp))
    SoftButton("Join my partner", onJoin, Modifier.fillMaxWidth())
    Spacer(Modifier.height(24.dp))
}

@Composable
private fun StepHeader(title: String, body: String, onBack: () -> Unit) {
    QuietTextButton("Back", onBack, Modifier.padding(top = 12.dp))
    Text(title, style = MaterialTheme.typography.headlineLarge, color = Ours.colors.ink)
    Spacer(Modifier.height(8.dp))
    Text(body, style = MaterialTheme.typography.bodyLarge, color = Ours.colors.inkSoft)
    Spacer(Modifier.height(28.dp))
}

@Composable
private fun CreateForm(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var anniversary by remember { mutableStateOf<LocalDate?>(null) }
    var picking by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    StepHeader(
        "Let's set up",
        "Tell us who you are. Your partner joins with a code in the next step.",
        onBack,
    )
    OurField(name, { name = it.take(24) }, "Your name")
    Spacer(Modifier.height(12.dp))
    PickerField(
        "Our anniversary (optional)",
        anniversary?.let { TimeFmt.date(it) } ?: "Not set",
        onClick = { picking = true },
    )
    if (error != null) {
        Spacer(Modifier.height(12.dp))
        Text(error!!, style = MaterialTheme.typography.bodyMedium, color = Ours.colors.accent)
    }
    Spacer(Modifier.height(28.dp))
    PrimaryButton(
        if (busy) "Creating..." else "Create",
        onClick = {
            busy = true
            error = null
            scope.launch {
                try {
                    Repo.createCouple(name.trim(), anniversary)
                } catch (e: Exception) {
                    error = "Couldn't connect. Check your internet and try again."
                    busy = false
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
        enabled = name.isNotBlank() && !busy,
    )
    if (picking) {
        OurDatePickerDialog(
            initial = anniversary,
            onDismiss = { picking = false },
            onPicked = { anniversary = it; picking = false },
        )
    }
}

@Composable
private fun CodeShown(code: String) {
    val ctx = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val pretty = Repo.formatCode(code)
    var copied by remember { mutableStateOf(false) }

    Spacer(Modifier.height(56.dp))
    Text("You're all set", style = MaterialTheme.typography.headlineLarge, color = Ours.colors.ink)
    Spacer(Modifier.height(8.dp))
    Text(
        "Send this code to your partner. They open the app, tap Join, and type it in.",
        style = MaterialTheme.typography.bodyLarge,
        color = Ours.colors.inkSoft,
    )
    Spacer(Modifier.height(28.dp))
    OurCard(tinted = true, padding = androidx.compose.foundation.layout.PaddingValues(vertical = 28.dp, horizontal = 16.dp)) {
        Text(
            pretty,
            style = MaterialTheme.typography.displaySmall.copy(fontSize = 34.sp, letterSpacing = 2.sp, fontFamily = Fraunces),
            color = Ours.colors.ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    Spacer(Modifier.height(16.dp))
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SoftButton(
            if (copied) "Copied" else "Copy code",
            onClick = { clipboard.setText(AnnotatedString(pretty)); copied = true },
            modifier = Modifier.fillMaxWidth(),
        )
        SoftButton(
            "Share code",
            onClick = {
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "Join me in Our Jar. Open the app, tap Join my partner, and enter this code: $pretty")
                }
                ctx.startActivity(Intent.createChooser(send, "Share code"))
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
    Spacer(Modifier.height(32.dp))
    PrimaryButton("Continue", { Repo.welcomeCode.value = null }, Modifier.fillMaxWidth())
    Spacer(Modifier.height(8.dp))
    Text(
        "You can find this code again any time in the Us tab.",
        style = MaterialTheme.typography.bodySmall,
        color = Ours.colors.inkSoft,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(24.dp))
}

@Composable
private fun JoinForm(onBack: () -> Unit, onFound: (String, List<Member>) -> Unit) {
    val scope = rememberCoroutineScope()
    var code by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    StepHeader("Join your partner", "Type the code they sent you. It has 10 letters and numbers.", onBack)
    OurField(
        code,
        { code = it.uppercase().take(12) },
        "Couple code",
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
    )
    if (error != null) {
        Spacer(Modifier.height(12.dp))
        Text(error!!, style = MaterialTheme.typography.bodyMedium, color = Ours.colors.accent)
    }
    Spacer(Modifier.height(28.dp))
    PrimaryButton(
        if (busy) "Looking..." else "Find us",
        onClick = {
            busy = true
            error = null
            scope.launch {
                try {
                    val (clean, people) = Repo.lookupCouple(code)
                    onFound(clean, people)
                } catch (e: IllegalStateException) {
                    error = e.message
                } catch (e: Exception) {
                    error = "Couldn't connect. Check your internet and try again."
                }
                busy = false
            }
        },
        modifier = Modifier.fillMaxWidth(),
        enabled = code.length >= 5 && !busy,
    )
}

@Composable
private fun PickPerson(code: String, people: List<Member>, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun join(existing: Member?) {
        busy = true
        error = null
        scope.launch {
            try {
                Repo.joinCouple(code, existing, name)
            } catch (e: Exception) {
                error = e.message ?: "Couldn't connect. Try again."
                busy = false
            }
        }
    }

    val full = people.size >= 2
    StepHeader(
        "Found you",
        if (people.isEmpty()) "Add your name to join."
        else if (full) "This couple is already full. If you were set up before, pick your name to get back in."
        else "If you were already set up on another phone, pick your name. Otherwise add yours below.",
        onBack,
    )
    if (people.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            people.forEach { p ->
                SoftButton("I'm ${p.name}", { join(p) }, Modifier.fillMaxWidth(), enabled = !busy)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
    if (!full) {
        OurField(name, { name = it.take(24) }, "Your name")
        Spacer(Modifier.height(20.dp))
        PrimaryButton("Join", { join(null) }, Modifier.fillMaxWidth(), enabled = name.isNotBlank() && !busy)
    }
    if (error != null) {
        Spacer(Modifier.height(12.dp))
        Text(error!!, style = MaterialTheme.typography.bodyMedium, color = Ours.colors.accent)
    }
}
