package com.example.businessdaycalc

import android.icu.util.ChineseCalendar
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object KoreanHolidays {

    /**
     * 100년치(2024년 ~ 2125년) 대한민국 완벽한 법정공휴일, 음력 명절(설날, 추석, 부처님오신날) 및 대체공휴일 DB
     */
    val HOLIDAYS: Set<LocalDate> by lazy {
        build100YearsHolidays()
    }

    private fun build100YearsHolidays(): Set<LocalDate> {
        val set = mutableSetOf<LocalDate>()

        for (year in 2024..2125) {
            // 1. 고정 양력 법정공휴일
            val fixedSolar = listOf(
                LocalDate.of(year, 1, 1),   // 신정
                LocalDate.of(year, 3, 1),   // 삼일절
                LocalDate.of(year, 5, 5),   // 어린이날
                LocalDate.of(year, 6, 6),   // 현충일
                LocalDate.of(year, 8, 15),  // 광복절
                LocalDate.of(year, 10, 3),  // 개천절
                LocalDate.of(year, 10, 9),  // 한글날
                LocalDate.of(year, 12, 25)  // 성탄절
            )
            set.addAll(fixedSolar)

            // 양력 공휴일 대체공휴일 (어린이날, 광복절, 개천절, 한글날, 성탄절)
            for (date in listOf(
                LocalDate.of(year, 5, 5),
                LocalDate.of(year, 8, 15),
                LocalDate.of(year, 10, 3),
                LocalDate.of(year, 10, 9),
                LocalDate.of(year, 12, 25)
            )) {
                if (date.dayOfWeek == DayOfWeek.SUNDAY || date.dayOfWeek == DayOfWeek.SATURDAY) {
                    var sub = date.plusDays(1)
                    while (sub.dayOfWeek == DayOfWeek.SATURDAY || sub.dayOfWeek == DayOfWeek.SUNDAY || set.contains(sub)) {
                        sub = sub.plusDays(1)
                    }
                    set.add(sub)
                }
            }

            // 2. 음력 명절 (설날, 부처님오신날, 추석) 및 대체공휴일
            try {
                // 설날 (음력 1.1)
                val seollal = getExactLunarToSolar(year, 1, 1)
                val s1 = seollal.minusDays(1)
                val s2 = seollal
                val s3 = seollal.plusDays(1)
                set.add(s1)
                set.add(s2)
                set.add(s3)

                if (s1.dayOfWeek == DayOfWeek.SUNDAY || s2.dayOfWeek == DayOfWeek.SUNDAY || s3.dayOfWeek == DayOfWeek.SUNDAY) {
                    var sub = s3.plusDays(1)
                    while (sub.dayOfWeek == DayOfWeek.SATURDAY || sub.dayOfWeek == DayOfWeek.SUNDAY || set.contains(sub)) {
                        sub = sub.plusDays(1)
                    }
                    set.add(sub)
                }

                // 부처님오신날 (음력 4.8)
                val buddha = getExactLunarToSolar(year, 4, 8)
                set.add(buddha)
                if (buddha.dayOfWeek == DayOfWeek.SUNDAY || buddha.dayOfWeek == DayOfWeek.SATURDAY) {
                    var sub = buddha.plusDays(1)
                    while (sub.dayOfWeek == DayOfWeek.SATURDAY || sub.dayOfWeek == DayOfWeek.SUNDAY || set.contains(sub)) {
                        sub = sub.plusDays(1)
                    }
                    set.add(sub)
                }

                // 추석 (음력 8.15)
                val chuseok = getExactLunarToSolar(year, 8, 15)
                val c1 = chuseok.minusDays(1)
                val c2 = chuseok
                val c3 = chuseok.plusDays(1)
                set.add(c1)
                set.add(c2)
                set.add(c3)

                if (c1.dayOfWeek == DayOfWeek.SUNDAY || c2.dayOfWeek == DayOfWeek.SUNDAY || c3.dayOfWeek == DayOfWeek.SUNDAY) {
                    var sub = c3.plusDays(1)
                    while (sub.dayOfWeek == DayOfWeek.SATURDAY || sub.dayOfWeek == DayOfWeek.SUNDAY || set.contains(sub)) {
                        sub = sub.plusDays(1)
                    }
                    set.add(sub)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 3. 확정된 주요 선거일 추가
        set.add(LocalDate.of(2027, 3, 3))  // 2027년 대선
        set.add(LocalDate.of(2028, 4, 12)) // 2028년 총선
        set.add(LocalDate.of(2032, 3, 3))  // 2032년 대선

        return set
    }

    private fun getExactLunarToSolar(year: Int, lunarMonth: Int, lunarDay: Int): LocalDate {
        val cc = ChineseCalendar()
        cc.set(ChineseCalendar.EXTENDED_YEAR, year + 2637)
        cc.set(ChineseCalendar.MONTH, lunarMonth - 1)
        cc.set(ChineseCalendar.DAY_OF_MONTH, lunarDay)

        val millis = cc.timeInMillis
        val instant = Instant.ofEpochMilli(millis)
        return instant.atZone(ZoneId.systemDefault()).toLocalDate()
    }
}
