package com.projectx

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.Cake
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.ui.graphics.vector.ImageVector

/** Every mini-app inside PR-X. `id` is persisted, never rename it. `phase` = TASK_PLAN.md phase. */
enum class Module(val id: String, val title: String, val tagline: String, val icon: ImageVector, val phase: Int) {
    TODO("todo", "Todo", "Tasks & due dates", Icons.Rounded.TaskAlt, 1),
    REMINDERS("reminders", "Reminders", "Alarms & alerts", Icons.Rounded.Alarm, 2),
    HABITS("habits", "Habits", "Daily streaks", Icons.Rounded.Autorenew, 3),
    BIRTHDAYS("birthdays", "Birthdays", "Never forget one", Icons.Rounded.Cake, 4),
    VAULT("vault", "Vault", "Passwords", Icons.Rounded.Key, 7),
    LOCKER("locker", "Locker", "Private files", Icons.Rounded.Lock, 8);

    companion object {
        fun byId(id: String?) = entries.find { it.id == id }
    }
}
