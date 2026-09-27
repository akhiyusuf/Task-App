package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ScheduleScreen
import com.example.ui.viewmodel.TaskViewModel

enum class AppTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    TASKS("Tasks", Icons.Filled.Checklist, Icons.Outlined.Checklist, "tab_tasks"),
    SCHEDULE("Schedule", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth, "tab_schedule"),
    ANALYTICS("Analytics", Icons.Filled.Insights, Icons.Outlined.Insights, "tab_analytics")
}

@Composable
fun MainApp(
    viewModel: TaskViewModel
) {
    var currentTab by remember { mutableStateOf(AppTab.TASKS) }

    // BackHandler: If user is on Schedule or Analytics, Back button returns to Tasks
    if (currentTab != AppTab.TASKS) {
        BackHandler {
            currentTab = AppTab.TASKS
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("bottom_navigation_bar")
            ) {
                AppTab.entries.forEach { tab ->
                    val selected = currentTab == tab
                    NavigationBarItem(
                        selected = selected,
                        onClick = { currentTab = tab },
                        icon = {
                            Icon(
                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.label
                            )
                        },
                        label = { Text(tab.label) },
                        modifier = Modifier.testTag(tab.testTag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                AppTab.TASKS -> HomeScreen(viewModel = viewModel)
                AppTab.SCHEDULE -> ScheduleScreen(viewModel = viewModel)
                AppTab.ANALYTICS -> AnalyticsScreen(viewModel = viewModel)
            }
        }
    }
}
