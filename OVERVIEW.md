# Project-X — Overview

## What
One personal Android app that replaces several cluttered, ad-filled apps:

| Module | Replaces | Core job |
|---|---|---|
| Habits | habit trackers | Daily check-off, streaks |
| Reminders & Alarms | alarm/reminder apps | Exact-time alerts, repeat rules |
| Birthdays | birthday apps | Yearly alert N days before + on the day |
| Todo | todo apps | Tasks, due dates, done/undone |
| Password Vault | password managers | Encrypted credentials, locked by fingerprint/PIN |
| File Locker | file vault apps | Encrypted private files/photos |

## Principles
1. **Offline only.** No internet permission, so no ads, analytics, or cloud.
2. **Only the features you use.** A feature gets added when you actually miss it.
3. **Private by default.** Vault and Locker are encrypted with keys stored in Android Keystore.
4. **One home screen.** A dashboard showing today's habits, todos, reminders, and upcoming birthdays.
5. **Backups are yours.** Manual encrypted export to a file you pick (USB, your own drive, etc.).

## Not goals
- Multi-user, accounts, sync servers
- Play Store release (install by sideloading an APK)
- iOS
