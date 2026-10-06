package com.projectx.birthdays

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.projectx.MainActivity
import com.projectx.Prefs
import com.projectx.R
import com.projectx.data.AppDatabase
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

private const val CHANNEL = "birthdays"
private const val REMIND_HOUR = 9

/** Runs once a day around 9 AM: refreshes the widget and sends due birthday reminders. */
class BirthdayWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val ctx = applicationContext
        BirthdayWidget().updateAll(ctx) // "today" moved on
        val prefs = Prefs(ctx)
        val today = LocalDate.now()
        if (prefs.notifications && prefs.bdayRemind && prefs.bdayLastNotified != today.toEpochDay()) {
            val due = dueReminders(AppDatabase.get(ctx).birthdays().all(), today, prefs.bdayDaysBefore, prefs.bdayDaily)
            notify(ctx, due, today)
            prefs.markBdayNotified(today.toEpochDay())
        }
        return Result.success()
    }

    companion object {
        // ponytail: WorkManager's daily run is inexact (can drift by minutes); fine for birthdays. Use AlarmManager if exact time matters.
        fun schedule(context: Context) {
            val now = LocalDateTime.now()
            var next = now.toLocalDate().atTime(REMIND_HOUR, 0)
            if (!next.isAfter(now)) next = next.plusDays(1)
            val request = PeriodicWorkRequestBuilder<BirthdayWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(Duration.between(now, next))
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork("birthdays", ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}

@SuppressLint("MissingPermission") // checked via areNotificationsEnabled(); SecurityException caught anyway
private fun notify(ctx: Context, due: List<Pair<Person, Int>>, today: LocalDate) {
    val nm = NotificationManagerCompat.from(ctx)
    if (due.isEmpty() || !nm.areNotificationsEnabled()) return
    nm.createNotificationChannel(
        NotificationChannelCompat.Builder(CHANNEL, NotificationManagerCompat.IMPORTANCE_DEFAULT).setName("Birthdays").build()
    )
    val open = PendingIntent.getActivity(
        ctx,
        0,
        Intent(ctx, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_ROUTE, "birthdays")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
    due.forEach { (p, days) ->
        val date = p.next(today)
        val title = when (days) {
            0 -> "🎂 It's ${p.name}'s birthday today!"
            1 -> "🎂 ${p.name}'s birthday is tomorrow"
            else -> "🎂 ${p.name}'s birthday is in $days days"
        }
        val text = (p.turns(date.year)?.let { "Turning $it · " } ?: "") + date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM"))
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            nm.notify(p.id.toInt(), n)
        } catch (_: SecurityException) {
        }
    }
}
