package com.example.businessdaycalc

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class CalendarHelper(private val context: Context) {

    fun getCalendarHolidays(startDate: LocalDate, endDate: LocalDate): Set<LocalDate> {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            return emptySet()
        }

        val holidays = mutableSetOf<LocalDate>()
        try {
            // Find calendars that might contain holidays
            val calendarIds = getHolidayCalendarIds()
            if (calendarIds.isEmpty()) return emptySet()

            val startMillis = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endMillis = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

            val projection = arrayOf(
                CalendarContract.Events.DTSTART,
                CalendarContract.Events.DTEND,
                CalendarContract.Events.ALL_DAY
            )

            // Build selection for calendars
            val selection = StringBuilder("(")
            val selectionArgs = mutableListOf<String>()
            
            calendarIds.forEachIndexed { index, id ->
                if (index > 0) selection.append(" OR ")
                selection.append("${CalendarContract.Events.CALENDAR_ID} = ?")
                selectionArgs.add(id.toString())
            }
            selection.append(") AND ${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} < ?")
            selectionArgs.add(startMillis.toString())
            selectionArgs.add(endMillis.toString())

            val cursor = context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                selection.toString(),
                selectionArgs.toTypedArray(),
                null
            )

            cursor?.use {
                val startIdx = it.getColumnIndexOrThrow(CalendarContract.Events.DTSTART)
                val allDayIdx = it.getColumnIndexOrThrow(CalendarContract.Events.ALL_DAY)

                while (it.moveToNext()) {
                    // Holiday events are usually all-day
                    val isAllDay = it.getInt(allDayIdx) == 1
                    if (isAllDay) {
                        val dtStart = it.getLong(startIdx)
                        val date = Instant.ofEpochMilli(dtStart).atZone(ZoneId.of("UTC")).toLocalDate()
                        holidays.add(date)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return holidays
    }

    private fun getHolidayCalendarIds(): List<Long> {
        val ids = mutableListOf<Long>()
        val projection = arrayOf(CalendarContract.Calendars._ID, CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
        val cursor = context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            null,
            null,
            null
        )

        cursor?.use {
            val idIdx = it.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
            val nameIdx = it.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)

            while (it.moveToNext()) {
                val name = it.getString(nameIdx)?.lowercase() ?: ""
                // Add calendars that likely contain holidays
                if (name.contains("holiday") || name.contains("휴일") || name.contains("공휴일")) {
                    ids.add(it.getLong(idIdx))
                }
            }
        }
        return ids
    }
}
