package com.spearson.wakeapp.core.presentation

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.spearson.wakeapp.alarm_home.presentation.AlarmHomeRoot
import com.spearson.wakeapp.interval_alarm.presentation.IntervalAlarmRoot
import com.spearson.wakeapp.stopwatch.presentation.StopwatchRoot
import com.spearson.wakeapp.timer.presentation.TimerRoot

@Composable
fun NavigationRoot(
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (currentDestination.isTopLevelDestination()) {
                NavigationBar(
                    containerColor = BottomNavBackground,
                ) {
                    topLevelDestinations.forEach { destination ->
                        val isSelected = currentDestination.isDestinationSelected(destination.route)
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo<Route.Alarms> {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = destination.icon,
                                    contentDescription = destination.label,
                                    modifier = Modifier.size(if (isSelected) 28.dp else 24.dp),
                                )
                            },
                            label = { Text(destination.label) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = Color.Transparent,
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = BottomNavItemColor,
                                unselectedTextColor = BottomNavItemColor,
                            ),
                        )
                    }
                }
            }
        },
    ) { paddingValues ->
        NavHost(
            modifier = Modifier.padding(paddingValues),
            navController = navController,
            startDestination = Route.Alarms,
        ) {
            composable<Route.Alarms> {
                AlarmHomeRoot(
                    onOpenAlarmEditor = { planId ->
                        navController.navigate(Route.CreateAlarm(planId = planId))
                    },
                )
            }
            composable<Route.CreateAlarm> { backStackEntry ->
                val route = backStackEntry.toRoute<Route.CreateAlarm>()
                IntervalAlarmRoot(
                    planId = route.planId,
                    onClose = {
                        navController.navigateUp()
                    },
                )
            }
            composable<Route.Timer> {
                TimerRoot()
            }
            composable<Route.Stopwatch> {
                StopwatchRoot()
            }
        }
    }
}

private data class TopLevelDestination(
    val route: Route,
    val label: String,
    val icon: ImageVector,
)

private val topLevelDestinations = listOf(
    TopLevelDestination(
        route = Route.Alarms,
        label = "Alarms",
        icon = Icons.Default.Alarm,
    ),
    TopLevelDestination(
        route = Route.Timer,
        label = "Timer",
        icon = Icons.Default.Timer,
    ),
    TopLevelDestination(
        route = Route.Stopwatch,
        label = "Stopwatch",
        icon = Icons.Default.AccessTime,
    ),
)

private val BottomNavBackground = Color(0xFF0A0A0A)
private val BottomNavItemColor = Color(0xFFB9C5D9)

private fun NavDestination?.isTopLevelDestination(): Boolean {
    if (this == null) return false
    return hierarchy.any { destination ->
        destination.hasRoute<Route.Alarms>() ||
            destination.hasRoute<Route.Timer>() ||
            destination.hasRoute<Route.Stopwatch>()
    }
}

private fun NavDestination?.isDestinationSelected(route: Route): Boolean {
    if (this == null) return false
    return when (route) {
        Route.Alarms -> hierarchy.any { it.hasRoute<Route.Alarms>() }
        is Route.CreateAlarm -> hierarchy.any { it.hasRoute<Route.CreateAlarm>() }
        Route.Timer -> hierarchy.any { it.hasRoute<Route.Timer>() }
        Route.Stopwatch -> hierarchy.any { it.hasRoute<Route.Stopwatch>() }
    }
}
