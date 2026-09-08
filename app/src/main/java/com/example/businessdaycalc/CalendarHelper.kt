package com.example.businessdaycalc

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import android.util.Log
import androidx.core.content.ContextCompat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

class CalendarHelper(private val context: Context) {

    companion object {
        private const val TAG = "CalendarHelper"
    }

    fun getCalendarHolidays(startDate: LocalDate, endDate: LocalDate): Set<LocalDate> {
        val holidays = mutableSetOf<LocalDate>()

        // 1. Always include built-in 100-year Korean Legal Public Holidays DB as baseline
        holidays.addAll(KoreanHolidays.HOLIDAYS.filter { !it.isBefore(startDate) && !it.isAfter(endDate) })

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "READ_CALENDAR 권한이 없습니다.")
            return holidays
        }

        try {
            val startMillis = startDate.minusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            val endMillis = endDate.plusDays(2).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

            val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
            ContentUris.appendId(builder, startMillis)
            ContentUris.appendId(builder, endMillis)

            // ACCOUNT_NAME 제거: Instances URI에서 항상 보장되는 컬럼이 아니라
            // 기기/OS 버전에 따라 쿼리 자체가 예외를 던져 조용히 실패할 수 있었음
            val projection = arrayOf(
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.ALL_DAY,
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.CALENDAR_DISPLAY_NAME,
                CalendarContract.Instances.OWNER_ACCOUNT
            )

            val cursor = context.contentResolver.query(
                builder.build(),
                projection,
                null,
                null,
                null
            )

            cursor?.use {
                val beginIdx = it.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
                val allDayIdx = it.getColumnIndexOrThrow(CalendarContract.Instances.ALL_DAY)
                val calNameIdx = it.getColumnIndexOrThrow(CalendarContract.Instances.CALENDAR_DISPLAY_NAME)
                val ownerIdx = it.getColumnIndexOrThrow(CalendarContract.Instances.OWNER_ACCOUNT)

                Log.d(TAG, "총 ${it.count}개의 인스턴스 조회됨")

                while (it.moveToNext()) {
                    val isAllDay = it.getInt(allDayIdx) == 1
                    val calName = it.getString(calNameIdx)?.lowercase() ?: ""
                    val owner = it.getString(ownerIdx)?.lowercase() ?: ""

                    // 디버그용: 실제로 어떤 캘린더 데이터가 들어오는지 확인하고 싶으면 주석 해제
                    // Log.d(TAG, "calName=$calName, owner=$owner")

                    // 구글 공휴일 계정 ID 또는 캘린더 표시명 매칭
                    // "대한민국의 휴일"처럼 "공휴일"이 아닌 "휴일"만 포함된 이름도 잡히도록 넓게 매칭
                    val isHolidayCalendar = owner.contains("holiday@group.v.calendar.google.com")
                            || calName.contains("휴일")
                            || calName.contains("holiday")

                    if (isHolidayCalendar) {
                        val dtStart = it.getLong(beginIdx)
                        // All-Day 이벤트는 DB에 UTC 기준으로 저장되므로 UTC로 날짜를 변환해야 하루 밀림 방지
                        val date = if (isAllDay) {
                            Instant.ofEpochMilli(dtStart).atZone(ZoneOffset.UTC).toLocalDate()
                        } else {
                            Instant.ofEpochMilli(dtStart).atZone(ZoneId.systemDefault()).toLocalDate()
                        }

                        if (!date.isBefore(startDate) && !date.isAfter(endDate)) {
                            holidays.add(date)
                        }
                    }
                }
            } ?: Log.w(TAG, "쿼리 결과 cursor가 null입니다.")

        } catch (e: Exception) {
            Log.e(TAG, "캘린더 조회 실패", e)
        }

        Log.d(TAG, "최종 공휴일 개수: ${holidays.size}")
        return holidays
    }
}