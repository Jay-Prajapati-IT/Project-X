@file:OptIn(ExperimentalFoundationApi::class)

package com.projectx.ui

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.sign

// Shared month/year calendar used by Habits and Birthdays. Days are LocalDate.toEpochDay().

private val CellShape = RoundedCornerShape(14.dp)
private val WEEKDAYS = listOf("M", "T", "W", "T", "F", "S", "S")

/** ‹  Month | Year  › */
@Composable
fun PeriodBar(monthView: Boolean, onMonthView: (Boolean) -> Unit, onShift: (Int) -> Unit, canNext: Boolean = true) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onShift(-1) }) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, "Previous") }
        PillGroup(
            listOf("Month", "Year"),
            selected = if (monthView) 0 else 1,
            onSelect = { onMonthView(it == 0) },
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = { onShift(1) }, enabled = canNext) {
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, "Next")
        }
    }
}

/** Rounded card with a title, swipe left/right to change period, and slide animation between periods. */
@Composable
fun CalendarCard(
    monthView: Boolean,
    ym: YearMonth,
    onShift: (Int) -> Unit,
    content: @Composable (isMonth: Boolean, ym: YearMonth) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.pointerInput(monthView, ym) {
            var dx = 0f
            detectHorizontalDragGestures(
                onDragStart = { dx = 0f },
                onDragEnd = { if (dx > 120) onShift(-1) else if (dx < -120) onShift(1) },
            ) { _, delta -> dx += delta }
        },
    ) {
        AnimatedContent(
            targetState = monthView to ym,
            transitionSpec = {
                val dir = if (targetState.first != initialState.first) 0
                else targetState.second.compareTo(initialState.second).sign
                if (dir == 0) {
                    (fadeIn(tween(250)) + scaleIn(initialScale = 0.96f)) togetherWith fadeOut(tween(150))
                } else {
                    (slideInHorizontally(tween(320)) { it * dir / 3 } + fadeIn(tween(320))) togetherWith
                        (slideOutHorizontally(tween(320)) { -it * dir / 3 } + fadeOut(tween(160)))
                }
            },
            label = "calendar",
        ) { (isMonth, period) ->
            Column(Modifier.padding(14.dp)) {
                Text(
                    if (isMonth) period.format(DateTimeFormatter.ofPattern("MMMM yyyy")) else "${period.year}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 4.dp, bottom = 12.dp),
                )
                content(isMonth, period)
            }
        }
    }
}

/** Monday-first month grid; `cell` draws each day. */
@Composable
fun MonthGrid(ym: YearMonth, cell: @Composable (day: Long, dayOfMonth: Int) -> Unit) {
    val first = ym.atDay(1).toEpochDay()
    val lead = ym.atDay(1).dayOfWeek.value - 1
    val count = ym.lengthOfMonth()
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            WEEKDAYS.forEach {
                Text(
                    it,
                    Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        for (week in 0 until (lead + count + 6) / 7) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (col in 0 until 7) {
                    val i = week * 7 + col - lead
                    Box(Modifier.weight(1f).aspectRatio(1f)) {
                        if (i in 0 until count) cell(first + i, i + 1)
                    }
                }
            }
        }
    }
}

/** One rounded day square. */
@Composable
fun DayCell(
    dayOfMonth: Int,
    bg: Color,
    fg: Color,
    isToday: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    badge: String? = null,
    badgeSize: TextUnit = TextUnit.Unspecified,
    /** Today's outline; use onSurface when the cell is filled with primary so it stays visible. */
    ring: Color = MaterialTheme.colorScheme.primary,
) {
    val source = remember { MutableInteractionSource() }
    val scale = pressScale(source)
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (enabled) 1f else 0.35f }
            .clip(CellShape)
            .background(bg)
            .then(if (isToday) Modifier.border(2.dp, ring, CellShape) else Modifier)
            .combinedClickable(
                interactionSource = source,
                indication = ripple(),
                enabled = enabled,
                onLongClick = onLongClick,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$dayOfMonth", style = MaterialTheme.typography.labelMedium, color = fg)
            if (badge != null) Text(badge, fontSize = badgeSize, color = fg, maxLines = 1, lineHeight = badgeSize)
        }
    }
}

/** 12 mini months with a dot per day. `dot` returns the dot color for a day in the month. */
@Composable
fun YearGrid(year: Int, total: (YearMonth) -> Int, dot: @Composable (day: Long) -> Color, onMonth: (YearMonth) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        for (row in 0 until 4) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (col in 0 until 3) {
                    val ym = YearMonth.of(year, row * 3 + col + 1)
                    MiniMonth(ym, total(ym), dot, Modifier.weight(1f)) { onMonth(ym) }
                }
            }
        }
    }
}

@Composable
private fun MiniMonth(ym: YearMonth, total: Int, dot: @Composable (Long) -> Color, modifier: Modifier, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val first = ym.atDay(1).toEpochDay()
    val lead = ym.atDay(1).dayOfWeek.value - 1
    val count = ym.lengthOfMonth()
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(cs.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                ym.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f),
            )
            if (total > 0) Text("$total", style = MaterialTheme.typography.labelMedium, color = cs.primary, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(6.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            for (week in 0 until 6) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    for (col in 0 until 7) {
                        val i = week * 7 + col - lead
                        val color = if (i in 0 until count) dot(first + i) else Color.Transparent
                        Box(Modifier.weight(1f).aspectRatio(1f).clip(CircleShape).background(color))
                    }
                }
            }
        }
    }
}
