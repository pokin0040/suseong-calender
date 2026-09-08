package com.example.businessdaycalc

import java.time.LocalDate

object KoreanHolidays {

    /**
     * 2024년 ~ 2027년 대한민국 법정공휴일 및 대체공휴일 목록
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
        LocalDate.of(2027, 12, 27)  // 대체공휴일
    )
}
