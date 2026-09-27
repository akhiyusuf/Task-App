package com.example.data.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.PriorityHigh
import com.example.ui.theme.PriorityLow
import com.example.ui.theme.PriorityMedium
import com.example.ui.theme.PriorityUrgent
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class Priority(val label: String, val level: Int) {
    LOW("Low", 1),
    MEDIUM("Medium", 2),
    HIGH("High", 3),
    URGENT("Urgent", 4);

    fun getColor(): Color {
        return when (this) {
            LOW -> PriorityLow
            MEDIUM -> PriorityMedium
            HIGH -> PriorityHigh
            URGENT -> PriorityUrgent
        }
    }

    companion object {
        fun fromString(value: String): Priority {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: MEDIUM
        }
    }
}

data class Subtask(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val isCompleted: Boolean = false
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("title", title)
            put("isCompleted", isCompleted)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): Subtask {
            return Subtask(
                id = json.optString("id", UUID.randomUUID().toString()),
                title = json.optString("title", ""),
                isCompleted = json.optBoolean("isCompleted", false)
            )
        }

        fun listToJsonString(list: List<Subtask>): String {
            val array = JSONArray()
            list.forEach { array.put(it.toJson()) }
            return array.toString()
        }

        fun listFromJsonString(jsonString: String): List<Subtask> {
            if (jsonString.isBlank()) return emptyList()
            return try {
                val array = JSONArray(jsonString)
                val list = mutableListOf<Subtask>()
                for (i in 0 until array.length()) {
                    list.add(fromJson(array.getJSONObject(i)))
                }
                list
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}

data class TaskCategory(
    val id: String,
    val name: String,
    val colorHex: String,
    val iconName: String
) {
    companion object {
        val ALL = listOf(
            TaskCategory("work", "Work", "#6366F1", "work"),
            TaskCategory("personal", "Personal", "#EC4899", "person"),
            TaskCategory("health", "Health", "#10B981", "fitness"),
            TaskCategory("study", "Study", "#F59E0B", "school"),
            TaskCategory("shopping", "Shopping", "#06B6D4", "shopping_cart"),
            TaskCategory("finance", "Finance", "#8B5CF6", "account_balance"),
            TaskCategory("general", "General", "#64748B", "label")
        )

        fun find(name: String): TaskCategory {
            return ALL.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: ALL.last()
        }
    }
}
