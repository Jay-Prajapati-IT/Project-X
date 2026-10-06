package com.projectx

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import com.projectx.habits.CheckType
import com.projectx.habits.Habit
import com.projectx.ui.theme.AccentColor
import com.projectx.ui.theme.ModeChoice

enum class ThemeMode(val id: String, val label: String) { SYSTEM("system", "System"), DARK("dark", "Dark"), LIGHT("light", "Light") }

/** App settings, observable by Compose and persisted to SharedPreferences. */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)

    var home by mutableStateOf(
        sp.getString("home", null)?.split(',')?.mapNotNull(Module::byId)
            ?: listOf(Module.TODO, Module.HABITS, Module.REMINDERS)
    )
        private set
    var theme by mutableStateOf(ThemeMode.entries.find { it.id == sp.getString("theme", null) } ?: ThemeMode.DARK)
        private set
    var notifications by mutableStateOf(sp.getBoolean("notifications", true))
        private set

    /** Starting values for every new habit (set from the habit list's ⚙). */
    var habitTemplate by mutableStateOf(
        Habit(
            type = sp.getString("habit_type", null) ?: CheckType.CHECK.id,
            multi = sp.getBoolean("habit_multi", false),
            options = sp.getString("habit_options", null) ?: "",
            color = sp.getString("habit_color", null) ?: AccentColor.PURPLE.id,
            mode = sp.getString("habit_mode", null) ?: ModeChoice.APP.id,
        )
    )
        private set

    fun saveHabitTemplate(h: Habit) {
        habitTemplate = h.copy(id = 0, name = "", createdAt = 0)
        sp.edit {
            putString("habit_type", h.type)
            putBoolean("habit_multi", h.multi)
            putString("habit_options", h.options)
            putString("habit_color", h.color)
            putString("habit_mode", h.mode)
        }
    }

    // Birthdays module
    var bdayColor by mutableStateOf(sp.getString("bday_color", null) ?: AccentColor.PINK.id)
        private set
    var bdayMode by mutableStateOf(sp.getString("bday_mode", null) ?: ModeChoice.APP.id)
        private set
    var bdayRemind by mutableStateOf(sp.getBoolean("bday_remind", true))
        private set
    /** 0 = only on the day. */
    var bdayDaysBefore by mutableStateOf(sp.getInt("bday_days_before", 1))
        private set
    /** true = remind every day from `bdayDaysBefore` until the day; false = once ahead + on the day. */
    var bdayDaily by mutableStateOf(sp.getBoolean("bday_daily", false))
        private set
    /** Epoch day reminders were last sent, so a worker re-run can't notify twice. */
    val bdayLastNotified get() = sp.getLong("bday_last_notified", -1)

    fun updateBdayColor(v: String) { bdayColor = v; sp.edit { putString("bday_color", v) } }
    fun updateBdayMode(v: String) { bdayMode = v; sp.edit { putString("bday_mode", v) } }
    fun updateBdayRemind(v: Boolean) { bdayRemind = v; sp.edit { putBoolean("bday_remind", v) } }
    fun updateBdayDaysBefore(v: Int) { bdayDaysBefore = v; sp.edit { putInt("bday_days_before", v) } }
    fun updateBdayDaily(v: Boolean) { bdayDaily = v; sp.edit { putBoolean("bday_daily", v) } }
    fun markBdayNotified(day: Long) = sp.edit { putLong("bday_last_notified", day) }

    fun pin(m: Module) = saveHome((home + m).distinct())
    fun unpin(m: Module) = saveHome(home - m)

    private fun saveHome(list: List<Module>) {
        home = list
        sp.edit { putString("home", list.joinToString(",") { it.id }) }
    }

    fun updateTheme(t: ThemeMode) {
        theme = t
        sp.edit { putString("theme", t.id) }
    }

    fun updateNotifications(on: Boolean) {
        notifications = on
        sp.edit { putBoolean("notifications", on) }
    }
}
