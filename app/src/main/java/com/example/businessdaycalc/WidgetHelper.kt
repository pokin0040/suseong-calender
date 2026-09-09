package com.example.businessdaycalc

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
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

            // Apply Opacity and Theme Color using official RemoteViews methods
            val opacity = WidgetPreferences.getOpacity(context, appWidgetId)
            val isDark = WidgetPreferences.isDark(context, appWidgetId)

            if (isDark) {
                views.setImageViewResource(R.id.widget_bg_view, R.drawable.widget_bg_dark)
                if (widgetType == WidgetType.LARGE) {
                    views.setInt(R.id.btnWidgetRefresh, "setColorFilter", Color.WHITE)
                }
            } else {
                views.setImageViewResource(R.id.widget_bg_view, R.drawable.widget_bg)
                if (widgetType == WidgetType.LARGE) {
                    views.setInt(R.id.btnWidgetRefresh, "setColorFilter", Color.parseColor("#333333"))
                }
            }

            val alpha255 = (opacity * 255 / 100).coerceIn(0, 255)
            views.setInt(R.id.widget_bg_view, "setImageAlpha", alpha255)

            val today = LocalDate.now(ZoneId.systemDefault())

            val holidayManager = HolidayManager(context)
            val allCustom = holidayManager.getCustomHolidays()
            val customHolidays = allCustom.filter { !it.isWorkDay }.map { it.date }.toSet()
            val forcedWorkDays = allCustom.filter { it.isWorkDay }.map { it.date }.toSet()

            val calendarHelper = CalendarHelper(context)
            val deviceHolidays = calendarHelper.getCalendarHolidays(today, today.plusDays(30))

            val calculator = BusinessDayCalculator(customHolidays, deviceHolidays, forcedWorkDays)
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
                val totalStorageSteps = setting.deliverySteps + setting.storageSteps
                val storageDate = if (setting.useStorage) calculator.addBusinessDays(today, totalStorageSteps) else null

                when (widgetType) {
                    WidgetType.LARGE, WidgetType.MEDIUM -> bindLargeOrMediumRow(views, type, deliveryDates, storageDate, isDark)
                    WidgetType.SMALL -> bindSmallRow(views, type, deliveryDates, storageDate, isDark)
                }
            }

            if (widgetType == WidgetType.LARGE) {
                applyLargeWidgetTheme(views, isDark)
            }

            // Click widget root to open app (open Calculator tab)
            val openIntent = Intent(context, MainActivity::class.java).apply {
                putExtra("OPEN_TAB", "CALC")
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

    private fun bindLargeOrMediumRow(views: RemoteViews, type: DeliveryType, deliveryDates: List<LocalDate>, storageDate: LocalDate?, isDark: Boolean) {
        val (del1, dash1, del2, dash2, del3, colon, sto) = when (type) {
            DeliveryType.NORMAL -> Tuple7(R.id.chipNormalDel1, R.id.dashNormalDel1, R.id.chipNormalDel2, R.id.dashNormalDel2, R.id.chipNormalDel3, R.id.colonNormal, R.id.chipNormalSto)
            DeliveryType.CERTIFIED -> Tuple7(R.id.chipCertDel1, R.id.dashCertDel1, R.id.chipCertDel2, R.id.dashCertDel2, R.id.chipCertDel3, R.id.colonCert, R.id.chipCertSto)
            DeliveryType.COURT -> Tuple7(R.id.chipCourtDel1, R.id.dashCourtDel1, R.id.chipCourtDel2, R.id.dashCourtDel2, R.id.chipCourtDel3, R.id.colonCourt, R.id.chipCourtSto)
            DeliveryType.CONTRACT -> Tuple7(R.id.chipContractDel1, R.id.dashContractDel1, R.id.chipContractDel2, R.id.dashContractDel2, R.id.chipContractDel3, R.id.colonContract, R.id.chipContractSto)
        }

        val titleId = when (type) {
            DeliveryType.NORMAL -> R.id.tvTitleNormal
            DeliveryType.CERTIFIED -> R.id.tvTitleCert
            DeliveryType.COURT -> R.id.tvTitleCourt
            DeliveryType.CONTRACT -> R.id.tvTitleContract
        }

        val titleColor = if (isDark) Color.WHITE else Color.parseColor("#333333")
        val secondaryTextColor = if (isDark) Color.parseColor("#E0E0E0") else Color.parseColor("#777777")

        try { views.setTextColor(titleId, titleColor) } catch (e: Exception) {}
        try { views.setTextColor(dash1, secondaryTextColor) } catch (e: Exception) {}
        try { views.setTextColor(dash2, secondaryTextColor) } catch (e: Exception) {}
        try { views.setTextColor(colon, secondaryTextColor) } catch (e: Exception) {}

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

    /** Applies only visual surfaces; date calculation and visibility remain unchanged. */
    private fun applyLargeWidgetTheme(views: RemoteViews, isDark: Boolean) {
        val rowBackground = if (isDark) R.drawable.widget_schedule_row_dark else R.drawable.widget_schedule_row
        val deliveryBackground = if (isDark) R.drawable.widget_chip_delivery_dark else R.drawable.widget_chip_delivery
        val storageBackground = if (isDark) R.drawable.widget_chip_storage_dark else R.drawable.widget_chip_storage

        val rowIds = intArrayOf(R.id.rowNormal, R.id.rowCert, R.id.rowCourt, R.id.rowContract)
        val deliveryIds = intArrayOf(
            R.id.chipNormalDel1, R.id.chipNormalDel2, R.id.chipNormalDel3,
            R.id.chipCertDel1, R.id.chipCertDel2, R.id.chipCertDel3,
            R.id.chipCourtDel1, R.id.chipCourtDel2, R.id.chipCourtDel3,
            R.id.chipContractDel1, R.id.chipContractDel2, R.id.chipContractDel3
        )
        val storageIds = intArrayOf(R.id.chipNormalSto, R.id.chipCertSto, R.id.chipCourtSto, R.id.chipContractSto)

        rowIds.forEach { views.setInt(it, "setBackgroundResource", rowBackground) }
        deliveryIds.forEach {
            views.setInt(it, "setBackgroundResource", deliveryBackground)
            views.setTextColor(it, if (isDark) Color.parseColor("#89C2FF") else Color.parseColor("#1769C2"))
        }
        storageIds.forEach {
            views.setInt(it, "setBackgroundResource", storageBackground)
            views.setTextColor(it, if (isDark) Color.parseColor("#FFABB4") else Color.parseColor("#C74650"))
        }

        views.setTextColor(R.id.tvWidgetTitle, if (isDark) Color.parseColor("#F2F5FC") else Color.parseColor("#172B4D"))
        views.setTextColor(R.id.tvLegendDelivery, if (isDark) Color.parseColor("#89C2FF") else Color.parseColor("#1769C2"))
        views.setTextColor(R.id.tvLegendStorage, if (isDark) Color.parseColor("#FFABB4") else Color.parseColor("#C74650"))
    }

    private fun bindSmallRow(views: RemoteViews, type: DeliveryType, deliveryDates: List<LocalDate>, storageDate: LocalDate?, isDark: Boolean) {
        val (delId, colonId, stoId) = when (type) {
            DeliveryType.NORMAL -> Triple(R.id.chipNormalDel, R.id.colonNormal, R.id.chipNormalSto)
            DeliveryType.CERTIFIED -> Triple(R.id.chipCertDel, R.id.colonCert, R.id.chipCertSto)
            DeliveryType.COURT -> Triple(R.id.chipCourtDel, R.id.colonCourt, R.id.chipCourtSto)
            DeliveryType.CONTRACT -> Triple(R.id.chipContractDel, R.id.colonContract, R.id.chipContractSto)
        }

        val titleId = when (type) {
            DeliveryType.NORMAL -> R.id.tvTitleNormal
            DeliveryType.CERTIFIED -> R.id.tvTitleCert
            DeliveryType.COURT -> R.id.tvTitleCourt
            DeliveryType.CONTRACT -> R.id.tvTitleContract
        }

        val titleColor = if (isDark) Color.WHITE else Color.parseColor("#333333")
        val secondaryTextColor = if (isDark) Color.parseColor("#E0E0E0") else Color.parseColor("#777777")

        try { views.setTextColor(titleId, titleColor) } catch (e: Exception) {}
        try { views.setTextColor(colonId, secondaryTextColor) } catch (e: Exception) {}

        if (deliveryDates.isNotEmpty()) {
            val text = if (deliveryDates.size == 1) {
                formatWithDayOfWeek(deliveryDates[0])
            } else {
                deliveryDates.joinToString("-") { formatMMdd(it) }
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
