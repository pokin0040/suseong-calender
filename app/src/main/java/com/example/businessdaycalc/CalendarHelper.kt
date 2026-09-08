package com.example.businessdaycalc

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

class CalendarHelper(private val context: Context) {

    fun getCalendarHolidays(startDate: LocalDate, endDate: LocalDate): Set<LocalDate> {
        val holidays = mutableSetOf<LocalDate>()

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
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
                CalendarContract.Instances.CALENDAR_DISPLAY_NAME,
                CalendarContract.Calendars.ACCOUNT_NAME,
                CalendarContract.Calendars.OWNER_ACCOUNT
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
                val accNameIdx = it.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_NAME)
                val ownerIdx = it.getColumnIndexOrThrow(CalendarContract.Calendars.OWNER_ACCOUNT)

                while (it.moveToNext()) {
                    val isAllDay = it.getInt(allDayIdx) == 1
                    val calName = it.getString(calNameIdx)?.lowercase() ?: ""
                    val accName = it.getString(accNameIdx)?.lowercase() ?: ""
                    val owner = it.getString(ownerIdx)?.lowercase() ?: ""

                    // 구글 공휴일 계정 ID 또는 캘린더 표시명 매칭
                    val isHolidayCalendar = owner.contains("holiday@group.v.calendar.google.com")
                            || accName.contains("holiday")
                            || calName.contains("대한민국 공휴일")
                            || calName.contains("대한민국의 공휴일")
                            || calName.contains("holidays in south korea")
                            || calName.contains("korean holidays")

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
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return holidays
    }
}