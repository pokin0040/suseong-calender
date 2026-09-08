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

class CalendarHelper(private val context: Context) {

    fun getCalendarHolidays(startDate: LocalDate, endDate: LocalDate): Set<LocalDate> {
        val holidays = mutableSetOf<LocalDate>()

        // 1. Always include built-in Korean Legal Public Holidays DB as baseline
        holidays.addAll(KoreanHolidays.HOLIDAYS.filter { !it.isBefore(startDate) && !it.isAfter(endDate) })

        // 2. Query Android System Calendar using Instances API (properly expands recurring annual holidays)
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED) {
            try {
                val startMillis = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val endMillis = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

                val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
                ContentUris.appendId(builder, startMillis)
                ContentUris.appendId(builder, endMillis)

                val projection = arrayOf(
                    CalendarContract.Instances.BEGIN,
                    CalendarContract.Instances.ALL_DAY,
                    CalendarContract.Instances.TITLE,
                    CalendarContract.Instances.CALENDAR_DISPLAY_NAME
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
                    val calNameIdx = it.getColumnIndexOrThrow(CalendarContract.Instances.CALENDAR_DISPLAY_NAME)

                    while (it.moveToNext()) {
                        val title = it.getString(titleIdx)?.lowercase() ?: ""
                        val calName = it.getString(calNameIdx)?.lowercase() ?: ""
                        val isAllDay = it.getInt(allDayIdx) == 1

                        val isHolidayCalendar = calName.contains("holiday") || calName.contains("휴일") || calName.contains("공휴일") || calName.contains("korea")
                        val isHolidayTitle = title.contains("휴일") || title.contains("공휴일") || title.contains("대체") || title.contains("설날") || title.contains("추석")

                        if ((isHolidayCalendar || isHolidayTitle) && isAllDay) {
                            val dtStart = it.getLong(beginIdx)
                            val date = Instant.ofEpochMilli(dtStart).atZone(ZoneId.systemDefault()).toLocalDate()
                            if (!date.isBefore(startDate) && !date.isAfter(endDate)) {
                                holidays.add(date)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return holidays
    }
}
