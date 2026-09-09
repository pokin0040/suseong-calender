package com.example.businessdaycalc

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import android.util.Log
import androidx.core.content.ContextCompat
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

data class CalendarEventDetail(
    val title: String,
    val availability: Int,
    val isPublicHoliday: Boolean
)

class CalendarHelper(private val context: Context) {

    companion object {
        private const val TAG = "CalendarHelper"

        internal fun selectHolidaySources(sources: List<HolidaySource>): List<HolidaySource> {
            val localSources = sources.filter {
                it.accountType.equals(CalendarContract.ACCOUNT_TYPE_LOCAL, ignoreCase = true)
            }
            return localSources.ifEmpty {
                sources.filter { it.accountType.equals("com.google", ignoreCase = true) }
            }
        }

        private val LEGAL_HOLIDAY_KEYWORDS = setOf(
            "신정", "설날", "삼일절", "어린이날", "부처님", "현충일",
            "광복절", "추석", "개천절", "한글날", "성탄절", "크리스마스",
            "임시공휴일", "대체공휴일", "새해첫날", "부처님오신날"
        )

        /**
         * availability == BUSY(0) 이면 "실제로 쉬는 날"로 등록된 진짜 공휴일로 간주합니다.
         * (크리스마스이브 같은 단순 기념일은 FREE(1)로 등록되어 여기서 걸러짐)
         * availability 정보가 없거나 애매한 경우엔 법정공휴일 키워드로 폴백합니다.
         */
        fun isPublicHoliday(title: String, availability: Int): Boolean {
            if (availability == CalendarContract.Instances.AVAILABILITY_BUSY) return true
            return LEGAL_HOLIDAY_KEYWORDS.any { title == it } || title.contains("선거일") || title.contains("쉬는 날")
        }

        /**
         * "설날" / "추석" 날짜의 바로 앞/뒤가 평일(월~금)인 경우에만 공휴일로 추가합니다.
         */
        fun expandSeollalChuseokHolidays(
            holidays: Set<LocalDate>,
            seollalChuseokDates: Set<LocalDate>
        ): Set<LocalDate> {
            if (seollalChuseokDates.isEmpty()) return holidays
            val result = holidays.toMutableSet()
            for (d in seollalChuseokDates) {
                val prev = d.minusDays(1)
                if (prev.dayOfWeek != DayOfWeek.SATURDAY && prev.dayOfWeek != DayOfWeek.SUNDAY) {
                    result.add(prev)
                }
                val next = d.plusDays(1)
                if (next.dayOfWeek != DayOfWeek.SATURDAY && next.dayOfWeek != DayOfWeek.SUNDAY) {
                    result.add(next)
                }
            }
            return result
        }
    }

    internal data class HolidaySource(val calendarId: Long, val accountType: String)

    /**
     * 공휴일 캘린더 중 LOCAL을 우선 사용하고, 없으면 Google만 사용합니다.
     */
    private fun findHolidayCalendarSources(): List<HolidaySource> {
        val sources = mutableListOf<HolidaySource>()
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.OWNER_ACCOUNT
        )

        try {
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection, null, null, null
            )?.use { c ->
                val idIdx = c.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
                val typeIdx = c.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_TYPE)
                val nameIdx = c.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
                val ownerIdx = c.getColumnIndexOrThrow(CalendarContract.Calendars.OWNER_ACCOUNT)

                while (c.moveToNext()) {
                    val name = c.getString(nameIdx)?.lowercase() ?: ""
                    val owner = c.getString(ownerIdx)?.lowercase() ?: ""
                    val type = c.getString(typeIdx) ?: ""

                    val isHolidayCalendar = owner.contains("holiday@group.v.calendar.google.com")
                            || name.contains("휴일")
                            || name.contains("holiday")

                    if (isHolidayCalendar) {
                        sources.add(HolidaySource(c.getLong(idIdx), type))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "캘린더 목록 조회 실패", e)
        }

        return selectHolidaySources(sources)
    }

    /**
     * 특정 날짜의 캘린더 이벤트 상세 정보(제목, availability, 공휴일 인정 여부)를 가져옵니다.
     */
    fun getEventDetailsForDate(date: LocalDate): List<CalendarEventDetail> {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            return emptyList()
        }

        val targetSources = findHolidayCalendarSources()
        if (targetSources.isEmpty()) return emptyList()

        val calendarIds = targetSources.map { it.calendarId }
        val details = mutableListOf<CalendarEventDetail>()

        try {
            val startMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            val endMillis = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

            val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
            ContentUris.appendId(builder, startMillis)
            ContentUris.appendId(builder, endMillis)

            val projection = arrayOf(
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.ALL_DAY,
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.AVAILABILITY
            )

            val placeholders = calendarIds.joinToString(",") { "?" }
            val selection = "${CalendarContract.Instances.CALENDAR_ID} IN ($placeholders)"
            val selectionArgs = calendarIds.map { it.toString() }.toTypedArray()

            context.contentResolver.query(
                builder.build(),
                projection,
                selection,
                selectionArgs,
                null
            )?.use { c ->
                val beginIdx = c.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
                val allDayIdx = c.getColumnIndexOrThrow(CalendarContract.Instances.ALL_DAY)
                val titleIdx = c.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
                val availIdx = c.getColumnIndex(CalendarContract.Instances.AVAILABILITY)

                while (c.moveToNext()) {
                    val isAllDay = c.getInt(allDayIdx) == 1
                    val title = c.getString(titleIdx) ?: ""
                    val availability = if (availIdx >= 0) c.getInt(availIdx) else -1

                    val dtStart = c.getLong(beginIdx)
                    val eventDate = if (isAllDay) {
                        Instant.ofEpochMilli(dtStart).atZone(ZoneOffset.UTC).toLocalDate()
                    } else {
                        Instant.ofEpochMilli(dtStart).atZone(ZoneId.systemDefault()).toLocalDate()
                    }

                    if (eventDate == date) {
                        details.add(CalendarEventDetail(title, availability, isPublicHoliday(title, availability)))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "이벤트 상세 조회 실패", e)
        }

        return details
    }

    /**
     * 특정 날짜 범위 내의 법정 공휴일 목록(날짜 및 이름)을 가져옵니다.
     */
    fun getPublicHolidaysWithNames(startDate: LocalDate, endDate: LocalDate): List<CustomHoliday> {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            return emptyList()
        }

        val targetSources = findHolidayCalendarSources()
        if (targetSources.isEmpty()) return emptyList()

        val calendarIds = targetSources.map { it.calendarId }
        val rawHolidaysMap = mutableMapOf<LocalDate, String>()
        val seollalChuseokDates = mutableSetOf<LocalDate>()

        try {
            val queryStartDate = startDate.minusDays(3)
            val queryEndDate = endDate.plusDays(3)

            val startMillis = queryStartDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            val endMillis = queryEndDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

            val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
            ContentUris.appendId(builder, startMillis)
            ContentUris.appendId(builder, endMillis)

            val projection = arrayOf(
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.ALL_DAY,
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.AVAILABILITY
            )

            val placeholders = calendarIds.joinToString(",") { "?" }
            val selection = "${CalendarContract.Instances.CALENDAR_ID} IN ($placeholders)"
            val selectionArgs = calendarIds.map { it.toString() }.toTypedArray()

            context.contentResolver.query(
                builder.build(),
                projection,
                selection,
                selectionArgs,
                null
            )?.use { c ->
                val beginIdx = c.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
                val allDayIdx = c.getColumnIndexOrThrow(CalendarContract.Instances.ALL_DAY)
                val titleIdx = c.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
                val availIdx = c.getColumnIndex(CalendarContract.Instances.AVAILABILITY)

                while (c.moveToNext()) {
                    val isAllDay = c.getInt(allDayIdx) == 1
                    val title = c.getString(titleIdx) ?: ""
                    val availability = if (availIdx >= 0) c.getInt(availIdx) else -1

                    if (!isPublicHoliday(title, availability)) continue

                    val dtStart = c.getLong(beginIdx)
                    val date = if (isAllDay) {
                        Instant.ofEpochMilli(dtStart).atZone(ZoneOffset.UTC).toLocalDate()
                    } else {
                        Instant.ofEpochMilli(dtStart).atZone(ZoneId.systemDefault()).toLocalDate()
                    }

                    rawHolidaysMap[date] = title.ifEmpty { "공휴일" }
                    if (title == "설날" || title == "추석") {
                        seollalChuseokDates.add(date)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "공휴일 이름 목록 조회 실패", e)
            return emptyList()
        }

        val expandedDates = expandSeollalChuseokHolidays(rawHolidaysMap.keys, seollalChuseokDates)

        val resultList = mutableListOf<CustomHoliday>()
        for (date in expandedDates) {
            if (!date.isBefore(startDate) && !date.isAfter(endDate)) {
                val name = rawHolidaysMap[date] ?: "공휴일 연휴"
                resultList.add(CustomHoliday(date, name))
            }
        }

        return resultList.sortedBy { it.date }
    }

    /**
     * 기기에 등록된 공휴일 캘린더에서 공휴일 날짜 집합을 가져옵니다.
     */
    fun getCalendarHolidays(startDate: LocalDate, endDate: LocalDate): Set<LocalDate> {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "READ_CALENDAR 권한이 없습니다.")
            return emptySet()
        }

        val targetSources = findHolidayCalendarSources()
        if (targetSources.isEmpty()) {
            Log.w(TAG, "사용 가능한 공휴일 캘린더가 없습니다.")
            return emptySet()
        }

        val calendarIds = targetSources.map { it.calendarId }
        val holidays = mutableSetOf<LocalDate>()
        val seollalChuseokDates = mutableSetOf<LocalDate>()

        try {
            // 설날/추석 전후 평일 판별을 위해 조회 범위를 앞뒤로 3일 넓힘
            val queryStartDate = startDate.minusDays(3)
            val queryEndDate = endDate.plusDays(3)

            val startMillis = queryStartDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            val endMillis = queryEndDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

            val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
            ContentUris.appendId(builder, startMillis)
            ContentUris.appendId(builder, endMillis)

            val projection = arrayOf(
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.ALL_DAY,
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.AVAILABILITY,
                CalendarContract.Instances.CALENDAR_ID
            )

            val placeholders = calendarIds.joinToString(",") { "?" }
            val selection = "${CalendarContract.Instances.CALENDAR_ID} IN ($placeholders)"
            val selectionArgs = calendarIds.map { it.toString() }.toTypedArray()

            context.contentResolver.query(
                builder.build(),
                projection,
                selection,
                selectionArgs,
                null
            )?.use { c ->
                val beginIdx = c.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
                val allDayIdx = c.getColumnIndexOrThrow(CalendarContract.Instances.ALL_DAY)
                val titleIdx = c.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
                val availIdx = c.getColumnIndex(CalendarContract.Instances.AVAILABILITY)

                while (c.moveToNext()) {
                    val isAllDay = c.getInt(allDayIdx) == 1
                    val title = c.getString(titleIdx) ?: ""
                    val availability = if (availIdx >= 0) c.getInt(availIdx) else -1

                    if (!isPublicHoliday(title, availability)) continue

                    val dtStart = c.getLong(beginIdx)
                    // All-Day 이벤트는 DB에 UTC 기준으로 저장되므로 UTC로 변환해야 하루 밀림 방지
                    val date = if (isAllDay) {
                        Instant.ofEpochMilli(dtStart).atZone(ZoneOffset.UTC).toLocalDate()
                    } else {
                        Instant.ofEpochMilli(dtStart).atZone(ZoneId.systemDefault()).toLocalDate()
                    }

                    holidays.add(date)
                    if (title == "설날" || title == "추석") {
                        seollalChuseokDates.add(date)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "캘린더 조회 실패", e)
            return emptySet()
        }

        val expanded = expandSeollalChuseokHolidays(holidays, seollalChuseokDates)

        return expanded.filter { !it.isBefore(startDate) && !it.isAfter(endDate) }.toSet()
    }
}
