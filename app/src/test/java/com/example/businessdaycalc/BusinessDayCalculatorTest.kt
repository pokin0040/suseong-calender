package com.example.businessdaycalc

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class BusinessDayCalculatorTest {

    @Test
    fun testNormalWeekday() {
        val calculator = BusinessDayCalculator(emptySet(), emptySet())
        val start = LocalDate.of(2023, 10, 2) // Monday
        assertEquals(LocalDate.of(2023, 10, 2), calculator.addBusinessDays(start, 1)) // Monday
        assertEquals(LocalDate.of(2023, 10, 3), calculator.addBusinessDays(start, 2)) // Tuesday
    }

    @Test
    fun testFridayPlusOne() {
        val calculator = BusinessDayCalculator(emptySet(), emptySet())
        val start = LocalDate.of(2023, 10, 6) // Friday
        assertEquals(LocalDate.of(2023, 10, 6), calculator.addBusinessDays(start, 1)) // Friday
        assertEquals(LocalDate.of(2023, 10, 9), calculator.addBusinessDays(start, 2)) // Monday
    }

    @Test
    fun testWithHoliday() {
        val holidays = setOf(LocalDate.of(2023, 10, 3)) // Tuesday is holiday
        val calculator = BusinessDayCalculator(holidays, emptySet())
        val start = LocalDate.of(2023, 10, 2) // Monday
        assertEquals(LocalDate.of(2023, 10, 2), calculator.addBusinessDays(start, 1)) // Monday
        assertEquals(LocalDate.of(2023, 10, 4), calculator.addBusinessDays(start, 2)) // Wednesday (skipping Tue)
    }

    @Test
    fun testTodayIsHoliday() {
        val holidays = setOf(LocalDate.of(2023, 10, 3)) // Tuesday is holiday
        val calculator = BusinessDayCalculator(holidays, emptySet())
        val start = LocalDate.of(2023, 10, 3) // Start on holiday (Tuesday)
        assertEquals(LocalDate.of(2023, 10, 4), calculator.addBusinessDays(start, 1)) // Wednesday
    }

    @Test
    fun testDeliveryDates() {
        val calculator = BusinessDayCalculator(emptySet(), emptySet())
        val start = LocalDate.of(2026, 9, 3) // Thursday
        val deliveryDates = calculator.calculateDeliveryDates(start, 3)
        assertEquals(listOf(
            LocalDate.of(2026, 9, 3), // Thursday (Today)
            LocalDate.of(2026, 9, 4), // Friday
            LocalDate.of(2026, 9, 7)  // Monday
        ), deliveryDates)
    }
}
