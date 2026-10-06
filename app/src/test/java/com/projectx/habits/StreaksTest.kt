package com.projectx.habits

import org.junit.Assert.assertEquals
import org.junit.Test

class StreaksTest {
    @Test
    fun streaks() {
        assertEquals(0 to 0, streaks(emptySet(), 100))
        assertEquals(3 to 3, streaks(setOf(98, 99, 100), 100))
        // today not logged yet: streak through yesterday still counts
        assertEquals(2 to 2, streaks(setOf(98, 99), 100))
        // gap of a day breaks the current streak; best remembers the older run
        assertEquals(1 to 4, streaks(setOf(10, 11, 12, 13, 50, 100), 100))
        assertEquals(0 to 2, streaks(setOf(50, 51), 100))
    }
}
