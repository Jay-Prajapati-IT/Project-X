package com.projectx.habits

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.projectx.ui.AnimatedNumber
import com.projectx.ui.RoundIconButton
import com.projectx.ui.TopBar
import com.projectx.ui.dashedBorder
import com.projectx.ui.pressScale
import com.projectx.ui.theme.LocalDark
import java.time.YearMonth

private val RowShape = RoundedCornerShape(24.dp)

@Composable
fun HabitListScreen(
    dao: HabitDao,
    onBack: () -> Unit,
    onOpen: (Long) -> Unit,
    onEdit: (Long) -> Unit,
    onNew: () -> Unit,
    onDefaults: () -> Unit,
) {
    val month = remember { YearMonth.now() }
    val habits by remember { dao.habits() }.collectAsState(null)
    val totals by remember {
        dao.totals(month.atDay(1).toEpochDay(), month.atEndOfMonth().toEpochDay())
    }.collectAsState(emptyList())
    val totalOf = totals.associate { it.habitId to it.total }

    Scaffold { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "top") {
                TopBar("Habits", onBack) { RoundIconButton(Icons.Rounded.Tune, "Habit defaults", onDefaults) }
            }
            val list = habits ?: return@LazyColumn // still loading: avoid flashing the empty state
            if (list.isEmpty()) {
                item(key = "empty") {
                    Text(
                        "No habits yet. Create your first one below.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.animateItem().padding(horizontal = 28.dp),
                    )
                }
            }
            items(list, key = { it.id }) { h ->
                HabitRow(
                    h, totalOf[h.id] ?: 0, Modifier.animateItem().padding(horizontal = 20.dp),
                    { onOpen(h.id) }, { onEdit(h.id) },
                )
            }
            item(key = "new") { NewHabitRow(Modifier.animateItem().padding(horizontal = 20.dp), onNew) }
        }
    }
}

@Composable
private fun HabitRow(h: Habit, total: Int, modifier: Modifier, onOpen: () -> Unit, onEdit: () -> Unit) {
    val accent = HabitColor.of(h.color).accent(LocalDark.current ?: true)
    val source = remember { MutableInteractionSource() }
    val scale = pressScale(source)
    Surface(
        onClick = onOpen,
        shape = RowShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        interactionSource = source,
        modifier = modifier.fillMaxWidth().graphicsLayer { scaleX = scale; scaleY = scale },
    ) {
        Row(Modifier.padding(start = 14.dp, top = 14.dp, bottom = 14.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(46.dp).clip(RoundedCornerShape(16.dp)).background(accent.container),
                contentAlignment = Alignment.Center,
            ) {
                when (h.checkType) {
                    CheckType.CHECK -> Icon(Icons.Rounded.Check, null, tint = accent.onContainer)
                    CheckType.NUMBER -> Icon(Icons.Rounded.Tag, null, tint = accent.onContainer)
                    CheckType.EMOJI -> Text(h.optionList.firstOrNull() ?: "🙂", fontSize = 22.sp)
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    h.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    h.checkType.label + if (h.multi) " · multiple a day" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                AnimatedNumber(total, MaterialTheme.typography.headlineSmall, accent.primary)
                Text("this month", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Rounded.Settings, "Habit settings", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun NewHabitRow(modifier: Modifier, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val scale = pressScale(source)
    Row(
        modifier
            .fillMaxWidth()
            .height(64.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RowShape)
            .dashedBorder(MaterialTheme.colorScheme.outline, 24.dp)
            .clickable(source, androidx.compose.material3.ripple(), onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Add, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        Text("New habit", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
