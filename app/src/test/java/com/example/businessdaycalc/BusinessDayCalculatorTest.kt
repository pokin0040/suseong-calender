package com.example.businessdaycalc

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class BusinessDayCalculatorTest {

    @Test
    fun testNormalWeekday() {
        val calculator = BusinessDayCalculator(emptySet(), emptySet())
        val start = LocalDate.of(2023, 10, 2) // Monday
        assertEquals(LocalDate.of(2023, 10, 3), calculator.addBusinessDays(start, 1)) // Tuesday
        assertEquals(LocalDate.of(2023, 10, 4), calculator.addBusinessDays(start, 2)) // Wednesday
    }

    @Test
    fun testFridayPlusOne() {
        val calculator = BusinessDayCalculator(emptySet(), emptySet())
        val start = LocalDate.of(2023, 10, 6) // Friday
        assertEquals(LocalDate.of(2023, 10, 9), calculator.addBusinessDays(start, 1)) // Monday
    }

    @Test
    fun testFridayPlusTwo() {
        val calculator = BusinessDayCalculator(emptySet(), emptySet())
        val start = LocalDate.of(2023, 10, 6) // Friday
        assertEquals(LocalDate.of(2023, 10, 10), calculator.addBusinessDays(start, 2)) // Tuesday
    }

    @Test
    fun testWithHoliday() {
        val holidays = setOf(LocalDate.of(2023, 10, 3)) // Tuesday is holiday
        val calculator = BusinessDayCalculator(holidays, emptySet())
        val start = LocalDate.of(2023, 10, 2) // Monday
        assertEquals(LocalDate.of(2023, 10, 4), calculator.addBusinessDays(start, 1)) // Wednesday
        assertEquals(LocalDate.of(2023, 10, 5), calculator.addBusinessDays(start, 2)) // Thursday
    }

    @Test
    fun testConsecutiveHolidays() {
        val holidays = setOf(LocalDate.of(2023, 10, 3), LocalDate.of(2023, 10, 4)) // Tue, Wed holiday
        val calculator = BusinessDayCalculator(emptySet(), holidays)
        val start = LocalDate.of(2023, 10, 2) // Monday
        assertEquals(LocalDate.of(2023, 10, 5), calculator.addBusinessDays(start, 1)) // Thursday
    }

    @Test
    fun testWeekendAndHolidayCombined() {
        val holidays = setOf(LocalDate.of(2023, 10, 9)) // Monday is holiday
        val calculator = BusinessDayCalculator(holidays, emptySet())
        val start = LocalDate.of(2023, 10, 6) // Friday
        assertEquals(LocalDate.of(2023, 10, 10), calculator.addBusinessDays(start, 1)) // Tuesday
    }

    @Test
    fun testTodayIsHoliday() {
        val holidays = setOf(LocalDate.of(2023, 10, 3)) // Tuesday is holiday
        val calculator = BusinessDayCalculator(holidays, emptySet())
        val start = LocalDate.of(2023, 10, 3) // Start on holiday (Tuesday)
        assertEquals(LocalDate.of(2023, 10, 4), calculator.addBusinessDays(start, 1)) // Wednesday
    }
}
