package com.example.businessdaycalc

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

data class HolidayItem(
    val date: LocalDate,
    val name: String,
    val isCustom: Boolean
)

class HolidayFragment : Fragment() {

    private lateinit var holidayManager: HolidayManager
    private lateinit var llHolidaysContainer: LinearLayout
    private lateinit var tvEmpty: TextView

    private lateinit var cbOnlyCustom: CheckBox
    private lateinit var spFilterYear: Spinner
    private lateinit var spFilterMonth: Spinner

    private lateinit var btnAddHoliday: Button
    private lateinit var cardCalendar: LinearLayout
    private lateinit var etHolidayName: EditText
    private lateinit var btnPrevMonth: ImageView
    private lateinit var btnNextMonth: ImageView
    private lateinit var tvCalendarMonthTitle: TextView
    private lateinit var gridCalendarDays: GridLayout
    private lateinit var btnConfirmAddHoliday: Button

    private var pendingSelectedDate: LocalDate = LocalDate.now()
    private var displayYearMonth: YearMonth = YearMonth.now()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_holiday, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        holidayManager = HolidayManager(requireContext())
        llHolidaysContainer = view.findViewById(R.id.llHolidaysContainer)
        tvEmpty = view.findViewById(R.id.tvEmpty)

        cbOnlyCustom = view.findViewById(R.id.cbOnlyCustom)
        spFilterYear = view.findViewById(R.id.spFilterYear)
        spFilterMonth = view.findViewById(R.id.spFilterMonth)

        btnAddHoliday = view.findViewById(R.id.btnAddHoliday)
        cardCalendar = view.findViewById(R.id.cardCalendar)
        etHolidayName = view.findViewById(R.id.etHolidayName)
        btnPrevMonth = view.findViewById(R.id.btnPrevMonth)
        btnNextMonth = view.findViewById(R.id.btnNextMonth)
        tvCalendarMonthTitle = view.findViewById(R.id.tvCalendarMonthTitle)
        gridCalendarDays = view.findViewById(R.id.gridCalendarDays)
        btnConfirmAddHoliday = view.findViewById(R.id.btnConfirmAddHoliday)

        cbOnlyCustom.setOnCheckedChangeListener { _, _ ->
            setupFilters()
            updateList()
        }

        setupFilters()

        btnAddHoliday.setOnClickListener {
            hideKeyboard()
            if (cardCalendar.isVisible) {
                cardCalendar.visibility = View.GONE
            } else {
                pendingSelectedDate = LocalDate.now()
                displayYearMonth = YearMonth.now()
                etHolidayName.setText("대체공휴일")
                etHolidayName.setSelection(etHolidayName.text.length)
                populateCalendarGrid()
                cardCalendar.visibility = View.VISIBLE
            }
        }

        btnPrevMonth.setOnClickListener {
            hideKeyboard()
            displayYearMonth = displayYearMonth.minusMonths(1)
            populateCalendarGrid()
        }

        btnNextMonth.setOnClickListener {
            hideKeyboard()
            displayYearMonth = displayYearMonth.plusMonths(1)
            populateCalendarGrid()
        }

        btnConfirmAddHoliday.setOnClickListener {
            hideKeyboard()

            val calendarHelper = CalendarHelper(requireContext())
            val deviceHolidays = calendarHelper.getCalendarHolidays(pendingSelectedDate, pendingSelectedDate)

            // Check if already weekend or public holiday
            if (pendingSelectedDate.dayOfWeek == java.time.DayOfWeek.SATURDAY || 
                pendingSelectedDate.dayOfWeek == java.time.DayOfWeek.SUNDAY || 
                deviceHolidays.contains(pendingSelectedDate)) {
                android.widget.Toast.makeText(requireContext(), "이미 주말 또는 공휴일로 지정되어 있는 날짜입니다.", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Check if already registered custom holiday
            val existing = holidayManager.getCustomHolidays().find { it.date == pendingSelectedDate }
            if (existing != null) {
                android.widget.Toast.makeText(requireContext(), "이미 등록되어 있는 임시 휴무일입니다.", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val name = etHolidayName.text.toString().trim().ifEmpty { "대체공휴일" }
            holidayManager.addHoliday(CustomHoliday(pendingSelectedDate, name))
            cardCalendar.visibility = View.GONE
            setupFilters()
            updateList()
            updateWidgets()
            android.widget.Toast.makeText(requireContext(), "임시 휴무일이 등록되었습니다.", android.widget.Toast.LENGTH_SHORT).show()
        }

        updateList()
    }

    private fun setupFilters() {
        val isOnlyCustom = cbOnlyCustom.isChecked
        val customHolidays = holidayManager.getCustomHolidays()

        val uniqueYears = if (isOnlyCustom) {
            customHolidays.map { it.date.year }.distinct().sorted()
        } else {
            val calendarHelper = CalendarHelper(requireContext())
            val currentYear = LocalDate.now().year
            val deviceHolidays = calendarHelper.getPublicHolidaysWithNames(
                LocalDate.of(currentYear - 2, 1, 1),
                LocalDate.of(currentYear + 2, 12, 31)
            )
            (customHolidays.map { it.date.year } + deviceHolidays.map { it.date.year }).distinct().sorted()
        }

        val years = mutableListOf("전체 연도")
        uniqueYears.forEach { years.add("${it}년") }

        val yearAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, years).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spFilterYear.adapter = yearAdapter

        val months = mutableListOf("전체 월")
        for (m in 1..12) {
            months.add("${m}월")
        }
        val monthAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, months).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spFilterMonth.adapter = monthAdapter

        spFilterYear.setSelection(0, false)
        spFilterMonth.setSelection(0, false)

        val listener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateList()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spFilterYear.onItemSelectedListener = listener
        spFilterMonth.onItemSelectedListener = listener
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        val currentFocusView = activity?.currentFocus ?: view
        currentFocusView?.let {
            imm?.hideSoftInputFromWindow(it.windowToken, 0)
        }
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
            val isRedDay = isHolidayOrWeekend(date)

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

                if (date == pendingSelectedDate) {
                    setBackgroundResource(R.drawable.bg_day_selected)
                    setTextColor(resources.getColor(R.color.white, null))
                    setTypeface(null, android.graphics.Typeface.BOLD)
                } else if (date == LocalDate.now()) {
                    setBackgroundResource(R.drawable.bg_day_today)
                    val colorRes = if (isRedDay) R.color.primary else R.color.text_main
                    setTextColor(resources.getColor(colorRes, null))
                    setTypeface(null, android.graphics.Typeface.BOLD)
                } else if (date.monthValue != displayYearMonth.monthValue) {
                    val colorRes = if (isRedDay) R.color.text_disabled_red else R.color.text_disabled
                    setTextColor(resources.getColor(colorRes, null))
                } else {
                    val colorRes = if (isRedDay) R.color.primary else R.color.text_main
                    setTextColor(resources.getColor(colorRes, null))
                }

                setOnClickListener {
                    hideKeyboard()
                    pendingSelectedDate = date
                    populateCalendarGrid()
                }
            }
            gridCalendarDays.addView(cell)
        }
    }

    private fun isHolidayOrWeekend(date: LocalDate): Boolean {
        if (date.dayOfWeek == java.time.DayOfWeek.SATURDAY || date.dayOfWeek == java.time.DayOfWeek.SUNDAY) {
            return true
        }
        val customHolidays = holidayManager.getCustomHolidays().map { it.date }.toSet()
        if (customHolidays.contains(date)) {
            return true
        }
        val calendarHelper = CalendarHelper(requireContext())
        val deviceHolidays = calendarHelper.getCalendarHolidays(date, date)
        if (deviceHolidays.contains(date)) {
            return true
        }
        return false
    }

    private fun updateList() {
        val isOnlyCustom = cbOnlyCustom.isChecked
        val calendarHelper = CalendarHelper(requireContext())
        val currentYear = LocalDate.now().year

        val customItems = holidayManager.getCustomHolidays().map {
            HolidayItem(it.date, it.name, isCustom = true)
        }
        val customDates = customItems.map { it.date }.toSet()

        val combined = if (isOnlyCustom) {
            customItems.sortedByDescending { it.date }
        } else {
            val deviceItems = calendarHelper.getPublicHolidaysWithNames(
                LocalDate.of(currentYear - 2, 1, 1),
                LocalDate.of(currentYear + 2, 12, 31)
            ).map {
                HolidayItem(it.date, it.name, isCustom = false)
            }.filter { !customDates.contains(it.date) }

            (customItems + deviceItems).sortedByDescending { it.date }
        }

        val selectedYearPos = if (spFilterYear.selectedItemPosition < 0) 0 else spFilterYear.selectedItemPosition
        val selectedMonthPos = if (spFilterMonth.selectedItemPosition < 0) 0 else spFilterMonth.selectedItemPosition

        val filteredList = combined.filter { item ->
            val matchesYear = if (selectedYearPos <= 0) true else {
                val yearText = spFilterYear.selectedItem as? String ?: ""
                val year = yearText.replace("년", "").toIntOrNull()
                year == null || item.date.year == year
            }
            val matchesMonth = if (selectedMonthPos <= 0) true else {
                item.date.monthValue == selectedMonthPos
            }
            matchesYear && matchesMonth
        }

        llHolidaysContainer.removeAllViews()

        if (filteredList.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            llHolidaysContainer.visibility = View.GONE
        } else {
            tvEmpty.visibility = View.GONE
            llHolidaysContainer.visibility = View.VISIBLE

            val inflater = LayoutInflater.from(requireContext())
            val formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
            val days = arrayOf("월", "화", "수", "목", "금", "토", "일")

            for (item in filteredList) {
                val itemView = inflater.inflate(R.layout.list_item_holiday, llHolidaysContainer, false)
                val tvDate = itemView.findViewById<TextView>(R.id.tvDate)
                val tvName = itemView.findViewById<TextView>(R.id.tvName)
                val btnDelete = itemView.findViewById<View>(R.id.btnDelete)

                val dayOfWeek = days[item.date.dayOfWeek.value - 1]
                tvDate.text = "${item.date.format(formatter)} ($dayOfWeek)"
                tvName.text = item.name

                if (item.isCustom) {
                    btnDelete.visibility = View.VISIBLE
                    btnDelete.setOnClickListener {
                        holidayManager.removeHoliday(item.date)
                        setupFilters()
                        updateList()
                        updateWidgets()
                    }
                } else {
                    btnDelete.visibility = View.GONE
                    btnDelete.setOnClickListener(null)
                }

                llHolidaysContainer.addView(itemView)
            }
        }
    }

    private fun updateWidgets() {
        val updateIntents = listOf(
            Intent(requireContext(), BusinessDayWidgetLarge::class.java),
            Intent(requireContext(), BusinessDayWidgetMedium::class.java),
            Intent(requireContext(), BusinessDayWidgetSmall::class.java)
        )
        
        for (intent in updateIntents) {
            intent.action = "android.appwidget.action.APPWIDGET_UPDATE"
            requireContext().sendBroadcast(intent)
        }
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }
}
