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

    /**
     * 구글 캘린더 (com.google) 우선 수집 후, 없을 경우 삼성 로컬 캘린더 (LOCAL)에서 수집합니다.
     */
    fun getCalendarHolidays(startDate: LocalDate, endDate: LocalDate): Set<LocalDate> {
        val googleHolidays = mutableSetOf<LocalDate>()
        val localHolidays = mutableSetOf<LocalDate>()

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "READ_CALENDAR 권한이 없습니다.")
            return emptySet()
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
                CalendarContract.Instances.OWNER_ACCOUNT,
                CalendarContract.Instances.AVAILABILITY,
                CalendarContract.Calendars.ACCOUNT_TYPE
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
                val availIdx = it.getColumnIndex(CalendarContract.Instances.AVAILABILITY)
                val accTypeIdx = it.getColumnIndex(CalendarContract.Calendars.ACCOUNT_TYPE)

                while (it.moveToNext()) {
                    val isAllDay = it.getInt(allDayIdx) == 1
                    val title = it.getString(titleIdx) ?: ""
                    val desc = it.getString(descIdx) ?: ""
                    val calName = it.getString(calNameIdx)?.lowercase() ?: ""
                    val owner = it.getString(ownerIdx)?.lowercase() ?: ""
                    val availability = if (availIdx >= 0) it.getInt(availIdx) else -1
                    val accType = if (accTypeIdx >= 0) it.getString(accTypeIdx) ?: "" else ""

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

                        // 단순 기념일(Observance) 여부 판별
                        val isNonHolidayObservance = isObservance(title, desc, availability)

                        if (!isNonHolidayObservance && !date.isBefore(startDate) && !date.isAfter(endDate)) {
                            if (accType.equals("com.google", ignoreCase = true) || owner.contains("google.com")) {
                                googleHolidays.add(date)
                            } else {
                                localHolidays.add(date)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "캘린더 조회 실패", e)
        }

        // 구글 캘린더 (com.google) 우선 사용 후, 없으면 삼성 로컬 캘린더 (LOCAL) 폴백
        return if (googleHolidays.isNotEmpty()) googleHolidays else localHolidays
    }



    private val LEGAL_HOLIDAY_KEYWORDS = setOf(
        "신정", "설날", "삼일절", "어린이날", "부처님", "현충일",
        "광복절", "추석", "개천절", "한글날", "성탄절", "크리스마스",
        "임시공휴일", "대체공휴일", "국회의원", "대통령", "지방선거"
    )

    private fun isLegalHoliday(title: String, availability: Int): Boolean {
        // 1차 판별: availability가 BUSY(0)면 실제 공휴일로 간주
        if (availability == CalendarContract.Instances.AVAILABILITY_BUSY) {
            return true
        }
        // availability가 없거나 애매한 경우를 대비한 폴백
        return LEGAL_HOLIDAY_KEYWORDS.any { title.contains(it) } || title.contains("선거일")
    }

    private fun isObservance(title: String, desc: String, availability: Int): Boolean {
        // 1차 판별: availability가 BUSY(0)면 실제 공휴일로 간주
        if (availability == CalendarContract.Instances.AVAILABILITY_BUSY) {
            return true
        }
        // availability가 없거나 애매한 경우를 대비한 폴백
        return LEGAL_HOLIDAY_KEYWORDS.any { title.contains(it) } || title.contains("선거일")
}
