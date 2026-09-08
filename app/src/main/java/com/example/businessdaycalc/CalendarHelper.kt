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

class CalendarHelper(private val context: Context) {

    companion object {
        private const val TAG = "CalendarHelper"

        private val LEGAL_HOLIDAY_KEYWORDS = setOf(
            "신정", "설날", "삼일절", "어린이날", "부처님", "현충일",
            "광복절", "추석", "개천절", "한글날", "성탄절", "크리스마스",
            "임시공휴일", "대체공휴일"
        )

        /**
         * availability == BUSY(0) 이면 "실제로 쉬는 날"로 등록된 진짜 공휴일로 간주합니다.
         * (크리스마스이브 같은 단순 기념일은 FREE(1)로 등록되어 여기서 걸러짐)
         * availability 정보가 없거나 애매한 경우엔 법정공휴일 키워드로 폴백합니다.
         */
        private fun isPublicHoliday(title: String, availability: Int): Boolean {
            if (availability == CalendarContract.Instances.AVAILABILITY_BUSY) return true
            return LEGAL_HOLIDAY_KEYWORDS.any { title.contains(it) } || title.contains("선거일")
        }

        /**
         * "설날" / "추석" 날짜의 바로 앞/뒤가 평일(월~금)인 경우에만 공휴일로 추가합니다.
         */
        private fun expandSeollalChuseokHolidays(
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

    private data class HolidaySource(val calendarId: Long, val accountType: String)

    /**
     * 기기에 등록된 캘린더 중 "공휴일" 캘린더만 골라냅니다. (구글 / 삼성 로컬 구분용)
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

        return sources
    }

    /**
     * 구글 계정(com.google)의 공휴일 캘린더를 우선 사용하고,
     * 구글 공휴일 캘린더가 기기에 아예 없는 경우에만 삼성 로컬(LOCAL) 공휴일 캘린더를 사용합니다.
     */
    fun getCalendarHolidays(startDate: LocalDate, endDate: LocalDate): Set<LocalDate> {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "READ_CALENDAR 권한이 없습니다.")
            return emptySet()
        }

        val allSources = findHolidayCalendarSources()
        val googleSources = allSources.filter { it.accountType.equals("com.google", ignoreCase = true) }

        val targetSources = if (googleSources.isNotEmpty()) {
            Log.d(TAG, "구글 공휴일 캘린더 사용 (${googleSources.size}개)")
            googleSources
        } else {
            val localSources = allSources.filter { it.accountType.equals("LOCAL", ignoreCase = true) }
            Log.d(TAG, "구글 공휴일 캘린더 없음 → 삼성 로컬 공휴일 캘린더 사용 (${localSources.size}개)")
            localSources
        }

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

            // 앞서 선택한 캘린더(구글 or 삼성) ID에 속한 이벤트만 조회
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