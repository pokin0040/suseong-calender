package com.example.businessdaycalc

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.Calendar

class MidnightUpdateReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "MidnightUpdateReceiver"
        private const val ALARM_REQUEST_CODE = 1001

        /**
         * 매일 자정(00:00:00)에 위젯을 갱신하도록 AlarmManager에 알람을 예약합니다.
         */
        fun scheduleMidnightAlarm(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

            val intent = Intent(context, MidnightUpdateReceiver::class.java).apply {
                action = "com.example.businessdaycalc.MIDNIGHT_UPDATE"
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                ALARM_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val now = Calendar.getInstance()
            val midnight = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (before(now) || timeInMillis <= now.timeInMillis) {
                    add(Calendar.DAY_OF_MONTH, 1)
                }
            }

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            midnight.timeInMillis,
                            pendingIntent
                        )
                    } else {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            midnight.timeInMillis,
                            pendingIntent
                        )
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        midnight.timeInMillis,
                        pendingIntent
                    )
                }
                Log.d(TAG, "자정 알람 예약 완료: ${midnight.time}")
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException 발생, 일반 알람으로 등록", e)
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    midnight.timeInMillis,
                    pendingIntent
                )
            } catch (e: Exception) {
                Log.e(TAG, "알람 예약 실패, 일반 알람으로 등록", e)
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    midnight.timeInMillis,
                    pendingIntent
                )
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "수신된 액션: $action")

        if (action == "com.example.businessdaycalc.MIDNIGHT_UPDATE" ||
            action == Intent.ACTION_BOOT_COMPLETED) {

            // 1. 모든 위젯에 갱신 브로드캐스트 전송
            val updateIntents = listOf(
                Intent(context, BusinessDayWidgetLarge::class.java),
                Intent(context, BusinessDayWidgetMedium::class.java),
                Intent(context, BusinessDayWidgetSmall::class.java)
            )
            for (updateIntent in updateIntents) {
                updateIntent.action = "com.example.businessdaycalc.MIDNIGHT_UPDATE"
                context.sendBroadcast(updateIntent)
            }

            // 2. 다음 날 자정 알람 재예약
            scheduleMidnightAlarm(context)
        }
    }
}
