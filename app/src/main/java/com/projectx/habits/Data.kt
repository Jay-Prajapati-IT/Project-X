package com.projectx.habits

import androidx.compose.ui.graphics.Color
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.projectx.ui.theme.AccentColor
import com.projectx.ui.theme.ModeChoice
import kotlinx.coroutines.flow.Flow

// All `id` strings below are stored in the DB / prefs. Never rename them.

enum class CheckType(val id: String, val label: String, val hint: String, val defaultOptions: String) {
    CHECK("check", "Check", "Tap a day to mark it done", ""),
    EMOJI("emoji", "Emoji", "Pick one of your emojis for the day", "😄 🙂 😐 😕 😫"),
    NUMBER("number", "Number", "Pick one of your numbers for the day", "1 2 5 10");

    companion object {
        fun of(id: String) = entries.find { it.id == id } ?: CHECK
    }
}

@Entity
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String = "",
    val type: String = CheckType.CHECK.id,
    val multi: Boolean = false,
    /** Space-separated emojis or numbers (EMOJI / NUMBER types). */
    val options: String = "",
    val color: String = AccentColor.PURPLE.id,
    val mode: String = ModeChoice.APP.id,
    val createdAt: Long = 0,
) {
    val checkType get() = CheckType.of(type)
    val optionList get() = options.split(' ').filter { it.isNotBlank() }
}

/** One log on one day. `day` = LocalDate.toEpochDay(). Check = 1, Number = picked value, Emoji = 1 + emoji. */
@Entity(indices = [Index("habitId", "day")])
data class Entry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: Long,
    val day: Long,
    val value: Int = 1,
    val emoji: String? = null,
) {
    fun label(type: CheckType) = when (type) {
        CheckType.CHECK -> "✓"
        CheckType.EMOJI -> emoji ?: "?"
        CheckType.NUMBER -> "$value"
    }
}

data class HabitTotal(val habitId: Long, val total: Int)

@Dao
interface HabitDao {
    @Query("SELECT * FROM Habit ORDER BY createdAt")
    fun habits(): Flow<List<Habit>>

    @Query("SELECT * FROM Habit WHERE id = :id")
    fun habit(id: Long): Flow<Habit?>

    @Query("SELECT * FROM Habit WHERE id = :id")
    suspend fun get(id: Long): Habit?

    @Upsert
    suspend fun save(h: Habit)

    @Query("DELETE FROM Habit WHERE id = :id")
    suspend fun deleteHabit(id: Long)

    @Query("DELETE FROM Entry WHERE habitId = :id")
    suspend fun deleteEntries(id: Long)

    @Transaction
    suspend fun delete(id: Long) {
        deleteEntries(id)
        deleteHabit(id)
    }

    @Query("SELECT habitId, SUM(value) AS total FROM Entry WHERE day BETWEEN :from AND :to GROUP BY habitId")
    fun totals(from: Long, to: Long): Flow<List<HabitTotal>>

    // ponytail: loads all of a habit's entries; fine for years of personal logs, page by range if it ever lags.
    @Query("SELECT * FROM Entry WHERE habitId = :id ORDER BY day, id")
    fun entries(id: Long): Flow<List<Entry>>

    @Insert
    suspend fun add(e: Entry)

    @Delete
    suspend fun remove(e: Entry)

    @Query("DELETE FROM Entry WHERE habitId = :id AND day = :day")
    suspend fun clearDay(id: Long, day: Long)

    @Transaction
    suspend fun replaceDay(e: Entry) {
        clearDay(e.habitId, e.day)
        add(e)
    }
}

/** (current streak, best streak) in days. Current streak still counts if today isn't logged yet. */
fun streaks(days: Set<Long>, today: Long): Pair<Int, Int> {
    var current = 0
    var d = if (today in days) today else today - 1
    while (d in days) { current++; d-- }
    var best = 0
    var run = 0
    var prev = Long.MIN_VALUE
    for (x in days.sorted()) {
        run = if (x == prev + 1) run + 1 else 1
        best = maxOf(best, run)
        prev = x
    }
    return current to best
}
