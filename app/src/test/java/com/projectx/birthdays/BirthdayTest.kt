package com.projectx.birthdays

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class BirthdayTest {
    private val today = LocalDate.of(2026, 10, 6)

    @Test
    fun nextBirthday() {
        val june = Person(name = "A", month = 6, day = 17, year = 2000)
        assertEquals(LocalDate.of(2027, 6, 17), june.next(today)) // already passed this year
        assertEquals(26, june.turns(2026))
        assertEquals(0, Person(name = "T", month = 10, day = 6).daysUntil(today)) // today counts
        assertNull(Person(name = "N", month = 1, day = 1).turns(2026)) // unknown year

        val leap = Person(name = "L", month = 2, day = 29, year = 2000)
        assertEquals(LocalDate.of(2027, 2, 28), leap.next(today))
        assertEquals(LocalDate.of(2028, 2, 29), leap.dateIn(2028))
    }

    @Test
    fun reminders() {
        val inTwo = Person(id = 1, name = "Two", month = 10, day = 8)
        val inFive = Person(id = 2, name = "Five", month = 10, day = 11)
        val todayP = Person(id = 3, name = "Now", month = 10, day = 6)
        val all = listOf(inTwo, inFive, todayP)

        // once, 2 days before: exactly 2 days ahead + today
        assertEquals(listOf(inTwo to 2, todayP to 0), dueReminders(all, today, 2, daily = false))
        // daily, 7 days before: everyone within the week
        assertEquals(listOf(inTwo to 2, inFive to 5, todayP to 0), dueReminders(all, today, 7, daily = true))
        // on the day only
        assertEquals(listOf(todayP to 0), dueReminders(all, today, 0, daily = false))
    }
}
