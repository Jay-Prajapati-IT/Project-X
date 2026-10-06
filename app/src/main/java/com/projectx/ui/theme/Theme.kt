package com.projectx.ui.theme

import android.app.Activity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// "Black Panther": near-black graphite + lavender purple.
private val Dark = darkColorScheme(
    primary = Color(0xFFBCA8F7),
    onPrimary = Color(0xFF241A45),
    primaryContainer = Color(0xFF342A55),
    onPrimaryContainer = Color(0xFFE6DDFF),
    secondaryContainer = Color(0xFF342A55),
    onSecondaryContainer = Color(0xFFE6DDFF),
    background = Color(0xFF111114),
    onBackground = Color(0xFFE6E4EC),
    surface = Color(0xFF111114),
    onSurface = Color(0xFFE6E4EC),
    onSurfaceVariant = Color(0xFFA19EAD),
    surfaceContainerLowest = Color(0xFF0C0C0F),
    surfaceContainerLow = Color(0xFF17171B),
    surfaceContainer = Color(0xFF1C1C21),
    surfaceContainerHigh = Color(0xFF25252B),
    surfaceContainerHighest = Color(0xFF2E2E35),
    outline = Color(0xFF4A4852),
    outlineVariant = Color(0xFF302F37),
)

private val Light = lightColorScheme(
    primary = Color(0xFF6A50C9),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE9E1FF),
    onPrimaryContainer = Color(0xFF22125A),
    secondaryContainer = Color(0xFFE9E1FF),
    onSecondaryContainer = Color(0xFF22125A),
    background = Color(0xFFF5F4F9),
    onBackground = Color(0xFF1B1B20),
    surface = Color(0xFFF5F4F9),
    onSurface = Color(0xFF1B1B20),
    onSurfaceVariant = Color(0xFF615E6D),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFBFAFE),
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFECEAF2),
    surfaceContainerHighest = Color(0xFFE4E1EC),
    outline = Color(0xFFB4B0C0),
    outlineVariant = Color(0xFFDCD9E5),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

/** Replaces the lavender primary colors, e.g. for a habit's own color. */
class Accent(val primary: Color, val onPrimary: Color, val container: Color, val onContainer: Color)

/** Whether the nearest theme is dark. null = outside any ProjectXTheme. */
val LocalDark = compositionLocalOf<Boolean?> { null }

/**
 * Themes can be nested (a habit can force light mode inside a dark app).
 * The innermost theme owns the status/nav bar icon color and hands it back to the outer one when it leaves.
 */
@Composable
fun ProjectXTheme(dark: Boolean, accent: Accent? = null, content: @Composable () -> Unit) {
    val outer = LocalDark.current
    val view = LocalView.current
    DisposableEffect(dark) {
        val window = (view.context as? Activity)?.window
        fun setBars(d: Boolean) {
            if (window == null) return
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !d
                isAppearanceLightNavigationBars = !d
            }
        }
        setBars(dark)
        onDispose { outer?.let { setBars(it) } }
    }

    val base = if (dark) Dark else Light
    val scheme = if (accent == null) base else base.copy(
        primary = accent.primary, onPrimary = accent.onPrimary,
        primaryContainer = accent.container, onPrimaryContainer = accent.onContainer,
        secondaryContainer = accent.container, onSecondaryContainer = accent.onContainer,
    )
    CompositionLocalProvider(LocalDark provides dark) {
        MaterialTheme(colorScheme = scheme.animated(), shapes = AppShapes, content = content)
    }
}

/** Cross-fades the colors so switching dark/light is smooth instead of a hard flash. */
@Composable
private fun ColorScheme.animated(): ColorScheme {
    @Composable
    fun Color.a() = animateColorAsState(this, tween(450), label = "theme").value
    return copy(
        primary = primary.a(), onPrimary = onPrimary.a(),
        primaryContainer = primaryContainer.a(), onPrimaryContainer = onPrimaryContainer.a(),
        secondaryContainer = secondaryContainer.a(), onSecondaryContainer = onSecondaryContainer.a(),
        background = background.a(), onBackground = onBackground.a(),
        surface = surface.a(), onSurface = onSurface.a(), onSurfaceVariant = onSurfaceVariant.a(),
        surfaceContainerLow = surfaceContainerLow.a(), surfaceContainer = surfaceContainer.a(),
        surfaceContainerHigh = surfaceContainerHigh.a(), surfaceContainerHighest = surfaceContainerHighest.a(),
        outline = outline.a(), outlineVariant = outlineVariant.a(),
    )
}
