package com.example.businessdaycalc

import java.time.DayOfWeek
import java.time.LocalDate

class BusinessDayCalculator(
    private val customHolidays: Set<LocalDate>,
    private val deviceHolidays: Set<LocalDate>
) {

    fun addBusinessDays(startDate: LocalDate, daysToAdd: Int): LocalDate {
        var currentDate = startDate
        var addedDays = 0

        while (addedDays < daysToAdd) {
            currentDate = currentDate.plusDays(1)
            if (isBusinessDay(currentDate)) {
                addedDays++
            }
        }
        return currentDate
    }

    /**
     * 지정된 단계(steps) 수만큼의 영업일 목록을 반환합니다.
     * 예: steps가 3이면, [+1영업일, +2영업일, +3영업일] 의 리스트 반환
     */
    fun calculateDeliveryDates(startDate: LocalDate, steps: Int): List<LocalDate> {
        val dates = mutableListOf<LocalDate>()
        var currentDate = startDate
        var addedDays = 0

        while (addedDays < steps) {
            currentDate = currentDate.plusDays(1)
            if (isBusinessDay(currentDate)) {
                dates.add(currentDate)
                addedDays++
            }
        }
        return dates
    }

    private fun isBusinessDay(date: LocalDate): Boolean {
        if (date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY) {
            return false
        }
        if (customHolidays.contains(date)) {
            return false
        }
        if (deviceHolidays.contains(date)) {
            return false
        }
        return true
    }
}
