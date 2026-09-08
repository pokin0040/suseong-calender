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
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class HolidayFragment : Fragment() {

    private lateinit var holidayManager: HolidayManager
    private lateinit var rvHolidays: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: HolidayAdapter

    private lateinit var spFilterYear: Spinner
    private lateinit var spFilterMonth: Spinner
    private lateinit var btnLoadMore: Button

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

    private var displayedLimit = 10

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_holiday, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        holidayManager = HolidayManager(requireContext())
        rvHolidays = view.findViewById(R.id.rvHolidays)
        tvEmpty = view.findViewById(R.id.tvEmpty)

        spFilterYear = view.findViewById(R.id.spFilterYear)
        spFilterMonth = view.findViewById(R.id.spFilterMonth)
        btnLoadMore = view.findViewById(R.id.btnLoadMore)

        btnAddHoliday = view.findViewById(R.id.btnAddHoliday)
        cardCalendar = view.findViewById(R.id.cardCalendar)
        etHolidayName = view.findViewById(R.id.etHolidayName)
        btnPrevMonth = view.findViewById(R.id.btnPrevMonth)
        btnNextMonth = view.findViewById(R.id.btnNextMonth)
        tvCalendarMonthTitle = view.findViewById(R.id.tvCalendarMonthTitle)
        gridCalendarDays = view.findViewById(R.id.gridCalendarDays)
        btnConfirmAddHoliday = view.findViewById(R.id.btnConfirmAddHoliday)

        adapter = HolidayAdapter(
            onDelete = { date ->
                holidayManager.removeHoliday(date)
                setupFilters()
                updateList()
                updateWidgets()
            }
        )

        rvHolidays.layoutManager = LinearLayoutManager(requireContext())
        rvHolidays.adapter = adapter

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

        btnLoadMore.setOnClickListener {
            displayedLimit += 10
            updateList()
        }

        updateList()
    }

    private fun setupFilters() {
        val holidays = holidayManager.getCustomHolidays()
        val years = mutableListOf("전체 연도")
        val uniqueYears = holidays.map { it.date.year }.distinct().sorted()
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

        val listener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                displayedLimit = 10
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
        val holidays = holidayManager.getCustomHolidays().sortedByDescending { it.date }

        val selectedYearPos = spFilterYear.selectedItemPosition
        val selectedMonthPos = spFilterMonth.selectedItemPosition

        val filteredList = holidays.filter { item ->
            val matchesYear = if (selectedYearPos <= 0) true else {
                val yearText = spFilterYear.selectedItem as String
                val year = yearText.replace("년", "").toIntOrNull()
                item.date.year == year
            }
            val matchesMonth = if (selectedMonthPos <= 0) true else {
                item.date.monthValue == selectedMonthPos
            }
            matchesYear && matchesMonth
        }

        val displayedList = filteredList.take(displayedLimit)
        adapter.submitList(displayedList)

        if (filteredList.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            rvHolidays.visibility = View.GONE
            btnLoadMore.visibility = View.GONE
        } else {
            tvEmpty.visibility = View.GONE
            rvHolidays.visibility = View.VISIBLE

            if (filteredList.size > displayedList.size) {
                btnLoadMore.visibility = View.VISIBLE
                btnLoadMore.text = "+ 더보기 (${displayedList.size} / ${filteredList.size}개)"
            } else {
                btnLoadMore.visibility = View.GONE
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

class HolidayAdapter(private val onDelete: (LocalDate) -> Unit) : RecyclerView.Adapter<HolidayAdapter.ViewHolder>() {

    private var holidays = listOf<CustomHoliday>()
    private val formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
    private val days = arrayOf("월", "화", "수", "목", "금", "토", "일")

    fun submitList(list: List<CustomHoliday>) {
        holidays = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_holiday, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = holidays[position]
        val dayOfWeek = days[item.date.dayOfWeek.value - 1]
        holder.tvDate.text = "${item.date.format(formatter)} ($dayOfWeek)"
        holder.tvName.text = item.name
        holder.btnDelete.setOnClickListener { onDelete(item.date) }
    }

    override fun getItemCount() = holidays.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvName: TextView = view.findViewById(R.id.tvName)
        val btnDelete: View = view.findViewById(R.id.btnDelete)
    }
}
