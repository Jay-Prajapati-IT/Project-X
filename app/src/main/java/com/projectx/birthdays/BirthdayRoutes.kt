package com.projectx.birthdays

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.glance.appwidget.updateAll
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.projectx.Prefs
import com.projectx.ready
import kotlinx.coroutines.launch
import java.time.LocalDate

private suspend fun BirthdayDao.saveAndRefresh(context: Context, p: Person) {
    save(p)
    BirthdayWidget().updateAll(context)
}

fun NavGraphBuilder.birthdayRoutes(nav: NavController, dao: BirthdayDao, prefs: Prefs) {
    composable("birthdays") { e ->
        BirthdaysScreen(
            dao = dao,
            prefs = prefs,
            onBack = { if (e.ready()) nav.popBackStack() },
            onAdd = { day -> if (e.ready()) nav.navigate("birthdays/new?day=${day ?: -1}") },
            onEdit = { if (e.ready()) nav.navigate("birthday/$it") },
            onSettings = { if (e.ready()) nav.navigate("birthdays/settings") },
        )
    }
    composable(
        "birthdays/new?day={day}",
        arguments = listOf(navArgument("day") { type = NavType.LongType; defaultValue = -1L }),
    ) { e ->
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        // Opened from a calendar day: pre-fill that month/day, year left for the user to set.
        val day = e.arguments?.getLong("day")?.takeIf { it >= 0 }?.let(LocalDate::ofEpochDay)
        val start = day ?: LocalDate.now()
        BirthdayFormScreen(
            initial = Person(name = "", month = start.monthValue, day = start.dayOfMonth, year = if (day == null) start.year else null),
            isNew = true,
            prefs = prefs,
            onBack = { if (e.ready()) nav.popBackStack() },
            onSave = { scope.launch { dao.saveAndRefresh(context, it); if (e.ready()) nav.popBackStack() } },
            onDelete = null,
        )
    }
    composable("birthday/{id}") { e ->
        val id = e.arguments?.getString("id")?.toLongOrNull() ?: return@composable
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val person by produceState<Person?>(null, id) { value = dao.get(id) }
        person?.let { p ->
            BirthdayFormScreen(
                initial = p,
                isNew = false,
                prefs = prefs,
                onBack = { if (e.ready()) nav.popBackStack() },
                onSave = { scope.launch { dao.saveAndRefresh(context, it); if (e.ready()) nav.popBackStack() } },
                onDelete = {
                    scope.launch {
                        dao.delete(id)
                        BirthdayWidget().updateAll(context)
                        if (e.ready()) nav.popBackStack()
                    }
                },
            )
        }
    }
    composable("birthdays/settings") { e ->
        BirthdaySettingsScreen(prefs, onBack = { if (e.ready()) nav.popBackStack() })
    }
}
