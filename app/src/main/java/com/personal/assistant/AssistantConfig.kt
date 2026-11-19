package com.personal.assistant

import android.content.Context
import org.json.JSONArray
import java.time.DayOfWeek

data class ClassSlot(
    val day: DayOfWeek,
    val startHour: Int,
    val startMinute: Int,
    val subject: String,
    val room: String = ""
)

class AssistantConfig(context: Context) {
    private val prefs = context.getSharedPreferences("assistant_prefs", Context.MODE_PRIVATE)

    var wakeWord: String
        get() = prefs.getString("wake_word", "sara")?.lowercase()?.trim().orEmpty().ifEmpty { "sara" }
        set(value) = prefs.edit().putString("wake_word", value.lowercase().trim()).apply()

    var favFood: String
        get() = prefs.getString("fav_food", "Biryani") ?: "Biryani"
        set(value) = prefs.edit().putString("fav_food", value.trim()).apply()

    var favLine: String
        get() = prefs.getString("fav_line", "today is my day.")
            ?: "today is my day."
        set(value) = prefs.edit().putString("fav_line", value.trim()).apply()

    var latitude: Double
        get() = prefs.getFloat("lat", 13.0827f).toDouble()
        set(value) = prefs.edit().putFloat("lat", value.toFloat()).apply()

    var longitude: Double
        get() = prefs.getFloat("lon", 80.2707f).toDouble()
        set(value) = prefs.edit().putFloat("lon", value.toFloat()).apply()

    fun getTodoList(): MutableList<String> {
        val raw = prefs.getString("todo_list", """["Review class notes","Drink enough water"]""") ?: "[]"
        val arr = JSONArray(raw)
        val list = mutableListOf<String>()
        for (i in 0 until arr.length()) {
            val item = arr.optString(i).trim()
            if (item.isNotEmpty()) list.add(item)
        }
        return list
    }

    fun saveTodoList(items: List<String>) {
        val arr = JSONArray()
        items.map { it.trim() }.filter { it.isNotEmpty() }.forEach { arr.put(it) }
        prefs.edit().putString("todo_list", arr.toString()).apply()
    }

    fun addTodoItem(item: String) {
        val list = getTodoList()
        list.add(item.trim())
        saveTodoList(list)
    }

    /**
     * Edit your pre-given weekly class schedule here.
     */
    fun getWeeklySchedule(): List<ClassSlot> = listOf(
        ClassSlot(DayOfWeek.MONDAY, 9, 0, "Mathematics", "Room 101"),
        ClassSlot(DayOfWeek.MONDAY, 11, 0, "Computer Science", "Lab 2"),
        ClassSlot(DayOfWeek.TUESDAY, 9, 30, "Physics", "Room 204"),
        ClassSlot(DayOfWeek.TUESDAY, 14, 0, "English", "Room 105"),
        ClassSlot(DayOfWeek.WEDNESDAY, 10, 0, "Data Structures", "Lab 1"),
        ClassSlot(DayOfWeek.THURSDAY, 9, 0, "Operating Systems", "Room 302"),
        ClassSlot(DayOfWeek.FRIDAY, 10, 30, "Database Systems", "Room 201"),
        ClassSlot(DayOfWeek.SATURDAY, 9, 30, "Project Seminar", "Auditorium")
    )
}
