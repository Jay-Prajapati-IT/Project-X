@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)

package com.projectx.habits

import com.projectx.ui.theme.ModuleTheme
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.projectx.ui.AnimatedNumber
import com.projectx.ui.CalendarCard
import com.projectx.ui.DayCell
import com.projectx.ui.MonthGrid
import com.projectx.ui.PeriodBar
import com.projectx.ui.YearGrid
import com.projectx.ui.RoundIconButton
import com.projectx.ui.TopBar
import com.projectx.ui.pressScale
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.sign


@Composable
fun HabitCalendarScreen(dao: HabitDao, id: Long, onBack: () -> Unit, onEdit: () -> Unit) {
    val habit by remember(id) { dao.habit(id) }.collectAsState(null)
    val h = habit ?: return
    ModuleTheme(h.color, h.mode) { CalendarContent(dao, h, onBack, onEdit) }
}

@Composable
private fun CalendarContent(dao: HabitDao, h: Habit, onBack: () -> Unit, onEdit: () -> Unit) {
    val entries by remember(h.id) { dao.entries(h.id) }.collectAsState(emptyList())
    val byDay = remember(entries) { entries.groupBy { it.day } }
    val today = LocalDate.now().toEpochDay()
    val now = YearMonth.now()
    var monthView by rememberSaveable { mutableStateOf(true) }
    var ym by rememberSaveable { mutableStateOf(now) }
    var sheetDay by rememberSaveable { mutableStateOf<Long?>(null) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    val canNext = if (monthView) ym < now else ym.year < now.year
    fun shift(by: Long) {
        if (by > 0 && !canNext) return
        ym = minOf(if (monthView) ym.plusMonths(by) else ym.plusYears(by), now)
    }

    fun add(day: Long, value: Int = 1, emoji: String? = null) = scope.launch {
        val e = Entry(habitId = h.id, day = day, value = value, emoji = emoji)
        if (h.multi) dao.add(e) else dao.replaceDay(e)
    }

    // Check habits log straight from a tap; emoji/number habits need a choice, so they open the day sheet.
    fun tap(day: Long) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        when {
            h.checkType != CheckType.CHECK -> sheetDay = day
            h.multi || byDay[day].isNullOrEmpty() -> add(day)
            else -> scope.launch { dao.clearDay(h.id, day) }
        }
    }

    Scaffold { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            TopBar(h.name, onBack) { RoundIconButton(Icons.Rounded.Settings, "Habit settings", onEdit) }

            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PeriodBar(monthView, { monthView = it }, { shift(it.toLong()) }, canNext)
                CalendarCard(monthView, ym, { shift(it.toLong()) }) { isMonth, period ->
                    if (isMonth) {
                        HabitMonth(period, h, byDay, today, ::tap) { sheetDay = it }
                    } else {
                        YearGrid(
                            period.year,
                            total = { m -> (1..m.lengthOfMonth()).sumOf { byDay[m.atDay(it).toEpochDay()].total() } },
                            dot = { day ->
                                val cs = MaterialTheme.colorScheme
                                when {
                                    byDay[day] != null -> cs.primary
                                    day > today -> cs.outlineVariant.copy(alpha = 0.4f)
                                    else -> cs.outlineVariant
                                }
                            },
                        ) { monthView = true; ym = it }
                    }
                }
                Text(
                    when {
                        h.checkType != CheckType.CHECK -> "Tap a day to log it"
                        h.multi -> "Tap a day to add one · hold to edit"
                        else -> "Tap a day to mark or unmark it"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }

            StatsCard(h, entries, byDay, today, monthView, ym) { tap(today) }
        }
    }

    sheetDay?.let { day ->
        DaySheet(
            h = h,
            day = day,
            list = byDay[day].orEmpty(),
            onAdd = { v, emoji -> add(day, v, emoji) },
            onRemove = { scope.launch { dao.remove(it) } },
            onDismiss = { sheetDay = null },
        )
    }
}

private fun List<Entry>?.total() = this?.sumOf { it.value } ?: 0

@Composable
private fun HabitMonth(
    ym: YearMonth,
    h: Habit,
    byDay: Map<Long, List<Entry>>,
    today: Long,
    onTap: (Long) -> Unit,
    onHold: (Long) -> Unit,
) {
    val first = ym.atDay(1).toEpochDay()
    val max = (0 until ym.lengthOfMonth()).maxOf { byDay[first + it].total() }.coerceAtLeast(1)
    val cs = MaterialTheme.colorScheme
    MonthGrid(ym) { day, label ->
        val list = byDay[day].orEmpty()
        val total = list.total()
        // Heat: single checks are full color; counts/numbers get stronger the bigger they are in this month.
        val heat = when {
            list.isEmpty() -> 0f
            h.checkType == CheckType.CHECK && !h.multi -> 1f
            else -> 0.35f + 0.65f * total / max
        }
        val bg by animateColorAsState(
            if (list.isEmpty()) cs.surfaceContainerHigh else lerp(cs.primaryContainer, cs.primary, heat),
            label = "cell",
        )
        val fg by animateColorAsState(
            when {
                list.isEmpty() -> cs.onSurface
                heat > 0.6f -> cs.onPrimary
                else -> cs.onPrimaryContainer
            },
            label = "cellText",
        )
        val emoji = h.checkType == CheckType.EMOJI
        DayCell(
            dayOfMonth = label,
            bg = bg,
            fg = fg,
            isToday = day == today,
            ring = if (list.isEmpty()) cs.primary else cs.onSurface,
            enabled = day <= today,
            onClick = { onTap(day) },
            onLongClick = { onHold(day) },
            badge = when {
                list.isEmpty() -> null
                emoji -> list.last().emoji
                h.checkType == CheckType.NUMBER -> "$total"
                h.multi -> "×$total"
                else -> null
            },
            badgeSize = if (emoji) 14.sp else 10.sp,
        )
    }
}

@Composable
private fun StatsCard(
    h: Habit,
    entries: List<Entry>,
    byDay: Map<Long, List<Entry>>,
    today: Long,
    monthView: Boolean,
    ym: YearMonth,
    onToday: () -> Unit,
) {
    val range = if (monthView) ym.atDay(1).toEpochDay()..ym.atEndOfMonth().toEpochDay()
    else LocalDate.of(ym.year, 1, 1).toEpochDay()..LocalDate.of(ym.year, 12, 31).toEpochDay()
    val periodTotal = entries.filter { it.day in range }.sumOf { it.value }
    val (current, best) = remember(byDay, today) { streaks(byDay.keys, today) }
    val doneToday = byDay[today] != null

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth().padding(20.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row {
                Stat(
                    if (monthView) ym.month.getDisplayName(TextStyle.FULL, Locale.getDefault()) else "${ym.year}",
                    periodTotal,
                    "",
                    Modifier.weight(1f),
                )
                Stat("Streak", current, "d", Modifier.weight(1f))
                Stat("Best", best, "d", Modifier.weight(1f))
            }
            Spacer(Modifier.height(14.dp))
            val undo = h.checkType == CheckType.CHECK && !h.multi && doneToday
            val container by animateColorAsState(
                if (undo) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.primary,
                label = "todayBtn",
            )
            val content by animateColorAsState(
                if (undo) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onPrimary,
                label = "todayBtnText",
            )
            Button(
                onClick = onToday,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Text(
                    when {
                        h.checkType != CheckType.CHECK -> "Log today"
                        h.multi -> "Add one for today"
                        doneToday -> "Done today ✓  ·  tap to undo"
                        else -> "Mark today done"
                    },
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: Int, suffix: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        AnimatedNumber(value, MaterialTheme.typography.headlineSmall, MaterialTheme.colorScheme.primary, suffix = suffix)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DaySheet(
    h: Habit,
    day: Long,
    list: List<Entry>,
    onAdd: (Int, String?) -> Unit,
    onRemove: (Entry) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    fun pick(value: Int, emoji: String?) {
        onAdd(value, emoji)
        // Single-entry habits are done after one pick: slide the sheet away.
        if (!h.multi) scope.launch { state.hide() }.invokeOnCompletion { onDismiss() }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp).navigationBarsPadding()) {
            Text(
                LocalDate.ofEpochDay(day).format(DateTimeFormatter.ofPattern("EEEE, d MMMM")),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(20.dp))
            SheetLabel("Logged")
            if (list.isEmpty()) {
                Text("Nothing yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    list.forEach { e ->
                        InputChip(
                            selected = false,
                            onClick = { onRemove(e) },
                            label = { Text(e.label(h.checkType), fontSize = 16.sp) },
                            trailingIcon = { Icon(Icons.Rounded.Close, "Remove", Modifier.size(18.dp)) },
                            shape = CircleShape,
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            when (h.checkType) {
                CheckType.CHECK -> if (h.multi || list.isEmpty()) {
                    Button(onClick = { pick(1, null) }, shape = CircleShape, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                        Text(if (h.multi) "Add one" else "Mark done")
                    }
                }
                CheckType.EMOJI, CheckType.NUMBER -> {
                    SheetLabel(if (h.multi) "Add" else "Choose")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        h.optionList.forEach { o ->
                            val emoji = h.checkType == CheckType.EMOJI
                            val n = o.toIntOrNull()
                            if (emoji || n != null) OptionTile(o, emoji) { pick(n ?: 1, if (emoji) o else null) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

@Composable
private fun OptionTile(text: String, emoji: Boolean, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val scale = pressScale(source)
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        interactionSource = source,
        modifier = Modifier.size(60.dp).graphicsLayer { scaleX = scale; scaleY = scale },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text,
                fontSize = if (emoji) 26.sp else 18.sp,
                fontWeight = if (emoji) null else FontWeight.SemiBold,
            )
        }
    }
}
