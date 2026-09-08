package com.example.businessdaycalc

import android.icu.util.ChineseCalendar
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object KoreanHolidays {

    /**
     * 2024년 ~ 2126년 (100년 이상) 대한민국 법정공휴일, 음력 명절(설날, 추석, 부처님오신날) 및 대체공휴일 자동 생성 DB
     */
    val HOLIDAYS: Set<LocalDate> by lazy {
        generateHolidays(2024, 2126)
    }

    private fun generateHolidays(startYear: Int, endYear: Int): Set<LocalDate> {
        val set = mutableSetOf<LocalDate>()

        for (year in startYear..endYear) {
            // 1. 고정 양력 법정공휴일
            val solarHolidays = listOf(
                LocalDate.of(year, 1, 1),   // 신정
                LocalDate.of(year, 3, 1),   // 삼일절
                LocalDate.of(year, 5, 5),   // 어린이날
                LocalDate.of(year, 6, 6),   // 현충일
                LocalDate.of(year, 8, 15),  // 광복절
                LocalDate.of(year, 10, 3),  // 개천절
                LocalDate.of(year, 10, 9),  // 한글날
                LocalDate.of(year, 12, 25)  // 성탄절
            )
            set.addAll(solarHolidays)

            // 양력 공휴일 대체공휴일 (어린이날, 광복절, 개천절, 한글날, 성탄절)
            for (fixedDate in listOf(
                LocalDate.of(year, 5, 5),
                LocalDate.of(year, 8, 15),
                LocalDate.of(year, 10, 3),
                LocalDate.of(year, 10, 9),
                LocalDate.of(year, 12, 25)
            )) {
                if (fixedDate.dayOfWeek == DayOfWeek.SUNDAY || fixedDate.dayOfWeek == DayOfWeek.SATURDAY) {
                    var sub = fixedDate.plusDays(1)
                    while (sub.dayOfWeek == DayOfWeek.SATURDAY || sub.dayOfWeek == DayOfWeek.SUNDAY || set.contains(sub)) {
                        sub = sub.plusDays(1)
                    }
                    set.add(sub)
                }
            }

            // 2. 음력 명절 (설날, 부처님오신날, 추석) 및 대체공휴일
            try {
                // 설날 (음력 1월 1일)
                val seollalDay = getSolarFromLunar(year, 1, 1)
                val seollal1 = seollalDay.minusDays(1)
                val seollal2 = seollalDay
                val seollal3 = seollalDay.plusDays(1)
                set.add(seollal1)
                set.add(seollal2)
                set.add(seollal3)

                // 설날 연휴 중 일요일 포함 시 대체공휴일
                if (seollal1.dayOfWeek == DayOfWeek.SUNDAY || seollal2.dayOfWeek == DayOfWeek.SUNDAY || seollal3.dayOfWeek == DayOfWeek.SUNDAY) {
                    var sub = seollal3.plusDays(1)
                    while (sub.dayOfWeek == DayOfWeek.SATURDAY || sub.dayOfWeek == DayOfWeek.SUNDAY || set.contains(sub)) {
                        sub = sub.plusDays(1)
                    }
                    set.add(sub)
                }

                // 부처님오신날 (음력 4월 8일)
                val buddhaDay = getSolarFromLunar(year, 4, 8)
                set.add(buddhaDay)
                if (buddhaDay.dayOfWeek == DayOfWeek.SUNDAY || buddhaDay.dayOfWeek == DayOfWeek.SATURDAY) {
                    var sub = buddhaDay.plusDays(1)
                    while (sub.dayOfWeek == DayOfWeek.SATURDAY || sub.dayOfWeek == DayOfWeek.SUNDAY || set.contains(sub)) {
                        sub = sub.plusDays(1)
                    }
                    set.add(sub)
                }

                // 추석 (음력 8월 15일)
                val chuseokDay = getSolarFromLunar(year, 8, 15)
                val chuseok1 = chuseokDay.minusDays(1)
                val chuseok2 = chuseokDay
                val chuseok3 = chuseokDay.plusDays(1)
                set.add(chuseok1)
                set.add(chuseok2)
                set.add(chuseok3)

                // 추석 연휴 중 일요일 포함 시 또는 다른 공휴일 중복 시 대체공휴일
                if (chuseok1.dayOfWeek == DayOfWeek.SUNDAY || chuseok2.dayOfWeek == DayOfWeek.SUNDAY || chuseok3.dayOfWeek == DayOfWeek.SUNDAY) {
                    var sub = chuseok3.plusDays(1)
                    while (sub.dayOfWeek == DayOfWeek.SATURDAY || sub.dayOfWeek == DayOfWeek.SUNDAY || set.contains(sub)) {
                        sub = sub.plusDays(1)
                    }
                    set.add(sub)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return set
    }

    private fun getSolarFromLunar(year: Int, lunarMonth: Int, lunarDay: Int): LocalDate {
        val cc = ChineseCalendar()
        cc.set(ChineseCalendar.EXTENDED_YEAR, year + 2637)
        cc.set(ChineseCalendar.MONTH, lunarMonth - 1)
        cc.set(ChineseCalendar.DAY_OF_MONTH, lunarDay)

        val millis = cc.timeInMillis
        val instant = Instant.ofEpochMilli(millis)
        return instant.atZone(ZoneId.systemDefault()).toLocalDate()
    }
}
