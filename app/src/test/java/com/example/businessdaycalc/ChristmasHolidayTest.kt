package com.example.businessdaycalc

import android.provider.CalendarContract
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ChristmasHolidayTest {
    private data class FakeEvent(val date: LocalDate, val title: String, val availability: Int)

    private val eve = LocalDate.of(2026, 12, 24)
    private val christmas = LocalDate.of(2026, 12, 25)
    private val free = CalendarContract.Instances.AVAILABILITY_FREE
    private val busy = CalendarContract.Instances.AVAILABILITY_BUSY

    private fun holidaysFrom(events: List<FakeEvent>): Set<LocalDate> = events
        .filter { CalendarHelper.isPublicHoliday(it.title, it.availability) }
        .map { it.date }
        .toSet()

    @Test
    fun freeChristmasEveIsExcludedButChristmasIsIncluded() {
        val events = listOf(
            FakeEvent(eve, "크리스마스이브", free),
            FakeEvent(christmas, "크리스마스", free)
        )
        assertEquals(setOf(christmas), holidaysFrom(events))
    }

    @Test
    fun christmasEveWithSpaceIsAlsoExcluded() {
        assertFalse(CalendarHelper.isPublicHoliday("크리스마스 이브", free))
    }

    @Test
    fun fakeEventsProduceCorrectBusinessAndDeliveryDates() {
        val events = listOf(
            FakeEvent(eve, "크리스마스이브", free),
            FakeEvent(christmas, "크리스마스", busy)
        )
        val calculator = BusinessDayCalculator(emptySet(), holidaysFrom(events))
        assertTrue(calculator.isBusinessDay(eve))
        assertFalse(calculator.isBusinessDay(christmas))
        assertEquals(listOf(eve, LocalDate.of(2026, 12, 28), LocalDate.of(2026, 12, 29)),
            calculator.calculateDeliveryDates(eve, 3))
        assertEquals(LocalDate.of(2026, 12, 28), calculator.addBusinessDays(eve, 2))
    }

    @Test
    fun unknownAvailabilityStillUsesExactNames() {
        assertEquals(setOf(christmas), holidaysFrom(listOf(
            FakeEvent(eve, "크리스마스이브", -1),
            FakeEvent(christmas, "크리스마스", -1)
        )))
    }

    @Test
    fun busyChristmasEveIsStillClassifiedAsHolidayByExistingRule() {
        // 현재 BUSY 우선 규칙의 한계를 기록: 제목 일치 검사보다 먼저 휴일로 인정됩니다.
        assertEquals(setOf(eve, christmas), holidaysFrom(listOf(
            FakeEvent(eve, "크리스마스이브", busy),
            FakeEvent(christmas, "크리스마스", busy)
        )))
    }
}
