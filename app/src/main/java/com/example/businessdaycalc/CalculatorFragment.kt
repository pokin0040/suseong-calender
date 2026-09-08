package com.example.businessdaycalc

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class CalculatorFragment : Fragment() {

    private lateinit var calcContainer: LinearLayout
    private lateinit var cardBaseDate: LinearLayout
    private lateinit var tvBaseDateValue: TextView
    private lateinit var btnToday: Button

    private lateinit var cardCalendar: LinearLayout
    private lateinit var btnPrevMonth: ImageView
    private lateinit var btnNextMonth: ImageView
    private lateinit var tvCalendarMonthTitle: TextView
    private lateinit var gridCalendarDays: GridLayout
    private lateinit var btnCloseCalendar: Button

    private lateinit var settingsManager: SettingsManager
    private lateinit var holidayManager: HolidayManager
    private lateinit var calendarHelper: CalendarHelper

    private var selectedBaseDate: LocalDate? = null
    private var pendingSelectedDate: LocalDate = LocalDate.now()
    private var displayYearMonth: YearMonth = YearMonth.now()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_calculator, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        calcContainer = view.findViewById(R.id.calcContainer)
        cardBaseDate = view.findViewById(R.id.cardBaseDate)
        tvBaseDateValue = view.findViewById(R.id.tvBaseDateValue)
        btnToday = view.findViewById(R.id.btnToday)

        cardCalendar = view.findViewById(R.id.cardCalendar)
        btnPrevMonth = view.findViewById(R.id.btnPrevMonth)
        btnNextMonth = view.findViewById(R.id.btnNextMonth)
        tvCalendarMonthTitle = view.findViewById(R.id.tvCalendarMonthTitle)
        gridCalendarDays = view.findViewById(R.id.gridCalendarDays)
        btnCloseCalendar = view.findViewById(R.id.btnCloseCalendar)

        settingsManager = SettingsManager(requireContext())
        holidayManager = HolidayManager(requireContext())
        calendarHelper = CalendarHelper(requireContext())

        cardBaseDate.setOnClickListener {
            if (cardCalendar.isVisible) {
                cardCalendar.visibility = View.GONE
            } else {
                pendingSelectedDate = selectedBaseDate ?: LocalDate.now()
                displayYearMonth = YearMonth.from(pendingSelectedDate)
                populateCalendarGrid()
                cardCalendar.visibility = View.VISIBLE
            }
        }

        btnPrevMonth.setOnClickListener {
            displayYearMonth = displayYearMonth.minusMonths(1)
            populateCalendarGrid()
        }

        btnNextMonth.setOnClickListener {
            displayYearMonth = displayYearMonth.plusMonths(1)
            populateCalendarGrid()
        }

        btnCloseCalendar.setOnClickListener {
            cardCalendar.visibility = View.GONE
        }

        btnToday.setOnClickListener {
            selectedBaseDate = LocalDate.now()
            pendingSelectedDate = LocalDate.now()
            displayYearMonth = YearMonth.now()
            cardCalendar.visibility = View.GONE
            refreshCalculations()
        }

        refreshCalculations()
    }

    private fun populateCalendarGrid() {
        gridCalendarDays.removeAllViews()
        tvCalendarMonthTitle.text = "${displayYearMonth.year}년 ${displayYearMonth.monthValue}월"

        val firstDayOfMonth = displayYearMonth.atDay(1)
        // Sunday = 0, Mon = 1 ... Sat = 6
        val firstDayOfWeekOffset = firstDayOfMonth.dayOfWeek.value % 7
        val gridStartDate = firstDayOfMonth.minusDays(firstDayOfWeekOffset.toLong())

        for (i in 0 until 42) {
            val date = gridStartDate.plusDays(i.toLong())
            val cell = TextView(requireContext()).apply {
                text = date.dayOfMonth.toString()
                textSize = 14f
                gravity = android.view.Gravity.CENTER
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    height = dpToPx(38)
                    columnSpec = GridLayout.spec(i % 7, 1f)
                    rowSpec = GridLayout.spec(i / 7)
                }

                if (date == (selectedBaseDate ?: pendingSelectedDate)) {
                    setBackgroundResource(R.drawable.bg_day_selected)
                    setTextColor(resources.getColor(R.color.white, null))
                    setTypeface(null, android.graphics.Typeface.BOLD)
                } else if (date == LocalDate.now()) {
                    setBackgroundResource(R.drawable.bg_day_today)
                    setTextColor(resources.getColor(R.color.primary, null))
                    setTypeface(null, android.graphics.Typeface.BOLD)
                } else if (date.monthValue != displayYearMonth.monthValue) {
                    setTextColor(resources.getColor(R.color.divider, null))
                } else {
                    setTextColor(resources.getColor(R.color.text_main, null))
                }

                setOnClickListener {
                    pendingSelectedDate = date
                    selectedBaseDate = date
                    populateCalendarGrid()
                    refreshCalculations() // <--- Real-time recalculation!
                }
            }
            gridCalendarDays.addView(cell)
        }
    }

    private fun refreshCalculations() {
        // Remove old dynamic rows (keep table header idx 0, divider idx 1, and footer note idx last)
        val childCount = calcContainer.childCount
        if (childCount > 3) {
            calcContainer.removeViews(2, childCount - 3)
        }

        val baseDate = selectedBaseDate ?: LocalDate.now()

        // Format Date string e.g. 2026.09.08 (화)
        val days = arrayOf("월", "화", "수", "목", "금", "토", "일")
        val dayOfWeek = days[baseDate.dayOfWeek.value - 1]
        val formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
        tvBaseDateValue.text = "${baseDate.format(formatter)} ($dayOfWeek)"

        val customHolidays = holidayManager.getCustomHolidays().map { it.date }.toSet()
        val deviceHolidays = calendarHelper.getCalendarHolidays(baseDate, baseDate.plusDays(30))
        val calculator = BusinessDayCalculator(customHolidays, deviceHolidays)

        val settings = settingsManager.getAllSettings()

        var insertIndex = 2
        for (setting in settings) {
            val row = buildRow(setting, baseDate, calculator)
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

    private fun buildRow(setting: DeliverySetting, baseDate: LocalDate, calculator: BusinessDayCalculator): View {
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
        
        val deliveryDates = calculator.calculateDeliveryDates(baseDate, setting.deliverySteps)
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

            val storageDate = calculator.addBusinessDays(baseDate, setting.storageSteps)
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
