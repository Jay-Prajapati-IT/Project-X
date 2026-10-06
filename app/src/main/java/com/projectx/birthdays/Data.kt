package com.projectx.birthdays

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.Year
import java.time.temporal.ChronoUnit

/** Someone's date of birth. `year` is null when it isn't known (then no age is shown). */
@Entity
data class Person(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val month: Int,
    val day: Int,
    val year: Int? = null,
    val note: String = "",
) {
    /** The birthday in a given year. Feb 29 falls on Feb 28 in non-leap years. */
    fun dateIn(y: Int): LocalDate =
        if (month == 2 && day == 29 && !Year.isLeap(y.toLong())) LocalDate.of(y, 2, 28) else LocalDate.of(y, month, day)

    /** Next birthday on or after `from`. */
    fun next(from: LocalDate): LocalDate = dateIn(from.year).let { if (it < from) dateIn(from.year + 1) else it }

    fun daysUntil(from: LocalDate) = ChronoUnit.DAYS.between(from, next(from)).toInt()

    /** Age they turn on their birthday in year `y`. */
    fun turns(y: Int) = year?.let { y - it }?.takeIf { it > 0 }
}

@Dao
interface BirthdayDao {
    @Query("SELECT * FROM Person ORDER BY name COLLATE NOCASE")
    fun people(): Flow<List<Person>>

    @Query("SELECT * FROM Person")
    suspend fun all(): List<Person>

    @Query("SELECT * FROM Person WHERE id = :id")
    suspend fun get(id: Long): Person?

    @Upsert
    suspend fun save(p: Person)

    @Query("DELETE FROM Person WHERE id = :id")
    suspend fun delete(id: Long)
}

/**
 * Who to remind today, with days left. Always on the day itself, plus `daysBefore` days ahead.
 * `daily` = also every day in between.
 */
fun dueReminders(people: List<Person>, today: LocalDate, daysBefore: Int, daily: Boolean): List<Pair<Person, Int>> =
    people.mapNotNull { p ->
        val d = p.daysUntil(today)
        if (d == 0 || d == daysBefore || (daily && d < daysBefore)) p to d else null
    }

fun untilLabel(days: Int) = when (days) {
    0 -> "Today 🎉"
    1 -> "Tomorrow"
    else -> "in $days days"
}
