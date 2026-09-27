package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.Priority
import com.example.data.model.Subtask

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val category: String = "General",
    val priority: String = Priority.MEDIUM.name,
    val dueDateEpochMs: Long? = null,
    val isCompleted: Boolean = false,
    val completedAtEpochMs: Long? = null,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val subtasksJson: String = "",
    val isPinned: Boolean = false,
    val estimatedMinutes: Int? = null
) {
    fun getPriorityEnum(): Priority = Priority.fromString(priority)

    fun getSubtasks(): List<Subtask> = Subtask.listFromJsonString(subtasksJson)

    fun completedSubtasksCount(): Int = getSubtasks().count { it.isCompleted }

    fun totalSubtasksCount(): Int = getSubtasks().size
}
