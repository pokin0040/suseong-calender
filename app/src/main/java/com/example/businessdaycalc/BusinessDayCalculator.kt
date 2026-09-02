package com.example.businessdaycalc

import java.time.DayOfWeek
import java.time.LocalDate

class BusinessDayCalculator(
    private val customHolidays: Set<LocalDate>,
    private val deviceHolidays: Set<LocalDate>
) {

    /**
     * 기준일(startDate)부터 시작하여 지정된 영업일 수(daysToAdd)에 해당하는 날짜를 반환합니다.
     * 기준일이 영업일이면 기준일 자체가 1일차 영업일이 됩니다.
     */
    fun addBusinessDays(startDate: LocalDate, daysToAdd: Int): LocalDate {
        var currentDate = startDate
        var addedDays = 0

        while (addedDays < daysToAdd) {
            if (isBusinessDay(currentDate)) {
                addedDays++
                if (addedDays == daysToAdd) {
                    return currentDate
                }
            }
            currentDate = currentDate.plusDays(1)
        }
        return currentDate
    }

    /**
     * 지정된 단계(steps) 수만큼의 영업일 목록을 반환합니다.
     * 기준일(startDate)이 영업일이면 기준일 자체가 1차 배달일이 됩니다.
     * 예: steps가 3이면, [1차(오늘), 2차 영업일, 3차 영업일] 의 리스트 반환
     */
    fun calculateDeliveryDates(startDate: LocalDate, steps: Int): List<LocalDate> {
        val dates = mutableListOf<LocalDate>()
        var currentDate = startDate

        while (dates.size < steps) {
            if (isBusinessDay(currentDate)) {
                dates.add(currentDate)
            }
            currentDate = currentDate.plusDays(1)
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
