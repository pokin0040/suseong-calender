package com.example.businessdaycalc

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import android.util.Log
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

class CalendarHelper(private val context: Context) {

    companion object {
        private const val TAG = "CalendarHelper"
    }

    /**
     * 안드로이드/구글 캘린더에서 법정공휴일 날짜만 선별하여 수집합니다.
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
     * 특정 날짜의 캘린더 이벤트 전체 DB 컬럼 속성을 Pretty JSON 포맷으로 추출합니다.
     */
    fun getEventFullRawJsonForDate(date: LocalDate): String {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            return "{\"error\": \"READ_CALENDAR 권한이 필요합니다.\"}"
        }

        val jsonArray = JSONArray()

        try {
            val startMillis = date.minusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            val endMillis = date.plusDays(2).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

            val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
            ContentUris.appendId(builder, startMillis)
            ContentUris.appendId(builder, endMillis)

            val cursor = context.contentResolver.query(
                builder.build(),
                null, // 모든 인스턴스/이벤트 컬럼을 전체 조회
                null,
                null,
                null
            )

            cursor?.use {
                val columnNames = it.columnNames

                while (it.moveToNext()) {
                    val allDayIdx = it.getColumnIndex(CalendarContract.Instances.ALL_DAY)
                    val beginIdx = it.getColumnIndex(CalendarContract.Instances.BEGIN)

                    val isAllDay = if (allDayIdx >= 0) it.getInt(allDayIdx) == 1 else false
                    val dtStart = if (beginIdx >= 0) it.getLong(beginIdx) else 0L

                    val eventDate = if (isAllDay) {
                        Instant.ofEpochMilli(dtStart).atZone(ZoneOffset.UTC).toLocalDate()
                    } else {
                        Instant.ofEpochMilli(dtStart).atZone(ZoneId.systemDefault()).toLocalDate()
                    }

                    if (eventDate == date) {
                        val eventObj = JSONObject()
                        for (colName in columnNames) {
                            val colIdx = it.getColumnIndex(colName)
                            when (it.getType(colIdx)) {
                                android.database.Cursor.FIELD_TYPE_NULL -> eventObj.put(colName, JSONObject.NULL)
                                android.database.Cursor.FIELD_TYPE_INTEGER -> eventObj.put(colName, it.getLong(colIdx))
                                android.database.Cursor.FIELD_TYPE_FLOAT -> eventObj.put(colName, it.getDouble(colIdx))
                                android.database.Cursor.FIELD_TYPE_STRING -> eventObj.put(colName, it.getString(colIdx))
                                android.database.Cursor.FIELD_TYPE_BLOB -> eventObj.put(colName, "[BLOB Data]")
                            }
                        }
                        jsonArray.put(eventObj)
                    }
                }
            }
        } catch (e: Exception) {
            val errObj = JSONObject()
            errObj.put("error", e.message)
            return errObj.toString(2)
        }

        return if (jsonArray.length() == 0) {
            "[] (선택한 날짜에 기기 DB 이벤트가 없습니다.)"
        } else {
            jsonArray.toString(2) // Pretty JSON (indent = 2)
        }
    }

    private val LEGAL_HOLIDAY_KEYWORDS = setOf(
        "신정", "설날", "삼일절", "어린이날", "부처님", "현충일",
        "광복절", "추석", "개천절", "한글날", "성탄절", "크리스마스",
        "임시공휴일", "대체공휴일", "국회의원", "대통령", "지방선거", "선거"
    )

    private fun isLegalHoliday(title: String): Boolean {
        return LEGAL_HOLIDAY_KEYWORDS.any { title.contains(it) }
    }

    private fun isObservance(title: String, desc: String): Boolean {
        // 크리스마스 이브는 "크리스마스" 키워드가 걸리기 전에 "이브"를 먼저 체크하여 예외(true) 처리
        if (title.contains("이브") || title.contains("eve")) {
            return true
        }

        // 법정공휴일 키워드에 포함되면 단순 기념일이 아님 (false 리턴 -> 공휴일 인정)
        if (isLegalHoliday(title)) {
            return false
        }

        if (title.contains("어버이") || title.contains("스승") || title.contains("제헌절") || title.contains("국군의 날")) return true
        if (title.contains("발렌타인") || title.contains("화이트데이") || title.contains("만우절")) return true

        return desc.contains("observance", ignoreCase = true)
                || desc.contains("단순 기념일")
                || desc.contains("쉬지 않는")
    }
}
