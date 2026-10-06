@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.projectx.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.projectx.Module
import com.projectx.Prefs
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val TileRadius = 28.dp
private val TileShape = RoundedCornerShape(TileRadius)
private const val MIN_TILES = 6

@Composable
fun HomeScreen(prefs: Prefs, onOpen: (Module) -> Unit, onSettings: () -> Unit) {
    var picking by remember { mutableStateOf(false) }
    // Entrance animation plays once per app launch, not every time you come back to home.
    var shown by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }

    val pinned = prefs.home
    val available = Module.entries.filter { it !in pinned }
    val addTiles = if (available.isEmpty()) 0 else maxOf(1, MIN_TILES - pinned.size)

    Scaffold { pad ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "header", span = { GridItemSpan(maxLineSpan) }) {
                Appear(shown, 0) { Header(onSettings) }
            }
            itemsIndexed(pinned, key = { _, m -> m.id }) { i, m ->
                Appear(shown, i + 1, Modifier.animateItem()) {
                    AppTile(m, onClick = { onOpen(m) }, onRemove = { prefs.unpin(m) })
                }
            }
            items(addTiles, key = { "add$it" }) { i ->
                Appear(shown, pinned.size + i + 1, Modifier.animateItem()) { AddTile { picking = true } }
            }
        }
    }

    if (picking) AddSheet(available, onPick = { prefs.pin(it) }, onDismiss = { picking = false })
}

/** Staggered fade + rise + scale-in. */
@Composable
private fun Appear(shown: Boolean, index: Int, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val p by animateFloatAsState(
        if (shown) 1f else 0f,
        tween(520, delayMillis = 55 * index, easing = FastOutSlowInEasing),
        label = "appear",
    )
    Box(
        modifier.graphicsLayer {
            alpha = p
            translationY = (1 - p) * 32.dp.toPx()
            scaleX = 0.94f + 0.06f * p
            scaleY = scaleX
        },
    ) { content() }
}

@Composable
private fun Header(onSettings: () -> Unit) {
    val now = LocalDateTime.now()
    val greeting = when (now.hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Good night"
    }
    Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(greeting, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "PR-X",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
            )
            Text(
                now.format(DateTimeFormatter.ofPattern("EEEE, d MMMM")),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        RoundIconButton(Icons.Rounded.Settings, "Settings", onSettings)
    }
}

@Composable
private fun AppTile(m: Module, onClick: () -> Unit, onRemove: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val scale = pressScale(source)
    var menu by remember { mutableStateOf(false) }
    Box {
        Column(
            Modifier
                .aspectRatio(1f)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .clip(TileShape)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .combinedClickable(
                    interactionSource = source,
                    indication = ripple(),
                    onLongClick = { menu = true },
                    onClick = onClick,
                )
                .padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            ModuleBadge(m)
            Column {
                Text(m.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(
                    m.tagline,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        DropdownMenu(
            expanded = menu,
            onDismissRequest = { menu = false },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            DropdownMenuItem(
                text = { Text("Remove from home") },
                leadingIcon = { Icon(Icons.Rounded.RemoveCircleOutline, null) },
                onClick = { menu = false; onRemove() },
            )
        }
    }
}

@Composable
private fun AddTile(onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val scale = pressScale(source)
    val dash = MaterialTheme.colorScheme.outline
    Box(
        Modifier
            .aspectRatio(1f)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(TileShape)
            .dashedBorder(dash, TileRadius)
            .clickable(source, ripple(), onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Add, "Add app", tint = MaterialTheme.colorScheme.primary) }
            Spacer(Modifier.height(8.dp))
            Text("Add app", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AddSheet(available: List<Module>, onPick: (Module) -> Unit, onDismiss: () -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Text(
            "Add to home",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        available.forEach { m ->
            ListItem(
                headlineContent = { Text(m.title, fontWeight = FontWeight.Medium) },
                supportingContent = { Text(m.tagline) },
                leadingContent = { ModuleBadge(m, 44.dp) },
                trailingContent = { Icon(Icons.Rounded.Add, null, tint = MaterialTheme.colorScheme.primary) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable {
                        // Slide the sheet away first, then the tile animates into the grid.
                        scope.launch { state.hide() }.invokeOnCompletion { onPick(m); onDismiss() }
                    },
            )
        }
        Spacer(Modifier.navigationBarsPadding().height(16.dp))
    }
}
