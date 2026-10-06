# Architecture

## Stack
| Layer | Tech |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3, Navigation Compose |
| State | ViewModel + StateFlow |
| Storage | Room over SQLCipher (encrypted SQLite) |
| Files | Jetpack Security `EncryptedFile` in app-private storage |
| Keys | Android Keystore (hardware-backed), unlocked via BiometricPrompt |
| Scheduling | AlarmManager (exact alerts), WorkManager (daily housekeeping, e.g. birthday check) |
| Notifications | NotificationManager + channels per module |
| Widgets | Jetpack Glance (Compose-style home-screen widgets, reads the same Room DB) |
| Build | Gradle (Kotlin DSL), Android Studio |

## Build (Windows, no Android Studio needed)
- SDK: `C:\Users\admin\Android\Sdk` (path in `local.properties`)
- Release APK: `gradlew.bat assembleRelease` → `app/build/outputs/apk/release/app-release.apk`
- Signing key: `keystore/projectx.jks` + `keystore.properties`. **Back both up.** Without them you can't install updates over the existing app (you'd have to uninstall and lose your data).
- Bump `versionCode`/`versionName` in `app/build.gradle.kts` for every release.

## Current code (v0.1.0)
| File | Role |
|---|---|
| `MainActivity.kt` | Edge-to-edge window, theme, NavHost (home / settings / module/{id}) with slide+fade transitions |
| `Module.kt` | Enum of all mini-apps (id, title, icon, phase). Add a module here. |
| `Prefs.kt` | Settings (home apps, theme, notifications) in SharedPreferences, observable by Compose |
| `ui/theme/Theme.kt` | Graphite + lavender colors, rounded shapes, animated theme switch |
| `ui/Components.kt` | Shared pieces: press-scale, round icon button, module badge, top bar |
| `ui/HomeScreen.kt` | Header + 2-column tile grid, "+" tiles, add-app bottom sheet, long-press to remove |
| `ui/SettingsScreen.kt` | Apps-on-home switches, theme picker, notifications, about |
| `ui/ModuleScreen.kt` | Placeholder screen until each module is built |
| `data/AppDatabase.kt` | Room DB singleton (all modules' tables go here) |
| `habits/Data.kt` | Habit + Entry tables, DAO, check types, 5 habit colors, streak math |
| `habits/HabitRoutes.kt` | Habit nav routes + `HabitTheme` (per-habit color & light/dark) |
| `habits/HabitListScreen.kt` | Habit list with monthly counts |
| `habits/HabitCalendarScreen.kt` | Month/year calendar, day sheet, stats |
| `habits/HabitFormScreen.kt` | One form for new / edit / defaults |

Screens collect Room `Flow`s directly and write via coroutines. No ViewModels until a screen actually needs one.
Room schema JSON is exported to `app/schemas/`. Keep it, because future DB migrations are checked against it.

## Layers (kept thin)
```
UI (Compose screens)
   ↓ observes StateFlow / calls functions
ViewModel (one per screen)
   ↓
DAO (Room)  +  small helpers (AlarmScheduler, Crypto, Backup)
   ↓
Encrypted SQLite DB / Encrypted files / Keystore
```
No repository/use-case layers. ViewModels call DAOs directly. Add a layer only when two screens need the same non-trivial logic.

## Package layout
```
app/src/main/java/com/projectx/
  MainActivity.kt          # single activity, nav host
  App.kt                   # Application: builds DB, notification channels
  data/
    AppDatabase.kt         # Room DB, all entities
    entities + DAOs        # Habit, HabitLog, Reminder, Birthday, Todo, VaultEntry, LockedFile
  core/
    AlarmScheduler.kt      # schedule/cancel exact alarms
    AlarmReceiver.kt       # fires notification
    BootReceiver.kt        # reschedules everything after reboot
    Crypto.kt              # Keystore + biometric unlock, file encrypt/decrypt
    Backup.kt              # encrypted export/import
  ui/
    home/  habits/  reminders/  birthdays/  todo/  vault/  locker/  settings/
  widget/                  # one Glance widget file per widget (e.g. TodoWidget.kt, HabitWidget.kt)
```

## Adding a feature (the whole recipe)
Features are expected to stay simple, so any new one follows the same pattern:
1. Entity + DAO in `data/` (bump the DB version with a simple migration).
2. Screen + ViewModel in `ui/<feature>/`.
3. A nav route in `MainActivity`.
4. Optional: a widget in `widget/`, or an alert via `AlarmScheduler`.

Widgets never show Vault/Locker data.

## Data model (first cut)
| Table | Key fields |
|---|---|
| Habit | id, name, type (check/emoji/number), multi, options, color, mode, createdAt |
| Entry | id, habitId, day (epochDay), value, emoji? (many per day when multi) |
| reminder | id, title, triggerAt, repeat (none/daily/weekly/monthly), enabled |
| birthday | id, name, month, day, year?, notifyDaysBefore |
| todo | id, title, notes?, dueAt?, done, reminderId? |
| vault_entry | id, title, username, password, url?, notes? (DB encrypted as a whole) |
| locked_file | id, displayName, mimeType, encryptedPath, size, addedAt |

Streaks are computed from `habit_log`, not stored.

## Security model
- DB passphrase: random 32 bytes, wrapped by a Keystore AES key.
- Vault/Locker screens need BiometricPrompt (fingerprint or device PIN) every time you open them. They auto-lock after 1 minute in the background.
- `FLAG_SECURE` on Vault/Locker screens: no screenshots, blank in the recents list.
- No `INTERNET` permission in the manifest.
- Backup file is encrypted with a password you type at export time (not the Keystore key, so a restore works on a new phone).

## Permissions
`POST_NOTIFICATIONS`, `SCHEDULE_EXACT_ALARM`/`USE_EXACT_ALARM`, `RECEIVE_BOOT_COMPLETED`, `USE_BIOMETRIC`, `VIBRATE`. Optional: `READ_CONTACTS` (birthday import).
