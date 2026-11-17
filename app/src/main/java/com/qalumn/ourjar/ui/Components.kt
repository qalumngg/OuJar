@file:OptIn(ExperimentalMaterial3Api::class)

package com.qalumn.ourjar.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.qalumn.ourjar.ui.theme.Ours

@Composable
fun ScreenTitle(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(top = 20.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineLarge, color = Ours.colors.ink)
            if (subtitle != null) {
                Spacer(Modifier.size(2.dp))
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Ours.colors.inkSoft)
            }
        }
        if (trailing != null) trailing()
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = Ours.colors.inkSoft,
        modifier = modifier.padding(top = 24.dp, bottom = 10.dp),
    )
}

@Composable
fun OurCard(
    modifier: Modifier = Modifier,
    tinted: Boolean = false,
    onClick: (() -> Unit)? = null,
    padding: PaddingValues = PaddingValues(18.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = Ours.colors
    val shape = RoundedCornerShape(22.dp)
    val color = if (tinted) c.cardAlt else c.card
    val border = BorderStroke(1.dp, c.line)
    if (onClick != null) {
        Surface(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = shape, color = color, border = border) {
            Column(Modifier.padding(padding), content = content)
        }
    } else {
        Surface(modifier = modifier.fillMaxWidth(), shape = shape, color = color, border = border) {
            Column(Modifier.padding(padding), content = content)
        }
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val c = Ours.colors
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 52.dp),
        enabled = enabled,
        shape = CircleShape,
        elevation = null,
        colors = ButtonDefaults.buttonColors(
            containerColor = c.primary,
            contentColor = c.onPrimary,
            disabledContainerColor = c.line,
            disabledContentColor = c.inkSoft,
        ),
        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 12.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun SoftButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val c = Ours.colors
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        enabled = enabled,
        shape = CircleShape,
        elevation = null,
        border = BorderStroke(1.dp, c.line),
        colors = ButtonDefaults.buttonColors(
            containerColor = c.card,
            contentColor = c.ink,
            disabledContainerColor = c.line,
            disabledContentColor = c.inkSoft,
        ),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun QuietTextButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, danger: Boolean = false) {
    TextButton(onClick = onClick, modifier = modifier) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = if (danger) Ours.colors.accent else Ours.colors.inkSoft,
        )
    }
}

@Composable
fun OurField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minLines: Int = 1,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    val c = Ours.colors
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = keyboardOptions,
        textStyle = textStyle.copy(color = c.ink),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = c.ink,
            unfocusedTextColor = c.ink,
            focusedContainerColor = c.card,
            unfocusedContainerColor = c.card,
            cursorColor = c.accent,
            focusedBorderColor = c.accent,
            unfocusedBorderColor = c.line,
            focusedLabelColor = c.accent,
            unfocusedLabelColor = c.inkSoft,
        ),
    )
}

/** A tappable read-only row that looks like a field, used to open the date and time pickers. */
@Composable
fun PickerField(label: String, value: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Ours.colors
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = c.card,
        border = BorderStroke(1.dp, c.line),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = c.inkSoft)
            Spacer(Modifier.size(2.dp))
            Text(value, style = MaterialTheme.typography.bodyLarge, color = c.ink)
        }
    }
}

@Composable
fun Pill(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, dot: Color? = null) {
    val c = Ours.colors
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = CircleShape,
        color = if (selected) c.primaryContainer else c.card,
        border = BorderStroke(1.dp, if (selected) c.accent else c.line),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (dot != null) {
                Box(Modifier.size(8.dp).background(dot, CircleShape))
                Spacer(Modifier.size(8.dp))
            }
            Text(
                text,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) c.onPrimaryContainer else c.ink,
            )
        }
    }
}

@Composable
fun Initial(name: String, isMe: Boolean, size: Dp = 32.dp) {
    val c = Ours.colors
    Box(
        Modifier.size(size).background(if (isMe) c.primaryContainer else c.partnerTint, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            name.trim().take(1).uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = c.ink,
        )
    }
}

/** A small glassy marble, drawn the same way as the ones in the jar. */
@Composable
fun MarbleDot(color: Color, size: Dp, color2: Color? = null, seed: Int = 0x55) {
    Canvas(Modifier.size(size)) {
        val r = this.size.minDimension / 2f
        drawIntoCanvas { c ->
            com.qalumn.ourjar.render.JarRenderer.drawMarble(
                c.nativeCanvas, r, r, r, color.toArgb(), color2?.toArgb(), seed,
            )
        }
    }
}

@Composable
fun EmptyNote(title: String, body: String, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(vertical = 28.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = Ours.colors.ink, textAlign = TextAlign.Center)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = Ours.colors.inkSoft, textAlign = TextAlign.Center)
    }
}
