package com.projectx.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.projectx.habits.Entry
import com.projectx.habits.Habit
import com.projectx.habits.HabitDao

// ponytail: plain SQLite for now; switch to SQLCipher in Phase 6 when Vault data arrives.
@Database(entities = [Habit::class, Entry::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun habits(): HabitDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context, AppDatabase::class.java, "projectx.db").build().also { instance = it }
        }
    }
}
