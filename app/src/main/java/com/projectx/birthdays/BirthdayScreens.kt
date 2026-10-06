@file:OptIn(ExperimentalMaterial3Api::class)

package com.projectx.birthdays

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Cake
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.projectx.Prefs
import com.projectx.ui.CalendarCard
import com.projectx.ui.ColorRow
import com.projectx.ui.DayCell
import com.projectx.ui.FormCard
import com.projectx.ui.MonthGrid
import com.projectx.ui.PeriodBar
import com.projectx.ui.PillGroup
import com.projectx.ui.RoundIconButton
import com.projectx.ui.TopBar
import com.projectx.ui.YearGrid
import com.projectx.ui.pressScale
import com.projectx.ui.rememberEnableNotifications
import com.projectx.ui.theme.ModeChoice
import com.projectx.ui.theme.ModuleTheme
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val DayFormat = DateTimeFormatter.ofPattern("EEE, d MMM")

// ---------------------------------------------------------------- Main: calendar / list

@Composable
fun BirthdaysScreen(
    dao: BirthdayDao,
    prefs: Prefs,
    onBack: () -> Unit,
    onAdd: (prefillDay: Long?) -> Unit,
    onEdit: (Long) -> Unit,
    onSettings: () -> Unit,
) {
    val people by remember { dao.people() }.collectAsState(null)
    val today = LocalDate.now()
    var listView by rememberSaveable { mutableStateOf(false) }
    var monthView by rememberSaveable { mutableStateOf(true) }
    var ym by rememberSaveable { mutableStateOf(YearMonth.now()) }
    var sheetDay by rememberSaveable { mutableStateOf<Long?>(null) }

    ModuleTheme(prefs.bdayColor, prefs.bdayMode) {
        Scaffold { pad ->
            Column(Modifier.fillMaxSize().padding(pad)) {
                TopBar("Birthdays", onBack) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RoundIconButton(Icons.Rounded.Add, "Add birthday") { onAdd(null) }
                        RoundIconButton(Icons.Rounded.Settings, "Birthday settings", onSettings)
                    }
                }
                val list = people
                Box(Modifier.weight(1f)) {
                    if (list != null) {
                        Crossfade(listView, label = "view") { showList ->
                            if (showList) {
                                PeopleList(list, today, onEdit)
                            } else {
                                CalendarView(list, today, monthView, { monthView = it }, ym, { ym = it }) { sheetDay = it }
                            }
                        }
                    }
                }
                PillGroup(
                    listOf("Calendar", "List"),
                    selected = if (listView) 1 else 0,
                    onSelect = { listView = it == 1 },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                )
            }
        }

        sheetDay?.let { day ->
            DaySheet(
                day = LocalDate.ofEpochDay(day),
                people = people.orEmpty().filter { it.dateIn(LocalDate.ofEpochDay(day).year).toEpochDay() == day },
                onEdit = { sheetDay = null; onEdit(it) },
                onAdd = { sheetDay = null; onAdd(day) },
                onDismiss = { sheetDay = null },
            )
        }
    }
}

@Composable
private fun CalendarView(
    people: List<Person>,
    today: LocalDate,
    monthView: Boolean,
    onMonthView: (Boolean) -> Unit,
    ym: YearMonth,
    onYm: (YearMonth) -> Unit,
    onDay: (Long) -> Unit,
) {
    val byDay = remember(people, ym.year) { people.groupBy { it.dateIn(ym.year).toEpochDay() } }
    val todayDay = today.toEpochDay()
    val shift = { by: Int -> onYm(if (monthView) ym.plusMonths(by.toLong()) else ym.plusYears(by.toLong())) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PeriodBar(monthView, onMonthView, shift)
        CalendarCard(monthView, ym, shift) { isMonth, period ->
            val cs = MaterialTheme.colorScheme
            if (isMonth) {
                MonthGrid(period) { day, label ->
                    val here = byDay[day].orEmpty()
                    val bg by animateColorAsState(if (here.isEmpty()) cs.surfaceContainerHigh else cs.primary, label = "bday")
                    DayCell(
                        dayOfMonth = label,
                        bg = bg,
                        fg = if (here.isEmpty()) cs.onSurface else cs.onPrimary,
                        isToday = day == todayDay,
                        ring = if (here.isEmpty()) cs.primary else cs.onSurface,
                        onClick = { onDay(day) },
                        badge = when (here.size) {
                            0 -> null
                            1 -> "🎂"
                            else -> "🎂${here.size}"
                        },
                        badgeSize = 12.sp,
                    )
                }
            } else {
                YearGrid(
                    period.year,
                    total = { m -> people.count { it.month == m.monthValue } },
                    dot = { day -> if (byDay[day] != null) cs.primary else cs.outlineVariant },
                ) { onMonthView(true); onYm(it) }
            }
        }
        people.minByOrNull { it.daysUntil(today) }?.let { NextUp(it, today) }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun NextUp(p: Person, today: LocalDate) {
    val next = p.next(today)
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.primaryContainer) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Cake, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Next up", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(
                    p.name + (p.turns(next.year)?.let { " turns $it" } ?: ""),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Text(
                untilLabel(p.daysUntil(today)),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun PeopleList(people: List<Person>, today: LocalDate, onEdit: (Long) -> Unit) {
    val sorted = remember(people, today) { people.sortedBy { it.daysUntil(today) } }
    if (sorted.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Text(
                "No birthdays yet.\nTap + to add your first one.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
        return
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(sorted, key = { it.id }) { p -> PersonRow(p, today, Modifier.animateItem()) { onEdit(p.id) } }
    }
}

@Composable
private fun PersonRow(p: Person, today: LocalDate, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val next = p.next(today)
    val days = p.daysUntil(today)
    val source = remember { MutableInteractionSource() }
    val scale = pressScale(source)
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = if (days == 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        interactionSource = source,
        modifier = modifier.fillMaxWidth().graphicsLayer { scaleX = scale; scaleY = scale },
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(p.name)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(p.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    (p.turns(next.year)?.let { "Turns $it · " } ?: "") + next.format(DayFormat),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                untilLabel(days),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = if (days <= 7) FontWeight.Bold else null,
            )
        }
    }
}

@Composable
private fun Avatar(name: String, size: Int = 44) {
    Box(
        Modifier.size(size.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            name.trim().take(1).uppercase(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun DaySheet(day: LocalDate, people: List<Person>, onEdit: (Long) -> Unit, onAdd: () -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp).navigationBarsPadding()) {
            Text(
                day.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(16.dp))
            if (people.isEmpty()) {
                Text("No birthdays on this day", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            people.forEach { p ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).clickable { onEdit(p.id) }.padding(vertical = 10.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Avatar(p.name)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("🎂 " + p.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        val sub = listOfNotNull(
                            p.turns(day.year)?.let { "Turns $it" },
                            p.year?.let { "Born ${LocalDate.of(it, p.month, p.day).format(DateTimeFormatter.ofPattern("d MMM yyyy"))}" },
                        ).joinToString(" · ")
                        if (sub.isNotEmpty()) Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (p.note.isNotBlank()) Text(p.note, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            FilledTonalButton(onClick = onAdd, shape = CircleShape, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Icon(Icons.Rounded.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("Add a birthday on this day")
            }
        }
    }
}

// ---------------------------------------------------------------- Add / edit

@Composable
fun BirthdayFormScreen(
    initial: Person,
    isNew: Boolean,
    prefs: Prefs,
    onBack: () -> Unit,
    onSave: (Person) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var name by remember { mutableStateOf(initial.name) }
    // Stored as a full date for the picker; the year is ignored when unknown.
    var date by remember { mutableStateOf(LocalDate.of(initial.year ?: 2000, initial.month, initial.day)) }
    var yearKnown by remember { mutableStateOf(initial.year != null) }
    var note by remember { mutableStateOf(initial.note) }
    var picking by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    ModuleTheme(prefs.bdayColor, prefs.bdayMode) {
        Scaffold { pad ->
            Column(Modifier.fillMaxSize().padding(pad).imePadding()) {
                TopBar(if (isNew) "New birthday" else "Edit birthday", onBack)
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    FormCard("Name") {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            placeholder = { Text("e.g. Mom, Riya") },
                            singleLine = true,
                            shape = RoundedCornerShape(18.dp),
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    FormCard("Birthday") {
                        Surface(
                            onClick = { picking = true },
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Cake, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    date.format(DateTimeFormatter.ofPattern(if (yearKnown) "d MMMM yyyy" else "d MMMM")),
                                    style = MaterialTheme.typography.titleMedium,
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("I know the year", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "Shows the age they're turning",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(yearKnown, { yearKnown = it })
                        }
                    }
                    FormCard("Note", "Gift ideas, wishes… (optional)") {
                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            shape = RoundedCornerShape(18.dp),
                            minLines = 2,
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
                            Text("Delete birthday")
                        }
                    }
                }
                Button(
                    onClick = {
                        onSave(
                            initial.copy(
                                name = name.trim(),
                                month = date.monthValue,
                                day = date.dayOfMonth,
                                year = if (yearKnown) date.year else null,
                                note = note.trim(),
                            )
                        )
                    },
                    enabled = name.isNotBlank(),
                    shape = CircleShape,
                    modifier = Modifier.fillMaxWidth().padding(20.dp).height(56.dp),
                ) { Text(if (isNew) "Add birthday" else "Save", style = MaterialTheme.typography.titleMedium) }
            }
        }

        if (picking) {
            val state = rememberDatePickerState(
                initialSelectedDateMillis = date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
            )
            DatePickerDialog(
                onDismissRequest = { picking = false },
                confirmButton = {
                    TextButton(onClick = {
                        state.selectedDateMillis?.let { date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                        picking = false
                    }) { Text("OK") }
                },
                dismissButton = { TextButton(onClick = { picking = false }) { Text("Cancel") } },
                shape = RoundedCornerShape(28.dp),
            ) { DatePicker(state) }
        }

        if (confirmDelete && onDelete != null) {
            AlertDialog(
                onDismissRequest = { confirmDelete = false },
                title = { Text("Delete ${initial.name}'s birthday?") },
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

// ---------------------------------------------------------------- Settings

private val DaysBefore = listOf(0 to "On the day", 1 to "1 day", 3 to "3 days", 7 to "1 week")

@Composable
fun BirthdaySettingsScreen(prefs: Prefs, onBack: () -> Unit) {
    val context = LocalContext.current
    val enableNotifications = rememberEnableNotifications(prefs) { prefs.updateBdayRemind(it) }
    val remindersOn = prefs.bdayRemind && prefs.notifications

    ModuleTheme(prefs.bdayColor, prefs.bdayMode) {
        Scaffold { pad ->
            Column(
                Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TopBar("Birthday settings", onBack)
                Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FormCard("Color") { ColorRow(prefs.bdayColor, prefs::updateBdayColor) }
                    FormCard("Appearance", "Light or dark just for Birthdays") {
                        PillGroup(
                            ModeChoice.entries.map { it.label },
                            selected = ModeChoice.of(prefs.bdayMode).ordinal,
                            onSelect = { prefs.updateBdayMode(ModeChoice.entries[it].id) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    FormCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Reminders", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "A notification around 9 AM",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(remindersOn, { on -> if (on) enableNotifications() else prefs.updateBdayRemind(false) })
                        }
                        if (remindersOn) {
                            Spacer(Modifier.height(16.dp))
                            Text("Remind me", style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(8.dp))
                            PillGroup(
                                DaysBefore.map { it.second },
                                selected = DaysBefore.indexOfFirst { it.first == prefs.bdayDaysBefore }.coerceAtLeast(0),
                                onSelect = { prefs.updateBdayDaysBefore(DaysBefore[it].first) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            if (prefs.bdayDaysBefore > 0) {
                                Spacer(Modifier.height(16.dp))
                                Text("Repeat", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    if (prefs.bdayDaily) "Every day until the birthday" else "Once before, and again on the day",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.height(8.dp))
                                PillGroup(
                                    listOf("One time", "Every day"),
                                    selected = if (prefs.bdayDaily) 1 else 0,
                                    onSelect = { prefs.updateBdayDaily(it == 1) },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }

                    FormCard("Home screen widget", "This month's birthdays. Tap it to switch list ↔ calendar, tap the title to open the app.") {
                        val manager = AppWidgetManager.getInstance(context)
                        if (manager.isRequestPinAppWidgetSupported) {
                            Button(
                                onClick = {
                                    manager.requestPinAppWidget(ComponentName(context, BirthdayWidgetReceiver::class.java), null, null)
                                },
                                shape = CircleShape,
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                            ) { Text("Add widget to home screen") }
                        } else {
                            Text(
                                "Long-press your home screen → Widgets → PR-X.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}
