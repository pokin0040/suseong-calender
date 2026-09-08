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

data class CalendarEventDetail(
    val title: String,
    val description: String,
    val calName: String,
    val isPublicHoliday: Boolean
)

class CalendarHelper(private val context: Context) {

    companion object {
        private const val TAG = "CalendarHelper"
    }

    /**
     * 안드로이드/구글 캘린더에서 법정공휴일 날짜만 선별하여 수집합니다.
     * (크리스마스 이브, 어버이날, 스승의날 등 단순 기념일/Observance는 공휴일에서 제외)
     */
    fun getCalendarHolidays(startDate: LocalDate, endDate: LocalDate): Set<LocalDate> {
        val holidays = mutableSetOf<LocalDate>()

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

            val projection = arrayOf(
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.ALL_DAY,
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.DESCRIPTION,
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
                val titleIdx = it.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
                val descIdx = it.getColumnIndexOrThrow(CalendarContract.Instances.DESCRIPTION)
                val calNameIdx = it.getColumnIndexOrThrow(CalendarContract.Instances.CALENDAR_DISPLAY_NAME)
                val ownerIdx = it.getColumnIndexOrThrow(CalendarContract.Instances.OWNER_ACCOUNT)

                while (it.moveToNext()) {
                    val isAllDay = it.getInt(allDayIdx) == 1
                    val title = it.getString(titleIdx)?.lowercase() ?: ""
                    val desc = it.getString(descIdx)?.lowercase() ?: ""
                    val calName = it.getString(calNameIdx)?.lowercase() ?: ""
                    val owner = it.getString(ownerIdx)?.lowercase() ?: ""

                    val isHolidayCalendar = owner.contains("holiday@group.v.calendar.google.com")
                            || calName.contains("휴일")
                            || calName.contains("holiday")

                    if (isHolidayCalendar) {
                        val dtStart = it.getLong(beginIdx)
                        val date = if (isAllDay) {
                            Instant.ofEpochMilli(dtStart).atZone(ZoneOffset.UTC).toLocalDate()
                        } else {
                            Instant.ofEpochMilli(dtStart).atZone(ZoneId.systemDefault()).toLocalDate()
                        }

                        // 크리스마스 이브, 어버이날, 스승의 날 등 단순 기념일(Observance) 제외 검증
                        val isNonHolidayObservance = isObservance(title, desc)

                        if (!isNonHolidayObservance && !date.isBefore(startDate) && !date.isAfter(endDate)) {
                            holidays.add(date)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "캘린더 조회 실패", e)
        }

        return holidays
    }

    /**
     * 특정 날짜의 캘린더 이벤트 상세 정보 목록을 디버깅용으로 추출합니다.
     */
    fun getEventDetailsForDate(date: LocalDate): List<CalendarEventDetail> {
        val details = mutableListOf<CalendarEventDetail>()

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            return details
        }

        try {
            val startMillis = date.minusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            val endMillis = date.plusDays(2).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

            val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
            ContentUris.appendId(builder, startMillis)
            ContentUris.appendId(builder, endMillis)

            val projection = arrayOf(
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.ALL_DAY,
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.DESCRIPTION,
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
                val titleIdx = it.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
                val descIdx = it.getColumnIndexOrThrow(CalendarContract.Instances.DESCRIPTION)
                val calNameIdx = it.getColumnIndexOrThrow(CalendarContract.Instances.CALENDAR_DISPLAY_NAME)
                val ownerIdx = it.getColumnIndexOrThrow(CalendarContract.Instances.OWNER_ACCOUNT)

                while (it.moveToNext()) {
                    val isAllDay = it.getInt(allDayIdx) == 1
                    val title = it.getString(titleIdx) ?: "제목 없음"
                    val desc = it.getString(descIdx) ?: "설명 없음"
                    val calName = it.getString(calNameIdx) ?: "캘린더"
                    val owner = it.getString(ownerIdx)?.lowercase() ?: ""

                    val dtStart = it.getLong(beginIdx)
                    val eventDate = if (isAllDay) {
                        Instant.ofEpochMilli(dtStart).atZone(ZoneOffset.UTC).toLocalDate()
                    } else {
                        Instant.ofEpochMilli(dtStart).atZone(ZoneId.systemDefault()).toLocalDate()
                    }

                    if (eventDate == date) {
                        val isHolidayCal = owner.contains("holiday@group.v.calendar.google.com") || calName.lowercase().contains("휴일") || calName.lowercase().contains("holiday")
                        val isObserv = isObservance(title.lowercase(), desc.lowercase())
                        val isPublicHoliday = isHolidayCal && !isObserv

                        details.add(CalendarEventDetail(title, desc, calName, isPublicHoliday))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return details
    }

    private fun isObservance(title: String, desc: String): Boolean {
        // 크리스마스 이브, 어버이날, 스승의 날, 제헌절 등 쉬지 않는 단순 기념일 판별
        if (title.contains("이브") || title.contains("eve")) return true
        if (title.contains("어버이") || title.contains("스승") || title.contains("제헌절") || title.contains("국군의 날")) return true
        if (title.contains("발렌타인") || title.contains("화이트데이") || title.contains("만우절")) return true
        if (desc.contains("observance") || desc.contains("단순 기념일") || desc.contains("쉬지 않는")) return true
        return false
    }
}
