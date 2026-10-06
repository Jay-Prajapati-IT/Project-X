package com.projectx

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import com.projectx.data.AppDatabase
import com.projectx.birthdays.BirthdayWorker
import com.projectx.birthdays.birthdayRoutes
import com.projectx.habits.habitRoutes
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.projectx.ui.HomeScreen
import com.projectx.ui.ModuleScreen
import com.projectx.ui.SettingsScreen
import com.projectx.ui.theme.ProjectXTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val prefs = Prefs(applicationContext)
        val db = AppDatabase.get(applicationContext)
        BirthdayWorker.schedule(applicationContext)
        // Opened from a notification/widget: jump straight to that screen (not again after rotation).
        val startRoute = if (savedInstanceState == null) intent.getStringExtra(EXTRA_ROUTE) else null
        setContent {
            val dark = when (prefs.theme) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
            }
            ProjectXTheme(dark) { AppNav(prefs, db, startRoute) }
        }
    }

    companion object {
        const val EXTRA_ROUTE = "route"
    }
}

// Ignore taps while a screen is still animating in/out (prevents double navigation).
fun NavBackStackEntry.ready() = lifecycle.currentState == Lifecycle.State.RESUMED

private const val NAV_MS = 380

/** Built modules have their own screens; the rest show the "coming soon" placeholder. */
private val Module.route
    get() = when (this) {
        Module.HABITS -> "habits"
        Module.BIRTHDAYS -> "birthdays"
        else -> "module/$id"
    }

@Composable
private fun AppNav(prefs: Prefs, db: AppDatabase, startRoute: String?) {
    val nav = rememberNavController()
    LaunchedEffect(Unit) { startRoute?.let { nav.navigate(it) } }
    NavHost(
        navController = nav,
        startDestination = "home",
        enterTransition = {
            slideIntoContainer(SlideDirection.Start, tween(NAV_MS, easing = FastOutSlowInEasing)) { it / 4 } +
                fadeIn(tween(NAV_MS))
        },
        exitTransition = {
            slideOutOfContainer(SlideDirection.Start, tween(NAV_MS, easing = FastOutSlowInEasing)) { it / 4 } +
                fadeOut(tween(NAV_MS / 2))
        },
        popEnterTransition = {
            slideIntoContainer(SlideDirection.End, tween(NAV_MS, easing = FastOutSlowInEasing)) { it / 4 } +
                fadeIn(tween(NAV_MS))
        },
        popExitTransition = {
            slideOutOfContainer(SlideDirection.End, tween(NAV_MS, easing = FastOutSlowInEasing)) { it / 4 } +
                fadeOut(tween(NAV_MS / 2))
        },
    ) {
        composable("home") { e ->
            HomeScreen(
                prefs = prefs,
                onOpen = { if (e.ready()) nav.navigate(it.route) },
                onSettings = { if (e.ready()) nav.navigate("settings") },
            )
        }
        composable("settings") { e ->
            SettingsScreen(prefs, onBack = { if (e.ready()) nav.popBackStack() })
        }
        composable("module/{id}") { e ->
            val m = Module.byId(e.arguments?.getString("id")) ?: return@composable
            ModuleScreen(m, onBack = { if (e.ready()) nav.popBackStack() })
        }
        habitRoutes(nav, db.habits(), prefs)
        birthdayRoutes(nav, db.birthdays(), prefs)
    }
}
