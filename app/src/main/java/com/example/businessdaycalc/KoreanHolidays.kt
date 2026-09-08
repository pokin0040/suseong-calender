package com.example.businessdaycalc

import java.time.LocalDate

object KoreanHolidays {

    /**
     * 2024년 ~ 2035년 대한민국 법정공휴일, 음력 공휴일(설날, 추석, 부처님오신날) 및 대체공휴일 목록
     */
    val HOLIDAYS: Set<LocalDate> = setOf(
        // --- 2024년 ---
        LocalDate.of(2024, 1, 1),   // 신정
        LocalDate.of(2024, 2, 9),   // 설날 연휴
        LocalDate.of(2024, 2, 10),  // 설날
        LocalDate.of(2024, 2, 11),  // 설날 연휴
        LocalDate.of(2024, 2, 12),  // 대체공휴일
        LocalDate.of(2024, 3, 1),   // 삼일절
        LocalDate.of(2024, 4, 10),  // 국회의원 선거일
        LocalDate.of(2024, 5, 5),   // 어린이날
        LocalDate.of(2024, 5, 6),   // 대체공휴일
        LocalDate.of(2024, 5, 15),  // 부처님오신날
        LocalDate.of(2024, 6, 6),   // 현충일
        LocalDate.of(2024, 8, 15),  // 광복절
        LocalDate.of(2024, 9, 16),  // 추석 연휴
        LocalDate.of(2024, 9, 17),  // 추석
        LocalDate.of(2024, 9, 18),  // 추석 연휴
        LocalDate.of(2024, 10, 3),  // 개천절
        LocalDate.of(2024, 10, 9),  // 한글날
        LocalDate.of(2024, 12, 25), // 성탄절

        // --- 2025년 ---
        LocalDate.of(2025, 1, 1),   // 신정
        LocalDate.of(2025, 1, 28),  // 설날 연휴
        LocalDate.of(2025, 1, 29),  // 설날
        LocalDate.of(2025, 1, 30),  // 설날 연휴
        LocalDate.of(2025, 3, 1),   // 삼일절
        LocalDate.of(2025, 3, 3),   // 대체공휴일
        LocalDate.of(2025, 5, 5),   // 어린이날 / 부처님오신날
        LocalDate.of(2025, 5, 6),   // 대체공휴일
        LocalDate.of(2025, 6, 6),   // 현충일
        LocalDate.of(2025, 8, 15),  // 광복절
        LocalDate.of(2025, 10, 3),  // 개천절
        LocalDate.of(2025, 10, 5),  // 추석 연휴
        LocalDate.of(2025, 10, 6),  // 추석
        LocalDate.of(2025, 10, 7),  // 추석 연휴
        LocalDate.of(2025, 10, 8),  // 대체공휴일
        LocalDate.of(2025, 10, 9),  // 한글날
        LocalDate.of(2025, 12, 25), // 성탄절

        // --- 2026년 ---
        LocalDate.of(2026, 1, 1),   // 신정
        LocalDate.of(2026, 2, 16),  // 설날 연휴
        LocalDate.of(2026, 2, 17),  // 설날
        LocalDate.of(2026, 2, 18),  // 설날 연휴
        LocalDate.of(2026, 3, 1),   // 삼일절
        LocalDate.of(2026, 3, 2),   // 대체공휴일
        LocalDate.of(2026, 5, 5),   // 어린이날
        LocalDate.of(2026, 5, 24),  // 부처님오신날
        LocalDate.of(2026, 5, 25),  // 대체공휴일
        LocalDate.of(2026, 6, 6),   // 현충일
        LocalDate.of(2026, 8, 15),  // 광복절
        LocalDate.of(2026, 8, 17),  // 대체공휴일
        LocalDate.of(2026, 9, 24),  // 추석 연휴
        LocalDate.of(2026, 9, 25),  // 추석
        LocalDate.of(2026, 9, 26),  // 추석 연휴
        LocalDate.of(2026, 10, 3),  // 개천절
        LocalDate.of(2026, 10, 5),  // 대체공휴일
        LocalDate.of(2026, 10, 9),  // 한글날
        LocalDate.of(2026, 12, 25), // 성탄절

        // --- 2027년 ---
        LocalDate.of(2027, 1, 1),   // 신정
        LocalDate.of(2027, 2, 6),   // 설날 연휴
        LocalDate.of(2027, 2, 7),   // 설날
        LocalDate.of(2027, 2, 8),   // 설날 연휴
        LocalDate.of(2027, 2, 9),   // 대체공휴일
        LocalDate.of(2027, 3, 1),   // 삼일절
        LocalDate.of(2027, 3, 3),   // 대통령 선거일
        LocalDate.of(2027, 5, 5),   // 어린이날
        LocalDate.of(2027, 5, 13),  // 부처님오신날
        LocalDate.of(2027, 6, 6),   // 현충일
        LocalDate.of(2027, 6, 7),   // 대체공휴일
        LocalDate.of(2027, 8, 15),  // 광복절
        LocalDate.of(2027, 8, 16),  // 대체공휴일
        LocalDate.of(2027, 9, 14),  // 추석 연휴
        LocalDate.of(2027, 9, 15),  // 추석
        LocalDate.of(2027, 9, 16),  // 추석 연휴
        LocalDate.of(2027, 10, 3),  // 개천절
        LocalDate.of(2027, 10, 4),  // 대체공휴일
        LocalDate.of(2027, 10, 9),  // 한글날
        LocalDate.of(2027, 10, 11), // 대체공휴일
        LocalDate.of(2027, 12, 25), // 성탄절
        LocalDate.of(2027, 12, 27), // 대체공휴일

        // --- 2028년 ---
        LocalDate.of(2028, 1, 1),   // 신정
        LocalDate.of(2028, 1, 26),  // 설날 연휴
        LocalDate.of(2028, 1, 27),  // 설날
        LocalDate.of(2028, 1, 28),  // 설날 연휴
        LocalDate.of(2028, 3, 1),   // 삼일절
        LocalDate.of(2028, 5, 2),   // 부처님오신날
        LocalDate.of(2028, 5, 5),   // 어린이날
        LocalDate.of(2028, 6, 6),   // 현충일
        LocalDate.of(2028, 8, 15),  // 광복절
        LocalDate.of(2028, 10, 2),  // 추석 연휴
        LocalDate.of(2028, 10, 3),  // 추석 / 개천절
        LocalDate.of(2028, 10, 4),  // 추석 연휴
        LocalDate.of(2028, 10, 5),  // 대체공휴일
        LocalDate.of(2028, 10, 9),  // 한글날
        LocalDate.of(2028, 12, 25), // 성탄절

        // --- 2029년 ---
        LocalDate.of(2029, 1, 1),   // 신정
        LocalDate.of(2029, 2, 12),  // 설날 연휴
        LocalDate.of(2029, 2, 13),  // 설날
        LocalDate.of(2029, 2, 14),  // 설날 연휴
        LocalDate.of(2029, 3, 1),   // 삼일절
        LocalDate.of(2029, 5, 5),   // 어린이날
        LocalDate.of(2029, 5, 7),   // 대체공휴일
        LocalDate.of(2029, 5, 20),  // 부처님오신날
        LocalDate.of(2029, 5, 21),  // 대체공휴일
        LocalDate.of(2029, 6, 6),   // 현충일
        LocalDate.of(2029, 8, 15),  // 광복절
        LocalDate.of(2029, 9, 21),  // 추석 연휴
        LocalDate.of(2029, 9, 22),  // 추석
        LocalDate.of(2029, 9, 23),  // 추석 연휴
        LocalDate.of(2029, 9, 24),  // 대체공휴일
        LocalDate.of(2029, 10, 3),  // 개천절
        LocalDate.of(2029, 10, 9),  // 한글날
        LocalDate.of(2029, 12, 25), // 성탄절

        // --- 2030년 ---
        LocalDate.of(2030, 1, 1),   // 신정
        LocalDate.of(2030, 2, 2),   // 설날 연휴
        LocalDate.of(2030, 2, 3),   // 설날
        LocalDate.of(2030, 2, 4),   // 설날 연휴
        LocalDate.of(2030, 2, 5),   // 대체공휴일
        LocalDate.of(2030, 3, 1),   // 삼일절
        LocalDate.of(2030, 5, 5),   // 어린이날
        LocalDate.of(2030, 5, 6),   // 대체공휴일
        LocalDate.of(2030, 5, 9),   // 부처님오신날
        LocalDate.of(2030, 6, 6),   // 현충일
        LocalDate.of(2030, 8, 15),  // 광복절
        LocalDate.of(2030, 9, 11),  // 추석 연휴
        LocalDate.of(2030, 9, 12),  // 추석
        LocalDate.of(2030, 9, 13),  // 추석 연휴
        LocalDate.of(2030, 10, 3),  // 개천절
        LocalDate.of(2030, 10, 9),  // 한글날
        LocalDate.of(2030, 12, 25), // 성탄절

        // --- 2031년 ---
        LocalDate.of(2031, 1, 1),   // 신정
        LocalDate.of(2031, 1, 22),  // 설날 연휴
        LocalDate.of(2031, 1, 23),  // 설날
        LocalDate.of(2031, 1, 24),  // 설날 연휴
        LocalDate.of(2031, 3, 1),   // 삼일절
        LocalDate.of(2031, 3, 3),   // 대체공휴일
        LocalDate.of(2031, 5, 5),   // 어린이날
        LocalDate.of(2031, 5, 28),  // 부처님오신날
        LocalDate.of(2031, 6, 6),   // 현충일
        LocalDate.of(2031, 8, 15),  // 광복절
        LocalDate.of(2031, 9, 30),  // 추석 연휴
        LocalDate.of(2031, 10, 1),  // 추석
        LocalDate.of(2031, 10, 2),  // 추석 연휴
        LocalDate.of(2031, 10, 3),  // 개천절
        LocalDate.of(2031, 10, 9),  // 한글날
        LocalDate.of(2031, 12, 25), // 성탄절

        // --- 2032년 ---
        LocalDate.of(2032, 1, 1),   // 신정
        LocalDate.of(2032, 2, 10),  // 설날 연휴
        LocalDate.of(2032, 2, 11),  // 설날
        LocalDate.of(2032, 2, 12),  // 설날 연휴
        LocalDate.of(2032, 3, 1),   // 삼일절
        LocalDate.of(2032, 5, 5),   // 어린이날
        LocalDate.of(2032, 5, 16),  // 부처님오신날
        LocalDate.of(2032, 5, 17),  // 대체공휴일
        LocalDate.of(2032, 6, 6),   // 현충일
        LocalDate.of(2032, 6, 7),   // 대체공휴일
        LocalDate.of(2032, 8, 15),  // 광복절
        LocalDate.of(2032, 8, 16),  // 대체공휴일
        LocalDate.of(2032, 9, 18),  // 추석 연휴
        LocalDate.of(2032, 9, 19),  // 추석
        LocalDate.of(2032, 9, 20),  // 추석 연휴
        LocalDate.of(2032, 9, 21),  // 대체공휴일
        LocalDate.of(2032, 10, 3),  // 개천절
        LocalDate.of(2032, 10, 4),  // 대체공휴일
        LocalDate.of(2032, 10, 9),  // 한글날
        LocalDate.of(2032, 10, 11), // 대체공휴일
        LocalDate.of(2032, 12, 25), // 성탄절
        LocalDate.of(2032, 12, 27), // 대체공휴일

        // --- 2033년 ---
        LocalDate.of(2033, 1, 1),   // 신정
        LocalDate.of(2033, 1, 31),  // 설날 연휴
        LocalDate.of(2033, 2, 1),   // 설날
        LocalDate.of(2033, 2, 2),   // 설날 연휴
        LocalDate.of(2033, 3, 1),   // 삼일절
        LocalDate.of(2033, 5, 5),   // 어린이날
        LocalDate.of(2033, 5, 6),   // 부처님오신날
        LocalDate.of(2033, 6, 6),   // 현충일
        LocalDate.of(2033, 8, 15),  // 광복절
        LocalDate.of(2033, 10, 3),  // 개천절
        LocalDate.of(2033, 10, 6),  // 추석 연휴
        LocalDate.of(2033, 10, 7),  // 추석
        LocalDate.of(2033, 10, 8),  // 추석 연휴
        LocalDate.of(2033, 10, 9),  // 한글날
        LocalDate.of(2033, 10, 10), // 대체공휴일
        LocalDate.of(2033, 12, 25), // 성탄절
        LocalDate.of(2033, 12, 26), // 대체공휴일

        // --- 2034년 ---
        LocalDate.of(2034, 1, 1),   // 신정
        LocalDate.of(2034, 2, 18),  // 설날 연휴
        LocalDate.of(2034, 2, 19),  // 설날
        LocalDate.of(2034, 2, 20),  // 설날 연휴
        LocalDate.of(2034, 2, 21),  // 대체공휴일
        LocalDate.of(2034, 3, 1),   // 삼일절
        LocalDate.of(2034, 5, 5),   // 어린이날
        LocalDate.of(2034, 5, 25),  // 부처님오신날
        LocalDate.of(2034, 6, 6),   // 현충일
        LocalDate.of(2034, 8, 15),  // 광복절
        LocalDate.of(2034, 9, 26),  // 추석 연휴
        LocalDate.of(2034, 9, 27),  // 추석
        LocalDate.of(2034, 9, 28),  // 추석 연휴
        LocalDate.of(2034, 10, 3),  // 개천절
        LocalDate.of(2034, 10, 9),  // 한글날
        LocalDate.of(2034, 12, 25), // 성탄절

        // --- 2035년 ---
        LocalDate.of(2035, 1, 1),   // 신정
        LocalDate.of(2035, 2, 7),   // 설날 연휴
        LocalDate.of(2035, 2, 8),   // 설날
        LocalDate.of(2035, 2, 9),   // 설날 연휴
        LocalDate.of(2035, 3, 1),   // 삼일절
        LocalDate.of(2035, 5, 5),   // 어린이날
        LocalDate.of(2035, 5, 7),   // 대체공휴일
        LocalDate.of(2035, 5, 15),  // 부처님오신날
        LocalDate.of(2035, 6, 6),   // 현충일
        LocalDate.of(2035, 8, 15),  // 광복절
        LocalDate.of(2035, 9, 15),  // 추석 연휴
        LocalDate.of(2035, 9, 16),  // 추석
        LocalDate.of(2035, 9, 17),  // 추석 연휴
        LocalDate.of(2035, 9, 18),  // 대체공휴일
        LocalDate.of(2035, 10, 3),  // 개천절
        LocalDate.of(2035, 10, 9),  // 한글날
        LocalDate.of(2035, 12, 25)  // 성탄절
    )
}
