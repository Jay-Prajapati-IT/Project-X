# Context

## Owner
Single user (the developer). Personal-use app, sideloaded onto their own Android phone.

## Problem
- Too many separate apps, each with ads, upgrade popups, and bloat.
- Sensitive data (passwords, private files) is sitting in third-party apps.

## Constraints
- Android only. Target: Android 10+ (minSdk 29). Adjust once the phone's version is known.
- Must work fully offline.
- Must survive a reboot: alarms and reminders get rescheduled on boot.
- Android battery optimization can kill alarms, so we use `AlarmManager.setExactAndAllowWhileIdle`
  / `setAlarmClock` and ask the user to exempt the app from battery optimization.
- Android 13+ requires the POST_NOTIFICATIONS permission; Android 12+ requires SCHEDULE_EXACT_ALARM / USE_EXACT_ALARM.

## Decisions (and why)
| Decision | Why |
|---|---|
| Kotlin + Jetpack Compose (native) | Alarms, biometrics, Keystore, and file encryption are all native Android APIs. Cross-platform would only add plugin layers. |
| Room (SQLite) | Built-in, reliable, one DB for all modules |
| SQLCipher for the DB | Encrypts the whole DB, so vault data is safe even if the phone is rooted/copied |
| Jetpack Security / Tink for files | Standard, audited file encryption |
| No DI framework at the start | Single user, single app. A manual `AppContainer` object is enough. Add Hilt if it gets painful. |
| Single module Gradle project | Packages per feature. Split into modules only if build time hurts. |

## Open questions (please answer)
1. **Your phone model and Android version?** This sets minSdk and alarm reliability workarounds (Xiaomi/Oppo/Vivo kill background apps aggressively).
2. **App lock:** should the *whole app* be locked with fingerprint, or only Vault + Locker?
3. **Birthdays:** import from phone Contacts, or enter manually only?
4. **File Locker:** should files be *moved* into the locker (original deleted) or *copied*?
5. **Backup:** is a manual encrypted export file enough, or do you want auto-backup to a folder?
6. **Password vault extras:** password generator? autofill into other apps (Android Autofill service, adds more work)?
7. **Alarms:** full-screen ringing alarm (like a clock app) or just a loud notification?
8. ~~Home-screen widget~~: answered. Widgets will be requested over time (Jetpack Glance).
