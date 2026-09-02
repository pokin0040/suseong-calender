package com.example.businessdaycalc

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class CalculatorFragment : Fragment() {

    private lateinit var calcContainer: LinearLayout
    private lateinit var settingsManager: SettingsManager
    private lateinit var holidayManager: HolidayManager
    private lateinit var calendarHelper: CalendarHelper

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_calculator, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        calcContainer = view.findViewById(R.id.calcContainer)
        settingsManager = SettingsManager(requireContext())
        holidayManager = HolidayManager(requireContext())
        calendarHelper = CalendarHelper(requireContext())

        view.findViewById<Button>(R.id.btnRefresh).setOnClickListener {
            refreshCalculations()
        }

        refreshCalculations()
    }

    private fun refreshCalculations() {
        // Remove old dynamic rows
        val childCount = calcContainer.childCount
        // Keep header (idx 0, 1) and footer (last)
        if (childCount > 3) {
            calcContainer.removeViews(2, childCount - 3)
        }

        val today = LocalDate.now()
        val customHolidays = holidayManager.getCustomHolidays().map { it.date }.toSet()
        val deviceHolidays = calendarHelper.getCalendarHolidays(today, today.plusDays(30))
        val calculator = BusinessDayCalculator(customHolidays, deviceHolidays)

        val settings = settingsManager.getAllSettings()

        var insertIndex = 2
        for (setting in settings) {
            val row = buildRow(setting, today, calculator)
            calcContainer.addView(row, insertIndex)
            insertIndex++
            
            // Add spacing
            val space = View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 32)
            }
            calcContainer.addView(space, insertIndex)
            insertIndex++
        }
    }

    private fun buildRow(setting: DeliverySetting, today: LocalDate, calculator: BusinessDayCalculator): View {
        val context = requireContext()
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        // Title
        val tvTitle = TextView(context).apply {
            text = setting.type.displayName
            textSize = 13f
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(dpToPx(64), LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        row.addView(tvTitle)

        // Delivery Dates Container
        val deliveryContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL or android.view.Gravity.START
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        
        val deliveryDates = calculator.calculateDeliveryDates(today, setting.deliverySteps)
        deliveryDates.forEachIndexed { index, date ->
            val tv = TextView(context).apply {
                text = formatDate(date)
                setBackgroundResource(R.drawable.chip_green)
                setTextColor(resources.getColor(R.color.chip_green_text, null))
                setPadding(dpToPx(5), dpToPx(3), dpToPx(5), dpToPx(3))
                textSize = 11f
                isSingleLine = true
            }
            deliveryContainer.addView(tv)

            if (index < deliveryDates.size - 1) {
                val dash = TextView(context).apply {
                    text = "-"
                    setPadding(dpToPx(2), 0, dpToPx(2), 0)
                    textSize = 10f
                    setTextColor(resources.getColor(R.color.text_secondary, null))
                }
                deliveryContainer.addView(dash)
            }
        }
        row.addView(deliveryContainer)

        // Storage Date
        val storageContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.35f)
        }
        
        if (setting.useStorage) {
            val colon = TextView(context).apply {
                text = ":"
                setPadding(0, 0, dpToPx(4), 0)
                textSize = 11f
                setTextColor(resources.getColor(R.color.text_secondary, null))
            }
            storageContainer.addView(colon)

            val storageDate = calculator.addBusinessDays(today, setting.storageSteps)
            val tv = TextView(context).apply {
                text = formatDate(storageDate)
                setBackgroundResource(R.drawable.chip_blue)
                setTextColor(resources.getColor(R.color.chip_blue_text, null))
                setPadding(dpToPx(5), dpToPx(3), dpToPx(5), dpToPx(3))
                textSize = 11f
                isSingleLine = true
            }
            storageContainer.addView(tv)
        }
        
        row.addView(storageContainer)
        return row
    }

    private fun formatDate(date: LocalDate): String {
        val days = arrayOf("월", "화", "수", "목", "금", "토", "일")
        val dayOfWeek = days[date.dayOfWeek.value - 1]
        val formatter = DateTimeFormatter.ofPattern("MM.dd")
        return "${date.format(formatter)}($dayOfWeek)"
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }
}
