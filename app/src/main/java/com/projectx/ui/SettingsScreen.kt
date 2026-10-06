package com.projectx.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.projectx.BuildConfig
import com.projectx.Module
import com.projectx.Prefs
import com.projectx.ThemeMode

@Composable
fun SettingsScreen(prefs: Prefs, onBack: () -> Unit) {
    val enableNotifications = rememberEnableNotifications(prefs)

    Scaffold { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(bottom = 24.dp)) {
            item { TopBar("Settings", onBack) }

            item {
                Section("Apps on home") {
                    Module.entries.forEach { m ->
                        ListItem(
                            headlineContent = { Text(m.title, fontWeight = FontWeight.Medium) },
                            supportingContent = { Text(m.tagline) },
                            leadingContent = { ModuleBadge(m, 40.dp) },
                            trailingContent = {
                                Switch(m in prefs.home, { on -> if (on) prefs.pin(m) else prefs.unpin(m) })
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        )
                    }
                }
            }

            item {
                Section("Appearance") {
                    Column(Modifier.padding(16.dp)) {
                        Text("Theme", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(12.dp))
                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                            ThemeMode.entries.forEachIndexed { i, t ->
                                SegmentedButton(
                                    selected = prefs.theme == t,
                                    onClick = { prefs.updateTheme(t) },
                                    shape = SegmentedButtonDefaults.itemShape(i, ThemeMode.entries.size),
                                ) { Text(t.label) }
                            }
                        }
                    }
                }
            }

            item {
                Section("General") {
                    Row(Icons.Rounded.Notifications, "Notifications", "Reminders, alarms and birthday alerts") {
                        Switch(prefs.notifications, { on ->
                            if (on) enableNotifications() else prefs.updateNotifications(false)
                        })
                    }
                }
            }

            item {
                Section("About") {
                    Row(Icons.Rounded.Info, "PR-X ${BuildConfig.VERSION_NAME}", "Offline · No ads · No tracking")
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.2.sp,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
        )
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
            Column(Modifier.padding(vertical = 6.dp), content = content)
        }
    }
}

@Composable
private fun Row(icon: ImageVector, title: String, subtitle: String, trailing: @Composable () -> Unit = {}) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.Medium) },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(icon, null, tint = MaterialTheme.colorScheme.primary) },
        trailingContent = trailing,
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}
