package com.example.businessdaycalc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
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

    @Test
    fun testStorageDateLogicNormal() {
        // 일반등기: 배달 1 + 보관 4 = 총 5 영업일
        val calculator = BusinessDayCalculator(emptySet(), emptySet())
        val baseDate = LocalDate.of(2026, 9, 3) // Thursday
        val deliverySteps = 1
        val storageSteps = 4
        val totalSteps = deliverySteps + storageSteps
        assertEquals(LocalDate.of(2026, 9, 9), calculator.addBusinessDays(baseDate, totalSteps)) // Wednesday
    }

    @Test
    fun testStorageDateLogicCertified() {
        // 내용증명: 배달 2 + 보관 2 = 총 4 영업일
        val calculator = BusinessDayCalculator(emptySet(), emptySet())
        val baseDate = LocalDate.of(2026, 9, 3) // Thursday
        val deliverySteps = 2
        val storageSteps = 2
        val totalSteps = deliverySteps + storageSteps
        assertEquals(LocalDate.of(2026, 9, 8), calculator.addBusinessDays(baseDate, totalSteps)) // Tuesday
    }

    @Test
    fun testStorageDateLogicContract() {
        // 계약등기: 배달 3 + 보관 2 = 총 5 영업일
        val calculator = BusinessDayCalculator(emptySet(), emptySet())
        val baseDate = LocalDate.of(2026, 9, 3) // Thursday
        val deliverySteps = 3
        val storageSteps = 2
        val totalSteps = deliverySteps + storageSteps
        assertEquals(LocalDate.of(2026, 9, 9), calculator.addBusinessDays(baseDate, totalSteps)) // Wednesday
    }

    @Test
    fun testChuseokFriday() {
        // 추석이 금요일인 경우: 목요일(평일)만 추가, 토요일(주말)은 제외
        val chuseok = LocalDate.of(2026, 9, 25) // Friday
        val expanded = CalendarHelper.expandSeollalChuseokHolidays(setOf(chuseok), setOf(chuseok))

        assertTrue(expanded.contains(LocalDate.of(2026, 9, 24))) // Thursday (added)
        assertFalse(expanded.contains(LocalDate.of(2026, 9, 26))) // Saturday (weekend, excluded)
    }

    @Test
    fun testChuseokMonday() {
        // 추석이 월요일인 경우: 화요일(평일)만 추가, 일요일(주말)은 제외
        val chuseok = LocalDate.of(2026, 9, 21) // Monday
        val expanded = CalendarHelper.expandSeollalChuseokHolidays(setOf(chuseok), setOf(chuseok))

        assertTrue(expanded.contains(LocalDate.of(2026, 9, 22))) // Tuesday (added)
        assertFalse(expanded.contains(LocalDate.of(2026, 9, 20))) // Sunday (weekend, excluded)
    }

    @Test
    fun testChuseokTuesday() {
        // 추석이 화요일인 경우: 월요일(평일), 수요일(평일) 둘 다 추가
        val chuseok = LocalDate.of(2026, 9, 22) // Tuesday
        val expanded = CalendarHelper.expandSeollalChuseokHolidays(setOf(chuseok), setOf(chuseok))

        assertTrue(expanded.contains(LocalDate.of(2026, 9, 21))) // Monday (added)
        assertTrue(expanded.contains(LocalDate.of(2026, 9, 23))) // Wednesday (added)
    }
}
