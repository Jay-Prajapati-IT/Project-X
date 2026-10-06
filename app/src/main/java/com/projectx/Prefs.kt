package com.projectx

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import com.projectx.habits.CheckType
import com.projectx.habits.Habit
import com.projectx.habits.HabitColor
import com.projectx.habits.HabitMode

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
            color = sp.getString("habit_color", null) ?: HabitColor.PURPLE.id,
            mode = sp.getString("habit_mode", null) ?: HabitMode.APP.id,
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
