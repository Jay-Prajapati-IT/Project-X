# Claude History

A log of sessions and decisions. Newest entries go at the top.

## 2026-10-06 — Session 3: Habit tracker (v0.2.0)
**Done**
- Built the habit tracker from the user's second sketch: list → calendar → settings, plus defaults for new habits.
- Room added (KSP). Tables: Habit, Entry. Streak math has a unit test (`StreaksTest`).
- Per-habit theme: 5 colors (purple, blue, green, yellow, red), each with light and dark variants, and a per-habit light/dark/app mode. Themes nest, and the status bar icons follow the innermost theme.
- Shared components added: PillGroup (sliding segmented pill), AnimatedNumber, dashedBorder, TopBar action slot.
- APK: `PR-X-v0.2.0.apk` (~2.5 MB). Installs over v0.1.0 (same key, versionCode 2).

**Interpretations of the sketch (confirm with user)**
- The bottom "Calendar View" box became stats (period total, streak, best) + a "log today" button.
- The row count = total for this month. Number habits sum their values; check/emoji count entries.
- Check type can't change after creation (old logs would stop making sense). Everything else can.
- Tap a day: check = toggle (multi: +1); emoji/number = opens a picker sheet. Hold any day = sheet to remove entries.

## 2026-10-06 — Session 2: Base app (v0.1.0)
**Done**
- Installed Android cmdline-tools, SDK 35, build-tools, and Gradle 8.11.1 under `C:\Users\admin\Android`.
- Built the base app from the user's sketch: "PR-X" header, settings icon, 2-column rounded tiles, dashed "+" tiles that open an add-app sheet, long-press a tile to remove it.
- Settings: apps-on-home switches, theme (default Dark), notifications toggle (asks permission on Android 13+), about.
- Theme: graphite + lavender, 28dp rounded tiles, staggered entrance, press-spring, slide/fade navigation, animated theme switch, predictive back enabled.
- Release APK signed with a personal key (`keystore/`). Output: `PR-X-v0.1.0.apk` (~2 MB).

**Decisions**
- SharedPreferences for settings. Room is postponed until Phase 1 (no data yet).
- Stored ids are explicit strings (not enum names), so R8 shrinking can't break saved settings.

**Pending**
- Not yet tested on the real phone. Open questions 1–7 in CONTEXT.md are still open.
- Next: Phase 1, Todo.

## 2026-09-30 — Session 1b: Scope note
- User confirmed: features will stay simple; widgets will be requested over time.
- Added Jetpack Glance for widgets, a `widget/` package, and an "adding a feature" recipe. The rest of the structure is unchanged.

## 2026-09-30 — Session 1: Planning
**Done**
- Created OVERVIEW, CONTEXT, ARCHITECTURE, FLOWCHART, TASK_PLAN, CLAUDE_HISTORY docs.

**Decisions**
- Native Kotlin + Jetpack Compose, fully offline (no INTERNET permission).
- Room + SQLCipher for all data. Keystore + biometric for Vault/Locker.
- AlarmManager for exact reminders, WorkManager for the daily birthday check.
- Thin layers: Screen → ViewModel → DAO. No DI framework yet.

**Pending**
- User to answer the open questions in CONTEXT.md.
- Next: Phase 0 (project setup).
