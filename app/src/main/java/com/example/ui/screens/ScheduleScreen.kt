package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.TaskEntity
import com.example.ui.components.DateUtils
import com.example.ui.components.TaskCard
import com.example.ui.components.TaskEditSheet
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.RoseDanger
import com.example.ui.theme.TealAccent
import com.example.ui.viewmodel.TaskViewModel
import java.util.Calendar

data class ScheduleGroup(
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val tasks: List<TaskEntity>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    viewModel: TaskViewModel,
    modifier: Modifier = Modifier
) {
    val allTasks by viewModel.allTasks.collectAsStateWithLifecycle()
    var taskToEdit by remember { mutableStateOf<TaskEntity?>(null) }
    var showEditSheet by remember { mutableStateOf(false) }

    // Partition tasks into schedule buckets
    val now = System.currentTimeMillis()
    val startOfToday = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val endOfToday = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }.timeInMillis

    val endOfTomorrow = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, 1)
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
    }.timeInMillis

    val endOfWeek = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, 7)
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
    }.timeInMillis

    val pendingTasks = allTasks.filter { !it.isCompleted }

    val overdueTasks = pendingTasks.filter {
        it.dueDateEpochMs != null && it.dueDateEpochMs < startOfToday
    }

    val todayTasks = pendingTasks.filter {
        it.dueDateEpochMs != null && it.dueDateEpochMs in startOfToday..endOfToday
    }

    val tomorrowTasks = pendingTasks.filter {
        it.dueDateEpochMs != null && it.dueDateEpochMs in (endOfToday + 1)..endOfTomorrow
    }

    val thisWeekTasks = pendingTasks.filter {
        it.dueDateEpochMs != null && it.dueDateEpochMs in (endOfTomorrow + 1)..endOfWeek
    }

    val laterTasks = pendingTasks.filter {
        it.dueDateEpochMs == null || it.dueDateEpochMs > endOfWeek
    }

    val completedTasks = allTasks.filter { it.isCompleted }

    val groups = listOf(
        ScheduleGroup("Overdue", Icons.Default.WarningAmber, RoseDanger, overdueTasks),
        ScheduleGroup("Today", Icons.Default.CalendarToday, IndigoPrimary, todayTasks),
        ScheduleGroup("Tomorrow", Icons.Default.CalendarMonth, TealAccent, tomorrowTasks),
        ScheduleGroup("Next 7 Days", Icons.Default.DateRange, MaterialTheme.colorScheme.secondary, thisWeekTasks),
        ScheduleGroup("Later & Unscheduled", Icons.Default.CalendarMonth, MaterialTheme.colorScheme.onSurfaceVariant, laterTasks),
        ScheduleGroup("Completed", Icons.Default.CheckCircle, EmeraldSuccess, completedTasks)
    ).filter { it.tasks.isNotEmpty() }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Schedule Timeline",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Tasks organized by due date",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            groups.forEach { group ->
                item(key = "header_${group.title}") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(group.color.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = group.icon,
                                contentDescription = null,
                                tint = group.color,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = group.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = group.tasks.size.toString(),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                items(
                    items = group.tasks,
                    key = { it.id }
                ) { task ->
                    TaskCard(
                        task = task,
                        onToggleCompletion = { viewModel.toggleTaskCompletion(it) },
                        onTogglePin = { viewModel.togglePin(it) },
                        onToggleSubtask = { t, subId -> viewModel.toggleSubtask(t, subId) },
                        onEdit = {
                            taskToEdit = it
                            showEditSheet = true
                        },
                        onDelete = { viewModel.deleteTask(it) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showEditSheet) {
        TaskEditSheet(
            taskToEdit = taskToEdit,
            onDismiss = { showEditSheet = false },
            onSave = { id, title, desc, cat, prio, due, est, subs, pinned ->
                viewModel.saveTask(id, title, desc, cat, prio, due, est, subs, pinned)
            }
        )
    }
}
