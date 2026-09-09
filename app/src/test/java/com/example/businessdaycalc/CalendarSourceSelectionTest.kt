package com.example.businessdaycalc

import com.example.businessdaycalc.CalendarHelper.HolidaySource
import org.junit.Assert.assertEquals
import org.junit.Test

class CalendarSourceSelectionTest {
    private val local = HolidaySource(1L, "LOCAL")
    private val google = HolidaySource(2L, "com.google")
    private val other = HolidaySource(3L, "com.example.other")

    @Test
    fun localExcludesGoogleRegardlessOfOrder() {
        for (sources in listOf(listOf(local, google, other), listOf(google, other, local))) {
            assertEquals(listOf(local), CalendarHelper.selectHolidaySources(sources))
        }
    }

    @Test
    fun localOnlyIsRetained() {
        assertEquals(listOf(local), CalendarHelper.selectHolidaySources(listOf(local)))
    }

    @Test
    fun googleIsUsedWhenLocalIsAbsent() {
        assertEquals(listOf(google), CalendarHelper.selectHolidaySources(listOf(other, google)))
    }

    @Test
    fun unsupportedAccountsAreNotUsedAsFallback() {
        assertEquals(emptyList<HolidaySource>(), CalendarHelper.selectHolidaySources(listOf(other)))
    }

    @Test
    fun noCalendarsProducesNoSources() {
        assertEquals(emptyList<HolidaySource>(), CalendarHelper.selectHolidaySources(emptyList()))
    }

    @Test
    fun allLocalCalendarsAreRetainedWithoutGoogle() {
        val secondLocal = HolidaySource(4L, "LOCAL")
        assertEquals(listOf(local, secondLocal),
            CalendarHelper.selectHolidaySources(listOf(local, google, secondLocal)))
    }

    @Test
    fun allGoogleCalendarsAreRetainedWhenLocalIsAbsent() {
        val secondGoogle = HolidaySource(5L, "com.google")
        assertEquals(listOf(google, secondGoogle),
            CalendarHelper.selectHolidaySources(listOf(google, other, secondGoogle)))
    }

    @Test
    fun accountTypeMatchingIgnoresCase() {
        val lowerLocal = HolidaySource(6L, "local")
        val upperGoogle = HolidaySource(7L, "COM.GOOGLE")
        assertEquals(listOf(lowerLocal),
            CalendarHelper.selectHolidaySources(listOf(upperGoogle, lowerLocal)))
        assertEquals(listOf(upperGoogle),
            CalendarHelper.selectHolidaySources(listOf(upperGoogle)))
    }
}
