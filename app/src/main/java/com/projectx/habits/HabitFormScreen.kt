package com.projectx.habits

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.projectx.ui.PillGroup
import com.projectx.ui.TopBar
import com.projectx.ui.theme.LocalDark

enum class FormKind { NEW, EDIT, DEFAULTS }

private val Whitespace = Regex("[\\s,]+")

/** Cleans the options text; numbers keep only valid integers. */
private fun cleanOptions(text: String, type: CheckType) = text.split(Whitespace)
    .filter { it.isNotBlank() && (type != CheckType.NUMBER || it.toIntOrNull() != null) }
    .distinct()
    .joinToString(" ")

@Composable
fun HabitFormScreen(
    title: String,
    initial: Habit,
    kind: FormKind,
    onBack: () -> Unit,
    onSave: (Habit) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    var d by remember { mutableStateOf(initial.copy(options = initial.options.ifBlank { initial.checkType.defaultOptions })) }
    var confirmDelete by remember { mutableStateOf(false) }
    val type = d.checkType
    val options = cleanOptions(d.options, type)
    val valid = (kind == FormKind.DEFAULTS || d.name.isNotBlank()) && (type == CheckType.CHECK || options.isNotEmpty())

    // The form previews the habit's own color + light/dark live.
    HabitTheme(d.color, d.mode) {
        Scaffold { pad ->
            Column(Modifier.fillMaxSize().padding(pad).imePadding()) {
                TopBar(title, onBack)
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (kind == FormKind.DEFAULTS) {
                        Text(
                            "Every new habit starts with these settings.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp),
                        )
                    } else {
                        FormCard("Name") {
                            OutlinedTextField(
                                value = d.name,
                                onValueChange = { d = d.copy(name = it) },
                                placeholder = { Text("e.g. Gym, Study, Water") },
                                singleLine = true,
                                shape = RoundedCornerShape(18.dp),
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }

                    val typeLocked = kind == FormKind.EDIT
                    FormCard("Check type", if (typeLocked) "Can't be changed after creating the habit" else type.hint) {
                        PillGroup(
                            CheckType.entries.map { it.label },
                            selected = type.ordinal,
                            enabled = !typeLocked,
                            onSelect = { i ->
                                val t = CheckType.entries[i]
                                d = d.copy(type = t.id, options = t.defaultOptions)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    AnimatedVisibility(
                        type != CheckType.CHECK,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                        FormCard(
                            if (type == CheckType.EMOJI) "Emoji options" else "Number options",
                            "Separate with spaces",
                        ) {
                            OutlinedTextField(
                                value = d.options,
                                onValueChange = { d = d.copy(options = it) },
                                singleLine = true,
                                shape = RoundedCornerShape(18.dp),
                                isError = options.isEmpty(),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }

                    FormCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Multiple per day", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "Log more than once on the same day",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(d.multi, { d = d.copy(multi = it) })
                        }
                    }

                    FormCard("Color") {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            HabitColor.entries.forEach { c ->
                                ColorDot(c, selected = c.id == d.color) { d = d.copy(color = c.id) }
                            }
                        }
                    }

                    FormCard("Appearance", "Light or dark just for this habit, whatever the app uses") {
                        PillGroup(
                            HabitMode.entries.map { it.label },
                            selected = HabitMode.of(d.mode).ordinal,
                            onSelect = { d = d.copy(mode = HabitMode.entries[it].id) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    if (onDelete != null) {
                        TextButton(
                            onClick = { confirmDelete = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                        ) {
                            Icon(Icons.Rounded.DeleteOutline, null)
                            Spacer(Modifier.size(8.dp))
                            Text("Delete habit")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                Button(
                    onClick = { onSave(d.copy(name = d.name.trim(), options = options)) },
                    enabled = valid,
                    shape = CircleShape,
                    modifier = Modifier.fillMaxWidth().padding(20.dp).height(56.dp),
                ) {
                    Text(
                        when (kind) {
                            FormKind.NEW -> "Create habit"
                            else -> "Save"
                        },
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }

        if (confirmDelete && onDelete != null) {
            AlertDialog(
                onDismissRequest = { confirmDelete = false },
                title = { Text("Delete \"${d.name}\"?") },
                text = { Text("This removes the habit and all its history. It can't be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = { confirmDelete = false; onDelete() },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) { Text("Delete") }
                },
                dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
            )
        }
    }
}

@Composable
private fun FormCard(title: String? = null, hint: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            if (title != null) Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            if (hint != null) {
                Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (title != null || hint != null) Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun ColorDot(c: HabitColor, selected: Boolean, onClick: () -> Unit) {
    val accent = c.accent(LocalDark.current ?: true)
    val size by animateDpAsState(if (selected) 46.dp else 36.dp, spring(dampingRatio = 0.6f), label = "dot")
    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(size).clip(CircleShape).background(accent.primary).clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedVisibility(selected, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
                Icon(Icons.Rounded.Check, null, tint = accent.onPrimary)
            }
        }
    }
}
