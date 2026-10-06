package com.projectx.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// `id`s are stored in the DB / prefs. Never rename them.

/** Per-module (or per-habit) light/dark choice. */
enum class ModeChoice(val id: String, val label: String) {
    APP("app", "Same as app"), LIGHT("light", "Light"), DARK("dark", "Dark");

    companion object {
        fun of(id: String) = entries.find { it.id == id } ?: APP
    }
}

enum class AccentColor(val id: String, val dark: Accent, val light: Accent) {
    PURPLE(
        "purple",
        Accent(Color(0xFFBCA8F7), Color(0xFF241A45), Color(0xFF342A55), Color(0xFFE6DDFF)),
        Accent(Color(0xFF6A50C9), Color.White, Color(0xFFE9E1FF), Color(0xFF22125A)),
    ),
    BLUE(
        "blue",
        Accent(Color(0xFF8AB4FF), Color(0xFF0B2B5C), Color(0xFF1F2E4A), Color(0xFFD8E2FF)),
        Accent(Color(0xFF1E5BD8), Color.White, Color(0xFFDAE2FF), Color(0xFF001A43)),
    ),
    GREEN(
        "green",
        Accent(Color(0xFF7DDB9A), Color(0xFF0B3A1D), Color(0xFF1C3626), Color(0xFFC8F5D4)),
        Accent(Color(0xFF1E8A4C), Color.White, Color(0xFFC9F2D5), Color(0xFF002110)),
    ),
    YELLOW(
        "yellow",
        Accent(Color(0xFFFFD866), Color(0xFF3D2E00), Color(0xFF3D3418), Color(0xFFFFEDB0)),
        Accent(Color(0xFF9A7400), Color.White, Color(0xFFFFEDB0), Color(0xFF261A00)),
    ),
    RED(
        "red",
        Accent(Color(0xFFFF8A8A), Color(0xFF4A0E0E), Color(0xFF4A2224), Color(0xFFFFDADA)),
        Accent(Color(0xFFC62828), Color.White, Color(0xFFFFDAD8), Color(0xFF410002)),
    ),
    PINK(
        "pink",
        Accent(Color(0xFFFF9AD0), Color(0xFF4A0B2C), Color(0xFF4A2238), Color(0xFFFFD8EA)),
        Accent(Color(0xFFC2185B), Color.White, Color(0xFFFFD9E6), Color(0xFF3E0021)),
    );

    fun accent(dark: Boolean) = if (dark) this.dark else light

    companion object {
        fun of(id: String) = entries.find { it.id == id } ?: PURPLE
    }
}

/** Theme for a module/habit with its own color and light/dark choice, nested inside the app theme. */
@Composable
fun ModuleTheme(color: String, mode: String, content: @Composable () -> Unit) {
    val appDark = LocalDark.current ?: isSystemInDarkTheme()
    val dark = when (ModeChoice.of(mode)) {
        ModeChoice.APP -> appDark
        ModeChoice.LIGHT -> false
        ModeChoice.DARK -> true
    }
    ProjectXTheme(dark, AccentColor.of(color).accent(dark), content)
}
