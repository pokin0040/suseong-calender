package com.example.businessdaycalc

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class HolidayFragment : Fragment() {

    private lateinit var holidayManager: HolidayManager
    private lateinit var rvHolidays: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: HolidayAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_holiday, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        holidayManager = HolidayManager(requireContext())
        rvHolidays = view.findViewById(R.id.rvHolidays)
        tvEmpty = view.findViewById(R.id.tvEmpty)

        adapter = HolidayAdapter(
            onDelete = { date ->
                holidayManager.removeHoliday(date)
                updateList()
                updateWidgets()
            }
        )

        rvHolidays.layoutManager = LinearLayoutManager(requireContext())
        rvHolidays.adapter = adapter

        view.findViewById<Button>(R.id.btnAddHoliday).setOnClickListener {
            showDatePicker()
        }

        updateList()
    }

    private fun showDatePicker() {
        val today = LocalDate.now()
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                showNameInputDialog(selectedDate)
            },
            today.year,
            today.monthValue - 1,
            today.dayOfMonth
        ).show()
    }

    private fun showNameInputDialog(date: LocalDate) {
        val input = EditText(requireContext())
        input.hint = "예: 창립기념일"
        
        AlertDialog.Builder(requireContext())
            .setTitle("휴무일 이름 입력")
            .setView(input)
            .setPositiveButton("추가") { _, _ ->
                val name = input.text.toString().trim().ifEmpty { "사용자 지정 휴무일" }
                holidayManager.addHoliday(CustomHoliday(date, name))
                updateList()
                updateWidgets()
            }
            .setNegativeButton("취소", null)
            .show()
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
        // BroadCast to update ALL widgets (large, medium, small)
        // For simplicity, we just send a generic broadcast that our receivers will catch
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
