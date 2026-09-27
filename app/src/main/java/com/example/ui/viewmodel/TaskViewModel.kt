package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.TaskEntity
import com.example.data.model.Priority
import com.example.data.model.Subtask
import com.example.data.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

enum class FilterStatus(val label: String) {
    ALL("All"),
    TODAY("Today"),
    UPCOMING("Upcoming"),
    HIGH_PRIORITY("High Priority"),
    COMPLETED("Completed")
}

enum class SortOption(val label: String) {
    DUE_DATE("Due Date"),
    PRIORITY("Priority"),
    DATE_CREATED("Recently Created"),
    ALPHABETICAL("A-Z")
}

data class TaskStats(
    val totalCount: Int = 0,
    val completedCount: Int = 0,
    val pendingCount: Int = 0,
    val completionPercentage: Float = 0f,
    val urgentCount: Int = 0,
    val todayDueCount: Int = 0,
    val categoryCounts: Map<String, Int> = emptyMap()
)

class TaskViewModel(
    application: Application,
    private val repository: TaskRepository
) : AndroidViewModel(application) {

    val searchQuery = MutableStateFlow("")
    val filterStatus = MutableStateFlow(FilterStatus.ALL)
    val selectedCategory = MutableStateFlow<String?>(null)
    val sortOption = MutableStateFlow(SortOption.DUE_DATE)

    val allTasks: StateFlow<List<TaskEntity>> = repository.allTasks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val filteredTasks: StateFlow<List<TaskEntity>> = combine(
        allTasks,
        searchQuery,
        filterStatus,
        selectedCategory,
        sortOption
    ) { tasks, query, filter, category, sort ->
        var list = tasks

        // Search query filter
        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            list = list.filter { task ->
                task.title.lowercase().contains(q) ||
                task.description.lowercase().contains(q) ||
                task.category.lowercase().contains(q) ||
                task.getSubtasks().any { it.title.lowercase().contains(q) }
            }
        }

        // Category filter
        if (category != null) {
            list = list.filter { it.category.equals(category, ignoreCase = true) }
        }

        // Status filter
        val startOfToday = getStartOfDayMillis()
        val endOfToday = getEndOfDayMillis()

        list = when (filter) {
            FilterStatus.ALL -> list
            FilterStatus.TODAY -> list.filter { task ->
                val due = task.dueDateEpochMs
                due != null && due in startOfToday..endOfToday
            }
            FilterStatus.UPCOMING -> list.filter { task ->
                val due = task.dueDateEpochMs
                due != null && due > endOfToday && !task.isCompleted
            }
            FilterStatus.HIGH_PRIORITY -> list.filter {
                val p = it.getPriorityEnum()
                (p == Priority.HIGH || p == Priority.URGENT) && !it.isCompleted
            }
            FilterStatus.COMPLETED -> list.filter { it.isCompleted }
        }

        // Sort
        when (sort) {
            SortOption.DUE_DATE -> list.sortedWith(
                compareByDescending<TaskEntity> { it.isPinned }
                    .thenBy { it.isCompleted }
                    .thenBy { it.dueDateEpochMs ?: Long.MAX_VALUE }
            )
            SortOption.PRIORITY -> list.sortedWith(
                compareByDescending<TaskEntity> { it.isPinned }
                    .thenBy { it.isCompleted }
                    .thenByDescending { it.getPriorityEnum().level }
            )
            SortOption.DATE_CREATED -> list.sortedWith(
                compareByDescending<TaskEntity> { it.isPinned }
                    .thenBy { it.isCompleted }
                    .thenByDescending { it.createdAtEpochMs }
            )
            SortOption.ALPHABETICAL -> list.sortedWith(
                compareByDescending<TaskEntity> { it.isPinned }
                    .thenBy { it.isCompleted }
                    .thenBy { it.title.lowercase() }
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val stats: StateFlow<TaskStats> = allTasks.combine(MutableStateFlow(Unit)) { tasks, _ ->
        val total = tasks.size
        val completed = tasks.count { it.isCompleted }
        val pending = total - completed
        val percentage = if (total > 0) completed.toFloat() / total else 0f

        val urgent = tasks.count {
            !it.isCompleted && (it.getPriorityEnum() == Priority.URGENT || it.getPriorityEnum() == Priority.HIGH)
        }

        val startOfToday = getStartOfDayMillis()
        val endOfToday = getEndOfDayMillis()
        val todayDue = tasks.count {
            !it.isCompleted && it.dueDateEpochMs != null && it.dueDateEpochMs in startOfToday..endOfToday
        }

        val categoryMap = tasks.groupBy { it.category }
            .mapValues { it.value.size }

        TaskStats(
            totalCount = total,
            completedCount = completed,
            pendingCount = pending,
            completionPercentage = percentage,
            urgentCount = urgent,
            todayDueCount = todayDue,
            categoryCounts = categoryMap
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TaskStats()
    )

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
        }
    }

    fun toggleTaskCompletion(task: TaskEntity) {
        viewModelScope.launch {
            val updated = task.copy(
                isCompleted = !task.isCompleted,
                completedAtEpochMs = if (!task.isCompleted) System.currentTimeMillis() else null
            )
            repository.updateTask(updated)
        }
    }

    fun togglePin(task: TaskEntity) {
        viewModelScope.launch {
            repository.updateTask(task.copy(isPinned = !task.isPinned))
        }
    }

    fun toggleSubtask(task: TaskEntity, subtaskId: String) {
        viewModelScope.launch {
            val currentSubtasks = task.getSubtasks().map { sub ->
                if (sub.id == subtaskId) sub.copy(isCompleted = !sub.isCompleted) else sub
            }
            val allSubtasksDone = currentSubtasks.isNotEmpty() && currentSubtasks.all { it.isCompleted }
            val updated = task.copy(
                subtasksJson = Subtask.listToJsonString(currentSubtasks),
                isCompleted = if (allSubtasksDone) true else task.isCompleted,
                completedAtEpochMs = if (allSubtasksDone && !task.isCompleted) System.currentTimeMillis() else task.completedAtEpochMs
            )
            repository.updateTask(updated)
        }
    }

    fun saveTask(
        id: Long = 0,
        title: String,
        description: String,
        category: String,
        priority: Priority,
        dueDateMs: Long?,
        estimatedMinutes: Int?,
        subtasks: List<Subtask>,
        isPinned: Boolean = false
    ) {
        viewModelScope.launch {
            val jsonSubtasks = Subtask.listToJsonString(subtasks)
            val allSubtasksDone = subtasks.isNotEmpty() && subtasks.all { it.isCompleted }
            if (id == 0L) {
                val newTask = TaskEntity(
                    title = title.trim(),
                    description = description.trim(),
                    category = category,
                    priority = priority.name,
                    dueDateEpochMs = dueDateMs,
                    estimatedMinutes = estimatedMinutes,
                    subtasksJson = jsonSubtasks,
                    isPinned = isPinned,
                    isCompleted = allSubtasksDone
                )
                repository.insertTask(newTask)
            } else {
                val existing = allTasks.value.firstOrNull { it.id == id }
                val updated = TaskEntity(
                    id = id,
                    title = title.trim(),
                    description = description.trim(),
                    category = category,
                    priority = priority.name,
                    dueDateEpochMs = dueDateMs,
                    estimatedMinutes = estimatedMinutes,
                    subtasksJson = jsonSubtasks,
                    isPinned = isPinned,
                    isCompleted = existing?.isCompleted ?: allSubtasksDone,
                    completedAtEpochMs = existing?.completedAtEpochMs,
                    createdAtEpochMs = existing?.createdAtEpochMs ?: System.currentTimeMillis()
                )
                repository.updateTask(updated)
            }
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    fun clearCompletedTasks() {
        viewModelScope.launch {
            repository.clearCompletedTasks()
        }
    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun setFilterStatus(status: FilterStatus) {
        filterStatus.value = status
    }

    fun setSelectedCategory(category: String?) {
        selectedCategory.value = category
    }

    fun setSortOption(sort: SortOption) {
        sortOption.value = sort
    }

    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val db = AppDatabase.getDatabase(application)
                    val repo = TaskRepository(db.taskDao())
                    return TaskViewModel(application, repo) as T
                }
            }
        }

        private fun getStartOfDayMillis(): Long {
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

        private fun getEndOfDayMillis(): Long {
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            return cal.timeInMillis
        }
    }
}
