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
        get() = prefs.getString("wake_word", "nova")?.lowercase()?.trim().orEmpty().ifEmpty { "nova" }
        set(value) = prefs.edit().putString("wake_word", value.lowercase().trim()).apply()

    var favFood: String
        get() = prefs.getString("fav_food", "Biryani") ?: "Biryani"
        set(value) = prefs.edit().putString("fav_food", value.trim()).apply()

    var favLine: String
        get() = prefs.getString("fav_line", "Small steps every day lead to big results.")
            ?: "Small steps every day lead to big results."
        set(value) = prefs.edit().putString("fav_line", value.trim()).apply()

    var latitude: Double
        get() = prefs.getFloat("lat", 13.0827f).toDouble()
        set(value) = prefs.edit().putFloat("lat", value.toFloat()).apply()

    var longitude: Double
        get() = prefs.getFloat("lon", 80.2707f).toDouble()
        set(value) = prefs.edit().putFloat("lon", value.toFloat()).apply()

    fun getTodoList(): MutableList<String> {
        val defaultJson = JSONArray().apply {
            put("Review class notes")
            put("Drink enough water")
        }.toString()
        val raw = prefs.getString("todo_list", defaultJson) ?: defaultJson
        val list = mutableListOf<String>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val item = arr.optString(i).trim()
                if (item.isNotEmpty()) list.add(item)
            }
        } catch (_: Exception) {
            list.add("Review class notes")
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

    fun getScheduleText(): String {
        val defaultText = listOf(
            "MONDAY | 09:00 | Mathematics | Room 101",
            "MONDAY | 11:00 | Computer Science | Lab 2",
            "TUESDAY | 09:30 | Physics | Room 204",
            "TUESDAY | 14:00 | English | Room 105",
            "WEDNESDAY | 10:00 | Data Structures | Lab 1",
            "THURSDAY | 09:00 | Operating Systems | Room 302",
            "FRIDAY | 10:30 | Database Systems | Room 201",
            "SATURDAY | 09:30 | Project Seminar | Auditorium"
        ).joinToString("
")
        return prefs.getString("weekly_schedule_text", defaultText) ?: defaultText
    }

    fun saveScheduleText(text: String) {
        prefs.edit().putString("weekly_schedule_text", text.trim()).apply()
    }

    fun getWeeklySchedule(): List<ClassSlot> {
        val slots = mutableListOf<ClassSlot>()
        for (line in getScheduleText().lines()) {
            val parts = line.split("|").map { it.trim() }
            if (parts.size < 3) continue
            val day = try {
                DayOfWeek.valueOf(parts[0].uppercase())
            } catch (_: Exception) {
                continue
            }
            val timeParts = parts[1].split(":")
            if (timeParts.size != 2) continue
            val hour = timeParts[0].toIntOrNull() ?: continue
            val minute = timeParts[1].toIntOrNull() ?: continue
            val subject = parts[2]
            val room = if (parts.size >= 4) parts[3] else ""
            if (hour in 0..23 && minute in 0..59 && subject.isNotEmpty()) {
                slots.add(ClassSlot(day, hour, minute, subject, room))
            }
        }
        return slots
    }
}
