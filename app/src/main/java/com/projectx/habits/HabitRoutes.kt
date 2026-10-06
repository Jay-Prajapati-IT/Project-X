package com.projectx.habits

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.projectx.Prefs
import com.projectx.ready
import com.projectx.ui.theme.ModuleTheme
import kotlinx.coroutines.launch

fun NavGraphBuilder.habitRoutes(nav: NavController, dao: HabitDao, prefs: Prefs) {
    composable("habits") { e ->
        HabitListScreen(
            dao = dao,
            onBack = { if (e.ready()) nav.popBackStack() },
            onOpen = { if (e.ready()) nav.navigate("habit/$it") },
            onEdit = { if (e.ready()) nav.navigate("habit/$it/edit") },
            onNew = { if (e.ready()) nav.navigate("habits/new") },
            onDefaults = { if (e.ready()) nav.navigate("habits/defaults") },
        )
    }
    composable("habit/{id}") { e ->
        val id = e.arguments?.getString("id")?.toLongOrNull() ?: return@composable
        HabitCalendarScreen(
            dao = dao,
            id = id,
            onBack = { if (e.ready()) nav.popBackStack() },
            onEdit = { if (e.ready()) nav.navigate("habit/$id/edit") },
        )
    }
    composable("habit/{id}/edit") { e ->
        val id = e.arguments?.getString("id")?.toLongOrNull() ?: return@composable
        val scope = rememberCoroutineScope()
        // Loaded once: the form owns the draft, and a delete mustn't blank the screen mid-animation.
        val habit by produceState<Habit?>(null, id) { value = dao.get(id) }
        habit?.let { h ->
            HabitFormScreen(
                title = "Habit settings",
                initial = h,
                kind = FormKind.EDIT,
                onBack = { if (e.ready()) nav.popBackStack() },
                onSave = { scope.launch { dao.save(it); if (e.ready()) nav.popBackStack() } },
                onDelete = { scope.launch { dao.delete(id); if (e.ready()) nav.popBackStack("habits", false) } },
            )
        }
    }
    composable("habits/new") { e ->
        val scope = rememberCoroutineScope()
        HabitFormScreen(
            title = "New habit",
            initial = prefs.habitTemplate,
            kind = FormKind.NEW,
            onBack = { if (e.ready()) nav.popBackStack() },
            onSave = {
                scope.launch {
                    dao.save(it.copy(createdAt = System.currentTimeMillis()))
                    if (e.ready()) nav.popBackStack()
                }
            },
        )
    }
    composable("habits/defaults") { e ->
        HabitFormScreen(
            title = "Habit defaults",
            initial = prefs.habitTemplate,
            kind = FormKind.DEFAULTS,
            onBack = { if (e.ready()) nav.popBackStack() },
            onSave = { prefs.saveHabitTemplate(it); if (e.ready()) nav.popBackStack() },
        )
    }
}
