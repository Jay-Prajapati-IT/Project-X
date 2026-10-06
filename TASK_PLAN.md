# Task Plan

Build in this order. Each phase ends with a usable APK on your phone, so you can start using the app early.

## Phase 0 — Setup
- [x] SDK + Gradle set up (command line), Compose project `com.projectx`, signed release APK
- [x] Home screen: tile grid, "+" to add apps, long-press to remove, settings button
- [x] Settings: apps on home, theme (System/Dark/Light), notifications toggle, about
- [x] Navigation with smooth transitions, placeholder screen per module
- [x] Room database (added with Habits). SQLCipher encryption comes in Phase 6.

## Habits (built first, at the user's request — v0.2.0)
- [x] Habit list: name, this-month count, ⚙ per habit, "+ New habit", ⚙ top = defaults for new habits
- [x] Check types: check / emoji options / number options, optional multiple-per-day
- [x] Per-habit color (5 colors, light+dark variants) and per-habit light/dark/app appearance
- [x] Calendar: Month/Year toggle, ‹ › and swipe, tap to log, hold for day sheet, year heat-map
- [x] Stats: period total, current streak, best streak, "log today" button
- [x] Delete habit (with confirm)
- [ ] Optional daily nudge reminder (after Phase 2)

## Phase 1 — Todo (simplest, proves the stack)
- [ ] Todo entity/DAO, list screen, add/edit/delete, mark done
- [ ] Optional due date

## Phase 2 — Reminders & Alarms (the hardest reliability part)
- [ ] Notification channels + permission request
- [ ] AlarmScheduler + AlarmReceiver, one-time reminders
- [ ] Repeat rules (daily/weekly/monthly)
- [ ] BootReceiver reschedules alarms after reboot
- [ ] Battery-optimization exemption prompt
- [ ] Link a todo's due date to a reminder

## Birthdays (v0.3.0)
- [x] Add / edit / delete people (DOB, year optional, note)
- [x] Calendar (month/year, shared with Habits) with birthdays colored; tap a date for details
- [x] List view sorted by next birthday, "Next up" card
- [x] Settings: color + light/dark, reminders (days before, once/daily), add widget
- [x] Daily reminder worker (~9 AM, WorkManager)
- [x] Home-screen widget (Glance): list ↔ calendar
- [ ] Import from Contacts (only if wanted)

## Phase 5 — Home dashboard
- [ ] Today's view pulling data from all modules

## Phase 6 — Security core
- [ ] Keystore key + BiometricPrompt helper
- [ ] FLAG_SECURE, auto-lock timer

## Phase 7 — Password Vault
- [ ] CRUD entries, copy to clipboard (auto-clear after 30s), show/hide password
- [ ] Password generator (if wanted)

## Phase 8 — File Locker
- [ ] Pick files via the system picker, encrypt into app storage, list, open (decrypt to a temp file, then delete it), export back out

## Phase 9 — Backup & polish
- [ ] Encrypted export/import
- [ ] Dark mode, app icon, signed release APK

## Widgets (whenever you ask)
- [ ] Glance setup (one-time)
- [ ] Each widget = one file in `widget/` (e.g. todo list, today's habits, next birthday)

## Later (only if you miss it)
Autofill service, search across modules, stats.
