package com.example.businessdaycalc

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

data class CustomHoliday(val date: LocalDate, val name: String)

class HolidayManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("holidays_prefs", Context.MODE_PRIVATE)
    private val KEY_HOLIDAYS = "custom_holidays_json"

    fun getCustomHolidays(): List<CustomHoliday> {
        val jsonStr = prefs.getString(KEY_HOLIDAYS, "[]") ?: "[]"
        val list = mutableListOf<CustomHoliday>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val dateStr = obj.getString("date")
                val name = obj.getString("name")
                list.add(CustomHoliday(LocalDate.parse(dateStr), name))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list.sortedBy { it.date }
    }

    fun addHoliday(holiday: CustomHoliday) {
        val current = getCustomHolidays().filter { it.date != holiday.date }.toMutableList()
        current.add(holiday)
        saveHolidays(current)
    }

    fun removeHoliday(date: LocalDate) {
        val current = getCustomHolidays().filter { it.date != date }.toMutableList()
        saveHolidays(current)
    }

    private fun saveHolidays(list: List<CustomHoliday>) {
        val array = JSONArray()
        list.forEach {
            val obj = JSONObject()
            obj.put("date", it.date.toString())
            obj.put("name", it.name)
            array.put(obj)
        }
        prefs.edit().putString(KEY_HOLIDAYS, array.toString()).apply()
    }
}
