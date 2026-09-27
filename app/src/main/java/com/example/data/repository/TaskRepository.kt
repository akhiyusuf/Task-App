package com.example.data.repository

import com.example.data.local.TaskDao
import com.example.data.local.TaskEntity
import com.example.data.model.Priority
import com.example.data.model.Subtask
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class TaskRepository(private val taskDao: TaskDao) {

    val allTasks: Flow<List<TaskEntity>> = taskDao.getAllTasks()

    fun getTaskById(id: Long): Flow<TaskEntity?> = taskDao.getTaskById(id)

    suspend fun insertTask(task: TaskEntity): Long = taskDao.insertTask(task)

    suspend fun updateTask(task: TaskEntity) = taskDao.updateTask(task)

    suspend fun deleteTask(task: TaskEntity) = taskDao.deleteTask(task)

    suspend fun deleteTaskById(id: Long) = taskDao.deleteTaskById(id)

    suspend fun clearCompletedTasks() = taskDao.clearCompletedTasks()

    suspend fun seedInitialDataIfEmpty() {
        if (taskDao.getTaskCount() == 0) {
            val now = System.currentTimeMillis()
            val cal = Calendar.getInstance()

            cal.timeInMillis = now
            cal.add(Calendar.HOUR_OF_DAY, 4)
            val todayDue = cal.timeInMillis

            cal.timeInMillis = now
            cal.add(Calendar.DAY_OF_YEAR, 1)
            cal.set(Calendar.HOUR_OF_DAY, 18)
            val tomorrowDue = cal.timeInMillis

            cal.timeInMillis = now
            cal.add(Calendar.DAY_OF_YEAR, 3)
            val nextWeekDue = cal.timeInMillis

            val samples = listOf(
                TaskEntity(
                    title = "Complete Mobile App Architecture",
                    description = "Refine Jetpack Compose components and verify offline persistence.",
                    category = "Work",
                    priority = Priority.URGENT.name,
                    dueDateEpochMs = todayDue,
                    isCompleted = false,
                    isPinned = true,
                    estimatedMinutes = 45,
                    subtasksJson = Subtask.listToJsonString(
                        listOf(
                            Subtask(title = "Design clean M3 UI", isCompleted = true),
                            Subtask(title = "Configure Room local DB", isCompleted = true),
                            Subtask(title = "Add progress metrics & filters", isCompleted = false)
                        )
                    )
                ),
                TaskEntity(
                    title = "Evening 5K Run & Stretching",
                    description = "Keep up with the cardio routine for the marathon prep.",
                    category = "Health",
                    priority = Priority.HIGH.name,
                    dueDateEpochMs = todayDue,
                    isCompleted = false,
                    isPinned = false,
                    estimatedMinutes = 35,
                    subtasksJson = Subtask.listToJsonString(
                        listOf(
                            Subtask(title = "Hydrate before run", isCompleted = true),
                            Subtask(title = "5km jog in park", isCompleted = false),
                            Subtask(title = "Cool down & hamstrings stretch", isCompleted = false)
                        )
                    )
                ),
                TaskEntity(
                    title = "Review Weekly Budget & Investments",
                    description = "Update expense tracking and review portfolio allocation.",
                    category = "Finance",
                    priority = Priority.MEDIUM.name,
                    dueDateEpochMs = tomorrowDue,
                    isCompleted = false,
                    isPinned = false,
                    estimatedMinutes = 20,
                    subtasksJson = Subtask.listToJsonString(
                        listOf(
                            Subtask(title = "Check banking statement", isCompleted = false),
                            Subtask(title = "Allocate savings", isCompleted = false)
                        )
                    )
                ),
                TaskEntity(
                    title = "Grocery Restock for Healthy Meals",
                    description = "Pick up fresh vegetables, berries, oat milk, and Greek yogurt.",
                    category = "Shopping",
                    priority = Priority.LOW.name,
                    dueDateEpochMs = tomorrowDue,
                    isCompleted = false,
                    isPinned = false,
                    estimatedMinutes = 30,
                    subtasksJson = Subtask.listToJsonString(
                        listOf(
                            Subtask(title = "Spinach & Avocados", isCompleted = false),
                            Subtask(title = "Almonds & Chia seeds", isCompleted = false)
                        )
                    )
                ),
                TaskEntity(
                    title = "Read 2 Chapters of Clean Code",
                    description = "Chapter 4 on Comments and Chapter 5 on Formatting.",
                    category = "Study",
                    priority = Priority.MEDIUM.name,
                    dueDateEpochMs = nextWeekDue,
                    isCompleted = true,
                    completedAtEpochMs = now - 3600000,
                    isPinned = false,
                    estimatedMinutes = 40,
                    subtasksJson = Subtask.listToJsonString(
                        listOf(
                            Subtask(title = "Read chapter 4", isCompleted = true),
                            Subtask(title = "Take notes", isCompleted = true)
                        )
                    )
                )
            )
            taskDao.insertTasks(samples)
        }
    }
}
