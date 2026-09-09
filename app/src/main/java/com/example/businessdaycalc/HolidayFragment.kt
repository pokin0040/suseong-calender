package com.example.businessdaycalc

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
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
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

data class HolidayItem(
    val date: LocalDate,
    val name: String,
    val isCustom: Boolean,
    val isWorkDay: Boolean = false
)

class HolidayFragment : Fragment() {

    private lateinit var holidayManager: HolidayManager
    private lateinit var llHolidaysContainer: LinearLayout
    private lateinit var tvEmpty: TextView

    private lateinit var cbOnlyCustom: CheckBox
    private lateinit var spFilterYear: Spinner
    private lateinit var spFilterMonth: Spinner
    private lateinit var btnAddHoliday: Button

    private var activeDialog: AlertDialog? = null
    private var lastClickTime = 0L

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

        cbOnlyCustom.setOnCheckedChangeListener { _, _ ->
            setupFilters()
            updateList()
        }

        setupFilters()

        btnAddHoliday.setOnClickListener {
            if (isFastDoubleClick()) return@setOnClickListener
            hideKeyboard()
            showAddEditHolidayDialog(null)
        }

        updateList()
    }

    private fun isFastDoubleClick(): Boolean {
        val now = SystemClock.elapsedRealtime()
        if (now - lastClickTime < 500) {
            return true
        }
        lastClickTime = now
        return false
    }

    private fun showAddEditHolidayDialog(itemToEdit: HolidayItem? = null) {
        if (activeDialog?.isShowing == true) {
            return
        }

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_edit_holiday, null)

        val tvDialogTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
        val rgHolidayType = dialogView.findViewById<RadioGroup>(R.id.rgHolidayType)
        val rbTypeHoliday = dialogView.findViewById<RadioButton>(R.id.rbTypeHoliday)
        val rbTypeWorkDay = dialogView.findViewById<RadioButton>(R.id.rbTypeWorkDay)
        val etHolidayName = dialogView.findViewById<EditText>(R.id.etHolidayName)
        val btnPrevMonth = dialogView.findViewById<ImageView>(R.id.btnPrevMonth)
        val btnNextMonth = dialogView.findViewById<ImageView>(R.id.btnNextMonth)
        val tvCalendarMonthTitle = dialogView.findViewById<TextView>(R.id.tvCalendarMonthTitle)
        val gridCalendarDays = dialogView.findViewById<GridLayout>(R.id.gridCalendarDays)
        val btnCancelDialog = dialogView.findViewById<Button>(R.id.btnCancelDialog)
        val btnConfirmDialog = dialogView.findViewById<Button>(R.id.btnConfirmDialog)

        var pendingDate = itemToEdit?.date ?: LocalDate.now()
        var displayYM = YearMonth.from(pendingDate)

        if (itemToEdit != null) {
            tvDialogTitle.text = "휴무일/영업일 수정"
            btnConfirmDialog.text = "수정 완료"
            etHolidayName.setText(itemToEdit.name)
            if (itemToEdit.isWorkDay) {
                rbTypeWorkDay.isChecked = true
            } else {
                rbTypeHoliday.isChecked = true
            }
        } else {
            tvDialogTitle.text = "휴무일/영업일 추가"
            btnConfirmDialog.text = "추가"
            rbTypeHoliday.isChecked = true
            etHolidayName.setText("대체공휴일")
        }

        rgHolidayType.setOnCheckedChangeListener { _, checkedId ->
            if (itemToEdit == null) {
                if (checkedId == R.id.rbTypeWorkDay) {
                    etHolidayName.setText("영업일")
                } else {
                    etHolidayName.setText("대체공휴일")
                }
            }
        }

        fun populateDialogCalendar() {
            gridCalendarDays.removeAllViews()
            tvCalendarMonthTitle.text = "${displayYM.year}년 ${displayYM.monthValue}월"

            val firstDayOfMonth = displayYM.atDay(1)
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

                    if (date == pendingDate) {
                        setBackgroundResource(R.drawable.bg_day_selected)
                        setTextColor(resources.getColor(R.color.white, null))
                        setTypeface(null, android.graphics.Typeface.BOLD)
                    } else if (date == LocalDate.now()) {
                        setBackgroundResource(R.drawable.bg_day_today)
                        val colorRes = if (isRedDay) R.color.primary else R.color.text_main
                        setTextColor(resources.getColor(colorRes, null))
                        setTypeface(null, android.graphics.Typeface.BOLD)
                    } else if (date.monthValue != displayYM.monthValue) {
                        val colorRes = if (isRedDay) R.color.text_disabled_red else R.color.text_disabled
                        setTextColor(resources.getColor(colorRes, null))
                    } else {
                        val colorRes = if (isRedDay) R.color.primary else R.color.text_main
                        setTextColor(resources.getColor(colorRes, null))
                    }

                    setOnClickListener {
                        pendingDate = date
                        populateDialogCalendar()
                    }
                }
                gridCalendarDays.addView(cell)
            }
        }

        populateDialogCalendar()

        btnPrevMonth.setOnClickListener {
            displayYM = displayYM.minusMonths(1)
            populateDialogCalendar()
        }

        btnNextMonth.setOnClickListener {
            displayYM = displayYM.plusMonths(1)
            populateDialogCalendar()
        }

        val alertDialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        activeDialog = alertDialog
        alertDialog.setOnDismissListener {
            activeDialog = null
        }

        btnCancelDialog.setOnClickListener {
            alertDialog.dismiss()
        }

        btnConfirmDialog.setOnClickListener {
            val isWorkDay = rbTypeWorkDay.isChecked
            val calendarHelper = CalendarHelper(requireContext())
            val deviceHolidays = calendarHelper.getCalendarHolidays(pendingDate, pendingDate)

            // 중복 등록 방지 (수정 모드가 아닐 때)
            val existing = holidayManager.getCustomHolidays().find { it.date == pendingDate }
            if (itemToEdit == null && existing != null) {
                android.widget.Toast.makeText(requireContext(), "이미 등록되어 있는 날짜입니다. 기존 항목을 수정/삭제해 주세요.", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 휴무일 추가 시 이미 주말/공휴일인 경우 안내
            if (!isWorkDay && (pendingDate.dayOfWeek == java.time.DayOfWeek.SATURDAY || 
                pendingDate.dayOfWeek == java.time.DayOfWeek.SUNDAY || 
                deviceHolidays.contains(pendingDate))) {
                android.widget.Toast.makeText(requireContext(), "이미 주말 또는 공휴일로 지정되어 있는 날짜입니다.", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val defaultName = if (isWorkDay) "영업일" else "대체공휴일"
            val name = etHolidayName.text.toString().trim().ifEmpty { defaultName }

            if (itemToEdit != null) {
                holidayManager.removeHoliday(itemToEdit.date)
            }
            holidayManager.addHoliday(CustomHoliday(pendingDate, name, isWorkDay))

            setupFilters()
            updateList()
            updateWidgets()

            val msg = if (itemToEdit != null) "수정되었습니다." else if (isWorkDay) "영업일(근무일)로 지정되었습니다." else "휴무일로 등록되었습니다."
            android.widget.Toast.makeText(requireContext(), msg, android.widget.Toast.LENGTH_SHORT).show()

            alertDialog.dismiss()
        }

        alertDialog.show()
    }

    private fun setupFilters() {
        val now = LocalDate.now()
        val currentYear = now.year

        val isOnlyCustom = cbOnlyCustom.isChecked
        val customHolidays = holidayManager.getCustomHolidays()

        val uniqueYears = if (isOnlyCustom) {
            (customHolidays.map { it.date.year } + currentYear).distinct().sorted()
        } else {
            val calendarHelper = CalendarHelper(requireContext())
            val deviceHolidays = calendarHelper.getPublicHolidaysWithNames(
                LocalDate.of(currentYear - 2, 1, 1),
                LocalDate.of(currentYear + 2, 12, 31)
            )
            (customHolidays.map { it.date.year } + deviceHolidays.map { it.date.year } + currentYear).distinct().sorted()
        }

        val years = uniqueYears.map { "${it}년" }
        val yearAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, years).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spFilterYear.adapter = yearAdapter

        val defaultYearIndex = uniqueYears.indexOf(currentYear).coerceAtLeast(0)
        spFilterYear.setSelection(defaultYearIndex, false)

        val months = (1..12).map { "${it}월" }
        val monthAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, months).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spFilterMonth.adapter = monthAdapter

        val defaultMonthIndex = (now.monthValue - 1).coerceIn(0, 11)
        spFilterMonth.setSelection(defaultMonthIndex, false)

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

    private fun isHolidayOrWeekend(date: LocalDate): Boolean {
        val allCustom = holidayManager.getCustomHolidays()
        val forcedWorkDays = allCustom.filter { it.isWorkDay }.map { it.date }.toSet()
        if (forcedWorkDays.contains(date)) {
            return false // 강제 영업일이므로 주말/휴일 아님
        }

        if (date.dayOfWeek == java.time.DayOfWeek.SATURDAY || date.dayOfWeek == java.time.DayOfWeek.SUNDAY) {
            return true
        }
        val customHolidays = allCustom.filter { !it.isWorkDay }.map { it.date }.toSet()
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
        val now = LocalDate.now()
        val currentYear = now.year

        val customItems = holidayManager.getCustomHolidays().map {
            HolidayItem(it.date, it.name, isCustom = true, isWorkDay = it.isWorkDay)
        }
        val customDates = customItems.map { it.date }.toSet()

        val combined = if (isOnlyCustom) {
            customItems.sortedByDescending { it.date }
        } else {
            val deviceItems = calendarHelper.getPublicHolidaysWithNames(
                LocalDate.of(currentYear - 2, 1, 1),
                LocalDate.of(currentYear + 2, 12, 31)
            ).map {
                HolidayItem(it.date, it.name, isCustom = false, isWorkDay = false)
            }.filter { !customDates.contains(it.date) }

            (customItems + deviceItems).sortedByDescending { it.date }
        }

        val yearText = spFilterYear.selectedItem as? String ?: ""
        val selectedYear = yearText.replace("년", "").toIntOrNull() ?: currentYear

        val monthText = spFilterMonth.selectedItem as? String ?: ""
        val selectedMonth = monthText.replace("월", "").toIntOrNull() ?: now.monthValue

        val filteredList = combined.filter { item ->
            item.date.year == selectedYear && item.date.monthValue == selectedMonth
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
                val btnEdit = itemView.findViewById<View>(R.id.btnEdit)
                val btnDelete = itemView.findViewById<View>(R.id.btnDelete)

                val dayOfWeek = days[item.date.dayOfWeek.value - 1]
                val typePrefix = if (item.isWorkDay) "[영업일]" else "[휴무일]"
                tvDate.text = "${item.date.format(formatter)} ($dayOfWeek)"
                tvName.text = "$typePrefix ${item.name}"

                if (item.isCustom) {
                    btnEdit.visibility = View.VISIBLE
                    btnDelete.visibility = View.VISIBLE

                    btnEdit.setOnClickListener {
                        if (isFastDoubleClick()) return@setOnClickListener
                        showAddEditHolidayDialog(item)
                    }

                    btnDelete.setOnClickListener {
                        if (isFastDoubleClick()) return@setOnClickListener
                        showDeleteConfirmDialog(item)
                    }
                } else {
                    btnEdit.visibility = View.GONE
                    btnDelete.visibility = View.GONE
                    btnEdit.setOnClickListener(null)
                    btnDelete.setOnClickListener(null)
                }

                llHolidaysContainer.addView(itemView)
            }
        }
    }

    private fun showDeleteConfirmDialog(item: HolidayItem) {
        if (activeDialog?.isShowing == true) return

        val formatter = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일")
        val typeStr = if (item.isWorkDay) "영업일" else "휴무일"
        val message = "${item.date.format(formatter)}\n[${typeStr}] ${item.name}\n\n위 항목을 정말 삭제하시겠습니까?"

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("삭제 확인")
            .setMessage(message)
            .setPositiveButton("삭제") { _, _ ->
                holidayManager.removeHoliday(item.date)
                setupFilters()
                updateList()
                updateWidgets()
                android.widget.Toast.makeText(requireContext(), "삭제되었습니다.", android.widget.Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("취소", null)
            .create()

        activeDialog = dialog
        dialog.setOnDismissListener {
            activeDialog = null
        }
        dialog.show()
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
