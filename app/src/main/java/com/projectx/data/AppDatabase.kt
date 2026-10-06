package com.projectx.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.projectx.birthdays.BirthdayDao
import com.projectx.birthdays.Person
import com.projectx.habits.Entry
import com.projectx.habits.Habit
import com.projectx.habits.HabitDao

// ponytail: plain SQLite for now; switch to SQLCipher in Phase 6 when Vault data arrives.
@Database(
    entities = [Habit::class, Entry::class, Person::class],
    version = 2,
    autoMigrations = [AutoMigration(from = 1, to = 2)], // v2: birthdays
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun habits(): HabitDao
    abstract fun birthdays(): BirthdayDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context, AppDatabase::class.java, "projectx.db").build().also { instance = it }
        }
    }
}
