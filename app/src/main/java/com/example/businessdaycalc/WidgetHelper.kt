package com.example.businessdaycalc

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object WidgetHelper {

    fun updateWidgets(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, widgetType: WidgetType) {
        try {
            val layoutId = when (widgetType) {
                WidgetType.LARGE -> R.layout.widget_large
                WidgetType.MEDIUM -> R.layout.widget_medium
                WidgetType.SMALL -> R.layout.widget_small
            }

            val views = RemoteViews(context.packageName, layoutId)
            val today = LocalDate.now(ZoneId.systemDefault())

            val holidayManager = HolidayManager(context)
            val customHolidays = holidayManager.getCustomHolidays().map { it.date }.toSet()

            val calendarHelper = CalendarHelper(context)
            val deviceHolidays = calendarHelper.getCalendarHolidays(today, today.plusDays(30))

            val calculator = BusinessDayCalculator(customHolidays, deviceHolidays)
            val settingsManager = SettingsManager(context)

            // Refresh button (Large widget)
            if (widgetType == WidgetType.LARGE) {
                val refreshIntent = Intent(context, BusinessDayWidgetLarge::class.java).apply {
                    action = "com.example.businessdaycalc.WIDGET_REFRESH"
                }
                val pendingRefresh = PendingIntent.getBroadcast(
                    context, 0, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                try {
                    views.setOnClickPendingIntent(R.id.btnWidgetRefresh, pendingRefresh)
                } catch (e: Exception) {}
            }

            val typesToRender = when (widgetType) {
                WidgetType.LARGE, WidgetType.SMALL -> DeliveryType.values().toList()
                WidgetType.MEDIUM -> listOf(DeliveryType.NORMAL, DeliveryType.CERTIFIED)
            }

            for (type in typesToRender) {
                val setting = settingsManager.getSetting(type)
                val deliveryDates = calculator.calculateDeliveryDates(today, setting.deliverySteps)
                val storageDate = if (setting.useStorage) calculator.addBusinessDays(today, setting.storageSteps) else null

                when (widgetType) {
                    WidgetType.LARGE, WidgetType.MEDIUM -> bindLargeOrMediumRow(views, type, deliveryDates, storageDate)
                    WidgetType.SMALL -> bindSmallRow(views, type, deliveryDates, storageDate)
                }
            }

            // Click widget to open app
            val openIntent = Intent(context, MainActivity::class.java).apply {
                putExtra("OPEN_TAB", "HOLIDAY")
            }
            val pendingOpen = PendingIntent.getActivity(
                context, 0, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingOpen)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun bindLargeOrMediumRow(views: RemoteViews, type: DeliveryType, deliveryDates: List<LocalDate>, storageDate: LocalDate?) {
        val (del1, dash1, del2, dash2, del3, colon, sto) = when (type) {
            DeliveryType.NORMAL -> Tuple7(R.id.chipNormalDel1, R.id.dashNormalDel1, R.id.chipNormalDel2, R.id.dashNormalDel2, R.id.chipNormalDel3, R.id.colonNormal, R.id.chipNormalSto)
            DeliveryType.COURT -> Tuple7(R.id.chipCourtDel1, R.id.dashCourtDel1, R.id.chipCourtDel2, R.id.dashCourtDel2, R.id.chipCourtDel3, R.id.colonCourt, R.id.chipCourtSto)
            DeliveryType.CERTIFIED -> Tuple7(R.id.chipCertDel1, R.id.dashCertDel1, R.id.chipCertDel2, R.id.dashCertDel2, R.id.chipCertDel3, R.id.colonCert, R.id.chipCertSto)
            DeliveryType.CONTRACT -> Tuple7(R.id.chipContractDel1, R.id.dashContractDel1, R.id.chipContractDel2, R.id.dashContractDel2, R.id.chipContractDel3, R.id.colonContract, R.id.chipContractSto)
        }

        // Chip 1
        if (deliveryDates.isNotEmpty()) {
            views.setTextViewText(del1, formatWithDayOfWeek(deliveryDates[0]))
            views.setViewVisibility(del1, View.VISIBLE)
        } else {
            views.setViewVisibility(del1, View.GONE)
        }

        // Chip 2
        if (deliveryDates.size >= 2) {
            views.setViewVisibility(dash1, View.VISIBLE)
            views.setTextViewText(del2, formatWithDayOfWeek(deliveryDates[1]))
            views.setViewVisibility(del2, View.VISIBLE)
        } else {
            views.setViewVisibility(dash1, View.GONE)
            views.setViewVisibility(del2, View.GONE)
        }

        // Chip 3
        if (deliveryDates.size >= 3) {
            views.setViewVisibility(dash2, View.VISIBLE)
            views.setTextViewText(del3, formatWithDayOfWeek(deliveryDates[2]))
            views.setViewVisibility(del3, View.VISIBLE)
        } else {
            views.setViewVisibility(dash2, View.GONE)
            views.setViewVisibility(del3, View.GONE)
        }

        // Storage
        if (storageDate != null) {
            views.setViewVisibility(colon, View.VISIBLE)
            views.setTextViewText(sto, formatWithDayOfWeek(storageDate))
            views.setViewVisibility(sto, View.VISIBLE)
        } else {
            views.setViewVisibility(colon, View.GONE)
            views.setViewVisibility(sto, View.GONE)
        }
    }

    private fun bindSmallRow(views: RemoteViews, type: DeliveryType, deliveryDates: List<LocalDate>, storageDate: LocalDate?) {
        val (delId, colonId, stoId) = when (type) {
            DeliveryType.NORMAL -> Triple(R.id.chipNormalDel, R.id.colonNormal, R.id.chipNormalSto)
            DeliveryType.COURT -> Triple(R.id.chipCourtDel, R.id.colonCourt, R.id.chipCourtSto)
            DeliveryType.CERTIFIED -> Triple(R.id.chipCertDel, R.id.colonCert, R.id.chipCertSto)
            DeliveryType.CONTRACT -> Triple(R.id.chipContractDel, R.id.colonContract, R.id.chipContractSto)
        }

        if (deliveryDates.isNotEmpty()) {
            val text = if (deliveryDates.size == 1) {
                formatWithDayOfWeek(deliveryDates[0])
            } else {
                deliveryDates.joinToString(" - ") { formatMMdd(it) }
            }
            views.setTextViewText(delId, text)
            views.setViewVisibility(delId, View.VISIBLE)
        } else {
            views.setViewVisibility(delId, View.GONE)
        }

        if (storageDate != null) {
            views.setViewVisibility(colonId, View.VISIBLE)
            views.setTextViewText(stoId, formatWithDayOfWeek(storageDate))
            views.setViewVisibility(stoId, View.VISIBLE)
        } else {
            views.setViewVisibility(colonId, View.GONE)
            views.setViewVisibility(stoId, View.GONE)
        }
    }

    private fun formatWithDayOfWeek(date: LocalDate): String {
        val days = arrayOf("월", "화", "수", "목", "금", "토", "일")
        val dayOfWeek = days[date.dayOfWeek.value - 1]
        val formatter = DateTimeFormatter.ofPattern("MM.dd")
        return "${date.format(formatter)}($dayOfWeek)"
    }

    private fun formatMMdd(date: LocalDate): String {
        val formatter = DateTimeFormatter.ofPattern("MM.dd")
        return date.format(formatter)
    }

    private data class Tuple7<A, B, C, D, E, F, G>(
        val a: A, val b: B, val c: C, val d: D, val e: E, val f: F, val g: G
    )
}

enum class WidgetType { LARGE, MEDIUM, SMALL }
