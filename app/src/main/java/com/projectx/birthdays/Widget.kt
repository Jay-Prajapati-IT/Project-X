package com.projectx.birthdays

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.projectx.MainActivity
import com.projectx.Prefs
import com.projectx.data.AppDatabase
import com.projectx.ui.theme.AccentColor
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JTextStyle
import java.util.Locale

private val ListMode = booleanPreferencesKey("list")
private val RouteKey = ActionParameters.Key<String>(MainActivity.EXTRA_ROUTE)

private class WidgetColors(accentId: String) {
    private val a = AccentColor.of(accentId)
    val bg = ColorProvider(day = Color(0xFFF5F4F9), night = Color(0xFF16141D))
    val text = ColorProvider(day = Color(0xFF1B1B20), night = Color(0xFFE6E4EC))
    val muted = ColorProvider(day = Color(0xFF615E6D), night = Color(0xFFA19EAD))
    val accent = ColorProvider(day = a.light.primary, night = a.dark.primary)
    val onAccent = ColorProvider(day = a.light.onPrimary, night = a.dark.onPrimary)
}

/**
 * Home-screen widget: this month's birthdays as a list or a calendar.
 * Tap the body to switch view, tap the header to open the app.
 */
class BirthdayWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val dao = AppDatabase.get(context).birthdays()
        val colors = WidgetColors(Prefs(context).bdayColor)
        provideContent {
            val people by dao.people().collectAsState(emptyList())
            val list = currentState(ListMode) ?: true
            Content(people, list, colors)
        }
    }
}

class BirthdayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BirthdayWidget()
}

class ToggleWidgetView : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        updateAppWidgetState(context, glanceId) { it[ListMode] = !(it[ListMode] ?: true) }
        BirthdayWidget().update(context, glanceId)
    }
}

@Composable
private fun Content(people: List<Person>, listMode: Boolean, c: WidgetColors) {
    val today = LocalDate.now()
    val ym = YearMonth.from(today)
    Column(GlanceModifier.fillMaxSize().cornerRadius(24.dp).background(c.bg).padding(14.dp)) {
        Row(
            GlanceModifier.fillMaxWidth().clickable(actionStartActivity<MainActivity>(actionParametersOf(RouteKey to "birthdays"))),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "🎂  " + ym.month.getDisplayName(JTextStyle.FULL, Locale.getDefault()),
                style = TextStyle(color = c.text, fontSize = 16.sp, fontWeight = FontWeight.Bold),
                modifier = GlanceModifier.defaultWeight(),
            )
            Text("Open ›", style = TextStyle(color = c.accent, fontSize = 12.sp, fontWeight = FontWeight.Medium))
        }
        Spacer(GlanceModifier.height(8.dp))
        Box(GlanceModifier.fillMaxSize().clickable(actionRunCallback<ToggleWidgetView>())) {
            if (listMode) ListBody(people, today, c) else MonthBody(people, ym, today, c)
        }
    }
}

@Composable
private fun ListBody(people: List<Person>, today: LocalDate, c: WidgetColors) {
    val thisMonth = people
        .map { it to it.dateIn(today.year) }
        .filter { (_, d) -> d.month == today.month && d >= today }
        .sortedBy { it.second }
    Column(GlanceModifier.fillMaxSize()) {
        if (thisMonth.isEmpty()) {
            Text("No more birthdays this month", style = TextStyle(color = c.muted, fontSize = 13.sp))
            people.minByOrNull { it.daysUntil(today) }?.let { p ->
                Spacer(GlanceModifier.height(4.dp))
                Text(
                    "Next: ${p.name} · " + p.next(today).format(DateTimeFormatter.ofPattern("d MMM")),
                    style = TextStyle(color = c.text, fontSize = 13.sp),
                )
            }
        }
        thisMonth.take(5).forEach { (p, d) ->
            Row(GlanceModifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${d.dayOfMonth}",
                    style = TextStyle(color = c.accent, fontSize = 15.sp, fontWeight = FontWeight.Bold),
                    modifier = GlanceModifier.width(30.dp),
                )
                Text(p.name, style = TextStyle(color = c.text, fontSize = 14.sp), maxLines = 1, modifier = GlanceModifier.defaultWeight())
                Text(untilLabel(p.daysUntil(today)), style = TextStyle(color = c.muted, fontSize = 12.sp))
            }
        }
        if (thisMonth.size > 5) Text("+${thisMonth.size - 5} more", style = TextStyle(color = c.muted, fontSize = 12.sp))
    }
}

@Composable
private fun MonthBody(people: List<Person>, ym: YearMonth, today: LocalDate, c: WidgetColors) {
    val bdays = people.map { it.dateIn(ym.year) }.filter { it.month == ym.month }.map { it.dayOfMonth }.toSet()
    val lead = ym.atDay(1).dayOfWeek.value - 1
    val count = ym.lengthOfMonth()
    Column(GlanceModifier.fillMaxSize()) {
        Row(GlanceModifier.fillMaxWidth()) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach {
                Text(
                    it,
                    style = TextStyle(color = c.muted, fontSize = 10.sp, textAlign = TextAlign.Center),
                    modifier = GlanceModifier.defaultWeight(),
                )
            }
        }
        for (week in 0 until (lead + count + 6) / 7) {
            Row(GlanceModifier.fillMaxWidth().padding(top = 2.dp)) {
                for (col in 0 until 7) {
                    val day = week * 7 + col - lead + 1
                    val isBday = day in bdays
                    val isToday = day == today.dayOfMonth
                    Box(GlanceModifier.defaultWeight().height(20.dp), contentAlignment = Alignment.Center) {
                        if (day in 1..count) {
                            Box(
                                (if (isBday) GlanceModifier.background(c.accent).cornerRadius(10.dp) else GlanceModifier)
                                    .width(24.dp).height(20.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "$day",
                                    style = TextStyle(
                                        color = when {
                                            isBday -> c.onAccent
                                            isToday -> c.accent
                                            else -> c.text
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = if (isBday || isToday) FontWeight.Bold else FontWeight.Normal,
                                        textAlign = TextAlign.Center,
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
