package com.example.businessdaycalc

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
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
        rvHolidays = view.findViewById(R.id.rvHolidays)
        tvEmpty = view.findViewById(R.id.tvEmpty)

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
                updateList()
                updateWidgets()
            }
        )

        rvHolidays.layoutManager = LinearLayoutManager(requireContext())
        rvHolidays.adapter = adapter

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
            val name = etHolidayName.text.toString().trim().ifEmpty { "대체공휴일" }
            holidayManager.addHoliday(CustomHoliday(pendingSelectedDate, name))
            cardCalendar.visibility = View.GONE
            updateList()
            updateWidgets()
        }

        updateList()
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
                    setTextColor(resources.getColor(R.color.primary, null))
                    setTypeface(null, android.graphics.Typeface.BOLD)
                } else if (date.monthValue != displayYearMonth.monthValue) {
                    setTextColor(resources.getColor(R.color.divider, null))
                } else {
                    setTextColor(resources.getColor(R.color.text_main, null))
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

    private fun updateList() {
        val holidays = holidayManager.getCustomHolidays()
        adapter.submitList(holidays)

        if (holidays.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            rvHolidays.visibility = View.GONE
        } else {
            tvEmpty.visibility = View.GONE
            rvHolidays.visibility = View.VISIBLE
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
    private val formatter = DateTimeFormatter.ofPattern("MM.dd")
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
