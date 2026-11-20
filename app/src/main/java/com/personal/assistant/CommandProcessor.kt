package com.personal.assistant

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class CommandProcessor(private val context: Context) {
    private val config = AssistantConfig(context)

    fun process(rawQuery: String): String {
        val query = rawQuery.lowercase().trim()
        if (query.isEmpty()) return "I did not catch that. Please try again."

        return when {
            query.contains("next class") || query.contains("schedule") -> getNextClass()
            query.startsWith("add task ") || query.startsWith("add to do ") || query.startsWith("add todo ") -> {
                val task = query
                    .removePrefix("add task ")
                    .removePrefix("add to do ")
                    .removePrefix("add todo ")
                    .trim()
                if (task.isEmpty()) {
                    "Please say the task you want to add."
                } else {
                    config.addTodoItem(task)
                    "Added $task to your to-do list."
                }
            }
            query.contains("todo") || query.contains("to-do") || query.contains("to do") || query.contains("tasks") -> {
                getTodoSummary()
            }
            query.contains("fav food") || query.contains("favorite food") || query.contains("favourite food") || query.contains("food") -> {
                "Your favorite food is ${config.favFood}."
            }
            query.contains("fav line") || query.contains("favorite line") || query.contains("favourite line") || query.contains("quote") -> {
                config.favLine
            }
            query.contains("weather") -> getWeather()
            query.startsWith("play ") -> playOnSpotify(query.removePrefix("play ").replace(" on spotify", "").trim())
            query.contains("time") -> {
                val now = LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a"))
                "The current time is $now."
            }
            else -> "I heard: $rawQuery. Try asking about your next class, to-do list, weather, favorite food, or favorite line."
        }
    }

    fun getNextClass(): String {
        val now = LocalDateTime.now()
        val todaySlots = config.getWeeklySchedule()
            .filter { it.day == now.dayOfWeek }
            .sortedBy { it.startHour * 60 + it.startMinute }

        val currentMinutes = now.hour * 60 + now.minute
        val next = todaySlots.firstOrNull { (it.startHour * 60 + it.startMinute) > currentMinutes }
            ?: return "You have no more classes scheduled for today."

        val timeText = LocalTime.of(next.startHour, next.startMinute)
            .format(DateTimeFormatter.ofPattern("hh:mm a"))
        val roomPart = if (next.room.isNotEmpty()) " in ${next.room}" else ""
        return "Your next class is ${next.subject} at $timeText$roomPart."
    }

    private fun getTodoSummary(): String {
        val list = config.getTodoList()
        if (list.isEmpty()) return "Your to-do list for today is empty."
        val joined = list.mapIndexed { idx, item -> "${idx + 1}. $item" }.joinToString(", ")
        return "You have ${list.size} tasks today: $joined."
    }

    private fun getWeather(): String {
        return try {
            val endpoint = "https://api.open-meteo.com/v1/forecast?latitude=${config.latitude}&longitude=${config.longitude}&current_weather=true"
            val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                connectTimeout = 4000
                readTimeout = 4000
            }
            val body = conn.inputStream.bufferedReader().readText()
            val current = JSONObject(body).getJSONObject("current_weather")
            val temp = current.getDouble("temperature")
            val wind = current.getDouble("windspeed")
            "It is currently $temp degrees Celsius with wind at $wind kilometers per hour."
        } catch (_: Exception) {
            "Weather needs an internet connection, and you appear to be offline."
        }
    }

    private fun playOnSpotify(song: String): String {
        if (song.isEmpty()) return "Please say the name of the song to play."
        return try {
            val intent = Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH).apply {
                setPackage("com.spotify.music")
                putExtra(MediaStore.EXTRA_MEDIA_FOCUS, "vnd.android.cursor.item/*")
                putExtra(SearchManager.QUERY, song)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "Playing $song on Spotify."
        } catch (_: Exception) {
            "Could not open Spotify. Please check if Spotify is installed."
        }
    }
}
